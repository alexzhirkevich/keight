package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.JsObjectImpl
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.js
import kotlinx.coroutines.CoroutineScope

internal class ModuleRuntime(
    override val parent: DefaultRuntime,
) : DefaultRuntime(), CoroutineScope by parent {

    var exports : JsObject = JsObjectImpl()
        private set

    var defaultExport : JsAny? = Undefined

    private var defaults = initDefaults()

    override val thisRef: JsAny? get() = parent.thisRef

    internal var isEvaluated : Boolean = false

    override fun reset() {
        super.reset()
        defaults = initDefaults()
        defaultExport = Undefined
        isEvaluated = false
        exports = JsObjectImpl()
    }

    private fun initDefaults(): MutableMap<JsAny?, Pair<VariableType?, JsAny?>> {
        val defaults = parent.variables.toMutableMap()

        defaults["module".js] = VariableType.Const to Object {
            "exports".js eq exports
        }

        return defaults
    }

    override suspend fun get(property: JsAny?): JsAny? {
        return when {
            super.contains(property) -> super.get(property)
            else -> defaults[property]?.second
        }
    }

    override suspend fun contains(property: JsAny?): Boolean {
        return super.contains(property) || defaults.contains(property)
    }

    override fun isEmpty(): Boolean {
        return variables.isEmpty() && defaults.isEmpty()
    }

    override suspend fun set(property: JsAny?, value: JsAny?, type: VariableType?) {
        super<DefaultRuntime>.set(
            property = property,
            value = value,
            type = type,
            current = if (property in variables)
                variables[property]
            else defaults[property]
        )
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