package happy

import HappyLexer
import HappyParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import kotlin.test.assertEquals

class ScopeTest {

    @BeforeEach
    fun setUp() {
        resetContext()
    }

    @Test
    fun fileScope() {
        exec("let x = 5;")
        assertExpression("x", 5)

        val importFile = File("build/tmp/example.happy")
        try {
            importFile.writeText(
                """
                let y = 10;
                
                function visible(): Integer { y }
                function invisible(): String { "can't call me" }
                //function wrong(): Integer { x }
                function timesFive(z: Integer): Integer { z * 5 }
            """
            )
            exec("import build.tmp.example.{visible, timesFive}")
            assertExpression("visible()", 10)
            assertExpression("timesFive(x)", 25)

            assertTypeError("invisible();", UnknownIdentifier("invisible", "1:0-1:0".loc))

//            exec("import build.tmp.example.{wrong}")
            assertTypeError("y;", UnknownIdentifier("y", "1:0-1:0".loc))
//            assertThrows<IllegalStateException>("variable in importing file") { exec("wrong()") }
        } finally {
            importFile.delete()
        }
    }

    @Test
    fun blockScope() {
        exec("let x = 5;")
        assertExpression("{ x }", 5)
        assertExpression("{ { x } }", 5)
        assertExpression("{ x = 10; x }", 10)
        assertExpression("x", 10)
        assertExpression("{ let x = 15; x }", 15)
        assertExpression("x", 10)
        assertExpression("{ x = 20; { let x = 25; x } }", 25)
        assertExpression("x", 20)
        assertTypeError("{ let y = 5; y }; y;", UnknownIdentifier("y", "1:18-1:18".loc))
    }

    @Test
    fun functionScope() {
        exec("let x = 5;")
        exec("function getx(): Integer { x }")
        assertExpression("getx()", 5)
        exec("function setx(): Integer { x = 10; x }")
        assertExpression("setx()", 10)
        assertExpression("x", 10)
        exec("function shadowx(): Integer { let x = 15; x }")
        assertExpression("shadowx()", 15)
        assertExpression("x", 10)
        exec("function argshadowx(x: Integer): Integer { x = 20; x }")
        assertExpression("argshadowx(x)", 20)
        assertExpression("x", 10)
        exec("function leftovers(y: Integer): Integer { let z = y + 5; z }")
        assertThrows<IllegalStateException> { exec("leftovers(x); y;") }
        assertThrows<IllegalStateException> { exec("leftovers(x); z;") }
        exec("function wrong(): Integer { invisible }")
        exec("function crossover(): Integer { let invisible = 5; wrong() }")
        assertThrows<IllegalStateException> { exec("crossover();") }
    }

    @Test
    fun forLoopScope() {
        exec("let i = 42;")
        exec("let x = 5;")
        exec("for i in 1..5 { x = x + i; }")
        assertExpression("i", 42)
        assertExpression("x", 20)
        exec("for i in 1..5 { let x = x + i; }")
        assertExpression("x", 20)
        assertThrows<IllegalStateException> { exec("for j in 1..5 {} j;") }
        assertThrows<IllegalStateException> { exec("for j in 1..5 { let y = 5; } y;") }
    }

    @Test
    fun whileLoopScope() {
        exec("let x = 5;")
        exec("while x < 10 { x = x + 1; }")
        assertExpression("x", 10)
        exec(
            """
            let stop = false;
            while !stop {
              let x = 15;
              stop = true;
            }
        """
        )
        assertExpression("x", 10)
        assertThrows<IllegalStateException> {
            exec(
                """
                let stop = false;
                while !stop {
                  let y = 5;
                  stop = true;
                }
                y;
            """
            )
        }
    }

    private fun exec(code: String) {
        val parser = HappyParser(CommonTokenStream(HappyLexer(CharStreams.fromString(code))))
        val sourceFile = ParseTreeToAst().visitSourceFile(parser.sourceFile())
        sourceFile.typeCheck()
        sourceFile.eval()
    }

    private fun assertExpression(code: String, expected: Any) {
        val parser = HappyParser(CommonTokenStream(HappyLexer(CharStreams.fromString(code))))
        val expression = ParseTreeToAst().visitExpression(parser.expression())
        expression.type()
        assertEquals(expected, expression.eval())
    }

    private fun assertTypeError(code: String, expected: TypeError) {
        val parser = HappyParser(CommonTokenStream(HappyLexer(CharStreams.fromString(code))))
        val sourceFile = ParseTreeToAst().visitSourceFile(parser.sourceFile())
        sourceFile.typeCheck()
        assertEquals(listOf(expected), typeErrors)
        typeErrors.clear()
    }
}