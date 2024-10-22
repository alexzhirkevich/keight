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
        engine.eval("var test = 'hello'")
        engine.eval("test").assertEqualsTo("hello")
    }

    private fun engine() : ScriptEngine =
        ScriptEngineManager().getEngineByName("JavaScript")
}
