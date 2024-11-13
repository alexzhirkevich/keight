package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.TypeError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex


public enum class VariableType {
    Global, Local, Const
}

public abstract class ScriptRuntime : ScriptContext, CoroutineScope {

    public open val isSuspendAllowed: Boolean get() = true

    internal val lock: Mutex = Mutex()

    internal abstract val thisRef : Any

    public abstract fun isEmpty(): Boolean

    public abstract fun delete(property: Any?)

    public abstract operator fun contains(property: Any?): Boolean

    public abstract suspend fun get(property: Any?): Any?

    public abstract fun set(property: Any?, value: Any?, type: VariableType?)

    public abstract suspend fun withScope(
        thisRef: Any = this.thisRef,
        extraProperties: Map<String, Pair<VariableType, Any?>> = emptyMap(),
        isSuspendAllowed: Boolean = this.isSuspendAllowed,
        block: suspend (ScriptRuntime) -> Any?
    ): Any?

    /**
     * Restore runtime to its initial state, cancel all running jobs
     * */
    public abstract fun reset()
}



public operator fun ScriptRuntime.set(property: Any?, value: Any?): Unit =
    set(property, fromKotlin(value), null)



public abstract class DefaultRuntime : ScriptRuntime() {

    protected val variables: MutableMap<Any?, Pair<VariableType, Any?>> = mutableMapOf()

    override fun contains(property: Any?): Boolean {
        return property in variables
    }

    override fun delete(property: Any?) {
        variables.remove(property)
    }

    override fun set(property: Any?, value: Any?, type: VariableType?) {
        if (type != null && property in variables) {
            throw SyntaxError("Identifier '$property' is already declared")
        }
        if (type == null && variables[property]?.first == VariableType.Const) {
            throw TypeError("Assignment to constant variable ('$property')")
        }
        variables[property] = (type ?: variables[property]?.first ?: VariableType.Global) to value
    }

    override suspend fun get(property: Any?): Any? {
        return if (contains(property))
            variables[property]?.second
        else Unit
    }

    final override suspend fun withScope(
        thisRef : Any,
        extraProperties: Map<String, Pair<VariableType, Any?>>,
        isSuspendAllowed: Boolean,
        block: suspend (ScriptRuntime) -> Any?
    ): Any? {
        val child = ScopedRuntime(this, thisRef, isSuspendAllowed)
        extraProperties.forEach { (n, v) ->
            child.set(n, v.second, v.first)
        }
        return block(child)
    }

    override fun isEmpty(): Boolean {
        return variables.isEmpty()
    }

    override fun reset(){
        variables.clear()
    }
}



private class ScopedRuntime(
    val parent : ScriptRuntime,
    override val thisRef: Any = parent.thisRef,
    override val isSuspendAllowed: Boolean = parent.isSuspendAllowed
) : DefaultRuntime(),
    ScriptContext by parent,
    CoroutineScope by parent {

    override suspend fun get(property: Any?): Any? {
        return if (property in variables) {
            super.get(property)
        } else {
            parent.get(property)
        }
    }

    override fun contains(property: Any?): Boolean {
        return super.contains(property) || parent.contains(property)
    }

    override fun set(property: Any?, value: Any?, type: VariableType?) {
        when {
            type == VariableType.Global -> parent.set(property, value, type)
            type != null || property in variables -> super.set(property, value, type)
            else -> parent.set(property, value, type)
        }
    }

    override fun isEmpty(): Boolean {
        return variables.isEmpty() && parent.isEmpty()
    }
}



internal tailrec fun ScriptRuntime.findRoot() : ScriptRuntime {
    return if (this is ScopedRuntime) {
        parent.findRoot()
    } else this
}

