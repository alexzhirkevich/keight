package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JSLangContext
import io.github.alexzhirkevich.keight.js.JSMath
import io.github.alexzhirkevich.keight.js.JSNumberFunction
import io.github.alexzhirkevich.keight.js.JSObject
import io.github.alexzhirkevich.keight.js.JSObjectFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsConsole
import kotlin.jvm.JvmInline

public open class JSRuntime(
    override var io: ScriptIO = DefaultScriptIO,
) : DefaultRuntime(), JSObject, ScriptContext by JSLangContext {

    override val comparator: Comparator<Any?> by lazy {
        JSComparator(this)
    }

    override val keys: Set<String> get() = emptySet()

    override val entries: List<List<Any?>> get() = emptyList()

    override val values: List<Any?> get() = emptyList()

    init { init() }

    override fun contains(variable: Any?): Boolean {
        return super<DefaultRuntime>.contains(variable)
    }

    override fun reset() {
        super.reset()
        init()
    }

    private fun init() {
        set("console", JsConsole(), VariableType.Global)
        set("Math", JSMath(), VariableType.Global)
        set("Object", JSObjectFunction(), VariableType.Global)
        set("globalThis", this, VariableType.Global)
        set("this", this, VariableType.Const)
        set("Infinity", Double.POSITIVE_INFINITY, VariableType.Const)
        set("NaN", Double.NaN, VariableType.Const)
        set("undefined", Unit, VariableType.Const)

        val number = JSNumberFunction()
        set("Number", number, VariableType.Global)

        set("parseInt", number.parseInt, VariableType.Global)
        set("parseFloat", number.parseFloat, VariableType.Global)
        set("isFinite", number.isFinite, VariableType.Global)
        set("isNan", number.isNan, VariableType.Global)
        set("isInteger", number.isInteger, VariableType.Global)
        set("isSafeInteger", number.isSafeInteger, VariableType.Global)

    }

    final override fun get(variable: Any?): Any? {
        val v = getInternal(variable)
        return v
    }

    private fun getInternal(variable: Any?) : Any? {
        if (variable in this){
            return super<DefaultRuntime>.get(variable)
        }

        val globalThis = get("globalThis") as? JsAny?
            ?: return super<DefaultRuntime>.get(variable)

        if (variable in globalThis){
            return globalThis[variable]
        }

        return super<DefaultRuntime>.get(variable)
    }

    final override fun set(variable: Any?, value: Any?) {
        set(variable, fromKotlin(value), null)
    }

}

@JvmInline
internal value class JSComparator(
    private val runtime: ScriptRuntime
) : Comparator<Any?> {

    override fun compare(a: Any?, b: Any?): Int {
        val ra = runtime.toKotlin(a)
        val rb = runtime.toKotlin(b)
        return if (ra is Number && rb is Number) {
            ra.toDouble().compareTo(rb.toDouble())
        } else {
            ra.toString().compareTo(rb.toString())
        }
    }
}