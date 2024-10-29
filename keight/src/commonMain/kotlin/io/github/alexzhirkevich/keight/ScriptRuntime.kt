package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.TypeError
import kotlinx.coroutines.CoroutineScope


public enum class VariableType {
    Global, Local, Const
}

public interface ScriptRuntime : ScriptContext, CoroutineScope {

    public var io : ScriptIO

    public val isSuspendAllowed : Boolean

    public fun isEmpty() : Boolean

    public fun delete(property: Any?)

    public operator fun contains(variable: Any?): Boolean

    public suspend fun get(variable: Any?): Any?

    public fun set(variable: Any?, value: Any?, type: VariableType?)

    public suspend fun withScope(
        extraVariables: Map<String, Pair<VariableType, Any?>> = emptyMap(),
        isSuspendAllowed: Boolean = this.isSuspendAllowed,
        block: suspend (ScriptRuntime) -> Any?
    ): Any?

    /**
     * Restore runtime to its initial state
     * */
    public fun reset()
}

public operator fun ScriptRuntime.set(variable: Any?, value: Any?): Unit =
    set(variable, fromKotlin(value), null)

private class ScopedRuntime(
    val parent : ScriptRuntime,
    override var isSuspendAllowed: Boolean
) : DefaultRuntime(),
    ScriptContext by parent ,
    CoroutineScope by parent {

    override var io: ScriptIO by parent::io

    override fun fromKotlin(a: Any?): Any? {
        return parent.fromKotlin(a)
    }

    override fun toKotlin(a: Any?): Any? {
       return parent.toKotlin(a)
    }

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

    override fun set(variable: Any?, value: Any?, type: VariableType?) {
        when {
            type == VariableType.Global -> parent.set(variable, value, type)
            type != null || variable in variables -> super.set(variable, value, type)
            else -> parent.set(variable, value, type)
        }
    }

    override fun isEmpty(): Boolean {
        return variables.isEmpty() && parent.isEmpty()
    }
}

public abstract class DefaultRuntime : ScriptRuntime {

    protected val variables: MutableMap<Any?, Pair<VariableType, Any?>> = mutableMapOf()

    private val child by lazy {
        ScopedRuntime(this, isSuspendAllowed)
    }

    override val isSuspendAllowed: Boolean
        get() = true

    override fun contains(property: Any?): Boolean {
        return property in variables
    }

    override fun delete(property: Any?) {
        variables.remove(property)
    }

    override fun set(variable: Any?, value: Any?, type: VariableType?) {
        if (type != null && variable in variables) {
            throw SyntaxError("Identifier '$variable' is already declared")
        }
        if (type == null && variables[variable]?.first == VariableType.Const) {
            throw TypeError("Assignment to constant variable ('$variable')")
        }
        variables[variable] = (type ?: variables[variable]?.first ?: VariableType.Global) to value
    }

    override suspend fun get(property: Any?): Any? {
        return if (contains(property))
            variables[property]?.second
        else Unit
    }

    final override suspend fun withScope(
        extraVariables: Map<String, Pair<VariableType, Any?>>,
        isSuspendAllowed: Boolean,
        block: suspend (ScriptRuntime) -> Any?
    ): Any? {
        child.reset()
        child.isSuspendAllowed = isSuspendAllowed
        extraVariables.forEach { (n, v) ->
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

internal tailrec fun ScriptRuntime.findRoot() : ScriptRuntime {
    return if (this is ScopedRuntime) {
        parent.findRoot()
    } else this
}

