package io.github.alexzhirkevich.keight.es.fundamental

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.es.abstract.esToObject
import io.github.alexzhirkevich.keight.es.abstract.esToString
import io.github.alexzhirkevich.keight.js.JsObject

internal suspend fun JsObject.esGetOwnPropertyKeys(type : String, runtime: ScriptRuntime){
    val obj = esToObject(runtime)
}