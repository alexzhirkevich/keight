package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.es.abstract.esToNumber
import io.github.alexzhirkevich.keight.expressions.JSErrorFunction
import io.github.alexzhirkevich.keight.expressions.JSReferenceErrorFunction
import io.github.alexzhirkevich.keight.expressions.JSSyntaxErrorFunction
import io.github.alexzhirkevich.keight.expressions.JSTypeErrorFunction
import io.github.alexzhirkevich.keight.js.JSArrayFunction
import io.github.alexzhirkevich.keight.js.JSBooleanFunction
import io.github.alexzhirkevich.keight.js.JSDateFunction
import io.github.alexzhirkevich.keight.js.JSFunctionFunction
import io.github.alexzhirkevich.keight.js.JSIteratorFunction
import io.github.alexzhirkevich.keight.js.JSMapFunction
import io.github.alexzhirkevich.keight.js.JSMath
import io.github.alexzhirkevich.keight.js.JSNumberFunction
import io.github.alexzhirkevich.keight.js.JSON
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.JSObjectFunction
import io.github.alexzhirkevich.keight.js.JSPromiseFunction
import io.github.alexzhirkevich.keight.js.JSPropertyImpl
import io.github.alexzhirkevich.keight.js.JSRegExpFunction
import io.github.alexzhirkevich.keight.js.JsPropertyAccessor
import io.github.alexzhirkevich.keight.js.JSSetFunction
import io.github.alexzhirkevich.keight.js.JSStringFunction
import io.github.alexzhirkevich.keight.js.JsSymbol
import io.github.alexzhirkevich.keight.js.JSSymbolFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsConsole
import io.github.alexzhirkevich.keight.js.JsNumberWrapper
import io.github.alexzhirkevich.keight.js.JsProperty
import io.github.alexzhirkevich.keight.js.OpArgOmitted
import io.github.alexzhirkevich.keight.es.abstract.esToPrimitive
import io.github.alexzhirkevich.keight.es.abstract.esToString
import io.github.alexzhirkevich.keight.js.Constants
import io.github.alexzhirkevich.keight.js.JSError
import io.github.alexzhirkevich.keight.js.JsObjectImpl
import io.github.alexzhirkevich.keight.js.ReferenceError
import io.github.alexzhirkevich.keight.js.TypeError
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.argOrElse
import io.github.alexzhirkevich.keight.js.defaults
import io.github.alexzhirkevich.keight.js.func
import io.github.alexzhirkevich.keight.js.interpreter.NumberFormat
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import io.github.alexzhirkevich.keight.js.js
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

    override val parent: ScriptRuntime?
        get() = null

    override val coroutineContext: CoroutineContext =
        context + SupervisorJob(context[Job]) + CoroutineName("JS coroutine")

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
    internal lateinit var Error: JSErrorFunction<*>
    internal lateinit var TypeError: JSTypeErrorFunction
    internal lateinit var ReferenceError: JSReferenceErrorFunction
    internal lateinit var SyntaxError: JSSyntaxErrorFunction
    internal lateinit var Date: JSDateFunction
    internal lateinit var Iterator: JSIteratorFunction
    internal lateinit var RegExp: JSRegExpFunction

    private var jobsCounter = 1L
    private val jobsMap = mutableMapOf<Long, Job>()

    final override val thisRef: JsAny = RuntimeObject { this }

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
        Error = JSErrorFunction { JSError(it) }
        TypeError = JSTypeErrorFunction(Error)
        ReferenceError = JSReferenceErrorFunction(Error)
        SyntaxError = JSSyntaxErrorFunction(Error)
        Date = JSDateFunction()
        Iterator = JSIteratorFunction()
        RegExp = JSRegExpFunction()

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
            SyntaxError,
            Date,
            Iterator,
            RegExp
        ).fastForEach {
            variables[it.name.js] = VariableType.Global to it
        }

        variables["globalThis".js] = null to thisRef

        variables["Infinity".js] = null to JsNumberWrapper(Double.POSITIVE_INFINITY)
        variables["NaN".js] = null to JsNumberWrapper(Double.NaN)
        variables["undefined".js] = null to Undefined
        variables["console".js] = null to JsConsole(::console)
        variables["Math".js] = null to JSMath()
        variables["JSON".js] = null to JSON()

        numberMethods().forEach {
            variables[it.name.js] = null to it
        }

        regSetTimeout()
        regSetInterval()
        regClearJob("Timeout")
        regClearJob("Interval")

        variables["eval".js] = null to "eval".func("script" defaults OpArgOmitted) {
            val script = it.argOrElse(0) { return@func Undefined }?.toKotlin(this).toString()
            JavaScriptEngine(this@JSRuntime).compile(script).invoke(this)
        }
    }

    override suspend fun get(property: JsAny?): JsAny? {
        return when (property) {
            "Infinity".js -> Double.POSITIVE_INFINITY.js
            "NaN".js -> Double.NaN.js
            "undefined".js -> Undefined
            else -> super.get(property)
        }
    }

    override fun fromKotlin(value: Any): JsAny {
        return when (value) {
            is Unit -> Undefined
            is Number -> value.js
            is CharSequence -> value.js
            is Boolean -> value.js
            is List<*> -> value.map { it?.let(::fromKotlin) }.js
            is Set<*> -> value.mapTo(mutableSetOf()) { it?.let(::fromKotlin) }.js
            is Map<*, *> -> value.map { it.key?.let(::fromKotlin) to it.value?.let(::fromKotlin) }.toMap().js
            is Iterator<*> -> iterator { value.forEach { yield(it?.let(::fromKotlin)) } }.js
            is Job -> value.js
            is Regex -> value.js
            is Throwable -> value.js ?: Undefined
            else -> throw IllegalArgumentException("Can't cast $value to JS type")
        }
    }

    override suspend fun isFalse(a: Any?): Boolean {
        return a == null
                || a == false
                || a is Uninitialized
                || a is Undefined
                || a is Unit
                || a is CharSequence && a.isEmpty()
                || a is Number && a.toDouble().let { it == 0.0 || it.isNaN() }
                || (a as? Wrapper<*>)?.value?.let { isFalse(it) } == true
    }

    override fun makeObject(properties: Map<JsAny?, JsAny?>): JsObject {
        return JsObjectImpl(properties = properties)
    }

    override suspend fun referenceError(message: JsAny?): Nothing {
        throw ReferenceError.construct(message.listOf(), this) as ReferenceError
    }

    override suspend fun typeError(message: JsAny?): Nothing {
        throw TypeError.construct(message.listOf(), this) as TypeError
    }

    override suspend fun isComparable(a: JsAny?, b: JsAny?): Boolean {

        val ka = a?.toKotlin(this)
        val kb = b?.toKotlin(this)

        if (ka is Number && kb is CharSequence || kb is Number && ka is CharSequence){
            return isComparable(toNumber(a).js, toNumber(b).js)
        }

        if (ka is Double && ka.isNaN() || kb is Double && kb.isNaN()) {
            return false
        }
        if (ka is Number && kb is Number || ka is CharSequence && kb is CharSequence) {
            return true
        }
        return false
    }

    override suspend fun compare(a: JsAny?, b: JsAny?): Int {

        val ka = a?.toKotlin(this)
        val kb = b?.toKotlin(this)

        return if (ka is Number || kb is Number) {
            toNumber(a).toDouble().compareTo(toNumber(b).toDouble())
        } else {
            ka.toString().compareTo(kb.toString())
        }
    }

    override suspend fun sum(a: JsAny?, b: JsAny?): JsAny? {
        var ta: Any? = a
        var tb: Any? = b

        if (ta is JsObject) {
            ta = a.esToPrimitive(runtime = this)
        }
        if (tb is JsObject) {
            tb = b.esToPrimitive(runtime = this)
        }

        if (ta is CharSequence || tb is CharSequence) {
            return (ta.esToString(this) + tb.esToString(this)).js
        }

        val na = ta.esToNumber(this)
        val nb = tb.esToNumber(this)

        return when {
            na is Long && nb is Long -> (na + nb).js
            else -> (na.toDouble() + nb.toDouble()).js
        }
    }

    override suspend fun sub(a: JsAny?, b: JsAny?): JsAny? {
        return jssub(a?.numberOrThis(), b?.numberOrThis())
    }

    override suspend fun mul(a: JsAny?, b: JsAny?): JsAny? {
        return jsmul(a?.numberOrThis(), b?.numberOrThis())
    }

    override suspend fun div(a: JsAny?, b: JsAny?): JsAny? {
        return jsdiv(a?.numberOrThis(), b?.numberOrThis())
    }

    override suspend fun mod(a: JsAny?, b: JsAny?): JsAny? {
        return jsmod(a?.numberOrThis(), b?.numberOrThis(throwForObjects = true))
    }

    override suspend fun inc(a: JsAny?): JsAny? {
        return jsinc(a?.numberOrThis())
    }

    override suspend fun dec(a: JsAny?): JsAny? {
        return jsdec(a?.numberOrThis())
    }

    override suspend fun neg(a: JsAny?): JsAny? {
        return jsneg(a?.numberOrThis())
    }

    override suspend fun pos(a: JsAny?): JsAny? {
        return jspos(a?.numberOrThis())
    }

    override suspend fun toNumber(value: JsAny?): Number {
        return value.esToNumber(this)
    }

    override suspend fun toString(value: JsAny?): String {
        if (value is Undefined){
            return Undefined.toString()
        }
        
        return value?.get(Constants.toString.js, this)
            ?.callableOrNull()
            ?.call(value, emptyList(), this)
            ?.toString()
            ?: value?.get(JsSymbol.toStringTag, this)
                ?.callableOrNull()
                ?.call(value, emptyList(), this)
                ?.toString()
            ?: value.toString()
    }

    private suspend fun JsAny.numberOrThis(
        withMagic : Boolean = true,
        throwForObjects : Boolean = false
    ) : Any = numberOrNull(withMagic, throwForObjects) ?: this

    private tailrec suspend fun Any?.numberOrNull(
        withNaNs : Boolean = true,
        throwForObjects : Boolean = false
    ) : Number? = when(this) {
        is Undefined -> null
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
        is JsObject ->  if (withNaNs) {
            toPrimitive(Constants.number.js)?.numberOrNull(withNaNs, throwForObjects)
        } else {
            null
        }
        else -> null
    }


    /**
     * 11.1.6
     *
     * [11.1.6 Static Semantics: ParseText ( sourceText, goalSymbol )](https://tc39.es/ecma262/#sec-parsetext)
     *
     * TThe abstract operation ParseText takes arguments sourceText (a String or a sequence of
     * Unicode code points) and goalSymbol (a nonterminal in one of the ECMAScript grammars) and
     * returns a Parse Node or a non-empty List of SyntaxError objects.
     *
     * It performs the following steps when called:
     *
     * 1. If sourceText is a String, set sourceText to StringToCodePoints(sourceText).
     * 2. Attempt to parse sourceText using goalSymbol as the goal symbol, and analyse the parse
     * result for any early error conditions. Parsing and early error detection may be interleaved
     * in an implementation-defined manner.
     * 3. If the parse succeeded and no early errors were found, return the Parse Node (an instance
     * of goalSymbol) at the root of the parse tree resulting from the parse.
     * */
    internal fun CharSequence.ParseText(){

    }

    internal suspend fun JsAny.toPrimitive(
        type : JsAny? = Constants.default.js,
        symbolOnly : Boolean = false,
    ) : JsAny? {

        get(JsSymbol.toPrimitive, this@JSRuntime)
            ?.callableOrNull()
            ?.call(this, listOfNotNull(type), this@JSRuntime)
            ?.let {
                typeCheck(it !is JsObject){
                    "Cannot convert object to primitive value".js
                }
                return it
            }

        return if (!symbolOnly) {
            if (this is JsSymbol){
                typeError { "Symbol cannot be converted to a number".js }
            }

            get(Constants.valueOf.js, this@JSRuntime)?.callableOrNull()
                ?.call(this, emptyList(), this@JSRuntime)
                ?.takeIf { it !is JsObject }
                ?.let { return it }


            toString(this)
                .toDoubleOrNull()
                ?.takeUnless(Double::isNaN)
                ?.let { return it.js  }

            return null
        } else {
            null
        }
    }

    private suspend fun registerJob(
        args: List<JsAny?>,
        block: suspend CoroutineScope.(Callable, Long) -> Unit
    ): JsAny {
        val jobId = jobsCounter++

        val handler = args[0].callableOrThrow(this)
        val timeout = toNumber(args.getOrNull(1)).toLong()

        jobsMap[jobId] = launch(
            coroutineContext + CoroutineName("JS interval/timeout job $jobId")
        ) {
            block(handler, timeout)
        }.apply {
            invokeOnCompletion { jobsMap.remove(jobId) }
        }

        return jobId.js
    }

    private fun regSetTimeout() {
        variables["setTimeout".js] = null to "setTimeout".func("handler", "timeout") {
            findJsRoot().registerJob(it) { handler, timeout ->
                delay(timeout)
                handler.invoke(emptyList(), this@func)
            }
        }
    }

    private fun regSetInterval() {
        variables["setInterval".js] = null to "setInterval".func("handler", "interval") {
            findJsRoot().registerJob(it) { handler, timeout ->
                while (isActive) {
                    handler.invoke(emptyList(), this@func)
                    delay(timeout)
                }
            }
        }
    }

    private fun regClearJob(what: String) {
        variables[ "clear$what".js] =  null to  "clear$what".func("id") {
            val id = toNumber(it.getOrNull(0)).toLong()
            jobsMap[id]?.cancel()
            Undefined
        }
    }
}

