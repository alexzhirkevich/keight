package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.ObjectMap
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.interpreter.referenceCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import io.github.alexzhirkevich.keight.js.js


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

    internal val variables: MutableMap<JsAny?, Pair<VariableType?, JsAny?>> = ObjectMap(mutableMapOf())

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

    internal suspend fun set(property: JsAny?, value: JsAny?, type: VariableType?, current : Pair<VariableType?, JsAny?>?) {
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

    override suspend fun set(property: JsAny?, value: JsAny?, type: VariableType?) {
        set(property, value, type, variables[property])
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
        isIsolated: Boolean,
        isStrict: Boolean,
        block: suspend (ScriptRuntime) -> T
    ): T {
        val child = ScopedRuntime(
            parent = this,
            isIsolated = isIsolated,
            strict = isStrict,
            mThisRef = thisRef,
            isSuspendAllowed = this.isSuspendAllowed && isSuspendAllowed
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


internal tailrec fun ScriptRuntime.findIsolatedScope() : ScriptRuntime {
    return when (this){
        is ScopedRuntime -> if (isIsolated) this else parent.findIsolatedScope()
        is StrictRuntime -> parent.findIsolatedScope()
        else -> this
    }
}

public tailrec fun ScriptRuntime.findRoot() : ScriptRuntime {
    val p = parent

    return when {
        p != null -> p.findRoot()
        else -> this
    }
}

internal tailrec fun ScriptRuntime.findModule() : ModuleRuntime? {

    if (this is ModuleRuntime){
        return this
    }

    return parent?.findModule()
}


