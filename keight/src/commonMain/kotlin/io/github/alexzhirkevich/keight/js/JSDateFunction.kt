package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.js.interpreter.syntaxCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.nanoseconds

private val ScriptRuntime.thisDate
    get() = thisDateWrapper.value

private val ScriptRuntime.thisDateWrapper
    get() = thisRef as JSDateWrapper

internal class JSDateFunction : JSFunction(
    name = "Date",
    prototype = Object {
        "getDate".js().func { thisDate.dayOfMonth.js() }
        "getDay".js().func { (thisDate.dayOfWeek.isoDayNumber % 7).js() }
        "getFullYear".js().func { thisDate.year.js() }
        "getYear".js().func { thisDate.year.js() }
        "getHours".js().func { thisDate.hour.js() }
        "getMilliseconds".js().func { thisDate.nanosecond.nanoseconds.inWholeMilliseconds.js() }
        "getMinutes".js().func { thisDate.minute.js() }
        "getMonth".js().func { thisDate.monthNumber.js() }
        "getSeconds".js().func { thisDate.second.js() }
        "getTime".js().func { thisDateWrapper.toInstant().toEpochMilliseconds().js() }
        "getTimeZoneOffset".js().func {
            (thisDateWrapper.timeZone.offsetAt(thisDateWrapper.toInstant()).totalSeconds / 60).js()
        }
        "getUTCDate".js().func { thisDateWrapper.utc().dayOfMonth.js() }
        "getUTCDay".js().func { (thisDateWrapper.utc().dayOfWeek.isoDayNumber % 7).js() }
        "getUTCFullYear".js().func { thisDateWrapper.utc().year.js() }
        "getUTCHours".js().func { thisDateWrapper.utc().hour.js() }
        "getUTCMilliseconds".js().func { thisDateWrapper.utc().nanosecond.nanoseconds.inWholeMilliseconds.js() }
        "getUTCMinutes".js().func { thisDateWrapper.utc().minute.js() }
        "getUTCMonth".js().func { thisDateWrapper.utc().monthNumber.js() }
        "getUTCSeconds".js().func { thisDateWrapper.utc().second.js() }

        "setDate".js().func { thisDateWrapper.set(day = toNumber(it[0]).toInt()); Undefined }
        "setFullYear".js().func { thisDateWrapper.set(year = toNumber(it[0]).toInt()); Undefined }
        "setYear".js().func { thisDateWrapper.set(year = toNumber(it[0]).toInt()); Undefined }
        "setHours".js().func { thisDateWrapper.set(hour = toNumber(it[0]).toInt()) ; Undefined}
        "setMinutes".js().func { thisDateWrapper.set(minute = toNumber(it[0]).toInt()); Undefined }
        "setMonth".js().func { thisDateWrapper.set(month = toNumber(it[0]).toInt()); Undefined }
        "setSeconds".js().func { thisDateWrapper.set(second = toNumber(it[0]).toInt()); Undefined  }
        "setTime".js().func {
            thisDateWrapper.value = Instant
                .fromEpochMilliseconds(toNumber(it[0]).toLong())
                .toLocalDateTime(thisDateWrapper.timeZone);
            Undefined
        }
        "setUTCDate".js().func { thisDateWrapper.setUTC(day = toNumber(it[0]).toInt()); Undefined }
        "setUTCFullYear".js().func { thisDateWrapper.setUTC(year = toNumber(it[0]).toInt()); Undefined }
        "setUTCYear".js().func { thisDateWrapper.setUTC(year = toNumber(it[0]).toInt()); Undefined }
        "setUTCHours".js().func { thisDateWrapper.setUTC(hour = toNumber(it[0]).toInt()); Undefined }
        "setUTCMinutes".js().func { thisDateWrapper.setUTC(minute = toNumber(it[0]).toInt()); Undefined }
        "setUTCMonth".js().func { thisDateWrapper.setUTC(month = toNumber(it[0]).toInt()); Undefined }
        "setUTCSeconds".js().func { thisDateWrapper.setUTC(second = toNumber(it[0]).toInt()); Undefined }

        "toDateString".js().func { thisDate.date.toString().js() }
        "toLocaleDateString".js().func { thisDate.date.toString().js() }
        "toTimeString".js().func { thisDate.time.toString().js() }
        "toLocaleTimeString".js().func { thisDate.time.toString().js() }
        "toISOString".js().func { thisDate.toString().js() }
        "toUTCString".js().func { thisDateWrapper.utc().toString().js() }
        "toJSON".js().func { thisDate.toString().js() }

        JSSymbol.toPrimitive.func("hint" defaults OpConstant("default".js())) {
            when (val t = it.getOrNull(0)?.toKotlin(this)) {
                "string", "default" -> thisDate.toString().js()
                "number" -> thisDateWrapper.toInstant().toEpochMilliseconds().js()
                else -> typeError { "Invalid hint: $t".js() }
            }
        }

        "valueOf".js().func { thisDateWrapper.toInstant().toEpochMilliseconds().js() }
    },
    properties = listOf(
        "now".func {
            Clock.System.now().toEpochMilliseconds().js()
        },
        "parse".func {
            val str = JSStringFunction.toString(it[0], this)
            Instant.parse(str).toEpochMilliseconds().js()
        }
    ).associateBy { it.name.js() }.toMutableMap(),
    parameters = listOf(FunctionParam("date")),
    body = Expression {
        it.constructDate(it.get("date".js()).listOf()).value.format(LocalDateTime.Formats.ISO).js()
    }
) {
    override suspend fun constructObject(args: List<JsAny?>, runtime: ScriptRuntime): JSObject {
        return runtime.constructDate(args)
    }
}

private suspend fun ScriptRuntime.constructDate(args: List<JsAny?>) : JSDateWrapper{
    val tz = TimeZone.currentSystemDefault()
    if (args.isEmpty()) {
        return JSDateWrapper(Clock.System.now().toLocalDateTime(tz), tz)
    }

    return when (val  a = args[0]?.toKotlin(this)) {
        is String -> JSDateWrapper(LocalDateTime.parse(a), tz)
        is LocalDateTime -> JSDateWrapper(a, tz)
        else -> {
            val num = toNumber(args[0])
            syntaxCheck(num.toDouble().isFinite()) {
                "Unexpected number"
            }
            JSDateWrapper(
                Instant.fromEpochMilliseconds(num.toLong())
                    .toLocalDateTime(TimeZone.currentSystemDefault()),
                tz
            )
        }
    }
}