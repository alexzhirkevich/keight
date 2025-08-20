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
import kotlinx.coroutines.currentCoroutineContext
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

    private val mModules = mutableMapOf<String, Module>()
    internal val modules : Map<String, Module> = mModules

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
        coroutineContext.cancelChildren()
        jobsMap.clear()
        jobsCounter = 1
        init()
        modules.forEach { it.value.runtime.reset() }
    }

    internal fun addModule(name : String, module: Module) {
        mModules[name] = module
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


        variables["Infinity".js] = null to JsNumberWrapper(Double.POSITIVE_INFINITY)
        variables["NaN".js] = null to JsNumberWrapper(Double.NaN)
        variables["undefined".js] = null to Undefined

        variables["globalThis".js] = null to thisRef
        variables["console".js] = null to JsConsole(::console)
        variables["Math".js] = null to JSMath()
        variables["JSON".js] = null to JSON()

        numberMethods().forEach {
            variables[it.name.js] = null to it
        }

        variables["setTimeout".js] = null to "setTimeout".func("handler", "timeout") {
            findJsRoot().registerJob(it) { handler, timeout ->
                delay(timeout)
                handler.invoke(emptyList(), this@func)
            }
        }
        variables["setInterval".js] = null to "setInterval".func("handler", "interval") {
            findJsRoot().registerJob(it) { handler, timeout ->
                while (currentCoroutineContext().isActive) {
                    delay(timeout)
                    handler.invoke(emptyList(), this@func)
                }
            }
        }
        regClearJob("Timeout")
        regClearJob("Interval")

        variables["eval".js] = null to "eval".func("script" defaults OpArgOmitted) {
            val script = it.argOrElse(0) { return@func Undefined }?.toKotlin(this).toString()
            JSEngine(this@JSRuntime).compile(script).invoke(this)
        }

        variables["require".js] = null to "require".func("module"){
            val module = modules[toString(it[0])]
            module?.invokeIfNeeded()
            module?.runtime?.exports ?: Undefined
        }
    }

    override suspend fun get(property: JsAny?): JsAny? {
        return when (property) {
            "Infinity".js -> Double.POSITIVE_INFINITY.js
            "NaN".js -> Double.NaN.js
            "undefined".js -> Undefined
            else -> super.get(property)?.unpack(this)
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

//        if (a is CharSequence && b is CharSequence){
//            return a.toString().compareTo(b.toString())
//        }

        val pa = a?.toPrimitive(type = Constants.number.js) ?: a
        val pb = b?.toPrimitive(type = Constants.number.js) ?: b

        if (pa is CharSequence && pb is CharSequence){
            return pa.toString().compareTo(pb.toString())
        }

        val na = a.esToNumber(this).toDouble()
        val nb = b.esToNumber(this).toDouble()

        return when {
            na.isNaN() || nb.isNaN() -> 0
            else -> na.compareTo(nb)
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
        return jsmod(a?.numberOrThis(), b?.numberOrThis())
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
    ) : Any = numberOrNull(withMagic) ?: this

    private tailrec suspend fun Any?.numberOrNull(
        withNaNs : Boolean = true,
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
                singleOrNull()?.numberOrNull(withNaNs)
            } else {
                null
            }
        }

        is Wrapper<*> -> value.numberOrNull(withNaNs)
        is JsObject ->  if (withNaNs) {
            toPrimitive(Constants.number.js)?.numberOrNull(withNaNs)
        } else {
            null
        }
        else -> null
    }


    internal suspend fun JsAny.toPrimitive(
        type : JsAny? = Constants.default.js,
        symbolOnly : Boolean = false,
    ) : JsAny? {

        if (this is Undefined) {
            return if (type == Constants.number.js) 0.js else toString().js
        }

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

            val valueOf = get(Constants.valueOf.js, this@JSRuntime)
                ?.callableOrNull()
                ?.call(this, emptyList(), this@JSRuntime)

            if (valueOf != null && valueOf !is JsObject){
                return valueOf
            }

            if (this !is CharSequence) {
                toString(this)
                    .toDoubleOrNull()
                    ?.takeUnless(Double::isNaN)
                    ?.let { return it.js }
            } else {
                this
            }

            typeCheck(valueOf !is JsObject){
                "Cannot convert object to primitive value".js
            }

            return null
        } else {
            null
        }
    }

    private suspend fun registerJob(
        args: List<JsAny?>,
        block: suspend CoroutineScope.(handler : Callable, timeout : Long) -> Unit
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

    private fun regClearJob(what: String) {
        variables["clear$what".js] =  null to "clear$what".func("id") {
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

private tailrec suspend fun JsAny.unpack(runtime: ScriptRuntime) : JsAny? =
    if (this is JsPropertyAccessor)
        get(runtime)?.unpack(runtime)
    else this

internal fun ScriptRuntime.findJsRoot() : JSRuntime = findRoot() as JSRuntime

private fun jssub(a : Any?, b : Any?) : JsAny? {
    return when {
        a is Undefined || b is Undefined -> Double.NaN
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
        b == null
                || a == Undefined
                || b == Undefined
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
