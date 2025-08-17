import io.github.alexzhirkevich.keight.js.ReferenceError
import io.github.alexzhirkevich.keight.js.SyntaxError
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ModulesTest {

    @Test
    fun esm_base() = engineTest {

        it.compile(
            """
                export function moduleFunction() { return "moduleF" }
                export const moduleVariable = "moduleVar"
            """,
            "module.js"
        )

        it.evaluate(
            """
                import { moduleFunction, moduleVariable } from "module.js"
            """
        )

        it.evaluate("moduleFunction()").assertEqualsTo("moduleF")
        it.evaluate("moduleVariable").assertEqualsTo("moduleVar")

        it.reset()

        assertFailsWith<ReferenceError> { it.evaluate("moduleVariable") }
        assertFailsWith<ReferenceError> { it.evaluate("moduleFunction") }

        it.evaluate(
            """
                import moduleVariable from "module.js"
                import moduleFunction from "module.js"
            """
        )
        it.evaluate("moduleFunction()").assertEqualsTo("moduleF")
        it.evaluate("moduleVariable").assertEqualsTo("moduleVar")
    }

    @Test
    fun esm_global_objects_share() = engineTest {
        it.compile(
            """
                export function moduleFunction() { 
                    Number.test1 = 1
                }
                Number.test2 = 2
            """,
            "module.js"
        )

        it.evaluate(
            """
                import moduleFunction from "module.js"
                moduleFunction()
            """
        )
        it.evaluate("Number.test1").assertEqualsTo(1L)
        it.evaluate("Number.test2").assertEqualsTo(2L)
    }

    @Test
    fun esm_cache() = engineTest {
        it.compile(
            """
                export const moduleVariable = "moduleVariable"
                export function moduleFunction() { return Number.test }
               
                if (Number.test != undefined) {
                    throw Error()
                }
                
                Number.test = 'test'
            """,
            "module.js"
        )

        it.evaluate(
            """
                import moduleFunction from "module.js"
                import moduleVariable from "module.js"
                
                moduleFunction()
            """
        ).assertEqualsTo("test")

        it.reset()

        assertFailsWith<ReferenceError> { it.evaluate("moduleVariable") }
        assertFailsWith<ReferenceError> { it.evaluate("moduleFunction") }

        it.evaluate(
            """
                import { moduleFunction, moduleVariable} from "module.js"
                moduleFunction()
            """
        ).assertEqualsTo("test")
    }

    @Test
    fun esm_module_name_as_variable() = engineTest {
        it.compile(
            """
                export const moduleVariable = "var"
            """,
            "module.js"
        )

        it.evaluate(
            """
                const moduleName = 'module.js'
                import moduleVariable from moduleName
                
                moduleVariable
            """
        ).assertEqualsTo("var")
    }

    @Test
    fun esm_export_from_nonmodule() = engineTest {
        assertFailsWith<SyntaxError> {
            it.evaluate("export const moduleVariable = 1")
        }
    }

    @Test
    fun esm_import_from_module() {
        engineTest {
            it.compile("export const var1 = 1", "m1.js")
            it.compile("import var1 from 'm1.js'; export const var2 = var1 + 2", "m2.js")
            it.evaluate("import var2 from 'm2.js'; var2").assertEqualsTo(3L)
        }

        engineTest {
            it.compile("import var1 from 'm1.js'; export const var2 = var1 + 2", "m2.js")
            it.compile("export const var1 = 1", "m1.js")
            it.evaluate("import var2 from 'm2.js'; var2").assertEqualsTo(3L)
        }
    }

    @Test
    fun commonjs() = engineTest {
        it.compile(
            """
                module.exports.moduleFunction = function() { return "moduleF" }
                module.exports.moduleVariable = "moduleVar"
            """,
            "module.js"
        )

        it.evaluate(
            """
                const module = require('module.js')
            """
        )

        it.evaluate("module.moduleFunction()").assertEqualsTo("moduleF")
        it.evaluate("module.moduleVariable").assertEqualsTo("moduleVar")
    }

    @Test
    fun commonjs_esm_mix() = engineTest {
        it.compile(
            """
                module.exports.moduleFunction = function() { return "moduleF" }
                export const moduleVariable = "moduleVar"
            """,
            "module.js"
        )

        it.evaluate(
            """
                const module = require('module.js')
                import { moduleFunction, moduleVariable } from "module.js"
            """
        )

        it.evaluate("module.moduleFunction()").assertEqualsTo("moduleF")
        it.evaluate("module.moduleVariable").assertEqualsTo("moduleVar")
        it.evaluate("moduleFunction()").assertEqualsTo("moduleF")
        it.evaluate("moduleVariable").assertEqualsTo("moduleVar")
    }
}