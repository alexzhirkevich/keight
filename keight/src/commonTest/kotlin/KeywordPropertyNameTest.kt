import io.github.alexzhirkevich.keight.js.SyntaxError
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Reserved words and punctuator-keywords are valid property names in ECMAScript.
 *
 * The lexer maps `delete`, `void`, `typeof`, `in`, `instanceof` and `new` to operator tokens
 * (which carry no textual name), so the parser must recover the identifier when such a token is
 * used as a property name in member access (`a.delete`), optional chaining (`a?.delete`) or as the
 * key of an object literal / class element (`{ delete: 1 }`, `class C { delete(){} }`).
 *
 * Regression coverage for the bug where `a.delete()` (or any object with a `delete` method)
 * crashed with "Illegal symbol after '.'" / "Unexpected token 'Colon'".
 */
class KeywordPropertyNameTest {

    @Test
    fun deleteAsObjectKeyAndMethod() = runTest {
        "({delete: 1}).delete".eval().assertEqualsTo(1L)
        "({delete(){ return 7 }}).delete()".eval().assertEqualsTo(7L)
        "var a = {delete: () => 9}; a.delete()".eval().assertEqualsTo(9L)
    }

    @Test
    fun operatorKeywordsAsObjectKeys() = runTest {
        "({void: 1}).void".eval().assertEqualsTo(1L)
        "({typeof: 2}).typeof".eval().assertEqualsTo(2L)
        "({new: 3}).new".eval().assertEqualsTo(3L)
        "({in: 4}).in".eval().assertEqualsTo(4L)
        "({instanceof: 5}).instanceof".eval().assertEqualsTo(5L)
    }

    @Test
    fun operatorKeywordsViaMemberAccess() = runTest {
        "var a = {delete:1, void:2, typeof:3, new:4, in:5, instanceof:6}; a.delete".eval().assertEqualsTo(1L)
        "var a = {delete:1, void:2, typeof:3, new:4, in:5, instanceof:6}; a.instanceof".eval().assertEqualsTo(6L)
        "var a = {delete:1, void:2, typeof:3, new:4, in:5, instanceof:6}; a.new".eval().assertEqualsTo(4L)
    }

    @Test
    fun operatorKeywordsViaOptionalChaining() = runTest {
        "var a = {delete: 42}; a?.delete".eval().assertEqualsTo(42L)
        "var a = {in: 43}; a?.in".eval().assertEqualsTo(43L)
        "var a = {instanceof: 44}; a?.instanceof".eval().assertEqualsTo(44L)
    }

    @Test
    fun operatorKeywordsAsMethodNames() = runTest {
        "({void(){ return 1 }}).void()".eval().assertEqualsTo(1L)
        "({new(){ return 2 }}).new()".eval().assertEqualsTo(2L)
        "({in(){ return 3 }}).in()".eval().assertEqualsTo(3L)
    }

    @Test
    fun deleteOperatorStillWorks() = runTest {
        // The `delete` operator (statement form) must keep removing properties.
        assertEquals(
            "undefined",
            "var o = {a:1}; (delete o.a, o.a)".eval().toString()
        )
        // `delete` used as a property name must not be mistaken for the operator.
        "var o = {delete: 'kept'}; o.delete".eval().assertEqualsTo("kept")
    }

    /**
     * Ground truth verified against V8 (node v22): every ECMAScript keyword is a valid
     * property name in all four positions — object key, method name, member access
     * (including optional chaining and assignment target). See ECMA-262 `IdentifierName`
     * which includes all `ReservedWord`s.
     */
    @Test
    fun allKeywordsAreValidPropertyNamesLikeV8() = runTest {
        val keywords = listOf(
            // operator-keywords (lexed as Token.Operator in keight)
            "new", "in", "instanceof", "typeof", "void", "delete",
            // Token.Identifier.Keyword
            "var", "let", "const", "null", "true", "false", "if", "else", "for",
            "while", "do", "break", "continue", "function", "return", "class",
            "switch", "case", "default", "throw", "try", "catch", "finally",
            "async", "await", "with", "this", "export", "import", "extends",
            "debugger", "of", "super",
            // Token.Identifier.Reserved
            "enum", "implements", "package", "private", "protected", "public"
        )

        val cases = keywords.flatMap { kw ->
            listOf(
                "({$kw: 42}).$kw" to 42L,                                     // object key + read
                "({$kw(){ return 7 }}).$kw()" to 7L,                          // method definition
                "({$kw: 42})?.$kw" to 42L,                                    // optional chaining
                "(function(){ var o = {}; o.$kw = 1; return o.$kw; })()" to 1L // assignment target
            )
        }

        cases.forEach { (source, expected) ->
            try {
                source.eval().assertEqualsTo(expected)
            } catch (t: Throwable) {
                throw AssertionError("failed: $source\ncaused by: ${t.message}", t)
            }
        }
    }

    /**
     * V8 rejects reserved words in *shorthand* position — a reserved word can never be an
     * IdentifierReference — even though it is a valid property key. keight must match.
     */
    @Test
    fun reservedWordShorthandIsSyntaxErrorLikeV8() = runTest {
        for (source in listOf(
            "({enum})",
            "({enum, a: 1})",
            "(function(){ return {implements}; })"
        )) {
            try {
                source.eval()
                throw AssertionError("expected SyntaxError for: $source")
            } catch (e: SyntaxError) {
                // expected
            }
        }
    }
}
