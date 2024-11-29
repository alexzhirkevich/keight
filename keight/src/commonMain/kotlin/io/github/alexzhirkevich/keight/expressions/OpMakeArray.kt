package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.fastForEach
import io.github.alexzhirkevich.keight.js.JSObject
import io.github.alexzhirkevich.keight.js.JSObjectImpl
import io.github.alexzhirkevich.keight.js.JSPropertyAccessor
import io.github.alexzhirkevich.keight.js.JsAny

internal abstract class OpMake : Expression() {
    abstract val items : List<Expression>
}

internal class OpMakeArray(
    override val items : List<Expression>
) : OpMake() {
    override suspend fun execute(runtime: ScriptRuntime): Any? {
        return buildList {
            items.fastForEach { i ->
                val value = i(runtime)
                if (i is OpSpread && value is Iterable<*>) {
                    addAll(value as Iterable<Any?>)
                } else {
                    add(value)
                }
            }
        }
    }
}

internal class OpMakeObject(
    override val items : List<Expression>
) : OpMake() {

    override suspend fun execute(runtime: ScriptRuntime): Any? {
        return JSObjectImpl().apply {
            val getters = items.filterIsInstance<OpGetter>().associateBy { it.value.name }
            val setters = items.filterIsInstance<OpSetter>().associateBy { it.value.name }

            (getters.keys + setters.keys).forEach {
                set(it, JSPropertyAccessor.BackedField(getters[it]?.value, setters[it]?.value))
            }

            items.forEach { expr ->
                when (expr) {
                    is OpKeyValuePair -> {
                        set(expr.key, expr.value(runtime))
                    }

                    is PropertyAccessorFactory -> Unit

                    is OpSpread -> {
                        val any = expr.value(runtime)

                        if (any is JSObject) {
                            any.keys(
                                runtime = runtime,
                                excludeSymbols = false
                            ).fastForEach { k ->
                                val descriptor = any.ownPropertyDescriptor(k) ?: return@forEach

                                defineOwnProperty(
                                    property = k,
                                    value = descriptor.value.get(runtime),
                                    writable = descriptor.writable,
                                    enumerable = descriptor.enumerable,
                                    configurable = descriptor.configurable
                                )
                            }
                        } else {
                            if (any is JsAny) {
                                any.keys(runtime).fastForEach {
                                    set(it, any.get(it, runtime))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}