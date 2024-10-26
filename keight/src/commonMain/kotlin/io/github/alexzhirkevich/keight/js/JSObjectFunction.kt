package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.expressions.OpConstant

internal class JSObjectFunction : JSFunction(
    name = "Object",
    parameters = emptyList(),
    body = OpConstant(Unit)
) {

    private val assign by func("target", "source") {
        TODO()
    }

    private val create by func("object", "source") {
        TODO()
    }

    private val _entries by func("object") {
        (it.firstOrNull() as? JSObject)?.entries ?: emptyList<String>()
    }

    private val fromEntries by func {
        TODO()
    }

    private val _keys by func("object") {
        (it.firstOrNull() as? JSObject)?.keys ?: emptyList<String>()
    }

    private val _values by func("object") {
        (it.firstOrNull() as? JSObject)?.values ?: emptyList<String>()
    }

    private val groupBy by func("object", "callback") {
        TODO()
    }

    private val defineProperty by func("object", "property", "descriptor") {
        TODO()
    }

    private val defineProperties by func("object", "descriptors") {
        TODO()
    }

    private val getOwnPropertyDescriptor by func("object", "property") {
        TODO()
    }

    private val getOwnPropertyDescriptors by func("object") {
        TODO()
    }

    private val getOwnPropertyNames by func("object") {
        TODO()
    }

    private val getPrototypeOf by func("object") {
        TODO()
    }

    private val preventExtensions by func("object") {
        TODO()
    }

    private val isExtensible by func("object") {
        TODO()
    }

    private val seal by func("object") {
        TODO()
    }

    private val isSealed by func("object") {
        TODO()
    }

    private val freeze by func("object") {
        TODO()
    }

    private val isFrozen by func("object") {
        TODO()
    }

    override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any {
        return if (args.isEmpty()){
            JSObjectImpl()
        } else {
            JSObjectImpl()
        }
    }


    override fun construct(args: List<Expression>, runtime: ScriptRuntime): Any {
        return invoke(args, runtime)
    }
}