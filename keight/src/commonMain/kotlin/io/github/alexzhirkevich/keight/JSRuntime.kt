package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JSArrayFunction
import io.github.alexzhirkevich.keight.js.JSLangContext
import io.github.alexzhirkevich.keight.js.JSMapFunction
import io.github.alexzhirkevich.keight.js.JSMath
import io.github.alexzhirkevich.keight.js.JSNumberFunction
import io.github.alexzhirkevich.keight.js.JSObject
import io.github.alexzhirkevich.keight.js.JSObjectFunction
import io.github.alexzhirkevich.keight.js.JSObjectImpl
import io.github.alexzhirkevich.keight.js.JSSetFunction
import io.github.alexzhirkevich.keight.js.JSStringFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsConsole
import io.github.alexzhirkevich.keight.js.JsMapWrapper
import io.github.alexzhirkevich.keight.js.setupNumberMethods
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmInline

public open class JSRuntime(
    override val coroutineContext: CoroutineContext,
    override var io: ScriptIO = DefaultScriptIO
) : DefaultRuntime(), ScriptContext by JSLangContext {

    override val comparator: Comparator<Any?> by lazy {
        JSComparator(this)
    }

    internal lateinit var Number : JSNumberFunction
    internal lateinit var Object : JSObjectFunction
    internal lateinit var Array : JSArrayFunction
    internal lateinit var Map : JSMapFunction
    internal lateinit var Set : JSSetFunction
    internal lateinit var String : JSStringFunction

    init { init() }

    override fun reset() {
        super.reset()
        init()
    }


    private fun init() {
        Number = JSNumberFunction()
        Object = JSObjectFunction()
        Array = JSArrayFunction()
        Map = JSMapFunction()
        Set = JSSetFunction()
        String = JSStringFunction()

        val globalThis = RuntimeGlobalThis(this)

        set("console", JsConsole(), VariableType.Global)
        set("Math", JSMath(), VariableType.Global)
        set("globalThis", globalThis, VariableType.Global)
        set("this", this, VariableType.Const)
        set("Infinity", Double.POSITIVE_INFINITY, VariableType.Const)
        set("NaN", Double.NaN, VariableType.Const)
        set("undefined", Unit, VariableType.Const)

        set("Object", Object, VariableType.Global)
        set("Array", Array, VariableType.Global)
        set("Map", Map, VariableType.Global)
        set("Set", Set, VariableType.Global)
        set("Number", Number, VariableType.Global)
        set("String", String, VariableType.Global)

        globalThis.setupNumberMethods()
    }

    final override suspend fun get(property: Any?): Any? {
        if (contains(property)) {
            return super.get(property)
        }
        return (super.get("globalThis") as JsAny?)?.get(property, this)
    }
}

private class RuntimeGlobalThis(
    private val runtime: ScriptRuntime
) : JSObject {

    override val keys: Set<String>
        get() = emptySet()
    override val values: List<Any?>
        get() = emptyList()
    override val entries: List<List<Any?>>
        get() = emptyList()


    override suspend fun get(property: Any?, runtime: ScriptRuntime): Any? {
        return if (runtime.contains(property)){
            runtime.get(property)
        } else {
            super.get(property, runtime)
        }
    }

    override fun set(property: Any?, value: Any?) {
        runtime.set(property, value, null)
    }

    override suspend fun contains(property: Any?, runtime: ScriptRuntime): Boolean {
        return runtime.contains(property)
    }

    override fun delete(property: Any?) {
        runtime.delete(property)
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