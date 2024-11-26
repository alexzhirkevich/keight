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
        "getDate".func { thisDate.dayOfMonth }
        "getDay".func { thisDate.dayOfWeek.isoDayNumber % 7 }
        "getFullYear".func { thisDate.year }
        "getYear".func { thisDate.year }
        "getHours".func { thisDate.hour }
        "getMilliseconds".func { thisDate.nanosecond.nanoseconds.inWholeMilliseconds }
        "getMinutes".func { thisDate.minute }
        "getMonth".func { thisDate.monthNumber }
        "getSeconds".func { thisDate.second }
        "getTime".func { thisDateWrapper.toInstant().toEpochMilliseconds() }
        "getTimeZoneOffset".func {
            thisDateWrapper.timeZone.offsetAt(thisDateWrapper.toInstant()).totalSeconds / 60
        }
        "getUTCDate".func { thisDateWrapper.utc().dayOfMonth }
        "getUTCDay".func { thisDateWrapper.utc().dayOfWeek.isoDayNumber % 7 }
        "getUTCFullYear".func { thisDateWrapper.utc().year }
        "getUTCHours".func { thisDateWrapper.utc().hour }
        "getUTCMilliseconds".func { thisDateWrapper.utc().nanosecond.nanoseconds.inWholeMilliseconds }
        "getUTCMinutes".func { thisDateWrapper.utc().minute }
        "getUTCMonth".func { thisDateWrapper.utc().monthNumber }
        "getUTCSeconds".func { thisDateWrapper.utc().second }

        "setDate".func { thisDateWrapper.set(day = toNumber(it[0]).toInt()) }
        "setFullYear".func { thisDateWrapper.set(year = toNumber(it[0]).toInt()) }
        "setYear".func { thisDateWrapper.set(year = toNumber(it[0]).toInt()) }
        "setHours".func { thisDateWrapper.set(hour = toNumber(it[0]).toInt()) }
        "setMinutes".func { thisDateWrapper.set(minute = toNumber(it[0]).toInt()) }
        "setMonth".func { thisDateWrapper.set(month = toNumber(it[0]).toInt()) }
        "setSeconds".func { thisDateWrapper.set(second = toNumber(it[0]).toInt())  }
        "setTime".func {
            thisDateWrapper.value = Instant
                .fromEpochMilliseconds(toNumber(it[0]).toLong())
                .toLocalDateTime(thisDateWrapper.timeZone);
            Unit
        }
        "setUTCDate".func { thisDateWrapper.setUTC(day = toNumber(it[0]).toInt()) }
        "setUTCFullYear".func { thisDateWrapper.setUTC(year = toNumber(it[0]).toInt()) }
        "setUTCYear".func { thisDateWrapper.setUTC(year = toNumber(it[0]).toInt()) }
        "setUTCHours".func { thisDateWrapper.setUTC(hour = toNumber(it[0]).toInt()) }
        "setUTCMinutes".func { thisDateWrapper.setUTC(minute = toNumber(it[0]).toInt()) }
        "setUTCMonth".func { thisDateWrapper.setUTC(month = toNumber(it[0]).toInt()) }
        "setUTCSeconds".func { thisDateWrapper.setUTC(second = toNumber(it[0]).toInt()) }

        "toDateString".func { thisDate.date.toString() }
        "toLocaleDateString".func { thisDate.date.toString() }
        "toTimeString".func { thisDate.time.toString() }
        "toLocaleTimeString".func { thisDate.time.toString() }
        "toISOString".func { thisDate.toString() }
        "toUTCString".func { thisDateWrapper.utc().toString() }
        "toJSON".func { thisDate.toString() }

        JSSymbol.toPrimitive.func("hint" defaults OpConstant("default")) {
            when (val t = toKotlin(it.getOrNull(0))) {
                "string", "default" -> thisDate.toString()
                "number" -> thisDateWrapper.toInstant().toEpochMilliseconds()
                else -> typeError { "Invalid hint: $t" }
            }
        }

        "valueOf".func { thisDateWrapper.toInstant().toEpochMilliseconds() }
    },
    properties = listOf(
        "now".func {
            Clock.System.now().toEpochMilliseconds()
        },
        "parse".func {
            val str = JSStringFunction.toString(it[0], this)
            Instant.parse(str).toEpochMilliseconds()
        }
    ).associateBy { it.name }.toMutableMap(),
    parameters = listOf(FunctionParam("date")),
    body = Expression {
        it.constructDate(it.get("date").listOf()).value.format(LocalDateTime.Formats.ISO)
    }
) {
    override suspend fun constructObject(args: List<Any?>, runtime: ScriptRuntime): JSObject {
        return runtime.constructDate(args)
    }
}

private suspend fun ScriptRuntime.constructDate(args: List<Any?>) : JSDateWrapper{
    val tz = TimeZone.currentSystemDefault()
    if (args.isEmpty()) {
        return JSDateWrapper(Clock.System.now().toLocalDateTime(tz), tz)
    }

    return when (val a = toKotlin(args[0])) {
        is String -> JSDateWrapper(LocalDateTime.parse(a), tz)
        is LocalDateTime -> JSDateWrapper(a, tz)
        else -> {
            val num = toNumber(a)
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