private class RuntimeObject(val thisRef: () -> ScriptRuntime) : JsObject {

    override suspend fun ownPropertyDescriptor(property: JsAny?): JsProperty? {

        val r =  thisRef()

        if (!r.contains(property))
            return null

        return JSPropertyImpl(
            value = JsPropertyAccessor.Value(get(property, r)),
            writable = true,
            enumerable = true,
            configurable = true
        )
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        return if (thisRef().contains(property)){
            thisRef().get(property)
        } else {
            super.get(property, thisRef())
        }
    }

    override suspend fun set(
        property: JsAny?,
        value: JsAny?,
        runtime: ScriptRuntime
    ) {
        thisRef().set(property, value, null)
    }

    override suspend fun setProperty(
        property: JsAny?,
        value: JsPropertyAccessor,
        runtime: ScriptRuntime,
        enumerable: Boolean?,
        configurable: Boolean?,
        writable: Boolean?
    ) {
        set(property, value.get(runtime), runtime)
    }

    override suspend fun hasOwnProperty(name: JsAny?, runtime: ScriptRuntime): Boolean {
        return thisRef().contains(name)
    }

    override suspend fun contains(property: JsAny?, runtime: ScriptRuntime): Boolean {
        return thisRef().contains(property)
    }


    override suspend fun delete(property: JsAny?, runtime: ScriptRuntime): Boolean {

        thisRef().delete(property, true)
        return true
    }


}

