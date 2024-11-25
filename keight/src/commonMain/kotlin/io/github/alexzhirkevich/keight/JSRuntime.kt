package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.expressions.JSErrorFunction
import io.github.alexzhirkevich.keight.expressions.JSReferenceErrorFunction
import io.github.alexzhirkevich.keight.expressions.JSTypeErrorFunction
import io.github.alexzhirkevich.keight.js.JSArrayFunction
import io.github.alexzhirkevich.keight.js.JSBooleanFunction
import io.github.alexzhirkevich.keight.js.JSBooleanWrapper
import io.github.alexzhirkevich.keight.js.JSDateFunction
import io.github.alexzhirkevich.keight.js.JSError
import io.github.alexzhirkevich.keight.js.JSFunctionFunction
import io.github.alexzhirkevich.keight.js.JSKotlinError
import io.github.alexzhirkevich.keight.js.JSMapFunction
import io.github.alexzhirkevich.keight.js.JSMath
import io.github.alexzhirkevich.keight.js.JSNumberFunction
import io.github.alexzhirkevich.keight.js.JSON
import io.github.alexzhirkevich.keight.js.JSObject
import io.github.alexzhirkevich.keight.js.JSObjectFunction
import io.github.alexzhirkevich.keight.js.JSPromiseFunction
import io.github.alexzhirkevich.keight.js.JSPromiseWrapper
import io.github.alexzhirkevich.keight.js.JSPropertyAccessor
import io.github.alexzhirkevich.keight.js.JSSetFunction
import io.github.alexzhirkevich.keight.js.JSStringFunction
import io.github.alexzhirkevich.keight.js.JSSymbolFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsArrayWrapper
import io.github.alexzhirkevich.keight.js.JsConsole
import io.github.alexzhirkevich.keight.js.JsMapWrapper
import io.github.alexzhirkevich.keight.js.JsNumberWrapper
import io.github.alexzhirkevich.keight.js.JsSetWrapper
import io.github.alexzhirkevich.keight.js.JsStringObject
import io.github.alexzhirkevich.keight.js.JsStringWrapper
import io.github.alexzhirkevich.keight.js.OpArgOmitted
import io.github.alexzhirkevich.keight.js.argOrElse
import io.github.alexzhirkevich.keight.js.defaults
import io.github.alexzhirkevich.keight.js.func
import io.github.alexzhirkevich.keight.js.interpreter.NumberFormat
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import io.github.alexzhirkevich.keight.js.listOf
import io.github.alexzhirkevich.keight.js.numberMethods
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.math.absoluteValue

