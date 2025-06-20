package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.callableOrNull
import io.github.alexzhirkevich.keight.callableOrThrow
import io.github.alexzhirkevich.keight.findRoot
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import io.github.alexzhirkevich.keight.thisRef

private const val CALLBACK = "callback"

internal class JSIteratorFunction : JSFunction(
    name = "Iterator",
    prototype = Object {
        NEXT.js.func {
            val value = thisRef<Iterator<JsAny?>>()
            if(value.hasNext()) {
                Object {
                    VALUE.js eq value.next()
                    DONE.js eq false.js
                }
            } else {
                Object {
                    VALUE.js eq Undefined
                    DONE.js eq true.js
                }
            }
        }
        "every".js.func(CALLBACK) { args ->
            thisRef?.every(this, args[0].callableOrThrow(this))?.js
        }
        "some".js.func(CALLBACK) { args ->
            thisRef?.some(this, args[0].callableOrThrow(this))?.js
        }
        "filter".js.func(CALLBACK) { args ->
            thisRef?.filter(this, args[0].callableOrThrow(this))
        }
        "forEach".js.func(CALLBACK) { args ->
            val c = args[0].callableOrThrow(this)
            thisRef?.forEachIndexed(this) { idx, v ->
                c.invoke(listOf(v, idx.js), this)
            }
            Undefined
        }
        "map".js.func(CALLBACK) { args ->
            thisRef?.map(this, args[0].callableOrThrow(this))
        }
        "flatMap".js.func(CALLBACK) { args ->
            thisRef?.flatMap(this, args[0].callableOrThrow(this))
        }
        "find".js.func(CALLBACK) { args ->
            thisRef?.find(this, args[0].callableOrThrow(this))
        }
        "drop".js.func(CALLBACK) { args ->

            val count = toNumber(args[0]).toInt()
            val iter = thisRef

            repeat(count) {
                iter?.next(this)
            }

            Object {
                NEXT.js.func {
                    iter?.next(this)
                }
            }
        }
        "take".js.func(CALLBACK) { args ->
            Object {
                val count = toNumber(args[0]).toInt()
                var i = 0
                val iter = thisRef
                NEXT.js.func iter@ {
                    val obj = iter?.next(this@func)
                    when {
                        i >= count -> Object {
                            VALUE.js eq Undefined
                            DONE.js eq true.js
                        }

                        ++i == count -> Object {
                            VALUE.js eq obj?.value(this@iter)
                            DONE.js eq true.js
                        }

                        else -> obj
                    }
                }
            }
        }
        "toArray".js.func {
            val iter = thisRef<Iterator<JsAny?>>()

            buildList {
                while (iter.hasNext()) {
                    add(iter.next())
                }
            }.js
        }
    }
) {
    override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny {
        runtime.typeError { "Constructor Iterator requires 'new'".js }
    }

    override suspend fun construct(args: List<JsAny?>, runtime: ScriptRuntime): JsAny {
        runtime.typeError { "Abstract class Iterator not directly constructable".js }
    }
}

private suspend fun JsAny.next(runtime: ScriptRuntime) : JsAny {
    return get(NEXT.js, runtime)
        .callableOrThrow(runtime)
        .call(this, emptyList(), runtime)
        ?: Undefined
}

private suspend inline fun JsAny.value(runtime: ScriptRuntime) : JsAny? {
    return get(VALUE.js, runtime)
}

private suspend inline fun JsAny.done(runtime: ScriptRuntime) : Boolean {
    return runtime.isFalse(get(DONE.js, runtime)).not()
}

private suspend fun JsAny.isIterator(runtime: ScriptRuntime) : Boolean {
    return this is JSIteratorWrapper || (runtime.findRoot() as JSRuntime).Iterator
        .get(PROTOTYPE, runtime)
        ?.isPrototypeOf(this, runtime) == true
}

private suspend inline fun JsAny.forEach(runtime: ScriptRuntime, block : (JsAny?) -> Unit) {
    when(this) {
        is Iterator<*> -> forEach { block(it as JsAny?) }
        is Iterable<*> -> forEach { block(it as JsAny?) }
        else -> do {
            val next = next(runtime)
            block(next.value(runtime))
        } while (!next.done(runtime))
    }
}

