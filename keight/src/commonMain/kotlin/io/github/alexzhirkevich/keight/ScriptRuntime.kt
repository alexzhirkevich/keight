package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.ObjectMap
import io.github.alexzhirkevich.keight.js.ReferenceError
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.interpreter.referenceCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import io.github.alexzhirkevich.keight.js.js
import io.github.alexzhirkevich.keight.js.mapThisArg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex


public enum class VariableType {
    Global, Local, Const
}

public abstract class ScriptRuntime : CoroutineScope {

    @PublishedApi
    internal open val isStrict : Boolean get() = false

    public open val isSuspendAllowed: Boolean get() = true

    internal val lock: Mutex = Mutex()

    @PublishedApi
    internal abstract val thisRef : JsAny

    public abstract fun isEmpty(): Boolean

    public abstract suspend fun delete(property: JsAny?, ignoreConstraints: Boolean = false): Boolean

    public abstract operator fun contains(property: JsAny?): Boolean

    public abstract suspend fun get(property: JsAny?): JsAny?

    public abstract suspend fun set(property: JsAny?, value: JsAny?, type: VariableType?)

    public abstract suspend fun <T> withScope(
        thisRef: JsAny = this.thisRef,
        extraProperties: Map<String, Pair<VariableType, JsAny?>> = emptyMap(),
        isSuspendAllowed: Boolean = this.isSuspendAllowed,
        isFunction: Boolean = false,
        isStrict: Boolean = this.isStrict,
        block: suspend (ScriptRuntime) -> T
    ): T

    public abstract suspend fun <T> useStrict(
        block: suspend (ScriptRuntime) -> T
    ) : T

    /**
     * Restore runtime to its initial state, cancel all running jobs
     * */
    public abstract fun reset()



    public abstract suspend fun isFalse(a: Any?): Boolean

    /**
     * Returns true if [a] and [b] can be compared and false when
     * the comparison result for such types is always false
     **/
    public abstract suspend fun isComparable(a: JsAny?, b: JsAny?): Boolean

    /**
     * Should return negative value if [a] < [b],
     * positive if [a] > b and 0 if [a] equals to [b]
     *
     * Engine can call this function only in case [isComparable] result is true
     *
     * @see [Comparator]
     * */
    public abstract suspend fun compare(a: JsAny?, b: JsAny?): Int

    public abstract suspend fun sum(a: JsAny?, b: JsAny?): JsAny?
    public abstract suspend fun sub(a: JsAny?, b: JsAny?): JsAny?
    public abstract suspend fun mul(a: JsAny?, b: JsAny?): JsAny?
    public abstract suspend fun div(a: JsAny?, b: JsAny?): JsAny?
    public abstract suspend fun mod(a: JsAny?, b: JsAny?): JsAny?

    public abstract suspend fun inc(a: JsAny?): JsAny?
    public abstract suspend fun dec(a: JsAny?): JsAny?

    public abstract suspend fun neg(a: JsAny?): JsAny?
    public abstract suspend fun pos(a: JsAny?): JsAny?

    public abstract suspend fun toNumber(a: JsAny?): Number

}



public suspend fun ScriptRuntime.set(property: JsAny?, value: JsAny?): Unit =
    set(property, value, null)



public abstract class DefaultRuntime : ScriptRuntime() {

    protected val variables: MutableMap<JsAny?, Pair<VariableType?, JsAny?>> = ObjectMap(mutableMapOf())

    override fun contains(property: JsAny?): Boolean {
        return property in variables
    }

    override suspend fun delete(property: JsAny?, ignoreConstraints: Boolean): Boolean {
        val p = variables[property]
        return when {
            ignoreConstraints || (p != null && p.first == null) -> {
                variables.remove(property)
                true
            }

            p == null -> true
            isStrict -> throw SyntaxError("Delete of an unqualified identifier in strict mode.")
            else -> false
        }
    }

    override suspend fun set(property: JsAny?, value: JsAny?, type: VariableType?) {

        val current = variables[property]
        if (
            ((type == VariableType.Const || type == VariableType.Local) && current?.first != null) ||
            (type == VariableType.Global && (current?.first == VariableType.Local || current?.first == VariableType.Const))
        ) {
            throw SyntaxError("Identifier '$property' ($type) is already declared")
        }

        typeCheck(current?.first != VariableType.Const) {
            "Assignment to constant variable ('$property')".js()
        }

        referenceCheck(type != null || !isStrict || contains(property)) {
            "Unresolved reference $property".js()
        }
        variables[property] = (type ?: current?.first) to value
    }

    override suspend fun get(property: JsAny?): JsAny? {
        return when {
            contains(property) -> variables[property]?.second?.get(this)
            else -> Undefined
        }
    }

