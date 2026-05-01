import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class DateTest {

    // ========== constructor ==========

    @Test
    fun constructorNoArgs() = runtimeTest {
        val nowInstant = Clock.System.now()
        val result = "new Date().getTime()".eval(it)
        assertTrue(result is Number, "new Date() should return a timestamp-like value")
        val ts = result.toLong()
        // getTime() on the same instance
        assertTrue("Timestamp should be close to now") {
            kotlin.math.abs(ts - nowInstant.toEpochMilliseconds()) < 5000
        }
    }

    @Test
    fun constructorWithIsoString() = runtimeTest {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        "const d = new Date('$now')".eval(it)
        "d.getFullYear()".eval(it).assertEqualsTo(now.year.toLong())
        "d.getMonth()".eval(it).assertEqualsTo(now.month.ordinal.toLong())
        "d.getDate()".eval(it).assertEqualsTo(now.day.toLong())
        "d.getHours()".eval(it).assertEqualsTo(now.hour.toLong())
        "d.getMinutes()".eval(it).assertEqualsTo(now.minute.toLong())
        "d.getSeconds()".eval(it).assertEqualsTo(now.second.toLong())
    }

    @Test
    fun constructorWithTimestamp() = runtimeTest {
        // epoch + 1 second
        "const d = new Date(1000)".eval(it)
        "d.getTime()".eval(it).assertEqualsTo(1000L)
        // 1970-01-01T00:00:01 in UTC, local time depends on timezone
        "d.getUTCSeconds()".eval(it).assertEqualsTo(1L)
        "d.getUTCMinutes()".eval(it).assertEqualsTo(0L)
        "d.getUTCHours()".eval(it).assertEqualsTo(0L)
        "d.getUTCFullYear()".eval(it).assertEqualsTo(1970L)
        "d.getUTCMonth()".eval(it).assertEqualsTo(0L) // January = 0
        "d.getUTCDate()".eval(it).assertEqualsTo(1L)
    }

    // ========== static methods ==========

    @Test
    fun dateNow() = runtimeTest {
        val nowInstant = Clock.System.now()
        assertTrue { ("Date.now()".eval(it) as Long) - nowInstant.toEpochMilliseconds() in 0L..300L }
    }

    @Test
    fun dateParse() = runtimeTest {
        "Date.parse('1970-01-01T00:00:00Z')".eval(it).assertEqualsTo(0L)
    }

    // ========== getters ==========

    @Test
    fun getters() = runtimeTest {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        "const d = new Date('$now')".eval(it)
        "d.getDate()".eval(it).assertEqualsTo(now.day.toLong())
        "d.getDay()".eval(it).assertEqualsTo(now.dayOfWeek.isoDayNumber.toLong() % 7L)
        "d.getFullYear()".eval(it).assertEqualsTo(now.year.toLong())
        "d.getYear()".eval(it).assertEqualsTo(now.year.toLong())
        "d.getHours()".eval(it).assertEqualsTo(now.hour.toLong())
        "d.getMilliseconds()".eval(it)
            .assertEqualsTo(now.nanosecond.nanoseconds.inWholeMilliseconds)
        "d.getMinutes()".eval(it).assertEqualsTo(now.minute.toLong())
        "d.getMonth()".eval(it).assertEqualsTo(now.month.ordinal.toLong())
        "d.getSeconds()".eval(it).assertEqualsTo(now.second.toLong())
        "d.getTime()".eval(it).assertEqualsTo(
            now.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        )
        "d.getTimeZoneOffset()".eval(it).assertEqualsTo(
            TimeZone.currentSystemDefault()
                .offsetAt(Clock.System.now())
                .totalSeconds.toLong() / 60L
        )
    }

    @Test
    fun utcGetters() = runtimeTest {
        // Use a known timestamp: 2024-06-15T10:30:45Z
        "const d = new Date('2024-06-15T10:30:45Z')".eval(it)
        "d.getUTCFullYear()".eval(it).assertEqualsTo(2024L)
        "d.getUTCMonth()".eval(it).assertEqualsTo(5L)   // June = 5
        "d.getUTCDate()".eval(it).assertEqualsTo(15L)
        "d.getUTCDay()".eval(it).assertEqualsTo(6L)     // Saturday
        "d.getUTCHours()".eval(it).assertEqualsTo(10L)
        "d.getUTCMinutes()".eval(it).assertEqualsTo(30L)
        "d.getUTCSeconds()".eval(it).assertEqualsTo(45L)
    }

    // ========== setters ==========

    @Test
    fun setYear() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45')".eval(it)
        "d.setYear(2000)".eval(it)
        "d.getFullYear()".eval(it).assertEqualsTo(2000L)
    }

    @Test
    fun setFullYear() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45')".eval(it)
        "d.setFullYear(1990)".eval(it)
        "d.getFullYear()".eval(it).assertEqualsTo(1990L)
    }

    @Test
    fun setMonth() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45')".eval(it)
        "d.setMonth(0)".eval(it)
        "d.getMonth()".eval(it).assertEqualsTo(0L)
    }

    @Test
    fun setDate() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45')".eval(it)
        "d.setDate(28)".eval(it)
        "d.getDate()".eval(it).assertEqualsTo(28L)
    }

    @Test
    fun setHours() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45')".eval(it)
        "d.setHours(22)".eval(it)
        "d.getHours()".eval(it).assertEqualsTo(22L)
    }

    @Test
    fun setMinutes() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45')".eval(it)
        "d.setMinutes(55)".eval(it)
        "d.getMinutes()".eval(it).assertEqualsTo(55L)
    }

    @Test
    fun setSeconds() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45')".eval(it)
        "d.setSeconds(12)".eval(it)
        "d.getSeconds()".eval(it).assertEqualsTo(12L)
    }

    @Test
    fun setTime() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45')".eval(it)
        "d.setTime(0)".eval(it)
        "d.getTime()".eval(it).assertEqualsTo(0L)
    }

    // ========== UTC setters ==========

    @Test
    fun setUTCFullYear() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45Z')".eval(it)
        "d.setUTCFullYear(2000)".eval(it)
        "d.getUTCFullYear()".eval(it).assertEqualsTo(2000L)
    }

    @Test
    fun setUTCMonth() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45Z')".eval(it)
        "d.setUTCMonth(11)".eval(it)
        "d.getUTCMonth()".eval(it).assertEqualsTo(11L)
    }

    @Test
    fun setUTCDate() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45Z')".eval(it)
        "d.setUTCDate(1)".eval(it)
        "d.getUTCDate()".eval(it).assertEqualsTo(1L)
    }

    @Test
    fun setUTCHours() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45Z')".eval(it)
        "d.setUTCHours(5)".eval(it)
        "d.getUTCHours()".eval(it).assertEqualsTo(5L)
    }

    @Test
    fun setUTCMinutes() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45Z')".eval(it)
        "d.setUTCMinutes(0)".eval(it)
        "d.getUTCMinutes()".eval(it).assertEqualsTo(0L)
    }

    @Test
    fun setUTCSeconds() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45Z')".eval(it)
        "d.setUTCSeconds(59)".eval(it)
        "d.getUTCSeconds()".eval(it).assertEqualsTo(59L)
    }

    // ========== toString methods ==========

    @Test
    fun toStringMethods() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45')".eval(it)
        val toISOString = "d.toISOString()".eval(it)
        assertTrue(toISOString is String, "toISOString should return string")
        assertTrue(
            (toISOString as String).contains("2024"),
            "toISOString should contain year: $toISOString"
        )
    }

    @Test
    fun toDateString() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45')".eval(it)
        val result = "d.toDateString()".eval(it)
        assertTrue(result is String, "toDateString should return string")
        assertTrue(
            (result as String).isNotBlank(),
            "toDateString should not be blank"
        )
    }

    @Test
    fun toLocaleDateString() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45')".eval(it)
        val result = "d.toLocaleDateString()".eval(it)
        assertTrue(result is String, "toLocaleDateString should return string")
    }

    @Test
    fun toTimeString() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45')".eval(it)
        val result = "d.toTimeString()".eval(it)
        assertTrue(result is String, "toTimeString should return string")
    }

    @Test
    fun toLocaleTimeString() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45')".eval(it)
        val result = "d.toLocaleTimeString()".eval(it)
        assertTrue(result is String, "toLocaleTimeString should return string")
    }

    @Test
    fun toLocaleString() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45')".eval(it)
        val result = "d.toLocaleString()".eval(it)
        assertTrue(result is String, "toLocaleString should return string")
        assertTrue(
            (result as String).contains("2024"),
            "toLocaleString should contain year: $result"
        )
    }

    @Test
    fun toUTCString() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45Z')".eval(it)
        val result = "d.toUTCString()".eval(it)
        assertTrue(result is String, "toUTCString should return string")
    }

    @Test
    fun toJSON() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45')".eval(it)
        val result = "d.toJSON()".eval(it)
        assertTrue(result is String, "toJSON should return string")
    }

    @Test
    fun valueOf() = runtimeTest {
        "const d = new Date(1234567890)".eval(it)
        "d.valueOf()".eval(it).assertEqualsTo(1234567890L)
    }

    @Test
    fun toPrimitiveString() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45')".eval(it)
        val result = "String(d)".eval(it)
        assertTrue(result is String, "String(d) should return string")
    }

    @Test
    fun toPrimitiveNumber() = runtimeTest {
        "const d = new Date(9876543210)".eval(it)
        "Number(d)".eval(it).assertEqualsTo(9876543210L)
    }

    @Test
    fun toPrimitiveDefault() = runtimeTest {
        "const d = new Date('2024-06-15T10:30:45')".eval(it)
        val result = "d + ''".eval(it)
        assertTrue(result is String, "d + '' should return string via default toPrimitive")
    }
}