public open class JSRuntime(
    context: CoroutineContext,
    override val isSuspendAllowed: Boolean = true,
    override val isStrict: Boolean = false,
    public var console: Console = DefaultConsole,
) : DefaultRuntime() {

    override val coroutineContext: CoroutineContext =
        context + SupervisorJob(context[Job]).also {
            if (!isSuspendAllowed) {
                it.cancel()
            }
        } + CoroutineName("JS coroutine")

    internal lateinit var Number: JSNumberFunction
    internal lateinit var Object: JSObjectFunction
    internal lateinit var Function: JSFunctionFunction
    internal lateinit var Array: JSArrayFunction
    internal lateinit var Map: JSMapFunction
    internal lateinit var Set: JSSetFunction
    internal lateinit var String: JSStringFunction
    internal lateinit var Boolean: JSBooleanFunction
    internal lateinit var Promise: JSPromiseFunction
    internal lateinit var Symbol: JSSymbolFunction
    internal lateinit var Error: JSErrorFunction
    internal lateinit var TypeError: JSTypeErrorFunction
    internal lateinit var ReferenceError: JSReferenceErrorFunction
    internal lateinit var Date: JSDateFunction

    internal var jobsCounter = 1L
    internal val jobsMap = mutableMapOf<Long, Job>()

    final override val thisRef: JSObject = RuntimeObject { this }

    init {
        init()
    }

    /**
     * Restore runtime to its initial state. Cancels all pending promises.
     * */
    override fun reset() {
        super.reset()
        init()

        coroutineContext.cancelChildren()
        jobsMap.clear()
        jobsCounter = 1
    }

    private fun init() {
        Number = JSNumberFunction()
        Object = JSObjectFunction()
        Function = JSFunctionFunction()
        Array = JSArrayFunction()
        Map = JSMapFunction()
        Set = JSSetFunction()
        String = JSStringFunction()
        Boolean = JSBooleanFunction()
        Promise = JSPromiseFunction()
        Symbol = JSSymbolFunction()
        Error = JSErrorFunction()
        TypeError = JSTypeErrorFunction(Error)
        ReferenceError = JSReferenceErrorFunction(Error)
        Date = JSDateFunction()

        listOf(
            Number,
            Object,
            Function,
            Array,
            Map,
            Set,
            String,
            Boolean,
            Promise,
            Symbol,
            Error,
            TypeError,
            ReferenceError,
            Date
        ).fastForEach {
            set(it.name, it, VariableType.Global)
        }

        set("globalThis", thisRef, null)
        set("Infinity", JsNumberWrapper(Double.POSITIVE_INFINITY), null)
        set("NaN", JsNumberWrapper(Double.NaN), null)
        set("undefined", Unit, null)
        set("console", JsConsole(::console), null)
        set("Math", JSMath(), null)
        set("JSON", JSON(), null)

        thisRef as RuntimeObject

        numberMethods().forEach {
            thisRef.set(it.name, it)
        }

        thisRef.regSetTimeout()
        thisRef.regSetInterval()
        thisRef.regClearJob("Timeout")
        thisRef.regClearJob("Interval")
        thisRef.set(
            "eval",
            "eval".func("script" defaults OpArgOmitted) {
                val script = toKotlin(it.argOrElse(0) { return@func Unit }).toString()
                JavaScriptEngine(this).evaluate(script)
            },
        )
    }

    override suspend fun get(property: Any?): Any? {
        return when (property) {
            "Infinity" -> Double.POSITIVE_INFINITY
            "NaN" -> Double.NaN
            "undefined" -> Unit
            else -> super.get(property)
        }
    }

//    final override suspend fun get(property: Any?): Any? {
//        return if (contains(property)) {
//            super.get(property)
//        } else thisRef.get(property, this)
//    }

    override suspend fun isFalse(a: Any?): Boolean {
        return a == null
                || a == false
                || a is Unit
                || a is CharSequence && a.isEmpty()
                || a is Number && a.toDouble().let { it == 0.0 || it.isNaN() }
                || (a as? Wrapper<*>)?.value?.let { isFalse(it) } == true
    }

    override suspend fun isComparable(a: Any?, b: Any?): Boolean {

        val ka = toKotlin(a)
        val kb = toKotlin(b)

        if (ka is Number && kb is CharSequence || kb is Number && ka is CharSequence){
            return isComparable(toNumber(ka), toNumber(kb))
        }

        if (ka is Double && ka.isNaN() || kb is Double && kb.isNaN()) {
            return false
        }
        if (ka is Number && kb is Number || ka is CharSequence && kb is CharSequence) {
            return true
        }
        return false
    }

    override suspend fun compare(a: Any?, b: Any?): Int {

        val ka = toKotlin(a)
        val kb = toKotlin(b)

        return if (ka is Number || kb is Number) {
            toNumber(ka).toDouble().compareTo(toNumber(kb).toDouble())
        } else {
            ka.toString().compareTo(kb.toString())
        }
    }

    override suspend fun sum(a: Any?, b: Any?): Any? {
        return jssum(a, b)
    }

    override suspend fun sub(a: Any?, b: Any?): Any? {
        return jssub(a?.numberOrThis(), b?.numberOrThis())
    }

    override suspend fun mul(a: Any?, b: Any?): Any? {
        return jsmul(a?.numberOrThis(), b?.numberOrThis())
    }

    override suspend fun div(a: Any?, b: Any?): Any? {
        return jsdiv(a?.numberOrThis(), b?.numberOrThis())
    }

    override suspend fun mod(a: Any?, b: Any?): Any? {
        return jsmod(a?.numberOrThis(), b?.numberOrThis(throwForObjects = true))
    }

    override suspend fun inc(a: Any?): Any? {
        return jsinc(a?.numberOrThis())
    }

    override suspend fun dec(a: Any?): Any? {
        return jsdec(a?.numberOrThis())
    }

    override suspend fun neg(a: Any?): Any? {
        return jsneg(a?.numberOrThis())
    }

    override suspend fun pos(a: Any?): Any? {
        return jspos(a?.numberOrThis())
    }

    override suspend fun toNumber(a: Any?, strict: Boolean): Number {
        return a.numberOrNull(withNaNs = !strict) ?: Double.NaN
    }

    override fun fromKotlin(a: Any?): Any? {
        return when (a) {
            is Wrapper<*> -> a
            is Boolean -> JSBooleanWrapper(a)
            is Number -> JsNumberWrapper(a)
            is UByte -> JsNumberWrapper(a.toLong())
            is UShort -> JsNumberWrapper(a.toLong())
            is UInt -> JsNumberWrapper(a.toLong())
            is ULong -> JsNumberWrapper(a.toLong())
            is Map<*, *> -> JsMapWrapper(
                a.map { fromKotlin(it.key) to fromKotlin(it.value) }.toMap().toMutableMap()
            )

            is Set<*> -> JsSetWrapper(a.map(::fromKotlin).toMutableSet())
            is List<*> -> JsArrayWrapper(a.map(::fromKotlin).toMutableList())
            is CharSequence -> JsStringWrapper(a.toString())
            is Job -> JSPromiseWrapper(a)
            is Throwable -> if (a is JSError) a else JSKotlinError(a)
            else -> a
        }
    }

    override fun toKotlin(a: Any?): Any? {
        return when (a) {
            is JsMapWrapper -> a.value.map { toKotlin(it.key) to toKotlin(it.value) }.toMap()
            is JsSetWrapper -> a.value.map(::toKotlin).toSet()
            is JsArrayWrapper -> a.value.fastMap(::toKotlin)
            is Wrapper<*> -> toKotlin(a.value)
            is Int -> a.toLong()
            is Float -> a.toDouble()
            else -> a
        }
    }

    internal tailrec suspend fun Any?.numberOrNull(
        withNaNs : Boolean = true,
        throwForObjects : Boolean = false
    ) : Number? = when(this) {
        null -> 0L
        true -> 1L
        false -> 0L

        is CharSequence -> when {
            isBlank() -> 0L
            withNaNs -> {
                val s = trim().toString()
                    .trimStart('\n', '+')
                    .trimEnd('\n')
                    .lowercase()

                NumberFormat.entries.forEach {
                    val p = "0${it.prefix}"
                    if (it.prefix != null && s.startsWith(p)) {
                        return s.removePrefix(p).toLong(it.radix)
                    }
                }

                s.toLongOrNull() ?: s.toDoubleOrNull()
            }

            else -> null
        }

        is Byte -> toLong()
        is UByte -> toLong()
        is Short -> toLong()
        is UShort -> toLong()
        is Int -> toLong()
        is UInt -> toLong()
        is ULong -> toLong()
        is Float -> toDouble()
        is Long -> this
        is Double -> this
        is List<*> -> {
            if (withNaNs) {
                singleOrNull()?.numberOrNull(withNaNs,throwForObjects)
            } else {
                null
            }
        }

        is Wrapper<*> -> value.numberOrNull(withNaNs,throwForObjects)
        is JSObject -> {
            if (withNaNs) {
                this.get("toPrimitive", this@JSRuntime)?.callableOrNull()?.let {
                    return toNumber(it.call(this, Number.listOf(), this@JSRuntime))
                }
                this.get("valueOf", this@JSRuntime)?.callableOrNull()?.let {
                    it.call(this, emptyList(), this@JSRuntime)?.numberOrNull(false, throwForObjects)
                        ?.let { return it }
                }

                JSStringFunction.toString(this, this@JSRuntime)
                    .numberOrNull(withNaNs, throwForObjects)
                    ?.takeUnless { it.toDouble().isNaN() }
                    ?.let { return it }

                if (throwForObjects && this.keys(this@JSRuntime).contains("toString")){
                    typeError { "Can't convert object to primitive" }
                } else {
                    null
                }
            } else {
                null
            }
        }
        else -> null
    }

    private suspend fun Any?.numberOrThis(
        withMagic : Boolean = true,
        throwForObjects : Boolean = false
    ) : Any? = numberOrNull(withMagic, throwForObjects) ?: this


    private suspend fun registerJob(
        args: List<Any?>,
        block: suspend CoroutineScope.(Callable, Long) -> Unit
    ): Long {
        val jobId = jobsCounter++

        val handler = args[0]?.callableOrNull()
        val timeout = toNumber(args.getOrNull(1)).toLong()

        typeCheck(handler is Callable) {
            "${args[0]} is not a function"
        }

        jobsMap[jobId] = launch(
            coroutineContext + CoroutineName("JS interval/timeout job $jobId")
        ) {
            block(handler, timeout)
        }.apply {
            invokeOnCompletion { jobsMap.remove(jobId) }
        }

        return jobId
    }

    private fun RuntimeObject.regSetTimeout() = this.set(
        "setTimeout",
        "setTimeout".func("handler", "timeout") {
            (findRoot() as JSRuntime).registerJob(it) { handler, timeout ->
                delay(timeout)
                handler.invoke(emptyList(), this@func)
            }
        }
    )

    private fun RuntimeObject.regSetInterval() = this.set(
        "setInterval",
        "setInterval".func("handler", "interval") {
            (findRoot() as JSRuntime).registerJob(it) { handler, timeout ->
                while (isActive) {
                    handler.invoke(emptyList(), this@func)
                    delay(timeout)
                }
            }
        }
    )

    private fun RuntimeObject.regClearJob(what: String) = this.set(
        "clear$what",
        "clear$what".func("id") {
            val id = toNumber(it.getOrNull(0)).toLong()
            jobsMap[id]?.cancel()
        }
    )
}

