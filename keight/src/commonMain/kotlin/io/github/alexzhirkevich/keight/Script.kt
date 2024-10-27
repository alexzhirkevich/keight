package io.github.alexzhirkevich.keight

public interface Script {
    public suspend operator fun invoke(runtime: ScriptRuntime): Any?
}

public fun Expression.asScript(): Script = object : Script {
    override suspend fun invoke(runtime: ScriptRuntime): Any? {
        return this@asScript.invoke(runtime)
    }
}

