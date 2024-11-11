//import io.github.alexzhirkevich.keight.Module
//import io.github.alexzhirkevich.keight.evaluate
//import io.github.alexzhirkevich.keight.JavaScriptEngine
//import io.github.alexzhirkevich.keight.js.JSON
//import kotlin.test.Test
//
//class JsonTest {
//
//    @Test
//    fun parse() {
//        with(JavaScriptEngine(modules = arrayOf(Module.JSON))) {
//            evaluate(
//                """
//                    let string = `{
//                        "int" : 1,
//                        "float": 1.5,
//                        "string" : "test",
//                        "list": [1, 1.5, "test"],
//                        "object" : { "variable": "value" }
//                    }`
//                    var obj = JSON.parse(string)
//                """
//            )
//
//            evaluate("obj.int").assertEqualsTo(1L)
//            evaluate("obj.float").assertEqualsTo(1.5)
//            evaluate("obj.string").assertEqualsTo("test")
//            evaluate("obj.list").assertEqualsTo(listOf(1L, 1.5, "test"))
//            evaluate("obj.object.variable").assertEqualsTo("value")
//        }
//    }
//
//    @Test
//    fun stringify() {
//        with(JavaScriptEngine(modules = arrayOf(Module.JSON))) {
//            evaluate(
//                """
//                    let object = {
//                        int : 1,
//                        float: 1.5,
//                        string : "test",
//                        list: [1, 1.5, "test"],
//                        object : { "variable": "value" }
//                    }
//                    JSON.stringify(object)
//                """
//            ).assertEqualsTo("""{"int":1,"float":1.5,"string":"test","list":[1,1.5,"test"],"object":{"variable":"value"}}""")
//        }
//    }
//}