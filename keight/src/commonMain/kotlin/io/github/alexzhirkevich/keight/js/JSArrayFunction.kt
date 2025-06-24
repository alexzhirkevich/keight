package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.callableOrThrow
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.fastForEachIndexed
import io.github.alexzhirkevich.keight.findRoot
import io.github.alexzhirkevich.keight.requireThisRef
import io.github.alexzhirkevich.keight.thisRef
import kotlin.math.abs
import kotlin.math.min

internal class JSArrayFunction : JSFunction(
    name = "Array",
    properties = mutableMapOf(
        "isArray".js to "isArray".func("object") {
            (findRoot() as JSRuntime).Array.isInstance(it.getOrNull(0), this).js
        }
    ),
    prototype = Object {
        "length".js eq JsNumberWrapper(0)
        "indexOf".js.func(
            FunctionParam("search"),
            "position" defaults OpConstant(0.js)
        ) {
            thisRef<List<*>>().indexOf(this, it)
        }
        "lastIndexOf".js.func(
            FunctionParam("search"),
            "position" defaults OpArgOmitted
        ) {
            thisRef<List<*>>().lastIndexOf(this, it)
        }
        "forEach".js.func("callback") { args ->
            val arr = thisRef
            op(args) { callable ->
                thisRef<Iterable<JsAny?>>().forEachIndexed { idx, v ->
                    callable.invoke(listOf(v, idx.js, arr), this)
                }
            }
            Undefined
        }

        "map".js.func("callback") { args ->
            val arr = thisRef
            op(args) { callable ->
                thisRef<Iterable<JsAny?>>()
                    .mapIndexed { idx,v -> callable.invoke(listOf(v, idx.js, arr), this) }
            }.js
        }
        "filter".js.func("callback") { args ->
            val arr = thisRef
            op(args) { callable ->
                thisRef<Iterable<JsAny?>>().filterIndexed { idx, v ->
                    !isFalse(callable.invoke(listOf(v, idx.js, arr), this))
                }
            }.js
        }
        "reduce".js.func("callback") { args ->
            val v = if (args.size > 1) {
                listOf(args[1]) + thisRef<Iterable<JsAny?>>()
            } else {
                thisRef<Iterable<JsAny?>>()
            }
            val arr = thisRef

            op(args) { callable ->
                v.reduceIndexed { idx, acc, any ->
                    callable.invoke(listOf(acc, any, idx.js, arr), this)
                }
            }
        }
        "reduceRight".js.func("callback") { args ->
            val v = if (args.size > 1) {
                thisRef<List<JsAny?>>() + args[1]
            } else {
                thisRef<List<JsAny?>>()
            }
            val arr = thisRef

            op(args) { callable ->
                v.reduceRightIndexed { idx, acc, any ->
                    callable.invoke(listOf(acc, any, idx.js, arr), this)
                }
            }
        }
        "some".js.func("callback") { args ->
            var i = -1
            val arr = thisRef
            op(args) { callable ->
                thisRef<Iterable<JsAny?>>().any {
                    !isFalse(callable.invoke(listOf(it, (++i).js, arr), this))
                }
            }.js
        }
        "every".js.func("callback") { args ->
            var i = -1
            val arr = thisRef
            op(args) { callable ->
                thisRef<Iterable<JsAny?>>().all {
                    !isFalse(callable.invoke(listOf(it, (++i).js, arr), this))
                }
            }.js
        }
        "reverse".js.func { thisRef<MutableList<*>>().reverse(); Undefined }
        "toReversed".js.func { thisRef<Iterable<JsAny?>>().reversed().js }

        "sort".js.func(
            "comparator" defaults OpArgOmitted
        ) {
            val list = thisRef<MutableList<JsAny?>>()
            val res = list.sorted(it.argOrNull(0), this)
            res.fastForEachIndexed { i, v -> list[i] = v }
            Undefined
        }
        "toSorted".js.func(
            "comparator" defaults OpArgOmitted
        ) {
            thisRef<List<JsAny?>>()
                .sorted(it.argOrNull(0), this).js
        }
        "slice".js.func(
            "start" defaults OpConstant(0.js),
            "end" defaults OpArgOmitted
        ) { args ->
            if (args.isEmpty()) {
                thisRef()
            } else {
                val start = toNumber(args[0]).toInt()
                val list = thisRef<List<JsAny?>>()
                val end = toNumber(args.argOrElse(1) { list.size.js })
                    .toInt()
                    .coerceIn(0, list.size)
                list.slice(start until end).js
            }
        }
        "splice".js.func(
            "value" defaults OpConstant(mutableListOf<JsAny?>().js),
            "deleteCount" defaults OpArgOmitted,
            "items".vararg()
        ) { args ->
            val value = thisRef<MutableList<JsAny?>>()

            val pos = toNumber(args[0]).toInt()
            val remove = toNumber(args.argOrElse(1) { (value.size - pos).js }).toInt()
            val rest = args[2] as Collection<JsAny?>

            val deleted = buildList {
                repeat(remove) {
                    add(value.removeAt(pos))
                }
            }

            value.addAll(pos, rest)
            deleted.js
        }
        "toSpliced".js.func(
            "value" defaults OpConstant(mutableListOf<JsAny?>().js),
            "deleteCount" defaults OpArgOmitted,
            "items".vararg()
        ) { args ->

            val value = thisRef<List<JsAny?>>().toMutableList()
            val pos = toNumber(args[0]).toInt()
            val remove = toNumber(args.argOrElse(1) { (value.size - pos).js }).toInt()
            val rest = args[2] as List<JsAny?>

            repeat(remove) {
                value.removeAt(pos)
            }

            value.addAll(pos, rest)
            value.js
        }
        "at".js.func("idx") {
            requireThisRef("Array.prototype.at").get(it[0], this)
        }
        "includes".js.func("item") {
            val v = it[0]
            val fromIndex = it.getOrNull(1)?.let { toNumber(it) }?.toInt() ?: 0
            (thisRef<Iterable<*>>().indexOf(v) > fromIndex).js
        }
        "join".js.func("separator") {
            val sep = toString(it.getOrElse(0) { ",".js })
            thisRef<Iterable<JsAny?>>().joinToString(separator = sep).js
        }
        "push".js.func("items".vararg()) {
            val list = thisRef<MutableList<JsAny?>>()
            val size = list.size
            thisRef<MutableCollection<JsAny?>>().addAll(it[0] as Iterable<JsAny?>)
            (list.size - size).js
        }
        "pop".js.func {
            val value = thisRef<MutableList<JsAny?>>()
            if (value.isEmpty()) Undefined else value.removeLast()
        }
        "shift".js.func {
            val value = thisRef<MutableList<JsAny?>>()
            if (value.isEmpty()) Undefined else value.removeLast()
        }
        "unshift".js.func("items".vararg()) {
            val value = thisRef<MutableList<JsAny?>>()
            val items = it[0] as Collection<JsAny?>
            value.addAll(0, items)
            items.size.js
        }
        "concat".js.func("items".vararg()) {
            buildList {
                addAll(thisRef<Iterable<JsAny?>>())
                (it[0] as Iterable<JsAny?>).forEach {
                    when (it) {
                        is Iterable<*> -> addAll(it as Iterable<JsAny?>)
                        else -> add(it)
                    }
                }
            }.js
        }
        "copyWithin".js.func(
            FunctionParam("to"),
            FunctionParam("from"),
            "num" defaults OpArgOmitted
        ) {
            val to = toNumber(it[0]).toInt()
            val from = toNumber(it[1]).toInt()
            val value = thisRef<MutableList<JsAny?>>()
            val num = it.argOrNull(2)?.let { toNumber(it) }?.toInt() ?: value.size

            for (i in 0 until num.coerceAtMost(min(value.size - to, value.size - from))) {
                value[to + i] = value[from + i]
            }
            value.js
        }
        "flat".js.func("depth" defaults OpArgOmitted) {
            val depth = it.argOrNull(0)
                ?.let { toNumber(it) }
                ?.toInt()
                ?.coerceAtLeast(0)
                ?: 1
            thisRef<Iterable<JsAny?>>().flat(depth).js
        }
        "flatMap".js.func("block") {
            val value = thisRef<Iterable<JsAny?>>()
            op(it) { callable ->
                value.map {
                    callable.invoke(it.listOf(), this)
                }
            }.flat(1).js
        }
        "values".js.func {
            thisRef<Iterable<JsAny?>>().iterator().js
        }

        ToString.js.func {
            thisRef<Iterable<*>>().joinToString(separator = ",").js
        }

        JsSymbol.iterator.func {
            thisRef<Iterable<JsAny?>>().iterator().js
        }
    }
) {
    override suspend fun isInstance(obj: JsAny?, runtime: ScriptRuntime): Boolean {
        return obj is List<*>
    }

    override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny {
        if (args.isEmpty()) {
            return emptyList<Undefined>().js
        }

        if (args.size == 1) {
            val size = runtime.toNumber(args[0])
            when {
                size is Long || size is Double && size.isFinite() && abs(size - size.toInt()) < Float.MIN_VALUE ->
                    return List(size.toInt()) { Undefined }.js

                size is Double -> throw RangeError("Invalid array length")
            }
        }
        return args.js
    }

    override suspend fun construct(args: List<JsAny?>, runtime: ScriptRuntime): JsAny {
        return invoke(args, runtime)
    }
}

