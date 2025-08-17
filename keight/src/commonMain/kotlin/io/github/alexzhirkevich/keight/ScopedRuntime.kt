package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.ReferenceError
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.mapThisArg
import kotlinx.coroutines.CoroutineScope


internal class ScopedRuntime(
    override val parent: ScriptRuntime,
    val isIsolated: Boolean,
    private val strict: Boolean = parent.isStrict,
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
                if (isIsolated){
                    super.set(property, value, type)
                } else {
                    findIsolatedScope().set(property, value, type)
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