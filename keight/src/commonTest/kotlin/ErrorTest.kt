import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ErrorTest {

    @Test
    fun create() = runTest {
        "Error('test').message".eval().assertEqualsTo("test")
        "new Error('test').message".eval().assertEqualsTo("test")
    }

    @Test
    fun throwing() = runTest {
        """
            try {
                throw Error(52)
            } catch (e){
                res = e.message
            }
            res
        """.eval().assertEqualsTo("52")
    }
}