private class RuntimeObject(val thisRef: () -> ScriptRuntime) : JSObject {

    override suspend fun get(property: Any?, runtime: ScriptRuntime): Any? {
        return if (thisRef().contains(property)){
            thisRef().get(property)?.get(runtime)
        } else {
            super.get(property, thisRef())
        }
    }

    override suspend fun set(
        property: Any?,
        value: Any?,
        runtime: ScriptRuntime
    ) {
        thisRef().set(property, value, null)
    }

    fun set(
        property: Any?,
        value: Any?,
    ) {
        thisRef().set(property, value, null)
    }

    override suspend fun defineOwnProperty(
        property: Any?,
        value: JSPropertyAccessor,
        runtime: ScriptRuntime,
        enumerable: Boolean?,
        configurable: Boolean?,
        writable: Boolean?
    ) {
        set(property, value.get(runtime), runtime)
    }

    override fun descriptor(property: Any?): JSObject? {
        return null
    }

    override suspend fun contains(property: Any?, runtime: ScriptRuntime): Boolean {
        return thisRef().contains(property)
    }


    override suspend fun delete(property: Any?, runtime: ScriptRuntime): Boolean {

        thisRef().delete(property, true)
        return true
    }


}

internal inline fun <reified T> ScriptRuntime.thisRef() : T {
    return thisRef as T
}


