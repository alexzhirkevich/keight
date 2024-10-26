
import kotlin.test.Test

internal class JsStringExpressionsTest {


    @Test
    fun template(){
        "const x = 1, y ='test';`hello \${x} world \${y}`".eval()
            .assertEqualsTo("hello 1 world test")
    }

    @Test
    fun endsWith() {
        "'test123'.endsWith('123')".eval().assertEqualsTo(true)
        "'test123'.endsWith('1234')".eval().assertEqualsTo(false)
        "'test1234'.endsWith('123', 7)".eval().assertEqualsTo(true)
    }

    @Test
    fun startsWith() {
        "'123test'.startsWith('123')".eval().assertEqualsTo(true)
        "'123test'.startsWith('1234')".eval().assertEqualsTo(false)
        "'F1234test'.startsWith('123', 1)".eval().assertEqualsTo(true)
    }

    @Test
    fun includes() {
        "'x123test'.includes('123')".eval().assertEqualsTo(true)
        "'x123test'.includes('1234')".eval().assertEqualsTo(false)
    }

    @Test
    fun match() {
        "'a'.match('[a-z]')".eval().assertEqualsTo(true)
        "'A'.match('[a-z]')".eval().assertEqualsTo(false)
    }

    @Test
    fun padEnd() {
        "'abc'.padEnd(5)".eval().assertEqualsTo("abc  ")
        "'abc'.padEnd(5,'0')".eval().assertEqualsTo("abc00")
        "'abc'.padEnd(5,'12')".eval().assertEqualsTo("abc12")
        "'abc'.padEnd(6,'12')".eval().assertEqualsTo("abc121")
        "'abcdef'.padEnd(5,'0')".eval().assertEqualsTo("abcdef")
    }

    @Test
    fun padStart() {
        "'abc'.padStart(5)".eval().assertEqualsTo("  abc")
        "'abc'.padStart(5,'0')".eval().assertEqualsTo("00abc")
        "'abc'.padStart(5,'12')".eval().assertEqualsTo("12abc")
        "'abc'.padStart(6,'12')".eval().assertEqualsTo("121abc")
        "'abcdef'.padStart(5,'0')".eval().assertEqualsTo("abcdef")
    }

    @Test
    fun repeat() {
        "'abc'.repeat(3)".eval().assertEqualsTo("abcabcabc")
        "'abc'.repeat(0)".eval().assertEqualsTo("")
    }

    @Test
    fun replace() {
        "'aabbcc'.replace('b','f')".eval().assertEqualsTo("aafbcc")
        "'aabbcc'.replace('x','ff')".eval().assertEqualsTo("aabbcc")
        "'aabbcc'.replace('','ff')".eval().assertEqualsTo("ffaabbcc")
    }

    @Test
    fun replaceAll() {
        "'aabbcc'.replaceAll('b','f')".eval().assertEqualsTo("aaffcc")
        "'aabbcc'.replaceAll('x','ff')".eval().assertEqualsTo("aabbcc")
        "'aabbcc'.replaceAll('','ff')".eval().assertEqualsTo("ffaabbcc")
    }

    @Test
    fun trim() {
        "' abc '.trim()".eval().assertEqualsTo("abc")
        "'abc '.trim()".eval().assertEqualsTo("abc")
        "' abc'.trim()".eval().assertEqualsTo("abc")

        "' abc'.trimStart()".eval().assertEqualsTo("abc")
        "' abc '.trimStart()".eval().assertEqualsTo("abc ")
        "'abc '.trimStart()".eval().assertEqualsTo("abc ")

        "' abc'.trimEnd()".eval().assertEqualsTo(" abc")
        "' abc '.trimEnd()".eval().assertEqualsTo(" abc")
        "'abc '.trimEnd()".eval().assertEqualsTo("abc")
    }
    @Test
    fun substring() {
        "'123456'.substring(1)".eval().assertEqualsTo("23456")
        "'123456'.substring(2)".eval().assertEqualsTo("3456")
        "'123456'.substring(3)".eval().assertEqualsTo("456")
        "'123456'.substring(0)".eval().assertEqualsTo("123456")

        "'123456'.substring(1,2)".eval().assertEqualsTo("2")
        "'123456'.substring(1,3)".eval().assertEqualsTo("23")
        "'123456'.substring(1,4)".eval().assertEqualsTo("234")
        "'123456'.substring(0,10)".eval().assertEqualsTo("123456")
        "'123456'.substring(0,3)".eval().assertEqualsTo("123")
    }

    @Test
    fun split(){
        "'The quick brown fox jumps over the lazy dog'.split(' ')"
            .eval().assertEqualsTo(listOf("The", "quick", "brown", "fox", "jumps", "over", "the","lazy","dog"))
        "'a b'.split('')".eval().assertEqualsTo(listOf("a", " ", "b"))
    }

    @Test
    fun concat(){
        "'hello '.concat('world')".eval().assertEqualsTo("hello world")
        "'hello'.concat(', ','world')".eval().assertEqualsTo("hello, world")
    }
}