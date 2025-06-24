package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Wrapper
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

internal class JSDateWrapper(
    override var value: LocalDateTime,
    val timeZone: TimeZone
) : JsObjectImpl(), Wrapper<LocalDateTime> {

    fun toInstant() = value.toInstant(timeZone)

    fun utc(): LocalDateTime = if (timeZone == TimeZone.UTC) {
        value
    } else toInstant().toLocalDateTime(TimeZone.UTC)

    fun set(
        year : Int = value.year,
        month : Int = value.monthNumber,
        day : Int = value.dayOfMonth,
        hour : Int = value.dayOfMonth,
        minute : Int = value.minute,
        second : Int = value.second,
    ) {
        value = copy(year, month, day, hour, minute, second)
    }

    fun setUTC(
        year : Int = value.year,
        month : Int = value.monthNumber,
        day : Int = value.dayOfMonth,
        hour : Int = value.dayOfMonth,
        minute : Int = value.minute,
        second : Int = value.second,
    ) {
        value = copy(year, month, day, hour, minute, second)
            .toInstant(TimeZone.UTC)
            .toLocalDateTime(timeZone)
    }

    fun copy(
        year : Int = value.year,
        month : Int = value.monthNumber,
        day : Int = value.dayOfMonth,
        hour : Int = value.dayOfMonth,
        minute : Int = value.minute,
        second : Int = value.second,
    ) = LocalDateTime(
        year = year,
        monthNumber = month,
        dayOfMonth = day,
        hour = hour,
        minute = minute,
        second = second,
        nanosecond = value.nanosecond
    )

    override fun toString(): String {
        return value.toString()
    }
}