private suspend fun JSRuntime.jssum(a : Any?, b : Any?) : Any? {
    val ta = if (a is List<*>)
        a.joinToString(",")
    else a
    val tb = if (b is List<*>)
        b.joinToString(",")
    else b

    return when {
        ta is CharSequence || tb is CharSequence -> ta.toString() + tb.toString()
        ta is Long && tb is Long -> ta + tb
        ta is Number && tb is Number -> ta.toDouble() + tb.toDouble()
        else -> jssum(a.numberOrNull() ?: Double.NaN , b.numberOrNull()  ?: Double.NaN)
    }
}

private fun jssub(a : Any?, b : Any?) : Any? {
    return when {
        a is Unit || b is Unit -> Double.NaN
        a is Number && b is Unit || a is Unit && b is Number -> Double.NaN
        a is Long? && b is Long? -> (a ?: 0L) - (b ?: 0L)
        a is Double? && b is Double? ->(a ?: 0.0) - (b ?: 0.0)
        a is Number? && b is Number? ->(a?.toDouble() ?: 0.0) - (b?.toDouble() ?: 0.0)
        else -> Double.NaN
    }
}

private fun jsmul(a : Any?, b : Any?) : Any? {
    return when {
        a == Unit || b == Unit -> Double.NaN
        a == null || b == null -> 0L
        a is Long && b is Long -> a*b
        a is Double && b is Double -> a*b
        a is Long && b is Long -> a * b
        a is Number && b is Number -> a.toDouble() * b.toDouble()
        a is List<*> && b is Number -> {
            a as List<Number>
            val bf = b.toDouble()
            a.fastMap { it.toDouble() * bf }
        }
        a is Number && b is List<*> -> {
            b as List<Number>
            val af = a.toDouble()
            b.fastMap { it.toDouble() * af }
        }
        else -> Double.NaN
    }
}

