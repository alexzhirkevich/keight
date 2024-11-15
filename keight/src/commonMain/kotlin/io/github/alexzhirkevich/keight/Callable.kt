package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.expressions.asCallable
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import kotlin.jvm.JvmInline

public interface Callable : JsAny {

    public suspend fun call(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime) : Any? {
        return bind(thisArg, emptyList(), runtime).invoke(args, runtime)
    }

    public suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime) : Callable

    public suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime) : Any?

    override suspend fun get(property: Any?, runtime: ScriptRuntime): Any? {
        return when(property){
            "call" -> Call(this)
            "bind" -> Bind(this)
            "apply" -> Apply(this)
            else -> super.get(property, runtime)
        }
    }

    @JvmInline
    private value class Call(val thisRef: Callable) : Callable {

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            val callable = thisArg?.callableOrNull()
            runtime.typeCheck(callable != null){
                "$callable is not a function"
            }
            return Call(callable)
        }

        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            return thisRef.call(args.getOrNull(0), args.drop(1), runtime)
        }
    }

    @JvmInline
    private value class Bind(val thisRef: Callable) : Callable {

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            val callable = thisArg?.callableOrNull()
            runtime.typeCheck(callable != null){
                "$callable is not a function"
            }
            return Bind(callable)
        }

        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            return thisRef.bind(args.getOrNull(0), args.drop(1), runtime)
        }
    }

    @JvmInline
    private value class Apply(val thisRef: Callable) : Callable {

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            val callable = thisArg?.callableOrNull()
            runtime.typeCheck(callable != null){
                "$callable is not a function"
            }
            return Apply(callable)
        }

        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            return thisRef.call(args.getOrNull(0), (args.getOrNull(1) as? List<*>).orEmpty(), runtime)
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
