import io.github.alexzhirkevich.keight.Module
import io.github.alexzhirkevich.keight.JavaScriptEngine
import io.github.alexzhirkevich.keight.js.JSON
import kotlin.test.Test

class JsonTest {

    @Test
    fun parse() = runtimeTest {
        """
            let string = `{ 
                "int" : 1, 
                "float": 1.5, 
                "string" : "test", 
                "list": [1, 1.5, "test"],
                "object" : { "variable": "value" }
            }`
            var obj = JSON.parse(string)
        """.eval(it)
        "obj.int".eval(it).assertEqualsTo(1L)
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