private fun Iterable<JsAny?>.flat(
    depth : Int,
    collector : MutableList<JsAny?> = mutableListOf()
) : List<JsAny?> {
    forEach {
        if (depth == 0 || it !is List<*>) {
            collector.add(it)
        } else {
            it as List<JsAny?>
            it.flat(depth - 1, collector)
        }
    }
    return collector
}

internal suspend inline fun <R> ScriptRuntime.op(
    arguments: List<JsAny?>,
    op: (Callable) -> R
) : R = op(arguments[0].callableOrThrow(this))

private suspend fun List<*>.indexOf(
    context: ScriptRuntime,
    arguments: List<JsAny?>
): JsAny {
    val search = arguments[0]

    var position = context.toNumber(
        arguments.argOrElse(1){ 0.js }
    ).toInt()

    if (position >= size){
        return (-1).js
    }

    position = when {
        position >= size -> return (-1).js
        position < -size -> 0
        position < 0 -> position + size
        else -> position
    }

    for (i in position until size) {
        if (this[i] == search) {
            return i.js
        }
    }

    return (-1).js
}

private suspend fun List<*>.lastIndexOf(
    context: ScriptRuntime,
    arguments: List<JsAny?>
): JsAny {
    val search = arguments[0]

    var position = context.toNumber(
        arguments.argOrElse(1) { lastIndex.js }
    ).toInt()

    if (position < -size) {
        return (-1).js
    }

    position = when {
        position < -size -> return (-1).js
        position >= size -> lastIndex
        position < 0 -> position + size
        else -> position
    }

    for (i in position downTo 0) {
        if (this[i] == search) {
            return i.js
        }
    }

    return (-1).js
}

