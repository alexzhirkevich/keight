package io.github.alexzhirkevich.keight;

import io.github.alexzhirkevich.keight.js.js
import kotlinx.coroutines.runBlocking
import java.io.Reader
import java.io.Writer
import javax.script.Bindings
import javax.script.ScriptContext

internal class KeightScriptContext(
    private val factory : KeightScriptEngineFactory,
    val runtime: JSRuntime
) : ScriptContext {

    private var mWriter = runtime.console.asWriter(isError = false)
    private var mErrorWriter = runtime.console.asWriter(isError = true)

    init { runtime.console = ScriptIO(::mWriter, ::mErrorWriter) }

    override fun setBindings(bindings: Bindings?, scope: Int) {
        val r = if (scope == ScriptContext.GLOBAL_SCOPE) factory.globalRuntime else runtime
        r.reset()
        runBlocking {
            bindings?.forEach { (k, v) -> r.set(k.js(), runtime.fromKotlin(v), VariableType.Global) }
        }
    }

    override fun getBindings(scope: Int): Bindings {
        return if (scope == ScriptContext.GLOBAL_SCOPE) {
            KeightBindings(factory.globalRuntime)
        } else {
            KeightBindings(runtime)
        }
    }

    override fun setAttribute(name: String?, value: Any?, scope: Int) {
        runBlocking {
            if (scope == ScriptContext.GLOBAL_SCOPE) {
                factory.globalRuntime.set(name?.js(), runtime.fromKotlin(value), VariableType.Global)
            } else {
                runtime.set(name?.js(), runtime.fromKotlin(value), VariableType.Local)
            }
        }
    }

    override fun getAttribute(name: String?, scope: Int): Any? {
        return runBlocking {
            if (scope == ScriptContext.GLOBAL_SCOPE) {
                factory.globalRuntime.get(name?.js())
            } else {
                runtime.get(name?.js())
            }
        }
    }

    override fun getAttribute(name: String?): Any? {
        return runBlocking {
            runtime.get(name?.js())
        }
    }

    override fun removeAttribute(name: String?, scope: Int): Any? {
        return runBlocking {
            if (scope == ScriptContext.GLOBAL_SCOPE) {
                factory.globalRuntime.delete(name?.js())
            } else {
                runtime.delete(name?.js())
            }
        }
    }

    override fun getAttributesScope(name: String?): Int {
        return when (name?.js()) {
            in runtime -> ScriptContext.ENGINE_SCOPE
            in factory.globalRuntime -> ScriptContext.GLOBAL_SCOPE
            else -> 0
        }
    }

    override fun getWriter(): Writer {
        return runtime.console.asWriter(isError = false)
    }

    override fun getErrorWriter(): Writer {
        return runtime.console.asWriter(isError = true)
    }

    override fun setWriter(w: Writer) {
        mWriter = w
    }

    override fun setErrorWriter(w: Writer) {
        mErrorWriter = w
    }

    override fun getReader(): Reader {
        TODO("Not yet implemented")
    }

    override fun setReader(p0: Reader?) {
        TODO("Not yet implemented")
    }

    override fun getScopes(): MutableList<Int> {
        return mutableListOf(ScriptContext.GLOBAL_SCOPE, ScriptContext.ENGINE_SCOPE)
    }
}

