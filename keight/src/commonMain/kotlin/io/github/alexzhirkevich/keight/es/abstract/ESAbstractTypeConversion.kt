package io.github.alexzhirkevich.keight.es.abstract

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.callableOrNull
import io.github.alexzhirkevich.keight.js.Constants
import io.github.alexzhirkevich.keight.js.JSBooleanWrapper
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsNumberObject
import io.github.alexzhirkevich.keight.js.JsNumberWrapper
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.JsStringObject
import io.github.alexzhirkevich.keight.js.JsStringWrapper
import io.github.alexzhirkevich.keight.js.JsSymbol
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.interpreter.NumberFormat
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import io.github.alexzhirkevich.keight.js.js
import io.github.alexzhirkevich.keight.js.listOf

/**
 * 7.1.1
 *
 * [ToPrimitive (input [,preferredType])]((https://tc39.es/ecma262/#sec-toprimitive))
 *
 * The abstract operation ToPrimitive takes argument input (an ECMAScript language value) and
 * optional argument preferredType (string or number) and returns either a normal completion
 * containing an ECMAScript language value or a throw completion. It converts its input argument
 * to a non-Object type. If an object is capable of converting to more than one primitive type,
 * it may use the optional hint preferredType to favour that type.
 *
 * It performs the following steps when called:
 *
 * 1. If input is an Object, then
 *    a. Let exoticToPrim be ? GetMethod(input, %Symbol.toPrimitive%).
 *    b. If exoticToPrim is not undefined, then
 *       i. If preferredType is not present, then
 *          1. Let hint be "default".
 *       ii. Else if preferredType is string, then
 *           1. Let hint be "string".
 *       iii. Else,
 *            1. Assert: preferredType is number.
 *            2. Let hint be "number".
 *       iv. Let result be ? Call(exoticToPrim, input, « hint »).
 *       v. If result is not an Object, return result.
 *       vi. Throw a TypeError exception.
 *    c. If preferredType is not present, let preferredType be number.
 *    d. Return ? OrdinaryToPrimitive(input, preferredType).
 * 2. Return input.
 */
internal tailrec suspend fun JsAny?.esToPrimitive(
    type: String? = Constants.default,
    runtime: ScriptRuntime
) : JsAny? {

    return when  {
        this is JsObject -> {
            val exoticToPrim = get(JsSymbol.toPrimitive, runtime)?.callableOrNull()
            if (exoticToPrim != null){
                val hint = type ?: Constants.default
                val result = exoticToPrim.call(this, hint.js.listOf(), runtime)
                runtime.typeCheck(result !is JsObject){ "Cannot convert object to primitive value".js }
                return result
            }

            return esOrdinaryToPrimitive(type ?: Constants.number, runtime)
        }
        this is Wrapper<*> && value is JsAny -> (value as JsAny).esToPrimitive(type, runtime)
        else -> this
    }
}

/**
 * 7.1.1.1
 *
 * [OrdinaryToPrimitive(O,hint)](https://tc39.es/ecma262/#sec-ordinarytoprimitive)
 *
 * The abstract operation OrdinaryToPrimitive takes arguments O (an Object) and hint (string or
 * number) and returns either a normal completion containing an ECMAScript language value or a
 * throw completion.
 *
 * It performs the following steps when called:
 *
 * 1. If hint is string, then
 *    a. Let methodNames be « "toString", "valueOf" ».
 * 2. Else,
 *    a. Let methodNames be « "valueOf", "toString" ».
 * 3. For each element name of methodNames, do
 *    a. Let method be ? Get(O, name).
 *    b. If IsCallable(method) is true, then
 *       i. Let result be ? Call(method, O).
 *       ii. If result is not an Object, return result.
 * 4. Throw a TypeError exception.
 * */
private suspend fun JsAny.esOrdinaryToPrimitive(hint: String?, runtime: ScriptRuntime) : JsAny? {
    val methods = mutableListOf<JsAny?>(Constants.valueOf.js, Constants.toString.js)

    if (hint == Constants.string)
        methods.reverse()

    for (m in methods) {
        val callable = get(m, runtime)
            ?.callableOrNull() ?: continue
        val res = callable.call(this, emptyList(), runtime)
        if (res !is JsObject){
            return res
        }
    }

    runtime.typeError {
        "Cannot convert object to primitive value".js
    }
}


/**
 * 7.1.4.1.1
 *
 * [StringToNumber (str)](https://tc39.es/ecma262/#sec-stringtonumber)
 *
 * The abstract operation StringToNumber takes argument str (a String) and returns a Number.
 *
 * It performs the following steps when called:
 *
 * 1. Let literal be ParseText(str, StringNumericLiteral).
 * 2. If literal is a List of errors, return NaN.
 * 3. Return the StringNumericValue of literal.
 *
 * */
internal fun CharSequence.esStringToNumber() : Number {
    return if (isBlank()){
        0L
    } else {
        val s = toString()
            .trim()
            .trimStart('\n', '+')
            .trimEnd('\n')
            .lowercase()

        NumberFormat.entries.forEach {
            val p = "0${it.prefix}"
            if (it.prefix != null && s.startsWith(p)) {
                return s.removePrefix(p).toLong(it.radix)
            }
        }

        s.toLongOrNull() ?: s.toDoubleOrNull() ?: Double.NaN
    }
}

