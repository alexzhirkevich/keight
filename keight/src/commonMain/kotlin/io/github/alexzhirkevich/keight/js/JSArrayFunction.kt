package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.callableOrThrow
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.fastAll
import io.github.alexzhirkevich.keight.fastAny
import io.github.alexzhirkevich.keight.fastFilter
import io.github.alexzhirkevich.keight.fastForEach
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.findRoot
import io.github.alexzhirkevich.keight.thisRef
import io.github.alexzhirkevich.keight.valueAtIndexOrUnit
import kotlin.jvm.JvmInline
import kotlin.math.abs
import kotlin.math.min

internal class JSArrayFunction : JSFunction(
    name = "Array",
    properties = mutableMapOf(
        "isArray" to "isArray".func("object") {
            (findRoot() as JSRuntime).Array.isInstance(it.getOrNull(0), this)
        }
    ),
    prototype = Object {
        "length" eq JsNumberWrapper(0)
        "indexOf".func(
            FunctionParam("search"),
            "position" defaults OpConstant(0)
        ) {
            thisRef<List<*>>().indexOf(this, it)
        }
        "lastIndexOf".func(
            FunctionParam("search"),
            "position" defaults OpArgOmitted
        ) {
            thisRef<List<*>>().lastIndexOf(this, it)
        }
        "forEach".func("callback") { args ->
            op(args) { callable ->
                thisRef<List<*>>().fastForEach {
                    callable.invoke(it.listOf(), this)
                }
            }
        }

        "map".func("callback") { args ->
            op(args) { callable ->
                    thisRef<List<*>>()
                        .fastMap { callable.invoke(it.listOf(), this) }
            }
        }
        "filter".func("callback") { args ->
            op(args) { callable ->
                thisRef<List<*>>().fastFilter {
                    !isFalse(callable.invoke(it.listOf(), this))
                }
            }
        }
        "reduce".func("callback") { args ->
            val v = if (args.size > 1) {
                listOf(args[1]) + thisRef<List<*>>()
            } else {
                thisRef<List<*>>()
            }
            op(args) { callable ->
                v.reduce { acc, any ->
                    callable.invoke(listOf(acc, any), this)
                }
            }
        }
        "reduceRight".func("callback") { args ->
            val v = if (args.size > 1) {
                thisRef<List<*>>() + args[1]
            } else {
                thisRef<List<*>>()
            }
            op(args) { callable ->
                v.reduceRight { acc, any ->
                    callable.invoke(listOf(acc, any), this)
                }
            }
        }
        "some".func("callback") { args ->
            op(args) { callable ->
                thisRef<List<*>>().fastAny {
                    !isFalse(callable.invoke(it.listOf(), this))
                }
            }
        }
        "every".func("callback") { args ->
            op(args) { callable ->
                thisRef<List<*>>().fastAll {
                    !isFalse(callable.invoke(it.listOf(), this))
                }
            }
        }
        "reverse".func { thisRef<MutableList<*>>().reverse() }
        "toReversed".func { thisRef<List<*>>().reversed() }

//        "sort".func {
//            thisRef<MutableList<Any?>>().quickSort { a, b ->
//                if (isComparable(a, b)) compare(a, b) else 0
//            }
//        }
//
//        "toSorted".func {
//            thisRef<List<*>>().toMutableList().quickSort { a, b ->
//                if (isComparable(a, b)) compare(a, b) else 0
//            }
//        }
        "slice".func(
            "start" defaults OpConstant(0),
            "end" defaults OpArgOmitted
        ) { args ->
            if (args.isEmpty()) {
                thisRef()
            } else {
                val start = toNumber(args[0]).toInt()
                val list = thisRef<List<*>>()
                val end = toNumber(args.argOrElse(1) { list.size })
                    .toInt()
                    .coerceIn(0, list.size)
                list.slice(start until end)
            }
        }
        "splice" eq Splice(mutableListOf())
        "toSpliced" eq ToSpliced(mutableListOf())
        "at" eq At(mutableListOf())
        "includes" eq Includes(mutableListOf())
        "join" eq Join(mutableListOf())
        "push" eq Push(mutableListOf())
        "pop" eq Pop(mutableListOf())
        "shift" eq Shift(mutableListOf())
        "unshift" eq Unshift(mutableListOf())
        "concat" eq Concat(mutableListOf())
        "copyWithin" eq CopyWithin(mutableListOf())
        "flat" eq Flat(mutableListOf())
        "flatMap" eq FlatMap(mutableListOf())
        "toString".func{
            thisRef<List<*>>().joinToString(separator = ",")
        }
    }
) {
    override suspend fun isInstance(obj: Any?, runtime: ScriptRuntime): Boolean {
        return obj is List<*>
    }

    override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): List<*> {
        if (args.isEmpty()) {
            return emptyList<Unit>()
        }

        if (args.size == 1) {
            val size = runtime.toNumber(args[0])
            when {
                size is Long || size is Double && size.isFinite() && abs(size - size.toInt()) < Float.MIN_VALUE ->
                    return List(size.toInt()) { Unit }
                size is Double ->  throw RangeError("Invalid array length")
            }
        }
        return args
    }

    override suspend fun construct(args: List<Any?>, runtime: ScriptRuntime): List<*> {
        return invoke(args, runtime)
    }

    @JvmInline
    private value class Splice(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            val pos = runtime.toNumber(args[0]).toInt()
            val remove = runtime.toNumber(args[1]).toInt()
            val rest = args.drop(2)

            val deleted = buildList {
                repeat(remove) {
                    add(value.removeAt(pos))
                }
            }

            value.addAll(pos, rest)
            return deleted
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Splice((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class ToSpliced(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            val copy = value.toMutableList()
            Splice(copy).invoke(args, runtime)
            return copy
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return ToSpliced((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }


    @JvmInline
    private value class At(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            val idx = runtime.toNumber(args[0]).toInt()
            return value.valueAtIndexOrUnit(idx)
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return At((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class Includes(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            val v = args[0]
            val fromIndex = args.getOrNull(1)?.let { runtime.toNumber(it) }?.toInt() ?: 0
            return value.indexOf(v) > fromIndex
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Includes((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class Join(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            val sep = JSStringFunction.toString(args.getOrElse(0) { "," }, runtime)
            return value.joinToString(separator = sep)
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Join((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class Push(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            value.addAll(args)
            return args.size
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Push((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class Pop(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            return if (value.isEmpty()) {
                Unit
            } else {
                value.removeLast()
            }
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Pop((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class Shift(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            return if (value.isEmpty()) {
                Unit
            } else {
                value.removeFirst()
            }
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Shift((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class Unshift(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            value.addAll(0, args)
            return args.size
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Unshift((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class Concat(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            return buildList {
                addAll(value)
                args.forEach {
                    when (it) {
                        is List<*> -> addAll(it)
                        else -> add(it)
                    }
                }
            }
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Concat((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class CopyWithin(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            val to = runtime.toNumber(args[0]).toInt()
            val from = runtime.toNumber(args[1]).toInt()

            val num = if (args.size >= 3) {
                runtime.toNumber(args[2]).toInt()
            } else {
                value.size
            }

            for (i in 0 until num.coerceAtMost(min(value.size - to, value.size - from))){
                value[to + i] = value[from + i]
            }
            return value
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return CopyWithin((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class Flat(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            val depth = args.getOrNull(0)
                ?.let { runtime.toNumber(it) }
                ?.toInt()
                ?.coerceAtLeast(0)
                ?: 1
            return value.flat(depth)
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Flat((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class FlatMap(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            return with(runtime) {
                op(args) { callable ->
                    value.fastMap {
                        callable.invoke(listOf(it), runtime)
                    }
                }.flat(1)
            }
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return FlatMap((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }
}

private fun List<*>.flat(depth : Int, collector : MutableList<Any?> = mutableListOf()) : List<*> {
    fastForEach {
        if (depth == 0 || it !is List<*>) {
            collector.add(it)
        } else {
            it.flat(depth - 1, collector)
        }
    }
    return collector
}

private suspend fun <R> ScriptRuntime.op(
    arguments: List<Any?>,
    op: suspend (Callable) -> R
) : R {
    return op(arguments[0].callableOrThrow(this))
}

private suspend fun List<*>.indexOf(
    context: ScriptRuntime,
    arguments: List<Any?>
): Any {
    val search = arguments[0]

    var position = context.toNumber(
        arguments.argOrElse(1){ 0 }
    ).toInt()

    if (position >= size){
        return -1
    }

    position = when {
        position >= size -> return -1
        position < -size -> 0
        position < 0 -> position + size
        else -> position
    }

    for (i in position until size) {
        if (this[i] == search) {
            return i
        }
    }

    return -1
}

private suspend fun List<*>.lastIndexOf(
    context: ScriptRuntime,
    arguments: List<Any?>
): Any {
    val search = arguments[0]

    var position = context.toNumber(
        arguments.argOrElse(1) { lastIndex }
    ).toInt()

    if (position < -size) {
        return -1
    }

    position = when {
        position < -size -> return -1
        position >= size -> lastIndex
        position < 0 -> position + size
        else -> position
    }

    for (i in position downTo 0) {
        if (this[i] == search) {
            return i
        }
    }

    return -1
}

private suspend fun MutableList<Any?>.quickSort(
    left: Int = 0,
    right: Int = lastIndex,
    compare : suspend (Any?, Any?) -> Int
){
    var start = left
    var end = right
    val pivot = this[(left + right) / 2]

    while (start <= end) {
        while (compare(this[start], pivot) < 0){
            start++
        }
        while (compare(this[start], pivot) > 0){
            end--
        }
        if (start <= end) {
            val temp = this[start]
            this[start] = this[end]
            this[end] = temp
            start++
            end--
        }
    }

    if (left < end) {
        quickSort(left, end, compare)
    }
    if (start < right) {
        quickSort( start, right, compare)
    }
}
