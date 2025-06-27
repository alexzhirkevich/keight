package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.callableOrNull
import io.github.alexzhirkevich.keight.callableOrThrow
import io.github.alexzhirkevich.keight.expressions.OpCompare
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.findJsRoot
import io.github.alexzhirkevich.keight.findRoot
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import io.github.alexzhirkevich.keight.thisRef

private val CALLBACK = "callback" defaults OpConstant(Undefined)
private val COUNT = "count" defaults OpConstant(0.js)

internal class JSIteratorFunction : JSFunction(
    name = "Iterator",
    prototype = Object("Iterator Prototype") {
        NEXT.js.func {
            val value = thisRef<Iterator<JsAny?>>()
            if (value.hasNext()) {
                IteratorEntry(value.next())
            } else {
                IteratorDone()
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

            helperIterator {
                iter?.next(this)
            }
        }
        "take".js.func(COUNT) { args ->
            val count = toNumber(args[0]).toInt()
            val iter = thisRef
            helperIterator { i ->
                val obj = iter?.next(this@func)
                if (i >= count) IteratorDone() else obj
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

internal fun IteratorDone() = Object {
    DONE.js eq true.js
}

internal fun IteratorEntry(value : JsAny?) = Object {
    VALUE.js eq value
    DONE.js eq false.js
}

internal suspend fun ScriptRuntime.helperIterator(
    next : suspend ScriptRuntime.(Int) -> JsAny?,
): JsObject {
    var i = -1

    return Object { NEXT.js.func { next(this, ++i) } }.also {
        it.setProto(this, findJsRoot().Iterator.get(PROTOTYPE, this))
    }
}

internal suspend fun JsAny.next(runtime: ScriptRuntime) : JsAny {
    return get(NEXT.js, runtime)
        .callableOrThrow(runtime)
        .call(this, emptyList(), runtime)
        ?: Undefined
}

internal suspend inline fun JsAny.value(runtime: ScriptRuntime) : JsAny? {
    return get(VALUE.js, runtime)
}

private suspend inline fun JsAny.done(runtime: ScriptRuntime) : Boolean {
    return contains(DONE.js, runtime) && !runtime.isFalse(get(DONE.js, runtime))
}

internal suspend fun JsAny.isIterator(runtime: ScriptRuntime) : Boolean {
    return runtime.findJsRoot().Iterator
        .get(PROTOTYPE, runtime)
        ?.isPrototypeOf(this, runtime) == true
}

internal suspend inline fun JsAny.forEach(runtime: ScriptRuntime, block : (JsAny?) -> Unit) {
    when {
        this is Iterator<*> -> forEach { block(it as JsAny?) }
        this is Iterable<*> -> forEach { block(it as JsAny?) }
        isIterator(runtime) || contains(NEXT.js, runtime) -> while (true) {
            val next = next(runtime)
            if (next.done(runtime)) {
                break
            }
            block(next.value(runtime))
        }

        contains(LENGTH.js, runtime) -> {
            for (i in 0 until runtime.toNumber(get(LENGTH.js, runtime)).toInt()) {
                block(get(i.js, runtime))
            }
        }
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
    return runtime.helperIterator {
        val n = next(this)
        val done = n.done(this)
        Object {
            VALUE.js eq if (done) Undefined else block.invoke(
                listOf(n.value(this@helperIterator), (++i).js),
                this@helperIterator
            )
            DONE.js eq done.js
        }
    }
}

private suspend inline fun JsAny.flatMap(runtime: ScriptRuntime, block : Callable) : JsAny {

    var currentItem: JsAny? = Uninitialized
    var isIterating = false

    return runtime.helperIterator { i->
        while (true) {
            if (currentItem is Uninitialized) {
                val next = this@flatMap.next(this)
                if (next.done(this)) {
                    return@helperIterator IteratorDone()
                }
                val v = next.value(this)

                currentItem = block.invoke(listOf(v, i.js), this)

                val iter = currentItem
                    ?.get(JsSymbol.iterator, this)
                    ?.callableOrNull()
                    ?.call(currentItem, emptyList(), this)

                if (iter?.isIterator(this) == true) {
                    currentItem = iter
                }
                isIterating = currentItem?.isIterator(this) == true
            }

            if (isIterating) {
                val next = currentItem!!.next(this)
                if (next.done(this)) {
                    isIterating = false
                    currentItem = Uninitialized
                    continue
                }
                return@helperIterator IteratorEntry(next.value(this@helperIterator))
            } else {
                return@helperIterator IteratorEntry(currentItem).also {
                    currentItem = Uninitialized
                }
            }
        }

        @Suppress("UNREACHABLE_CODE")
        error("unreachable")
    }
}

private suspend inline fun JsAny.filter(runtime: ScriptRuntime, block : Callable) : JsAny {
    var i = -1
    return runtime.helperIterator {
        var res: JsAny? = Undefined
        var done: Boolean
        do {
            val next = next(this)
            val v = next.value(this)
            done = next.done(this)
            if (done) {
                break
            }
            if (!runtime.isFalse(block.invoke(listOf(v, (++i).js), this))) {
                res = v
                break
            }
        } while (!done)

        Object {
            VALUE.js eq res
            DONE.js eq done.js
        }
    }
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