private suspend inline fun JsAny.forEachIndexed(runtime: ScriptRuntime, block : (Int, JsAny?) -> Unit) {
    var i = -1
    forEach(runtime){
        block(++i, it)
    }
}

private suspend inline fun JsAny.every(runtime: ScriptRuntime, block : Callable) : Boolean {
    forEachIndexed(runtime) { idx, it ->
        if (runtime.isFalse(block.invoke(listOf(it, idx.js), runtime))) {
            return false
        }
    }
    return true
}

private suspend inline fun JsAny.some(runtime: ScriptRuntime, block : Callable) : Boolean {
    forEachIndexed(runtime) { idx, it ->
        if (!runtime.isFalse(block.invoke(listOf(it, idx.js), runtime))) {
            return true
        }
    }
    return false
}

private suspend inline fun JsAny.map(runtime: ScriptRuntime, block : Callable) : JsAny {
    var i = -1
    return Object {
        NEXT.js.func {
            val n = next(runtime)
            val done = n.done(runtime)
            Object {
                VALUE.js eq if (done) Undefined else block.invoke(listOf(n.value(runtime), (++i).js), runtime)
                DONE.js eq done.js
            }
        }
    }.setIteratorProto(runtime)
}

private inline fun JsAny.flatMap(runtime: ScriptRuntime, block : Callable) : JsAny {

    var i = -1
    var currentItem: JsAny? = Uninitialized
    var isIterating = false

    return Object {
        NEXT.js.func {
            while (true) {
                if (currentItem is Uninitialized) {
                    val next = this@flatMap.next(runtime)
                    if (next.done(runtime)) {
                        return@func Object {
                            VALUE.js eq Undefined
                            DONE.js eq true.js
                        }
                    }
                    val v = next.value(runtime)

                    currentItem = block.invoke(listOf(v, (++i).js), runtime)

                    val iter = currentItem
                        ?.get(JSSymbol.iterator, runtime)
                        ?.callableOrNull()
                        ?.call(currentItem, emptyList(), runtime)

                    if (iter?.isIterator(runtime) == true) {
                        currentItem = iter
                    }
                    isIterating = currentItem?.isIterator(runtime) == true
                }

                if (isIterating) {
                    val next = currentItem!!.next(runtime)
                    if (next.done(runtime)) {
                        isIterating = false
                        currentItem = Uninitialized
                        continue
                    }
                    return@func Object {
                        VALUE.js eq next.value(runtime)
                        DONE.js eq false.js
                    }
                } else {
                    return@func Object {
                        VALUE.js eq currentItem
                        DONE.js eq false.js
                    }.also {
                        currentItem = Uninitialized
                    }
                }
            }

            @Suppress("UNREACHABLE_CODE")
            error("unreachable")
        }
    }
}

private suspend inline fun JsAny.filter(runtime: ScriptRuntime, block : Callable) : JsAny {
    var i = -1
    return Object {
        NEXT.js.func {
            var res : JsAny? = Undefined
            var done: Boolean
            do {
                val next = next(runtime)
                val v = next.value(runtime)
                done = next.done(runtime)
                if (done){
                    break
                }
                if (!runtime.isFalse(block.invoke(listOf(v, (++i).js), runtime))) {
                    res = v
                    break
                }
            } while (!done)

            Object {
                VALUE.js eq res
                DONE.js eq done.js
            }
        }
    }.setIteratorProto(runtime)
}

private suspend inline fun JsAny.find(runtime: ScriptRuntime, block : Callable) : JsAny? {
    var i = -1
    forEach(runtime) {
        if (!runtime.isFalse(block.invoke(listOf(it, (++i).js), runtime))) {
            return it
        }
    }
    return Undefined
}


private suspend fun JSObject.setIteratorProto(runtime: ScriptRuntime) = apply {
    setProto(runtime, (runtime.findRoot() as JSRuntime).Iterator)
}