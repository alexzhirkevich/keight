package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.expressions.JSErrorFunction
import io.github.alexzhirkevich.keight.js.JSArrayFunction
import io.github.alexzhirkevich.keight.js.JSLangContext
import io.github.alexzhirkevich.keight.js.JSMapFunction
import io.github.alexzhirkevich.keight.js.JSMath
import io.github.alexzhirkevich.keight.js.JSNumberFunction
import io.github.alexzhirkevich.keight.js.JSObject
import io.github.alexzhirkevich.keight.js.JSObjectFunction
import io.github.alexzhirkevich.keight.js.JSPromiseFunction
import io.github.alexzhirkevich.keight.js.JSSetFunction
import io.github.alexzhirkevich.keight.js.JSStringFunction
import io.github.alexzhirkevich.keight.js.JSSymbolFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsConsole
import io.github.alexzhirkevich.keight.js.defaults
import io.github.alexzhirkevich.keight.js.func
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import io.github.alexzhirkevich.keight.js.setupNumberMethods
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
    override var io: ScriptIO = DefaultScriptIO,
) : DefaultRuntime(), ScriptContext by JSLangContext {

    override val coroutineContext: CoroutineContext =
        context + SupervisorJob(context[Job]).also {
            if (!isSuspendAllowed) {
                it.cancel()
            }
        } + CoroutineName("JS coroutine")

    internal lateinit var Number: JSNumberFunction
    internal lateinit var Object: JSObjectFunction
    internal lateinit var Array: JSArrayFunction
    internal lateinit var Map: JSMapFunction
    internal lateinit var Set: JSSetFunction
    internal lateinit var String: JSStringFunction
    internal lateinit var Promise: JSPromiseFunction
    internal lateinit var Symbol: JSSymbolFunction
    internal lateinit var Error: JSErrorFunction

    private var jobsCounter = 1L
    private val jobsMap = mutableMapOf<Long, Job>()

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
        Array = JSArrayFunction()
        Map = JSMapFunction()
        Set = JSSetFunction()
        String = JSStringFunction()
        Promise = JSPromiseFunction()
        Symbol = JSSymbolFunction()
        Error = JSErrorFunction()

        listOf(
            Number, Object, Array, Map, Set, String, Promise, Symbol, Error
        ).fastForEach {
            set(it.name, it, VariableType.Global)
        }

        val globalThis = RuntimeGlobalThis(this)

        set("console", JsConsole(), VariableType.Global)
        set("Math", JSMath(), VariableType.Global)
        set("globalThis", globalThis, VariableType.Global)
        set("this", this, VariableType.Const)
        set("Infinity", Double.POSITIVE_INFINITY, VariableType.Const)
        set("NaN", Double.NaN, VariableType.Const)
        set("undefined", Unit, VariableType.Const)

        globalThis.setupNumberMethods()

        globalThis.regSetTimeout()
        globalThis.regSetInterval()
        globalThis.regClearJob("Timeout")
        globalThis.regClearJob("Interval")
    }

    final override suspend fun get(property: Any?): Any? {
        if (contains(property)) {
            return super.get(property)
        }
        return (super.get("globalThis") as JsAny?)?.get(property, this)
    }

    private fun JSObject.regSetTimeout() = func("setTimeout", "handler", "timeout") {
        registerJob(it){ handler, timeout ->
            delay(timeout)
            handler.invoke(emptyList(), this@func)
        }
    }

    private fun JSObject.regSetInterval() = func("setInterval", "handler", "interval") {
        registerJob(it) { handler, timeout ->
            while (isActive) {
                handler.invoke(emptyList(), this@func)
                delay(timeout)
            }
        }
    }

    private fun JSObject.regClearJob(what : String) = func("clear$what", "id") {
        val id = toNumber(it.getOrNull(0)).toLong()
        jobsMap[id]?.cancel()
    }

    private fun registerJob(
        args : List<Any?>,
        block : suspend CoroutineScope.(Callable, Long) -> Unit
    ) : Long {
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
}

private class RuntimeGlobalThis(
    private val runtime: ScriptRuntime
) : JSObject {

    override val keys: List<String>
        get() = emptyList()
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


    override suspend fun delete(property: Any?, runtime: ScriptRuntime) {
        runtime.delete(property)
    }
}
