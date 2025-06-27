package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.fastForEach
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.JsObjectImpl
import io.github.alexzhirkevich.keight.js.JsPropertyAccessor
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.js

internal abstract class OpMake : Expression() {
    abstract val items : List<Expression>
}

internal class OpMakeArray(
    override val items : List<Expression>
) : OpMake() {
    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        return buildList<JsAny?> {
            items.fastForEach { i ->
                val value = i(runtime)
                if (i is OpSpread && value is Iterable<*>) {
                    addAll(value as Iterable<JsAny?>)
                } else {
                    add(value)
                }
            }
        }.toMutableList().js
    }
}

internal class OpMakeObject(
    override val items : List<Expression>
) : OpMake() {

    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        return JsObjectImpl().apply {
            val getters = items.filterIsInstance<OpGetter>().associateBy { it.value.name }
            val setters = items.filterIsInstance<OpSetter>().associateBy { it.value.name }

            (getters.keys + setters.keys).forEach {
                setOverwrite(it.js, JsPropertyAccessor.BackedField(getters[it]?.value, setters[it]?.value))
            }

            items.forEach { expr ->
                when (expr) {
                    is OpColonAssignment -> {
                        setOverwrite(expr.key, expr.value.invoke(runtime))
                    }

                    is PropertyAccessorFactory -> Unit

                    is OpSpread -> {
                        val any = expr.value.invoke(runtime)

                        if (any is JsObject) {
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
                                    setOverwrite(it, any.get(it, runtime))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}