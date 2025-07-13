package io.github.alexzhirkevich.keight.es.ordinary

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.es.abstract.esCanonicalNumericIndexString
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.isString

internal suspend fun JsObject.esOwnPropertyKeys(runtime: ScriptRuntime) {
    val keys = keys(runtime)
    buildList<JsAny>(keys.size) {
        keys.forEach {
            if (it?.isString() == true){
                val n = it.toString().esCanonicalNumericIndexString()
                if (n != null && n.toDouble() >= 0){

                }
            }
            if (it?.isString() == true && it.toString().esCanonicalNumericIndexString() != null){

            }
        }
    }
}


internal fun JsAny.isArrayIndex(){}