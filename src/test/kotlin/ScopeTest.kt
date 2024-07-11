import io.github.goyozi.kthappy.Interpreter
import io.github.goyozi.kthappy.Value
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import kotlin.test.assertEquals

class ScopeTest {

    lateinit var interpreter: Interpreter

    @BeforeEach
    fun setUp() {
        interpreter = Interpreter()
    }

    @Test
    fun fileScope() {
        exec("let x = 5")
        assertExpression("x", Value("Integer", 5))

        val importFile = File("build/tmp/example.happy")
        try {
            importFile.writeText(
                """
                let y = 10
                
                function visible(): Integer { y }
                function invisible(): String { "can't call me" }
                function wrong(): Integer { x }
            """
            )
            exec("import build.tmp.example.{visible}")
            assertExpression("visible()", Value("Integer", 10))

            assertThrows<IllegalStateException>("unimported function") { exec("invisible()") }

            exec("import build.tmp.example.{wrong}")
            assertThrows<IllegalStateException>("variable in imported file") { exec("y") }
            assertThrows<IllegalStateException>("variable in importing file") { exec("wrong()") }
        } finally {
            importFile.delete()
        }
    }

    @Test
    fun blockScope() {
        exec("let x = 5")
        assertExpression("{ x }", Value("Integer", 5))
        assertExpression("{ { x } }", Value("Integer", 5))
        assertExpression("{ x = 10 x }", Value("Integer", 10))
        assertExpression("x", Value("Integer", 10))
        assertExpression("{ let x = 15 x }", Value("Integer", 15))
        assertExpression("x", Value("Integer", 10))
        assertExpression("{ x = 20 { let x = 25 x } }", Value("Integer", 25))
        assertExpression("x", Value("Integer", 20))
        assertThrows<IllegalStateException> { exec("{ let y = 5 y } y") }
    }

    @Test
    fun functionScope() {
        exec("let x = 5")
        exec("function getx(): Integer { x }")
        assertExpression("getx()", Value("Integer", 5))
        exec("function setx(): Integer { x = 10 x }")
        assertExpression("setx()", Value("Integer", 10))
        assertExpression("x", Value("Integer", 10))
        exec("function shadowx(): Integer { let x = 15 x }")
        assertExpression("shadowx()", Value("Integer", 15))
        assertExpression("x", Value("Integer", 10))
        exec("function argshadowx(x: Integer): Integer { x = 20 x }")
        assertExpression("argshadowx(x)", Value("Integer", 20))
        assertExpression("x", Value("Integer", 10))
        exec("function leftovers(y: Integer): Integer { let z = y + 5 z }")
        assertThrows<IllegalStateException> { exec("leftovers(x) y") }
        assertThrows<IllegalStateException> { exec("leftovers(x) z") }
        exec("function crossover(): Integer { let invisible = 5 wrong() }")
        exec("function wrong(): Integer { invisible }")
        assertThrows<IllegalStateException> { exec("crossover()") }
    }

    @Test
    fun forLoopScope() {
        exec("let i = 42")
        exec("let x = 5")
        exec("for i in 1..5 { x = x + i }")
        assertExpression("i", Value("Integer", 42))
        assertExpression("x", Value("Integer", 20))
        exec("for i in 1..5 { let x = x + i }")
        assertExpression("x", Value("Integer", 20))
        assertThrows<IllegalStateException> { exec("for j in 1..5 {} j") }
        assertThrows<IllegalStateException> { exec("for j in 1..5 { let y = 5 } y") }
    }

    @Test
    fun whileLoopScope() {
        exec("let x = 5")
        exec("while x < 10 { x = x + 1 }")
        assertExpression("x", Value("Integer", 10))
        exec(
            """
            let stop = false
            while !stop {
              let x = 15
              stop = true
            }
        """
        )
        assertExpression("x", Value("Integer", 10))
        assertThrows<IllegalStateException> {
            exec(
                """
                let stop = false
                while !stop {
                  let y = 5
                  stop = true
                }
                y
            """
            )
        }
    }

    private fun exec(code: String) {
        val parser = HappyParser(CommonTokenStream(HappyLexer(CharStreams.fromString(code))))
        interpreter.visitSourceFile(parser.sourceFile())
    }

    private fun assertExpression(code: String, expected: Value) {
        val parser = HappyParser(CommonTokenStream(HappyLexer(CharStreams.fromString(code))))
        val result = interpreter.visitExpression(parser.expression())
        assertEquals(expected, result)
    }
}