package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.ReferenceError
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.mapThisArg
import kotlinx.coroutines.CoroutineScope

internal class StrictRuntime(
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
        isIsolated: Boolean,
        isStrict: Boolean,
        block: suspend (ScriptRuntime) -> T
    ): T = parent.withScope(
        thisRef = thisRef,
        extraProperties = extraProperties,
        isSuspendAllowed = isSuspendAllowed,
        isIsolated = isIsolated,
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