private suspend fun List<JsAny?>.sorted(comparator : JsAny?, runtime: ScriptRuntime): List<JsAny?> {
    return if (comparator == null) {
        mergeSort { a, b ->
            runtime.toString(a).compareTo(runtime.toString(b)).toDouble()
        }
    } else {
        val comp = comparator.callableOrThrow(runtime)
        mergeSort { a, b ->
            runtime
                .toNumber(comp.invoke(listOf(a, b), runtime))
                .toDouble().let { if (it.isNaN()) 0.0 else it }
        }
    }
}

private suspend fun merge(
    left: List<JsAny?>,
    right: List<JsAny?>,
    compare: suspend (JsAny?, JsAny?) -> Double
): List<JsAny?> {
    var i = 0
    var j = 0

    return List(left.size + right.size) {
        when {
            i >= left.size -> right[j++]
            j >= right.size -> left[i++]
            compare(left[i], right[j]) <= 0 -> left[i++]
            else -> right[j++]
        }
    }
}

private suspend fun List<JsAny?>.mergeSort(compare: suspend (JsAny?, JsAny?) -> Double): List<JsAny?> {
    if (size <= 1) return this
    val mid = size / 2
    return merge(
        take(mid).mergeSort(compare),
        drop(mid).mergeSort(compare),
        compare
    )
}

