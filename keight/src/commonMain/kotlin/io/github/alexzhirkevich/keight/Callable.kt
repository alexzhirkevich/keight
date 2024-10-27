package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.expressions.asCallable
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import kotlin.jvm.JvmInline

public interface Callable : JsAny {

    public suspend fun apply(args: List<Expression>, runtime: ScriptRuntime) : Any? {
        val a = args.getOrNull(1)?.invoke(runtime)?.let {
            if (it is Iterable<*>){
                it.map(::OpConstant)
            } else {
                listOf(OpConstant(it))
            }
        } ?: emptyList()
        return bind(listOf(args[0]), runtime).invoke(a, runtime)
    }

    public suspend fun bind(args: List<Expression>, runtime: ScriptRuntime) : Callable {
        TODO()
    }

    public suspend operator fun invoke(args: List<Expression>, runtime: ScriptRuntime) : Any?

    override suspend fun get(property: Any?, runtime: ScriptRuntime): Any? {
        return when(property){
            "call" -> this
            "bind" -> Bind(this)
            "apply" -> Apply(this)
            else -> super.get(property, runtime)
        }
    }

    @JvmInline
    private value class Bind(val callable: Callable) : Callable {

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            val callable = args[0].invoke(runtime)?.callableOrNull()
            typeCheck(callable != null){
                "$callable is not a function"
            }
            return Bind(callable)
        }

        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
            return callable.bind(args, runtime)
        }
    }

    @JvmInline
    private value class Apply(val callable: Callable) : Callable {

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            val callable = args[0].invoke(runtime)?.callableOrNull()
            typeCheck(callable != null){
                "$callable is not a function"
            }
            return Apply(callable)
        }

        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
            return callable.apply(args, runtime)
        }
    }
}

internal fun Any.callableOrNull() : Callable? {
    return when(this){
        is Callable -> this
        is Function<*> -> asCallable()
        else -> null
    }
}
