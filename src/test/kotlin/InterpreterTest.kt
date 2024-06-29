import io.github.goyozi.kthappy.Interpreter
import io.github.goyozi.kthappy.Value
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class InterpreterTest {

    @Test
    fun intLiteral() {
        assertExpression("123", Value("Integer", 123))
    }

    @Test
    fun stringLiteral() {
        assertExpression("\"it works\"", Value("String", "it works"))
    }

    @Test
    fun expressionBlock() {
        assertExpression(
            """
            {
              let x = 1
              let y = 2
              x + y
            }
            """, Value("Integer", 3)
        )
    }

    @Test
    fun ifExpression() {
        assertExpression("if 2 > 1 { \"it works\" } else { \"it doesn't\" }", Value("String", "it works"));
        assertExpression("if 1 > 2 { \"it doesn't\" } else { \"it works\" }", Value("String", "it works"));
        assertExpression("if 1 > 2 { \"it doesn't\" } else if 3 > 2 { \"it works\" } else { \"it doesn't\" }", Value("String", "it works"));
        assertExpression("if 1 > 2 { \"it doesn't\" } else if 2 > 3 { \"it doesn't\" } else { \"it works\" }", Value("String", "it works"));
    }

    private fun assertExpression(code: String, expected: Value) {
        val parser = HappyParser(CommonTokenStream(HappyLexer(CharStreams.fromString(code))))
        val result = Interpreter().visitExpression(parser.expression())
        assertEquals(expected, result)
    }
}