    final override suspend fun <T> withScope(
        thisRef: JsAny,
        extraProperties: Map<String, Pair<VariableType, JsAny?>>,
        isSuspendAllowed: Boolean,
        isFunction: Boolean,
        isStrict: Boolean,
        block: suspend (ScriptRuntime) -> T
    ): T {
        val child = ScopedRuntime(
            parent = this,
            isFunction = isFunction,
            strict = isStrict,
            mThisRef = thisRef,
            isSuspendAllowed = isSuspendAllowed
        )
        extraProperties.forEach { (n, v) ->
            child.set(n.js(), v.second, v.first)
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
    CoroutineScope by delegate {

    override val thisRef: JsAny get() = mapThisArg(delegate.thisRef, true)
    override val isStrict: Boolean get() = true
    override val isSuspendAllowed: Boolean get() = delegate.isSuspendAllowed
    override fun isEmpty(): Boolean = delegate.isEmpty()

    override suspend fun delete(property: JsAny?, ignoreConstraints: Boolean) : Boolean {
        throw SyntaxError("Delete of an unqualified identifier in strict mode.")
    }

    override fun contains(property: JsAny?): Boolean = delegate.contains(property)
    override suspend fun get(property: JsAny?): JsAny? = delegate.get(property)
    override suspend fun set(property: JsAny?, value: JsAny?, type: VariableType?) {
        if (type == null && !contains(property)) {
            throw ReferenceError("Unresolved reference $property")
        }
        delegate.set(property, value, type)
    }

    override suspend fun <T> withScope(
        thisRef: JsAny,
        extraProperties: Map<String, Pair<VariableType, JsAny?>>,
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


    override suspend fun isFalse(a: Any?): Boolean = delegate.isFalse(a)
    override suspend fun isComparable(a: JsAny?, b: JsAny?): Boolean = delegate.isComparable(a,b)
    override suspend fun compare(a: JsAny?, b: JsAny?): Int = delegate.compare(a,b)
    override suspend fun sum(a: JsAny?, b: JsAny?): JsAny? = delegate.sum(a,b)
    override suspend fun sub(a: JsAny?, b: JsAny?): JsAny? = delegate.sub(a,b)
    override suspend fun mul(a: JsAny?, b: JsAny?): JsAny? = delegate.mul(a,b)
    override suspend fun div(a: JsAny?, b: JsAny?): JsAny? = delegate.div(a,b)
    override suspend fun mod(a: JsAny?, b: JsAny?): JsAny? = delegate.mod(a,b)
    override suspend fun inc(a: JsAny?): JsAny? = delegate.inc(a)
    override suspend fun dec(a: JsAny?): JsAny? = delegate.dec(a)
    override suspend fun neg(a: JsAny?): JsAny? = delegate.neg(a)
    override suspend fun pos(a: JsAny?): JsAny? = delegate.pos(a)
    override suspend fun toNumber(a: JsAny?): Number = delegate.toNumber(a)
}

private class ScopedRuntime(
    val parent: ScriptRuntime,
    val isFunction: Boolean,
    private val strict: Boolean,
    mThisRef: JsAny = parent.thisRef,
    override val isSuspendAllowed: Boolean = parent.isSuspendAllowed
) : DefaultRuntime(),
    CoroutineScope by parent {

    override val isStrict get() = strict || parent.isStrict

    override val thisRef: JsAny = mThisRef
        get() = mapThisArg(field, isStrict)

    override suspend fun get(property: JsAny?): JsAny? {

//        val t = thisRef
//        if (t is JsAny && t.contains(property, this)){
//            return t.get(property, this)
//        }

        return when {
            property in variables -> super.get(property)
            else -> parent.get(property)
        }
    }

    override suspend fun delete(property: JsAny?, ignoreConstraints: Boolean): Boolean {
        val p = variables[property]
        return when {
            p != null && (ignoreConstraints || p.first == null) -> {
                variables.remove(property)
                true
            }

            p == null -> parent.delete(property, ignoreConstraints)
            isStrict -> throw SyntaxError("Delete of an unqualified identifier in strict mode.")
            else -> false
        }
    }

    override fun contains(property: JsAny?): Boolean {
        return super.contains(property) || parent.contains(property)
    }

    override suspend fun set(property: JsAny?, value: JsAny?, type: VariableType?) {
        when {
            type == VariableType.Global -> {
                if (isFunction){
                    super.set(property, value, type)
                } else {
                    closestFunctionScope().set(property, value, type)
                }
            }
            type != null || property in variables -> super.set(property, value, type)
            isStrict && !contains(property) -> throw ReferenceError("Unresolved reference $property")
            else -> parent.set(property, value, type)
        }
    }

    override fun isEmpty(): Boolean {
        return variables.isEmpty() && parent.isEmpty()
    }


    override suspend fun isFalse(a: Any?): Boolean = parent.isFalse(a)
    override suspend fun isComparable(a: JsAny?, b: JsAny?): Boolean = parent.isComparable(a,b)
    override suspend fun compare(a: JsAny?, b: JsAny?): Int = parent.compare(a,b)
    override suspend fun sum(a: JsAny?, b: JsAny?): JsAny? = parent.sum(a,b)
    override suspend fun sub(a: JsAny?, b: JsAny?): JsAny? = parent.sub(a,b)
    override suspend fun mul(a: JsAny?, b: JsAny?): JsAny? = parent.mul(a,b)
    override suspend fun div(a: JsAny?, b: JsAny?): JsAny? = parent.div(a,b)
    override suspend fun mod(a: JsAny?, b: JsAny?): JsAny? = parent.mod(a,b)
    override suspend fun inc(a: JsAny?): JsAny? = parent.inc(a)
    override suspend fun dec(a: JsAny?): JsAny? = parent.dec(a)
    override suspend fun neg(a: JsAny?): JsAny? = parent.neg(a)
    override suspend fun pos(a: JsAny?): JsAny? = parent.pos(a)
    override suspend fun toNumber(a: JsAny?): Number = parent.toNumber(a)
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

