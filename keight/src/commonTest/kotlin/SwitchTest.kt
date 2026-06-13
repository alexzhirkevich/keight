import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SwitchTest {

    @Test
    fun test()= runTest {
        """
            var x
            switch(3){
                case 1:
                    break
                case 2:
                    break
                case 3:
                    x = 3
                    break
                default:
                    break
            }
            x
        """.trimIndent().eval().assertEqualsTo(3L)
    }

    @Test
    fun multi_choice()= runTest {
        """
            var x
            switch(3){
                case 1:
                case 2:
                case 3:
                    x = 1
                    break
                case 4:
                    x = 2
                    break
            }
            x
        """.trimIndent().eval().assertEqualsTo(1L)
    }

    @Test
    fun default_missplacement()= runTest {
        """
            var x = ''
            switch(4){
                case 1:
                    x += '1'
                    break
                default:
                    break;
                case 2:
                    x += '2'
                    break
                case 3:
                    x += '3'
                    break
            }
            x
        """.trimIndent().eval().assertEqualsTo("")
    }

    @Test
    fun return_in_case() = runTest {
        """
            function test(s) {
                switch(s) {
                    case 1: return "one";
                    case 2: return "two";
                    default: return "unknown";
                }
            }
            test(1)
        """.trimIndent().eval().assertEqualsTo("one")

        """
            function test(s) {
                switch(s) {
                    case 1: return "one";
                    case 2: return "two";
                    default: return "unknown";
                }
            }
            test(2)
        """.trimIndent().eval().assertEqualsTo("two")

        """
            function test(s) {
                switch(s) {
                    case 1: return "one";
                    case 2: return "two";
                    default: return "unknown";
                }
            }
            test(3)
        """.trimIndent().eval().assertEqualsTo("unknown")
    }

    @Test
    fun simple_return_in_case() = runTest {
        """
            function test(s) {
                switch(s) {
                    case 1: return 1;
                    case 2: return "two";
                }
            }
            test(1)
        """.trimIndent().eval().assertEqualsTo(1L)
        
        """
            function test(s) {
                switch(s) {
                    case 1: return 1;
                    case 2: return "two";
                }
            }
            test(2)
        """.trimIndent().eval().assertEqualsTo("two")
    }
}