internal fun ScriptRuntime.findJsRoot() : JSRuntime = findRoot() as JSRuntime

private fun jssub(a : Any?, b : Any?) : JsAny? {
    return when {
        a is Unit || b is Unit -> Double.NaN
        a is Number && b is Unit || a is Unit && b is Number -> Double.NaN
        a is Long? && b is Long? -> (a ?: 0L) - (b ?: 0L)
        a is Double? && b is Double? ->(a ?: 0.0) - (b ?: 0.0)
        a is Number? && b is Number? ->(a?.toDouble() ?: 0.0) - (b?.toDouble() ?: 0.0)
        else -> Double.NaN
    }.js
}

private fun jsmul(a : Any?, b : Any?) : JsAny {
    return when {
        a == Undefined || b == Undefined -> Double.NaN.js
        a == null || b == null -> 0L.js
        a is Long && b is Long -> (a*b).js
        a is Double && b is Double -> (a*b).js
        a is Long && b is Long -> (a * b).js
        a is Number && b is Number -> (a.toDouble() * b.toDouble()).js
        a is List<*> && b is Number -> {
            a as List<Number>
            val bf = b.toDouble()
            a.fastMap { (it.toDouble() * bf).js }.js
        }
        a is Number && b is List<*> -> {
            b as List<Number>
            val af = a.toDouble()
            b.fastMap { (it.toDouble() * af).js }.js
        }
        else -> Double.NaN.js
    }
}

