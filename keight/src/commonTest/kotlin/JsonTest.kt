import kotlin.test.Test

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
}