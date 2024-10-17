package io.github.alexzhirkevich.keight

public interface Script {
    public operator fun invoke(runtime: ScriptRuntime): Any?
}

public fun Expression.asScript(): Script = object : Script {
    override fun invoke(runtime: ScriptRuntime): Any? {
        return this@asScript.invoke(runtime)
    }
}

