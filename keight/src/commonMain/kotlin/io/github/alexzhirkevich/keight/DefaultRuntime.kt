package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.ObjectMap
import io.github.alexzhirkevich.keight.js.ReferenceError
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.interpreter.referenceCheck
import io.github.alexzhirkevich.keight.js.interpreter.referenceError
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import io.github.alexzhirkevich.keight.js.js
import io.github.alexzhirkevich.keight.js.mapThisArg
import kotlinx.coroutines.CoroutineScope


internal suspend inline fun ScriptRuntime.requireThisRef(caller : String? = null) : JsAny {
    val t = thisRef
    typeCheck(t != null && t !is Undefined) {
        "${caller ?: "function"} called on null or undefined".js
    }
    return t
}

internal suspend inline fun <reified T > ScriptRuntime.thisRef() : T {
    return try {
        thisRef as T
    } catch (t: ClassCastException){
        typeError("Cannot convert $thisRef to ${T::class}".js)
    }
}

public abstract class DefaultRuntime : ScriptRuntime {


    protected val variables: MutableMap<JsAny?, Pair<VariableType?, JsAny?>> = ObjectMap(mutableMapOf())

    override suspend fun contains(property: JsAny?): Boolean {
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
            "Assignment to constant variable ('$property')".js
        }

        referenceCheck(type != null || !isStrict || contains(property)) {
            "Unresolved reference $property".js
        }
        variables[property] = (type ?: current?.first) to value
    }

    override suspend fun get(property: JsAny?): JsAny? {
        return when {
            contains(property) -> variables[property]?.second//?.get(this)
            else -> Undefined
        }
    }

    final override suspend fun <T> withScope(
        thisRef: JsAny?,
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
            child.set(n.js, v.second, v.first)
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
    override val parent : ScriptRuntime
) : ScriptRuntime,
    CoroutineScope by parent {

    override val thisRef: JsAny? get() = mapThisArg(parent.thisRef, true)
    override val isStrict: Boolean get() = true
    override val isSuspendAllowed: Boolean get() = parent.isSuspendAllowed
    override fun isEmpty(): Boolean = parent.isEmpty()

    override suspend fun delete(property: JsAny?, ignoreConstraints: Boolean) : Boolean {
        throw SyntaxError("Delete of an unqualified identifier in strict mode.")
    }

    override suspend fun contains(property: JsAny?): Boolean = parent.contains(property)
    override suspend fun get(property: JsAny?): JsAny? = parent.get(property)
    override suspend fun set(property: JsAny?, value: JsAny?, type: VariableType?) {
        if (type == null && !contains(property)) {
            throw ReferenceError("Unresolved reference $property")
        }
        parent.set(property, value, type)
    }

    override suspend fun <T> withScope(
        thisRef: JsAny?,
        extraProperties: Map<String, Pair<VariableType, JsAny?>>,
        isSuspendAllowed: Boolean,
        isFunction: Boolean,
        isStrict: Boolean,
        block: suspend (ScriptRuntime) -> T
    ): T = parent.withScope(
        thisRef = thisRef,
        extraProperties = extraProperties,
        isSuspendAllowed = isSuspendAllowed,
        isFunction = isFunction,
        isStrict = true,
        block = block
    )
    override suspend fun <T> useStrict(block: suspend (ScriptRuntime) -> T): T = block(this)
    override fun reset() = parent.reset()


    override fun fromKotlin(value: Any): JsAny = parent.fromKotlin(value)
    override suspend fun isFalse(a: Any?): Boolean = parent.isFalse(a)
    override fun makeObject(properties: Map<JsAny?, JsAny?>): JsObject = parent.makeObject(properties)
    override suspend fun referenceError(message: JsAny?): Nothing = parent.referenceError(message)
    override suspend fun typeError(message: JsAny?): Nothing = parent.typeError(message)
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
    override suspend fun toNumber(value: JsAny?): Number = parent.toNumber(value)
    override suspend fun toString(value: JsAny?): String = parent.toString(value)
}

private class ScopedRuntime(
    override val parent: ScriptRuntime,
    val isFunction: Boolean,
    private val strict: Boolean,
    mThisRef: JsAny? = parent.thisRef,
    override val isSuspendAllowed: Boolean = parent.isSuspendAllowed
) : DefaultRuntime(),
    CoroutineScope by parent {

    override val isStrict get() = strict || parent.isStrict

    override val thisRef: JsAny? = mThisRef
        get() = mapThisArg(field, isStrict)

    override suspend fun get(property: JsAny?): JsAny? {
        return when {
            super.contains(property)-> super.get(property)
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

    override suspend fun contains(property: JsAny?): Boolean {
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

    override fun makeObject(properties: Map<JsAny?, JsAny?>): JsObject = parent.makeObject(properties)
    override suspend fun referenceError(message: JsAny?): Nothing = parent.referenceError(message)
    override suspend fun typeError(message: JsAny?): Nothing = parent.typeError(message)
    override fun fromKotlin(value: Any): JsAny = parent.fromKotlin(value)
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
    override suspend fun toNumber(value: JsAny?): Number = parent.toNumber(value)
    override suspend fun toString(value: JsAny?): String = parent.toString(value)
}

private tailrec fun ScriptRuntime.closestFunctionScope() : ScriptRuntime {
    return when (this){
        is ScopedRuntime -> if (isFunction) this else parent.closestFunctionScope()
        is StrictRuntime -> parent.closestFunctionScope()
        else -> this
    }
}

public tailrec fun ScriptRuntime.findRoot() : ScriptRuntime {
    return when (this){
        is ScopedRuntime -> parent.findRoot()
        is StrictRuntime -> parent.findRoot()
        else -> this
    }
}