private fun jsdiv(a : Any?, b : Any?) : Any {
    return when {
        a is Unit || b is Unit
                || (a == null && b == null)
                || ((a as? Number)?.toDouble() == 0.0 && b == null)
                || ((b as? Number)?.toDouble() == 0.0 && a == null)
                || ((a as? CharSequence)?.toString()?.toDoubleOrNull() == 0.0 && b == null)
                || ((b as? CharSequence)?.toString()?.toDoubleOrNull() == 0.0 && a == null) -> Double.NaN
        a is Long && b is Long -> when {
            b == 0 -> a / b.toDouble()
            a % b == 0L -> a / b
            else -> a.toDouble() / b
        }
        a is Number && b is Number -> a.toDouble() / b.toDouble()
        a == null -> 0L
        b == null -> Double.POSITIVE_INFINITY

        a is List<*> && b is Number -> {
            a as List<Number>
            val bf = b.toDouble()
            a.fastMap { it.toDouble() / bf }
        }

        else -> Double.NaN
    }
}

private fun jsmod(a : Any?, b : Any?) : Any {
    return when {
        b == null || a == Unit || b == Unit -> Double.NaN
        (b as? Number)?.toDouble()?.absoluteValue?.let { it < Double.MIN_VALUE } == true -> Double.NaN
        a == null -> 0.0
//        a is Long && b is Long -> a % b
        a is Number && b is Number -> a.toDouble() % b.toDouble()
        else -> Double.NaN
    }
}


private fun jsinc(v : Any?) : Any {
    return when (v) {
        null -> 1L
        is Long -> v + 1
        is Double -> v + 1
        is Number -> v.toDouble() + 1
        else -> Double.NaN
    }
}

private fun jsdec(v : Any?) : Any {
    return when (v) {
        null -> -1L
        is Long -> v - 1
        is Double -> v - 1
        is Number -> v.toDouble() - 1
        else -> Double.NaN
    }
}

private fun jsneg(v : Any?) : Any {
    return when (v) {
        null -> -0
        0, 0L -> -0.0
        is Long -> -v
        is Number -> -v.toDouble()
        is List<*> -> {
            v as List<Number>
            v.fastMap { -it.toDouble() }
        }
        else -> Double.NaN
    }
}

private fun jspos(v : Any?) : Any {
    return when (v) {
        null -> 0
        is Long -> +v
        is Number -> +v.toDouble()
        else -> Double.NaN
    }
}
