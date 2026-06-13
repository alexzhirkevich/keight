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

    @Test
    fun complex_return_in_case() = runTest {
        """
            function test(s, flag) {
                switch(s) {
                    case 1:
                        var x = 10;
                        x += 5;
                        return x;
                    case 2:
                        if (flag) {
                            return "flagged";
                        }
                        return "unflagged";
                    default:
                        var res = "default";
                        return res + "_" + s;
                }
            }
            test(1, false)
        """.trimIndent().eval().assertEqualsTo(15L)

        """
            function test(s, flag) {
                switch(s) {
                    case 1:
                        var x = 10;
                        x += 5;
                        return x;
                    case 2:
                        if (flag) {
                            return "flagged";
                        }
                        return "unflagged";
                    default:
                        var res = "default";
                        return res + "_" + s;
                }
            }
            test(2, true)
        """.trimIndent().eval().assertEqualsTo("flagged")

        """
            function test(s, flag) {
                switch(s) {
                    case 1: 
                        var x = 10;
                        x += 5;
                        return x;
                    case 2:
                        if (flag) {
                            return "flagged";
                        }
                        return "unflagged";
                    default:
                        var res = "default";
                        return res + "_" + s;
                }
            }
            test(2, false)
        """.trimIndent().eval().assertEqualsTo("unflagged")

        """
            function test(s, flag) {
                switch(s) {
                    case 1:
                        var x = 10;
                        x += 5;
                        return x;
                    case 2:
                        if (flag) {
                            return "flagged";
                        }
                        return "unflagged";
                    default:
                        var res = "default";
                        return res + "_" + s;
                }
            }
            test(3, false)
        """.trimIndent().eval().assertEqualsTo("default_3")
    }

    @Test
    fun fallthrough_test() = runTest {
        """
            var foo = 1;
            var output = "Output: ";
            switch (foo) {
              case 0:
                output += "So ";
              case 1:
                output += "What ";
                output += "Is ";
              case 2:
                output += "Your ";
              case 3:
                output += "Name";
              case 4:
                output += "?";
                break;
              case 5:
                output += "!";
                break;
            }
            output
        """.trimIndent().eval().assertEqualsTo("Output: What Is Your Name?")
    }
}