/**
 * 7.1.4
 *
 * [ToNumber (argument)](https://tc39.es/ecma262/#sec-tonumber)
 *
 * The abstract operation ToNumber takes argument argument (an ECMAScript language value) and
 * returns either a normal completion containing a Number or a throw completion. It converts
 * argument to a value of type Number.
 *
 * It performs the following steps when called:
 *
 * 1. If argument is a Number, return argument.
 * 2. If argument is either a Symbol or a BigInt, throw a TypeError exception.
 * 3. If argument is undefined, return NaN.
 * 4. If argument is either null or false, return +0𝔽.
 * 5. If argument is true, return 1𝔽.
 * 6. If argument is a String, return StringToNumber(argument).
 * 7. Assert: argument is an Object.
 * 8. Let primValue be ? ToPrimitive(argument, number).
 * 9. Assert: primValue is not an Object.
 * 10. Return ? ToNumber(primValue).
 * */
internal tailrec suspend fun Any?.esToNumber(runtime: ScriptRuntime) : Number {
    return when(this) {
        is Wrapper<*> -> value.esToNumber(runtime)
        is Byte -> toLong()
        is UByte -> toLong()
        is Short -> toLong()
        is UShort -> toLong()
        is Int -> toLong()
        is UInt -> toLong()
        is ULong -> toLong()
        is Float -> toDouble()
        is Long -> this
        is Double -> this
        is JsSymbol -> runtime.typeError { "Symbol cannot be converted to a number".js }
        is Unit, is Undefined -> return Double.NaN
        null, false -> +0L
        true -> 1L
        is CharSequence -> esStringToNumber()
        is JsAny -> esToPrimitive(Constants.number, runtime).esToNumber(runtime)
        else -> Double.NaN
    }
}

/**
 * 7.1.17
 *
 * [ToString (argument)](https://tc39.es/ecma262/multipage/abstract-operations.html#sec-tostring)
 *
 * The abstract operation ToString takes argument argument (an ECMAScript language value) and
 * returns either a normal completion containing a String or a throw completion. It converts
 * argument to a value of type String.
 *
 * It performs the following steps when called:
 *
 * 1. If argument is a String, return argument.
 * 2. If argument is a Symbol, throw a TypeError exception.
 * 3. If argument is undefined, return "undefined".
 * 4. If argument is null, return "null".
 * 5. If argument is true, return "true".
 * 6. If argument is false, return "false".
 * 7. If argument is a Number, return Number::toString(argument, 10).
 * 8. If argument is a BigInt, return BigInt::toString(argument, 10).
 * 9. Assert: argument is an Object.
 * 10. Let primValue be ? ToPrimitive(argument, string).
 * 11. Assert: primValue is not an Object.
 * 12. Return ? ToString(primValue).
 * */
internal tailrec suspend fun Any?.esToString(runtime: ScriptRuntime) : String {
    return when(this){
        is Wrapper<*> -> value.esToString(runtime)
        is CharSequence -> toString()
        is JsSymbol -> runtime.typeError { "Symbol cannot be converted to a string".js }
        Unit -> "undefined"
        null, true, false, is Number -> toString()
        is JsObject -> esToPrimitive(Constants.string, runtime).esToString(runtime)
        else -> toString()
    }
}

internal suspend fun JsAny?.esToObject(runtime: ScriptRuntime) : JsObject {
    return when(this){
        is JsNumberWrapper -> JsNumberObject(this)
        is JsStringWrapper -> JsStringObject(this)
        is JsSymbol -> TODO("symbol object")
        is JSBooleanWrapper -> TODO("boolean object")
        is JsObject -> this
        else -> runtime.typeError {
            "$this is not an object".js
        }
    }
}

/**
 * 7.1.19
 *
 * [ToPropertyKey ( argument )](https://tc39.es/ecma262/#sec-topropertykey)
 *
 * The abstract operation ToPropertyKey takes argument argument (an ECMAScript language value)
 * and returns either a normal completion containing a property key or a throw completion.
 * It converts argument to a value that can be used as a property key.
 * It performs the following steps when called:
 *
 * 1. Let key be ? ToPrimitive(argument, string).
 * 2. If key is a Symbol, then
 *    a. Return key.
 * 3. Return ! ToString(key).
 * */
internal suspend fun JsAny.esToPropertyKey(runtime: ScriptRuntime) : JsAny {
    val key = esToPrimitive(
        type = Constants.string,
        runtime = runtime
    )

    if (key is JsSymbol){
        return key
    }

    return runtime.toString(key).js
}

/**
 * 7.1.21
 * []CanonicalNumericIndexString ( argument )](https://tc39.es/ecma262/#sec-canonicalnumericindexstring)
 * The abstract operation CanonicalNumericIndexString takes argument argument (a String) and returns a Number or undefined. If argument is either "-0" or exactly matches ToString(n) for some Number value n, it returns the respective Number value. Otherwise, it returns undefined. It performs the following steps when called:
 *
 * 1. If argument is "-0", return -0𝔽.
 * 2. Let n be ! ToNumber(argument).
 * 3. If ! ToString(n) is argument, return n.
 * 4. Return undefined.
 * A canonical numeric string is any String for which the CanonicalNumericIndexString abstract operation does not return undefined.
 *
 * 7.1.22 ToInde
 * */
internal fun String.esCanonicalNumericIndexString(): Number? {
    if (this == "-0"){
        return -0.0
    }
    val n = esStringToNumber()

    if (n.toString() == this){
        return n
    }

    return null
}