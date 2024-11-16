package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.ObjectMap
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.TypeError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex


public enum class VariableType {
    Global, Local, Const
}

public abstract class ScriptRuntime : ScriptContext, CoroutineScope {

    internal open val isStrict : Boolean get() = false

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
        isFunction: Boolean = false,
        isStrict : Boolean = false,
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

    protected val variables: MutableMap<Any?, Pair<VariableType?, Any?>> = ObjectMap(mutableMapOf())

    override fun contains(property: Any?): Boolean {
        return property in variables
    }

    override fun delete(property: Any?) {
        variables.remove(property)
    }

    override fun set(property: Any?, value: Any?, type: VariableType?) {
        if (type != null && variables[property]?.first != null) {
            throw SyntaxError("Identifier '$property' ($type) is already declared")
        }
        if (type == null && variables[property]?.first == VariableType.Const) {
            throw TypeError("Assignment to constant variable ('$property')")
        }
        variables[property] = (type ?: variables[property]?.first) to value
    }

    override suspend fun get(property: Any?): Any? {
        return if (contains(property))
            variables[property]?.second?.get()
        else Unit
    }

    final override suspend fun withScope(
        thisRef : Any,
        extraProperties: Map<String, Pair<VariableType, Any?>>,
        isSuspendAllowed: Boolean,
        isFunction: Boolean,
        isStrict : Boolean,
        block: suspend (ScriptRuntime) -> Any?
    ): Any? {
        val child = ScopedRuntime(
            parent = this,
            isFunction = isFunction,
            isStrict = isStrict,
            thisRef = thisRef,
            isSuspendAllowed = isSuspendAllowed
        )
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
    val isFunction : Boolean = false,
    isStrict : Boolean = false,
    override val thisRef: Any = parent.thisRef,
    override val isSuspendAllowed: Boolean = parent.isSuspendAllowed
) : DefaultRuntime(),
    ScriptContext by parent,
    CoroutineScope by parent {

    override val isStrict = parent.isStrict || isStrict

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
            type == VariableType.Global && !isFunction -> parent.set(property, value, type)
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

