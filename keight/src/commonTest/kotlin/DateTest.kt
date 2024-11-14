import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.nanoseconds

class DateTest {

    @Test
    fun set() = runtimeTest {
        val nowInstant = Clock.System.now()
        val now = nowInstant.toLocalDateTime(TimeZone.currentSystemDefault())
        assertTrue { ("Date.now()".eval() as Long) - nowInstant.toEpochMilliseconds() in 0L..300 }
        "const d = new Date('$now')".eval(it)
        "d.getDate()".eval(it).assertEqualsTo(now.dayOfMonth.toLong())
        "d.getDay()".eval(it).assertEqualsTo(now.dayOfWeek.isoDayNumber.toLong() % 7L)
        "d.getFullYear()".eval(it).assertEqualsTo(now.year.toLong())
        "d.getYear()".eval(it).assertEqualsTo(now.year.toLong())
        "d.getHours()".eval(it).assertEqualsTo(now.hour.toLong())
        "d.getMilliseconds()".eval(it)
            .assertEqualsTo(now.nanosecond.nanoseconds.inWholeMilliseconds)
        "d.getMinutes()".eval(it).assertEqualsTo(now.minute.toLong())
        "d.getSeconds()".eval(it).assertEqualsTo(now.second.toLong())
        "d.getTimeZoneOffset()".eval(it).assertEqualsTo(
            TimeZone.currentSystemDefault().offsetAt(nowInstant).totalSeconds.toLong() / 60L
        )
    }
}