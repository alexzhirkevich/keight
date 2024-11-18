package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.ObjectMap
import io.github.alexzhirkevich.keight.js.ReferenceError
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.TypeError
import io.github.alexzhirkevich.keight.js.interpreter.referenceError
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

    public abstract suspend fun <T> withScope(
        thisRef: Any = this.thisRef,
        extraProperties: Map<String, Pair<VariableType, Any?>> = emptyMap(),
        isSuspendAllowed: Boolean = this.isSuspendAllowed,
        isFunction: Boolean = false,
        isStrict : Boolean = this.isStrict,
        block: suspend (ScriptRuntime) -> T
    ): T

    public abstract suspend fun <T> useStrict(
        block: suspend (ScriptRuntime) -> T
    ) : T

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
        if (isStrict) {
            throw SyntaxError("Delete of an unqualified identifier in strict mode.")
        } else {
            variables.remove(property)
        }
    }

    override fun set(property: Any?, value: Any?, type: VariableType?) {
        if (type != null && variables[property]?.first != null) {
            throw SyntaxError("Identifier '$property' ($type) is already declared")
        }
        if (type == null && variables[property]?.first == VariableType.Const) {
            throw TypeError("Assignment to constant variable ('$property')")
        }
        if (type == null && isStrict && property !in variables){
            throw ReferenceError("Unresolved reference $property")
        }
        variables[property] = (type ?: variables[property]?.first) to value
    }

    override suspend fun get(property: Any?): Any? {
        return if (contains(property))
            variables[property]?.second?.get(this)
        else Unit
    }

    final override suspend fun <T> withScope(
        thisRef : Any,
        extraProperties: Map<String, Pair<VariableType, Any?>>,
        isSuspendAllowed: Boolean,
        isFunction: Boolean,
        isStrict : Boolean,
        block: suspend (ScriptRuntime) -> T
    ): T {
        val child = ScopedRuntime(
            parent = this,
            isFunction = isFunction,
            strict = isStrict,
            thisRef = thisRef,
            isSuspendAllowed = isSuspendAllowed
        )
        extraProperties.forEach { (n, v) ->
            child.set(n, v.second, v.first)
        }
        return block(child)
    }

    final override suspend fun <T> useStrict(block: suspend (ScriptRuntime) -> T) : T{
        return if (isStrict) {
            // scope is already strict
            block(this)
        } else {
            block(StrictRuntime(this))
        }
    }

    override fun isEmpty(): Boolean {
        return variables.isEmpty()
    }

    override fun reset(){
        variables.clear()
    }
}

private class StrictRuntime(
    val delegate : ScriptRuntime
) : ScriptRuntime(),
    ScriptContext by delegate,
    CoroutineScope by delegate {
    override val thisRef: Any get() = delegate.thisRef
    override val isStrict: Boolean get() = true
    override val isSuspendAllowed: Boolean get() = delegate.isSuspendAllowed
    override fun isEmpty(): Boolean = delegate.isEmpty()
    override fun delete(property: Any?) {
        throw SyntaxError("Delete of an unqualified identifier in strict mode.")
    }
    override fun contains(property: Any?): Boolean = delegate.contains(property)
    override suspend fun get(property: Any?): Any? = delegate.get(property)
    override fun set(property: Any?, value: Any?, type: VariableType?) {
        if (type == null && !contains(property)) {
            throw ReferenceError("Unresolved reference $property")
        }
        delegate.set(property, value, type)
    }

    override suspend fun <T> withScope(
        thisRef: Any,
        extraProperties: Map<String, Pair<VariableType, Any?>>,
        isSuspendAllowed: Boolean,
        isFunction: Boolean,
        isStrict: Boolean,
        block: suspend (ScriptRuntime) -> T
    ): T = delegate.withScope(
        thisRef = thisRef,
        extraProperties = extraProperties,
        isSuspendAllowed = isSuspendAllowed,
        isFunction = isFunction,
        isStrict = true,
        block = block
    )
    override suspend fun <T> useStrict(block: suspend (ScriptRuntime) -> T): T = block(this)
    override fun reset() = delegate.reset()
}

private class ScopedRuntime(
    val parent : ScriptRuntime,
    val isFunction : Boolean = false,
    private val strict : Boolean = false,
    override val thisRef: Any = parent.thisRef,
    override val isSuspendAllowed: Boolean = parent.isSuspendAllowed
) : DefaultRuntime(),
    ScriptContext by parent,
    CoroutineScope by parent {

    override val isStrict get() = strict || parent.isStrict

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
            type == VariableType.Global -> {
                val scope = closestFunctionScope()
                if (scope === this){
                    super.set(property, value, type)
                } else {
                    scope.set(property, value, type)
                }
            }
            type != null || property in variables -> super.set(property, value, type)
            isStrict && !parent.contains(property) -> throw ReferenceError("Unresolved reference $property")
            else -> parent.set(property, value, type)
        }
    }

    override fun isEmpty(): Boolean {
        return variables.isEmpty() && parent.isEmpty()
    }
}

private tailrec fun ScriptRuntime.closestFunctionScope() : ScriptRuntime {
    return when (this){
        is ScopedRuntime -> if (isFunction) this else parent.closestFunctionScope()
        is StrictRuntime -> delegate.closestFunctionScope()
        else -> this
    }
}

internal tailrec fun ScriptRuntime.findRoot() : ScriptRuntime {
    return when (this){
        is ScopedRuntime -> parent.findRoot()
        is StrictRuntime -> delegate.findRoot()
        else -> this
    }
}

