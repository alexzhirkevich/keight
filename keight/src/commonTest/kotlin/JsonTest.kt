import io.github.alexzhirkevich.keight.js.Undefined
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonTest {

    @Test
    fun parse() = runtimeTest {
        """
            let str = `{ 
                "int" : 1, 
                "float": 1.5, 
                "nl" : null,
                "string" : "test", 
                "list": [1, 1.5, "test"],
                "object" : { "variable": "value" }
            }`
            var obj = JSON.parse(str)
        """.trimIndent().eval(it)
        "obj.int".eval(it).assertEqualsTo(1L)
        "obj.nl".eval(it).assertEqualsTo(null)
        "obj.float".eval(it).assertEqualsTo(1.5)
        "obj.string".eval(it).assertEqualsTo("test")
        "obj.list".eval(it).assertEqualsTo(listOf(1L, 1.5, "test"))
        "obj.object.variable".eval(it).assertEqualsTo("value")
    }

    @Test
    fun stringify() = runtimeTest{
        """
            let object = { 
                int : 1, 
                float: 1.5, 
                string : "test", 
                list: [1, 1.5, "test"],
                object : { "variable": "value" }
            }
            JSON.stringify(object)
        """.eval(it).assertEqualsTo("""{"int":1,"float":1.5,"string":"test","list":[1,1.5,"test"],"object":{"variable":"value"}}""")
    }

    @Test
    fun parse_null() = runtimeTest {
        """JSON.parse("null")""".eval(it).assertEqualsTo(null)
        
        // null in object
        """JSON.parse('{"key": null}').key""".eval(it).assertEqualsTo(null)
        
        // null in array
        """JSON.parse("[1, null, 3]")[1]""".eval(it).assertEqualsTo(null)
    }

    @Test
    fun parse_booleans() = runtimeTest {
        """JSON.parse("true")""".eval(it).assertEqualsTo(true)
        """JSON.parse("false")""".eval(it).assertEqualsTo(false)
        
        // booleans in object
        """JSON.parse('{"flag": true}').flag""".eval(it).assertEqualsTo(true)
        """JSON.parse('{"flag": false}').flag""".eval(it).assertEqualsTo(false)
        
        // booleans in array
        """JSON.parse("[true, false]")[0]""".eval(it).assertEqualsTo(true)
        """JSON.parse("[true, false]")[1]""".eval(it).assertEqualsTo(false)
    }

    @Test
    fun parse_undefined() = runtimeTest {
        """JSON.parse("undefined")""".eval(it).assertEqualsTo(Undefined)
        
        // undefined in object
        """JSON.parse('{"u": undefined}').u""".eval(it).assertEqualsTo(Undefined)
        
        // undefined in array
        """JSON.parse("[undefined]")[0]""".eval(it).assertEqualsTo(Undefined)
    }

    @Test
    fun parse_strings() = runtimeTest {
        // basic strings
        """JSON.parse("\"hello\"")""".eval(it).assertEqualsTo("hello")
        """JSON.parse("\"\"")""".eval(it).assertEqualsTo("")
        """JSON.parse("\"test string\"")""".eval(it).assertEqualsTo("test string")
        
        // unicode characters
        """JSON.parse("\"中文测试\"")""".eval(it).assertEqualsTo("中文测试")
        
        // strings in object
        """JSON.parse("{\"key\": \"value\"}").key""".eval(it).assertEqualsTo("value")
        
        // strings in array
        """JSON.parse("[\"a\", \"b\", \"c\"]")[1]""".eval(it).assertEqualsTo("b")
    }

    @Test
    fun parse_numbers_integer() = runtimeTest {
        // positive integers
        """JSON.parse("0")""".eval(it).assertEqualsTo(0L)
        """JSON.parse("1")""".eval(it).assertEqualsTo(1L)
        """JSON.parse("123")""".eval(it).assertEqualsTo(123L)
        """JSON.parse("999999999")""".eval(it).assertEqualsTo(999999999L)
        
        // negative integers
        """JSON.parse("-1")""".eval(it).assertEqualsTo(-1L)
        """JSON.parse("-42")""".eval(it).assertEqualsTo(-42L)
        """JSON.parse("-123456")""".eval(it).assertEqualsTo(-123456L)
        
        // explicit positive sign
        """JSON.parse("+1")""".eval(it).assertEqualsTo(1L)
        """JSON.parse("+10")""".eval(it).assertEqualsTo(10L)
        
        // numbers in object
        """JSON.parse('{"num": 42}').num""".eval(it).assertEqualsTo(42L)
        """JSON.parse('{"num": -99}').num""".eval(it).assertEqualsTo(-99L)
        
        // numbers in array
        """JSON.parse("[1, 2, 3]")[0]""".eval(it).assertEqualsTo(1L)
    }

    @Test
    fun parse_numbers_float() = runtimeTest {
        // positive floats
        """JSON.parse("0.0")""".eval(it).assertEqualsTo(0.0)
        """JSON.parse("1.0")""".eval(it).assertEqualsTo(1.0)
        """JSON.parse("3.14")""".eval(it).assertEqualsTo(3.14)
        """JSON.parse("123.456")""".eval(it).assertEqualsTo(123.456)
        
        // negative floats
        """JSON.parse("-1.5")""".eval(it).assertEqualsTo(-1.5)
        """JSON.parse("-3.14")""".eval(it).assertEqualsTo(-3.14)
        """JSON.parse("-0.001")""".eval(it).assertEqualsTo(-0.001)
        
        // leading/trailing zeros
        """JSON.parse("0.5")""".eval(it).assertEqualsTo(0.5)
        """JSON.parse("10.0")""".eval(it).assertEqualsTo(10.0)
        """JSON.parse("0.0001")""".eval(it).assertEqualsTo(0.0001)
    }

    @Test
    fun parse_numbers_scientific() = runtimeTest {
        // positive exponent
        """JSON.parse("1e10")""".eval(it).assertEqualsTo(1e10)
        """JSON.parse("1E10")""".eval(it).assertEqualsTo(1e10)
        """JSON.parse("2.5e3")""".eval(it).assertEqualsTo(2.5e3)
        """JSON.parse("1.5e10")""".eval(it).assertEqualsTo(1.5e10)
        
        // negative exponent
        """JSON.parse("1.5e-3")""".eval(it).assertEqualsTo(1.5e-3)
        """JSON.parse("1e-10")""".eval(it).assertEqualsTo(1e-10)
        """JSON.parse("-1e5")""".eval(it).assertEqualsTo(-1e5)
        
        // positive exponent with +
        """JSON.parse("2e+5")""".eval(it).assertEqualsTo(2e5)
        """JSON.parse("-2.5e+5")""".eval(it).assertEqualsTo(-2.5e5)
    }

    @Test
    fun parse_numbers_radix() = runtimeTest {
        // hexadecimal (0x)
        """JSON.parse("0xFF")""".eval(it).assertEqualsTo(255L)
        """JSON.parse("0x10")""".eval(it).assertEqualsTo(16L)
        """JSON.parse("0x1F")""".eval(it).assertEqualsTo(31L)
        """JSON.parse("-0x10")""".eval(it).assertEqualsTo(-16L)
        
        // octal (0o)
        """JSON.parse("0o77")""".eval(it).assertEqualsTo(63L)
        """JSON.parse("0o10")""".eval(it).assertEqualsTo(8L)
        """JSON.parse("0o777")""".eval(it).assertEqualsTo(511L)
        
        // binary (0b)
        """JSON.parse("0b101")""".eval(it).assertEqualsTo(5L)
        """JSON.parse("0b1111")""".eval(it).assertEqualsTo(15L)
        """JSON.parse("0b100000")""".eval(it).assertEqualsTo(32L)
    }

    @Test
    fun parse_object() = runtimeTest {
        // basic object
        """JSON.parse("{\"key\": \"value\", \"num\": 42}").key""".eval(it).assertEqualsTo("value")
        """JSON.parse("{\"key\": \"value\", \"num\": 42}").num""".eval(it).assertEqualsTo(42L)
        
        // nested object
        """JSON.parse("{\"nested\": {\"a\": 1}}").nested.a""".eval(it).assertEqualsTo(1L)
        """JSON.parse("{\"a\": {\"b\": {\"c\": \"deep\"}}}").a.b.c""".eval(it).assertEqualsTo("deep")
        
        // empty object
        """Object.keys(JSON.parse("{}")).length""".eval(it).assertEqualsTo(0L)
        
        // object with various value types
        """JSON.parse("{\"str\":\"text\",\"num\":10,\"bool\":true,\"nil\":null}").str""".eval(it).assertEqualsTo("text")
        """JSON.parse("{\"str\":\"text\",\"num\":10,\"bool\":true,\"nil\":null}").num""".eval(it).assertEqualsTo(10L)
    }

    @Test
    fun parse_array() = runtimeTest {
        // basic array
        """JSON.parse("[1, 2, 3]")[0]""".eval(it).assertEqualsTo(1L)
        """JSON.parse("[1, 2, 3]").length""".eval(it).assertEqualsTo(3L)
        
        // mixed types
        """JSON.parse("[1, \"str\", true, null]")[0]""".eval(it).assertEqualsTo(1L)
        """JSON.parse("[1, \"str\", true, null]")[1]""".eval(it).assertEqualsTo("str")
        """JSON.parse("[1, \"str\", true, null]")[2]""".eval(it).assertEqualsTo(true)
        """JSON.parse("[1, \"str\", true, null]")[3]""".eval(it).assertEqualsTo(null)
        
        // nested array
        """JSON.parse("[[1, 2], [3, 4]]")[0][0]""".eval(it).assertEqualsTo(1L)
        """JSON.parse("[[1, 2], [3, 4]]")[1][0]""".eval(it).assertEqualsTo(3L)
        """JSON.parse("[[[1]]]")[0][0][0]""".eval(it).assertEqualsTo(1L)
        
        // empty array
        """JSON.parse("[]").length""".eval(it).assertEqualsTo(0L)
        
        // single element
        """JSON.parse("[42]")[0]""".eval(it).assertEqualsTo(42L)
    }

    @Test
    fun parse_complex_nested() = runtimeTest {
        """JSON.parse("{\"users\":[{\"name\":\"Alice\",\"age\":30},{\"name\":\"Bob\",\"age\":25}]}").users[0].name""".eval(it).assertEqualsTo("Alice")
        
        """JSON.parse("{\"matrix\":[[1,2],[3,4]]}").matrix[1][1]""".eval(it).assertEqualsTo(4L)
    }

    @Test
    fun parse_whitespace() = runtimeTest {
        // spaces
        """JSON.parse("  true  ")""".eval(it).assertEqualsTo(true)
        """JSON.parse("  123  ")""".eval(it).assertEqualsTo(123L)
        
        // tabs
        """JSON.parse("\t123\t")""".eval(it).assertEqualsTo(123L)
        
        // newlines
        """JSON.parse("\n123\n")""".eval(it).assertEqualsTo(123L)
        
        // mixed whitespace
        """JSON.parse("\n  {  \"a\" : 1 } \n").a""".eval(it).assertEqualsTo(1L)
        
        // in arrays and objects
        """JSON.parse('[ 1 , 2 , 3 ]')[1]""".eval(it).assertEqualsTo(2L)
        """JSON.parse('{ "a" : 1 }').a""".eval(it).assertEqualsTo(1L)
    }

    @Test
    fun stringify_null() = runtimeTest {
        "JSON.stringify(null)".eval(it).assertEqualsTo("null")
        """JSON.stringify({key: null})""".eval(it).assertEqualsTo("""{"key":null}""")
        """JSON.stringify([null, 1])""".eval(it).assertEqualsTo("[null,1]")
    }

    @Test
    fun stringify_booleans() = runtimeTest {
        "JSON.stringify(true)".eval(it).assertEqualsTo("true")
        "JSON.stringify(false)".eval(it).assertEqualsTo("false")
        """JSON.stringify({bool: true})""".eval(it).assertEqualsTo("""{"bool":true}""")
        """JSON.stringify([true, false])""".eval(it).assertEqualsTo("[true,false]")
    }

    @Test
    fun stringify_undefined() = runtimeTest {
        """JSON.stringify(undefined)""".eval(it).assertEqualsTo("undefined")
        // Note: undefined in objects is typically omitted (JS standard behavior)
        """JSON.stringify({a: 1, c: 2})""".eval(it).assertEqualsTo("""{"a":1,"c":2}""")
        """JSON.stringify([undefined, 1])""".eval(it).assertEqualsTo("[undefined,1]")
    }

    @Test
    fun stringify_numbers() = runtimeTest {
        "JSON.stringify(123)".eval(it).assertEqualsTo("123")
        "JSON.stringify(-42)".eval(it).assertEqualsTo("-42")
        "JSON.stringify(0)".eval(it).assertEqualsTo("0")
        "JSON.stringify(3.14)".eval(it).assertEqualsTo("3.14")
        "JSON.stringify(-0.5)".eval(it).assertEqualsTo("-0.5")
    }

    @Test
    fun stringify_strings() = runtimeTest {
        """JSON.stringify("hello")""".eval(it).assertEqualsTo("\"hello\"")
        """JSON.stringify("")""".eval(it).assertEqualsTo("\"\"")
        """JSON.stringify("test string")""".eval(it).assertEqualsTo("\"test string\"")
    }

    @Test
    fun stringify_object() = runtimeTest {
        """JSON.stringify({a: 1, b: 2})""".eval(it).assertEqualsTo("""{"a":1,"b":2}""")
        
        // nested
        """JSON.stringify({outer: {inner: true}})""".eval(it).assertEqualsTo("""{"outer":{"inner":true}}""")
        
        // empty object
        """JSON.stringify({})""".eval(it).assertEqualsTo("{}")
        
        // various value types in object
        """
            JSON.stringify({
                bool: true,
                num: 42,
                str: "test",
                nil: null
            })
        """.eval(it).assertEqualsTo("""{"bool":true,"num":42,"str":"test","nil":null}""")
    }

    @Test
    fun stringify_array() = runtimeTest {
        """JSON.stringify([1, 2, 3])""".eval(it).assertEqualsTo("[1,2,3]")
        """JSON.stringify(["a", "b"])""".eval(it).assertEqualsTo("""["a","b"]""")
        """JSON.stringify([])""".eval(it).assertEqualsTo("[]")
        """JSON.stringify([true, 1, "text"])""".eval(it).assertEqualsTo("""[true,1,"text"]""")
    }

    @Test
    fun stringify_mixed() = runtimeTest {
        """
            JSON.stringify({
                bool: true,
                num: 42,
                str: "test",
                nil: null,
                arr: [1, 2]
            })
        """.trimIndent().eval(it).assertEqualsTo(
            """{"bool":true,"num":42,"str":"test","nil":null,"arr":[1,2]}"""
        )
    }

    @Test
    fun roundtrip_basic() = runtimeTest {
        val original = """{"name":"test","value":123,"active":true}"""
        """JSON.stringify(JSON.parse('$original'))""".eval(it).assertEqualsTo(original)
    }

    @Test
    fun roundtrip_array() = runtimeTest {
        val original = """[1,2,3,"text",true,null]"""
        """JSON.stringify(JSON.parse('$original'))""".eval(it).assertEqualsTo(original)
    }

    @Test
    fun roundtrip_nested() = runtimeTest {
        val original = """{"users":[{"name":"Alice"},{"name":"Bob"}]}"""
        """JSON.stringify(JSON.parse('$original'))""".eval(it).assertEqualsTo(original)
    }

    @Test
    fun roundtrip_numbers() = runtimeTest {
        """JSON.stringify(JSON.parse("123"))""".eval(it).assertEqualsTo("123")
        """JSON.stringify(JSON.parse("-42"))""".eval(it).assertEqualsTo("-42")
        """JSON.stringify(JSON.parse("3.14"))""".eval(it).assertEqualsTo("3.14")
        // scientific notation format may vary (1e10 vs 1.0E10)
        """JSON.parse("1e10")""".eval(it).assertEqualsTo(1e10)
    }
}
