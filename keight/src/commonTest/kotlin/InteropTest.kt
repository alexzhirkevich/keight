import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.set
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class InteropTest {

    @Test
    fun lambda()= runTest {

        var x = 0L
        val runtime  = JSRuntime(coroutineContext)

        runtime["test"] = { x = 2 }
        "test()".eval(runtime)
        x.assertEqualsTo(2L)

        runtime["test"] =  { a : Long -> x = a }
        "test(3)".eval(runtime)
        x.assertEqualsTo(3L)

        runtime["test"] = { a : Long -> a+1 }
        "test(3)".eval(runtime).assertEqualsTo(4L)

        runtime["sum"] = ::sum

        "sum(test(9), test(4))".eval(runtime).assertEqualsTo(15L)
    }

    fun sum(a : Long, b : Long) : Long {
        return a + b
    }
}

