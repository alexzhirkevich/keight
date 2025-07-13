package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsObject
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext


public enum class VariableType {
    Global, Local, Const
}

public interface ScriptRuntime : CoroutineScope {

    public val isStrict : Boolean get() = false

    public val isSuspendAllowed: Boolean get() = true

    public val thisRef: JsAny?
    
    public val parent : ScriptRuntime?

    public fun isEmpty(): Boolean

    public suspend fun delete(property: JsAny?, ignoreConstraints: Boolean = false): Boolean

    public suspend fun contains(property: JsAny?): Boolean

    public suspend fun get(property: JsAny?): JsAny?

    public suspend fun set(property: JsAny?, value: JsAny?, type: VariableType?)

    public suspend fun <T> withScope(
        thisRef: JsAny? = this.thisRef,
        extraProperties: Map<String, Pair<VariableType, JsAny?>> = emptyMap(),
        isSuspendAllowed: Boolean = this.isSuspendAllowed,
        isFunction: Boolean = false,
        isStrict: Boolean = this.isStrict,
        block: suspend (ScriptRuntime) -> T
    ): T

    public suspend fun <T> useStrict(
        block: suspend (ScriptRuntime) -> T
    ) : T

    /**
     * Restore runtime to its initial state, cancel all running jobs
     * */
    public fun reset()

    public suspend fun isFalse(a: Any?): Boolean

    public fun makeObject(properties : Map<JsAny?, JsAny?>) : JsObject

    public suspend fun referenceError(message : JsAny?) : Nothing

    public suspend fun typeError(message : JsAny?) : Nothing

    public fun fromKotlin(value: Any): JsAny

    /**
     * Returns true if [a] and [b] can be compared and false when
     * the comparison result for such types is always false
     **/
    public suspend fun isComparable(a: JsAny?, b: JsAny?): Boolean

    /**
     * Should return negative value if [a] < [b],
     * positive if [a] > b and 0 if [a] equals to [b]
     *
     * Engine can call this function only in case [isComparable] result is true
     *
     * @see [Comparator]
     * */
    public suspend fun compare(a: JsAny?, b: JsAny?): Int

    public suspend fun sum(a: JsAny?, b: JsAny?): JsAny?
    public suspend fun sub(a: JsAny?, b: JsAny?): JsAny?
    public suspend fun mul(a: JsAny?, b: JsAny?): JsAny?
    public suspend fun div(a: JsAny?, b: JsAny?): JsAny?
    public suspend fun mod(a: JsAny?, b: JsAny?): JsAny?

    public suspend fun inc(a: JsAny?): JsAny?
    public suspend fun dec(a: JsAny?): JsAny?

    public suspend fun neg(a: JsAny?): JsAny?
    public suspend fun pos(a: JsAny?): JsAny?

    public suspend fun toNumber(value: JsAny?): Number
    public suspend fun toString(value: JsAny?): String

}

public suspend fun ScriptRuntime.set(property: JsAny?, value: JsAny?): Unit =
    set(property, value, null)

public fun ScriptRuntime.findRoot() : ScriptRuntime {
    return parent?.findRoot() ?: this
}

public fun <T> ScriptRuntime.runSync(block : suspend ScriptRuntime.() -> T) : T {
    check(!isSuspendAllowed) {
        "Synchronous invocation is only available in runtimes with suspending calls disabled. See ScriptRuntime.isSuspendAllowed"
    }
    val res = try {
        @Suppress("UNCHECKED_CAST")
        (block as Function2<ScriptRuntime, Continuation<*>, T>)
            .invoke(this, EmptyContinuation(coroutineContext))
    } catch (t : ClassCastException){
        throw Exception("Synchronous invocation is not compatible with your version of kotlinx.coroutines. Please report this issue", t)
    }

    check(res !== SUSPENDED) {
        "Runtime with disallowed suspension was suspended. That should never happen. Please report this issue"
    }

    return res
}

private val SUSPENDED : Any get()  =  kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

@JvmInline
private value class EmptyContinuation(override val context: CoroutineContext) : Continuation<Any?> {
    override fun resumeWith(result: Result<Any?>) {}
}


