import java.io.Writer
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import kotlin.test.Test

class JavaxScriptEngineTest {

    @Test
    fun creation(){
        val engine = engine()
        engine.eval("'hello world'.toUpperCase()").assertEqualsTo("HELLO WORLD")
    }

    @Test
    fun writer(){
        val engine = engine()

        var result = ""
        var errResult = ""

        engine.context.writer = object : Writer(){
            override fun close() {}
            override fun flush() {}
            override fun write(p0: CharArray, p1: Int, p2: Int) {
                result = p0.asSequence().drop(p1).take(p2).joinToString(separator = "")
            }
        }

        engine.context.errorWriter = object : Writer(){
            override fun close() {}
            override fun flush() {}
            override fun write(p0: CharArray, p1: Int, p2: Int) {
                errResult = p0.asSequence().drop(p1).take(p2).joinToString(separator = "")
            }
        }
        engine.eval("console.log('hello world')")
        engine.eval("console.error('error!')")
        result.assertEqualsTo("hello world")
        errResult.assertEqualsTo("error!")
    }

    @Test
    fun persistence(){
        val engine = engine()
        engine.eval("var test1 = 'hello1'")
        engine.eval("let test2 = 'hello2'")
        engine.eval("const test3 = 'hello3'")
        engine.eval("test1").assertEqualsTo("hello1")
        engine.eval("test2").assertEqualsTo("hello2")
        engine.eval("test3").assertEqualsTo("hello3")
    }

    private fun engine() : ScriptEngine =
        ScriptEngineManager().getEngineByName("JavaScript")
}
