package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.callableOrNull
import io.github.alexzhirkevich.keight.checkNotEmpty
import io.github.alexzhirkevich.keight.comparator
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.fastAny
import io.github.alexzhirkevich.keight.fastFilter
import io.github.alexzhirkevich.keight.fastForEach
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import io.github.alexzhirkevich.keight.valueAtIndexOrUnit
import kotlin.jvm.JvmInline
import kotlin.math.abs
import kotlin.math.min

internal class JSArrayFunction : JSFunction(
    name = "Array",
    parameters = emptyList(),
    body = OpConstant(Unit)
) {
    init {
        func("isArray", "object") {
            isInstance(it.getOrNull(0), this)
        }

        setPrototype(Object {
            "length" eq JsNumberWrapper(0)
            "indexOf" eq IndexOf(mutableListOf())
            "lastIndexOf" eq LastIndexOf(mutableListOf())
            "forEach" eq ForEach(mutableListOf())
            "map" eq Map(mutableListOf())
            "filter" eq Filter(mutableListOf())
            "reduce" eq Reduce(mutableListOf())
            "reduceRight" eq ReduceRight(mutableListOf())
            "some" eq Some(mutableListOf())
            "every" eq Every(mutableListOf())
            "reverse" eq Reverse(mutableListOf())
            "toReversed" eq ToReversed(mutableListOf())
            "sort" eq Sort(mutableListOf())
            "toSorted" eq ToSorted(mutableListOf())
            "slice" eq Slice(mutableListOf())
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
        })
    }

    override suspend fun isInstance(obj: Any?, runtime: ScriptRuntime): Boolean {
        return obj is List<*>
    }

    override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): List<*> {
        if (args.isEmpty()) {
            return emptyList<Unit>()
        }

        if (args.size == 1) {
            val size = runtime.toNumber(args[0], strict = true)
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
    private value class IndexOf(val value: MutableList<Any?>) : Callable {

        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            return value.indexOf(false, runtime, args)
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return IndexOf((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class LastIndexOf(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            return value.indexOf(true, runtime, args)
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return LastIndexOf((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class ForEach(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            return op(runtime, args) { callable ->
                value.fastForEach {
                    callable.invoke(listOf(it), runtime)
                }
            }
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return ForEach((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class Map(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            return op(runtime, args) { callable ->
                value.fastMap {
                    callable.invoke(listOf(it), runtime)
                }
            }
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Map((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class Filter(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            return op(runtime, args) { callable ->
                value.fastFilter {
                    !runtime.isFalse(callable.invoke(listOf(it), runtime))
                }
            }
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Filter((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class Reduce(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            val v = if (args.size > 1) {
                listOf(args[1]) + value
            } else {
                value
            }
            return op(runtime, args) { callable ->
                v.reduce { acc, any ->
                    callable.invoke(listOf(acc, any), runtime)
                }
            }
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Reduce((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class ReduceRight(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            val v = if (args.size > 1) {
                value + args[1]
            } else {
                value
            }
            return op(runtime, args) { callable ->
                v.reduceRight { acc, any ->
                    callable.invoke(listOf(acc, any), runtime)
                }
            }
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return ReduceRight((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class Some(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            return op(runtime, args) { callable ->
                value.any {
                    !runtime.isFalse(callable.invoke(listOf(it), runtime))
                }
            }
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Some((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class Every(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            return op(runtime, args) { callable ->
                value.fastAny {
                    !runtime.isFalse(callable.invoke(listOf(it), runtime))
                }
            }
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Every((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class Reverse(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime) {
            return value.reverse()
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Reverse((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class ToReversed(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            return value.reversed()
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return ToReversed((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class Sort(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime) {
            return value.sortWith(runtime.comparator)
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Sort((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class ToSorted(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            return value.sortedWith(runtime.comparator)
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return ToSorted((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class Slice(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            return if (args.isEmpty()) {
                value
            } else {
                val start = args[0].let(runtime::toNumber).toInt()
                val end = if (args.size < 2)
                    value.size
                else
                    args[1].let(runtime::toNumber).toInt()
                        .coerceIn(0, value.size)

                value.slice(start..<end)
            }
        }
        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Slice((thisArg as? MutableList<Any?>) ?: mutableListOf())
        }
    }

    @JvmInline
    private value class Splice(val value: MutableList<Any?>) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            val pos = args[0].let(runtime::toNumber).toInt()
            val remove = args[1].let(runtime::toNumber).toInt()
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
            val idx = args[0].let(runtime::toNumber).toInt()
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
            val fromIndex = args.getOrNull(1)?.let(runtime::toNumber)?.toInt() ?: 0
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
                ?.let(runtime::toNumber)
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
            return op(runtime, args) { callable ->
                value.fastMap {
                    callable.invoke(listOf(it), runtime)
                }
            }.flat(1)
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

private suspend fun <R> op(
    runtime: ScriptRuntime,
    arguments: List<Any?>,
    op: suspend (Callable) -> R
) : R {
    val func = arguments[0]?.callableOrNull()
    typeCheck(func != null){
        throw TypeError("$func is not a function")
    }
    return op(func)
}

private suspend fun List<*>.indexOf(
    last : Boolean,
    context: ScriptRuntime,
    arguments: List<Any?>
): Any {
    val search = checkNotEmpty(arguments[0])

    return if (last)
        lastIndexOf(search)
    else indexOf(search)
}
