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
        exec("var x = 5;")
        assertExpression("x", 5)

        val importFile = File("build/tmp/example.happy")
        try {
            importFile.writeText(
                """
                var y = 10;
                
                func visible(): Integer { y }
                func invisible(): String { "can't call me" }
                //func wrong(): Integer { x }
                func timesFive(z: Integer): Integer { z * 5 }
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
        exec("var x = 5;")
        assertExpression("{ x }", 5)
        assertExpression("{ { x } }", 5)
        assertExpression("{ x = 10; x }", 10)
        assertExpression("x", 10)
        assertExpression("{ var x = 15; x }", 15)
        assertExpression("x", 10)
        assertExpression("{ x = 20; { var x = 25; x } }", 25)
        assertExpression("x", 20)
        assertTypeError("{ var y = 5; y }; y;", UnknownIdentifier("y", "1:18-1:18".loc))
    }

    @Test
    fun functionScope() {
        exec("var x = 5;")
        exec("func getx(): Integer { x }")
        assertExpression("getx()", 5)
        exec("func setx(): Integer { x = 10; x }")
        assertExpression("setx()", 10)
        assertExpression("x", 10)
        exec("func shadowx(): Integer { var x = 15; x }")
        assertExpression("shadowx()", 15)
        assertExpression("x", 10)
        exec("func argshadowx(x: Integer): Integer { x = 20; x }")
        assertExpression("argshadowx(x)", 20)
        assertExpression("x", 10)
        exec("func leftovers(y: Integer): Integer { var z = y + 5; z }")
        assertThrows<IllegalStateException> { exec("leftovers(x); y;") }
        assertThrows<IllegalStateException> { exec("leftovers(x); z;") }
        exec("func wrong(): Integer { invisible }")
        exec("func crossover(): Integer { var invisible = 5; wrong() }")
        assertThrows<IllegalStateException> { exec("crossover();") }
    }

    @Test
    fun forLoopScope() {
        exec("var i = 42;")
        exec("var x = 5;")
        exec("for i in 1..5 { x = x + i; }")
        assertExpression("i", 42)
        assertExpression("x", 20)
        exec("for i in 1..5 { var x = x + i; }")
        assertExpression("x", 20)
        assertThrows<IllegalStateException> { exec("for j in 1..5 {} j;") }
        assertThrows<IllegalStateException> { exec("for j in 1..5 { var y = 5; } y;") }
    }

    @Test
    fun whileLoopScope() {
        exec("var x = 5;")
        exec("while x < 10 { x = x + 1; }")
        assertExpression("x", 10)
        exec(
            """
            var stop = false;
            while !stop {
              var x = 15;
              stop = true;
            }
        """
        )
        assertExpression("x", 10)
        assertThrows<IllegalStateException> {
            exec(
                """
                var stop = false;
                while !stop {
                  var y = 5;
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