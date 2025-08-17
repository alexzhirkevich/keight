package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.js
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import java.io.Reader
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.ScriptEngineFactory
import javax.script.ScriptEngine as JXScriptEngine

internal class KeightScriptEngine(
    private val factory: KeightScriptEngineFactory,
    private var engine : JSEngine<*>
) : JXScriptEngine {

    override fun eval(script: String?, context: ScriptContext?): Any? {
        TODO("Not yet implemented")
    }

    override fun eval(p0: Reader?, context: ScriptContext?): Any {
        TODO("Not yet implemented")
    }

    override fun eval(script: String): Any? {
        val v = runBlocking { engine.evaluate(script) }
        return v
    }

    override fun eval(reader: Reader): Any? {
        return eval(reader.readText())
    }

    override fun eval(p0: String?, p1: Bindings?): Any {
        TODO("Not yet implemented")
    }

    override fun eval(reader: Reader, p1: Bindings): Any {
        return eval(reader.readText(), p1)
    }

    override fun put(name: String?, value: Any?) {
        runBlocking {
            engine.runtime.set(name?.js, fromKotlin(value), VariableType.Global)
        }
    }

    override fun get(name: String?): Any? {
        return runBlocking {
            engine.runtime.get(name?.js)
        }
    }

    override fun getBindings(scope: Int): Bindings {
        val r = if (scope == ScriptContext.GLOBAL_SCOPE)
            factory.globalRuntime
        else engine.runtime

        return KeightBindings(r)
    }

    override fun setBindings(bindings: Bindings, scope: Int) {
        runBlocking {
            if (scope == ScriptContext.GLOBAL_SCOPE) {
                factory.globalRuntime.reset()
                bindings.forEach { (k, v) ->
                    factory.globalRuntime.set(
                        k.js,
                        fromKotlin(v),
                        VariableType.Global
                    )
                }
            } else {
                engine.reset()
                bindings.forEach { (k, v) ->
                    engine.runtime.set(k.js, fromKotlin(v), VariableType.Global)
                }
            }
        }
    }

    override fun createBindings(): Bindings {
        return KeightBindings(JSRuntime(Job()))
    }

    override fun getContext(): ScriptContext {
        return KeightScriptContext(factory, engine.runtime.findJsRoot())
    }

    override fun setContext(context: ScriptContext) {
        engine.reset()
        if (context is KeightScriptContext){
            engine.runtime.findJsRoot().console = context.runtime.console
        } else {
            engine.runtime.findJsRoot().console = ScriptIO(
                { context.writer },
                { context.errorWriter }
            )
        }
        runBlocking {
            context.getBindings(ScriptContext.ENGINE_SCOPE).forEach { (k, v) ->
                engine.runtime.set(k.js, fromKotlin(v), VariableType.Global)
            }
        }
    }

    override fun getFactory(): ScriptEngineFactory {
        return factory
    }
}

