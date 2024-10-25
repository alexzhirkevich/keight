package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.argAt
import io.github.alexzhirkevich.keight.common.Callable
import io.github.alexzhirkevich.keight.common.OpConstant
import io.github.alexzhirkevich.keight.es.TypeError
import io.github.alexzhirkevich.keight.common.checkNotEmpty
import io.github.alexzhirkevich.keight.common.fastFilter
import io.github.alexzhirkevich.keight.common.fastMap
import io.github.alexzhirkevich.keight.common.valueAtIndexOrUnit
import io.github.alexzhirkevich.keight.es.ESAny
import io.github.alexzhirkevich.keight.invoke
import kotlin.jvm.JvmInline

@JvmInline
internal value class JsArray(
    override val value : MutableList<Any?>
) : ESAny, JsWrapper<MutableList<Any?>>, MutableList<Any?> by value {

    override fun get(variable: Any?): Any? {
        return when (variable) {
            "length" -> value.size

            "indexOf" -> IndexOf(value)
            "lastIndexOf" -> LastIndexOf(value)
            "forEach" -> ForEach(value)
            "map" -> Map(value)
            "filter" -> Filter(value)
            "reduce" -> Reduce(value)
            "reduceRight" -> ReduceRight(value)
            "some" -> Some(value)
            "every" -> Every(value)
            "reverse" -> Reverse(value)
            "toReversed" -> ToReversed(value)
            "sort" -> Sort(value)
            "toSorted" -> ToSorted(value)
            "slice" -> Slice(value)
            "at" -> At(value)
            "includes" -> Includes(value)
            else -> super.get(variable)
        }
    }

    override fun toString(): String {
        return value.joinToString(separator = ",")
    }

    @JvmInline
    private value class IndexOf(val value: MutableList<Any?>) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
            return value.indexOf(false, runtime, args)
        }
    }

    @JvmInline
    private value class LastIndexOf(val value: MutableList<Any?>) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
            return value.indexOf(true, runtime, args)
        }
    }

    @JvmInline
    private value class ForEach(val value: MutableList<Any?>) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
            return op(runtime, args) { callable ->
                value.forEach {
                    callable.invoke(listOf(OpConstant(it)), runtime)
                }
            }
        }
    }

    @JvmInline
    private value class Map(val value: MutableList<Any?>) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
            return op(runtime, args) { callable ->
                value.fastMap {
                    callable.invoke(listOf(OpConstant(it)), runtime)
                }
            }
        }
    }

    @JvmInline
    private value class Filter(val value: MutableList<Any?>) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
            return op(runtime, args) { callable ->
                value.fastFilter {
                    !runtime.isFalse(callable.invoke(listOf(OpConstant(it)), runtime))
                }
            }
        }
    }

    @JvmInline
    private value class Reduce(val value: MutableList<Any?>) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
            val v = if (args.size > 1) {
                listOf(args[1].invoke(runtime)) + value
            } else {
                value
            }
            return op(runtime, args) { callable ->
                v.reduce { acc, any ->
                    callable.invoke(
                        listOf(
                            OpConstant(acc),
                            OpConstant(any),
                        ),
                        runtime
                    )
                }
            }
        }
    }

    @JvmInline
    private value class ReduceRight(val value: MutableList<Any?>) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
            val v = if (args.size > 1) {
                value + args[1].invoke(runtime)
            } else {
                value
            }
            return op(runtime, args) { callable ->
                v.reduceRight { acc, any ->
                    callable.invoke(
                        listOf(
                            OpConstant(acc),
                            OpConstant(any),
                        ),
                        runtime
                    )
                }
            }
        }
    }

    @JvmInline
    private value class Some(val value: MutableList<Any?>) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
            return op(runtime, args) { callable ->
                value.any {
                    !runtime.isFalse(callable.invoke(listOf(OpConstant(it)), runtime))
                }
            }
        }
    }

    @JvmInline
    private value class Every(val value: MutableList<Any?>) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
            return op(runtime, args) { callable ->
                value.any {
                    !runtime.isFalse(callable.invoke(listOf(OpConstant(it)), runtime))
                }
            }
        }
    }

    @JvmInline
    private value class Reverse(val value: MutableList<Any?>) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime) {
            return value.reverse()
        }
    }

    @JvmInline
    private value class ToReversed(val value: MutableList<Any?>) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
            return value.reversed()
        }
    }

    @JvmInline
    private value class Sort(val value: MutableList<Any?>) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime) {
            return value.sortWith(runtime.comparator)
        }
    }

    @JvmInline
    private value class ToSorted(val value: MutableList<Any?>) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime) : Any?{
            return value.sortedWith(runtime.comparator)
        }
    }

    @JvmInline
    private value class Slice(val value: MutableList<Any?>) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime) : Any?{
            return if (args.isEmpty()) {
                value
            } else {
                val start = args[0].invoke(runtime).let(runtime::toNumber).toInt()
                val end = if (args.size < 2)
                    value.size
                else
                    args[1].invoke(runtime).let(runtime::toNumber).toInt()
                        .coerceIn(0, value.size)

                value.slice(start..<end)
            }
        }
    }

    @JvmInline
    private value class At(val value: MutableList<Any?>) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime) : Any?{
            val idx = args[0].invoke(runtime).let(runtime::toNumber).toInt()
            return value.valueAtIndexOrUnit(idx)
        }
    }

    @JvmInline
    private value class Includes(val value: MutableList<Any?>) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime) : Any?{
            val v = args[0].invoke(runtime)
            val fromIndex = args.getOrNull(1)?.let(runtime::toNumber)?.toInt() ?: 0
            return value.indexOf(v) > fromIndex
        }
    }
}


private fun <R> op(
    runtime: ScriptRuntime,
    arguments: List<Expression>,
    op: (Callable) -> R
) : R {
    val func = arguments[0].invoke(runtime)
    if (func !is Callable){
        throw TypeError("$func is not a function")
    }
    return op(func)
}

private fun List<*>.indexOf(
    last : Boolean,
    context: ScriptRuntime,
    arguments: List<Expression>
): Any {
    val search = checkNotEmpty(arguments.argAt(0).invoke(context))

    return if (last)
        lastIndexOf(search)
    else indexOf(search)
}
