package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.expressions.JSErrorFunction
import io.github.alexzhirkevich.keight.expressions.JSReferenceErrorFunction
import io.github.alexzhirkevich.keight.expressions.JSTypeErrorFunction
import io.github.alexzhirkevich.keight.js.JSArrayFunction
import io.github.alexzhirkevich.keight.js.JSDateFunction
import io.github.alexzhirkevich.keight.js.JSFunctionFunction
import io.github.alexzhirkevich.keight.js.JSLangContext
import io.github.alexzhirkevich.keight.js.JSMapFunction
import io.github.alexzhirkevich.keight.js.JSMath
import io.github.alexzhirkevich.keight.js.JSNumberFunction
import io.github.alexzhirkevich.keight.js.JSON
import io.github.alexzhirkevich.keight.js.JSObject
import io.github.alexzhirkevich.keight.js.JSObjectFunction
import io.github.alexzhirkevich.keight.js.JSPromiseFunction
import io.github.alexzhirkevich.keight.js.JSPropertyDescriptor
import io.github.alexzhirkevich.keight.js.JSSetFunction
import io.github.alexzhirkevich.keight.js.JSStringFunction
import io.github.alexzhirkevich.keight.js.JSSymbolFunction
import io.github.alexzhirkevich.keight.js.JsConsole
import io.github.alexzhirkevich.keight.js.JsNumberWrapper
import io.github.alexzhirkevich.keight.js.OpArgOmitted
import io.github.alexzhirkevich.keight.js.argOrElse
import io.github.alexzhirkevich.keight.js.defaults
import io.github.alexzhirkevich.keight.js.func
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
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

public open class JSRuntime(
    context: CoroutineContext,
    override val isSuspendAllowed: Boolean = true,
    override val isStrict: Boolean = false,
    public var console: Console = DefaultConsole,
) : DefaultRuntime(), ScriptContext by JSLangContext {

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

        numberMethods().forEach {
            thisRef.set(it.name, it, this)
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
            this
        )
    }

    final override suspend fun get(property: Any?): Any? {
        return if (contains(property)) {
            super.get(property)
        } else thisRef.get(property, this)
    }

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

    private fun JSObject.regSetTimeout() = this.set(
        "setTimeout",
        "setTimeout".func("handler", "timeout") {
            (findRoot() as JSRuntime).registerJob(it) { handler, timeout ->
                delay(timeout)
                handler.invoke(emptyList(), this@func)
            }
        },
        this@JSRuntime
    )

    private fun JSObject.regSetInterval() = this.set(
        "setInterval",
        "setInterval".func("handler", "interval") {
            (findRoot() as JSRuntime).registerJob(it) { handler, timeout ->
                while (isActive) {
                    handler.invoke(emptyList(), this@func)
                    delay(timeout)
                }
            }
        },
        this@JSRuntime
    )

    private fun JSObject.regClearJob(what: String) = this.set(
        "clear$what",
        "clear$what".func("id") {
            val id = toNumber(it.getOrNull(0)).toLong()
            jobsMap[id]?.cancel()
        }, this@JSRuntime
    )
}

private class RuntimeObject(val thisRef: () -> ScriptRuntime) : JSObject {

    override val values: List<Any?>
        get() = emptyList()
    override val entries: List<List<Any?>>
        get() = emptyList()

    override suspend fun get(property: Any?, runtime: ScriptRuntime): Any? {
        return if (thisRef().contains(property)){
            thisRef().get(property)?.get()
        } else {
            super.get(property, thisRef())
        }
    }

    override fun set(
        property: Any?,
        value: Any?,
        runtime: ScriptRuntime,
        enumerable: Boolean?,
        configurable: Boolean?,
        writable: Boolean?
    ) {
        thisRef().set(property, value, null)
    }

    override fun descriptor(property: Any?): JSPropertyDescriptor? {
        return null
    }

    override suspend fun contains(property: Any?, runtime: ScriptRuntime): Boolean {
        return thisRef().contains(property)
    }


    override suspend fun delete(property: Any?, runtime: ScriptRuntime): Boolean {
        thisRef().delete(property)
        return true
    }
}

internal inline fun <reified T> ScriptRuntime.thisRef() : T {
    return thisRef as T
}

