package io.github.alexzhirkevich.keight

import java.io.Reader
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.ScriptEngineFactory
import javax.script.ScriptEngine as JXScriptEngine

internal class KeightScriptEngine(
    private val factory: KeightScriptEngineFactory,
    private var engine : ScriptEngine
) : JXScriptEngine {

    override fun eval(script: String?, context: ScriptContext?): Any? {
        TODO("Not yet implemented")
    }

    override fun eval(p0: Reader?, context: ScriptContext?): Any {
        TODO("Not yet implemented")
    }

    override fun eval(script: String): Any? {
        val v =  engine.evaluate(script)
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
        engine.runtime.set(name, value, VariableType.Global)
    }

    override fun get(name: String?): Any? {
        return engine.runtime[name]
    }

    override fun getBindings(scope: Int): Bindings {
        val r = if (scope == ScriptContext.GLOBAL_SCOPE)
            factory.globalRuntime
        else engine.runtime

        return KeightBindings(r)
    }

    override fun setBindings(bindings: Bindings, scope: Int) {
        if (scope == ScriptContext.GLOBAL_SCOPE) {
            factory.globalRuntime.reset()
            bindings.forEach { (k, v) ->
                factory.globalRuntime.set(k, v, VariableType.Global)
            }
        } else {
            engine.reset()
            bindings.forEach { (k, v) ->
                engine.runtime.set(k, v, VariableType.Global)
            }
        }
    }

    override fun createBindings(): Bindings {
        return KeightBindings(JSRuntime())
    }

    override fun getContext(): ScriptContext {
        return KeightScriptContext(factory, engine.runtime)
    }

    override fun setContext(context: ScriptContext) {
        engine.reset()
        if (context is KeightScriptContext){
            engine.runtime.io = context.runtime.io
        } else {
            engine.runtime.io = ScriptIO(
                { context.writer },
                { context.errorWriter }
            )
        }
        context.getBindings(ScriptContext.ENGINE_SCOPE).forEach { (k, v) ->
            engine.runtime.set(k, v, VariableType.Global)
        }
    }

    override fun getFactory(): ScriptEngineFactory {
        return factory
    }
}