private fun jsdiv(a : Any?, b : Any?) : JsAny {
    return when {
        a is Undefined || b is Undefined ||
        (a == null && b == null)
                || ((a as? Number)?.toDouble() == 0.0 && b == null)
                || ((b as? Number)?.toDouble() == 0.0 && a == null)
                || ((a as? CharSequence)?.toString()?.toDoubleOrNull() == 0.0 && b == null)
                || ((b as? CharSequence)?.toString()?.toDoubleOrNull() == 0.0 && a == null) -> Double.NaN.js

        a is Long && b is Long -> when {
            b == 0 -> a / b.toDouble()
            a % b == 0L -> a / b
            else -> a.toDouble() / b
        }.js
        a is Number && b is Number -> (a.toDouble() / b.toDouble()).js
        a == null -> 0L.js
        b == null -> Double.POSITIVE_INFINITY.js

        a is List<*> && b is Number -> {
            a as List<Number>
            val bf = b.toDouble()
            a.fastMap { (it.toDouble() / bf).js }.js
        }

        else -> Double.NaN.js
    }
}

private fun jsmod(a : Any?, b : Any?) : JsAny {
    return when {
        b == null || a == Unit || b == Unit
                || (b as? Number)?.toDouble()?.absoluteValue?.let { it < Double.MIN_VALUE } == true -> Double.NaN.js
        a == null -> 0.0.js
//        a is Long && b is Long -> a % b
        a is Number && b is Number -> (a.toDouble() % b.toDouble()).js
        else -> Double.NaN.js
    }
}


private fun jsinc(v : Any?) : JsAny {
    return when (v) {
        null -> 1L
        is Long -> v + 1
        is Double -> v + 1
        is Number -> v.toDouble() + 1
        else -> Double.NaN
    }.js
}

private fun jsdec(v : Any?) : JsAny {
    return when (v) {
        null -> -1L
        is Long -> v - 1
        is Double -> v - 1
        is Number -> v.toDouble() - 1
        else -> Double.NaN
    }.js
}

private fun jsneg(v : Any?) : JsAny {
    return when (v) {
        null -> (-0).js
        0, 0L -> (-0.0).js
        is Long -> (-v).js
        is Number -> (-v.toDouble()).js
        is List<*> -> {
            v as List<Number>
            v.fastMap { (-it.toDouble()).js }.js
        }
        else -> Double.NaN.js
    }
}

private fun jspos(v : Any?) : JsAny {
    return when (v) {
        null -> 0
        is Long -> +v
        is Number -> +v.toDouble()
        else -> Double.NaN
    }.js
}
