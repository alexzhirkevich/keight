package io.github.alexzhirkevich.keight

internal interface Constructor : Callable {

    suspend fun isInstance(obj : Any?, runtime: ScriptRuntime) : Boolean

    suspend fun construct(args: List<Expression>, runtime: ScriptRuntime): Any

    override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
        return construct(args, runtime)
    }
}