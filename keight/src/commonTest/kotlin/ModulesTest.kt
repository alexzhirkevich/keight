import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.ReferenceError
import io.github.alexzhirkevich.keight.js.SyntaxError
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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
    fun exported_properties_use_from_module() = engineTest {
        it.compile(
            """
                    export const variable = 1
                    export default function funcDefault() { return variable }
                    export function func() { return funcDefault() }
                    export function test() { return func() }
                """,
            "module.js"
        )
        it.evaluate("import test from 'module.js'; test()").assertEqualsTo(1L)
    }

    @Test
    fun aggregating_esm() = engineTest {
        it.compile(
            """
                    export const var1 = 1
                    export function func1(){ return 'test' + var1 }
                """,
            "m1.js"
        )
        it.compile(
            """
                    export default "def"
                    export function func2(){ return 'test2' }
                """,
            "m2.js"
        )
        it.compile(
            """
                    export const something = "something"
                """,
            "m3.js"
        )
        it.compile(
            """
                    export * from "m3.js"
                    export * as obj from "m3.js"
                    export { var1, func1 } from "m1.js"
                    export { default, func2 as function2 } from "m2.js"
                """,
            "aggregate.js"
        )

        it.evaluate("import { def, var1, func1, function2, something, obj } from 'aggregate.js'")

        it.evaluate("def").assertEqualsTo("def")
        it.evaluate("var1").assertEqualsTo(1L)
        it.evaluate("func1()").assertEqualsTo("test1")
        it.evaluate("function2()").assertEqualsTo("test2")
        it.evaluate("something").assertEqualsTo("something")
        it.evaluate("obj.something").assertEqualsTo("something")
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