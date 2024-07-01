import io.github.goyozi.kthappy.Interpreter
import io.github.goyozi.kthappy.Value
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class InterpreterTest {

    lateinit var interpreter: Interpreter

    @BeforeEach
    fun setUp() {
        interpreter = Interpreter()
    }

    @Test
    fun intLiteral() {
        assertExpression("123", Value("Integer", 123))
    }

    @Test
    fun stringLiteral() {
        assertExpression("\"it works\"", Value("String", "it works"))
    }

    @Test
    fun arithmeticExpression() {
        assertExpression("1 + 2", Value("Integer", 3))
        assertExpression("3 - 2", Value("Integer", 1))
        assertExpression("2 * 3", Value("Integer", 6))
        assertExpression("6 / 3", Value("Integer", 2))
        assertExpression("8 % 3", Value("Integer", 2))
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

    @Test
    fun comparisonExpression() {
        assertExpression("1 == 1", Value("Boolean", true))
        assertExpression("1 == 2", Value("Boolean", false))
        assertExpression("1 != 2", Value("Boolean", true))
        assertExpression("1 != 1", Value("Boolean", false))
        assertExpression("2 > 1", Value("Boolean", true))
        assertExpression("1 > 2", Value("Boolean", false))
        assertExpression("1 < 2", Value("Boolean", true))
        assertExpression("2 < 1", Value("Boolean", false))
        assertExpression("1 >= 1", Value("Boolean", true))
        assertExpression("1 >= 2", Value("Boolean", false))
        assertExpression("1 <= 1", Value("Boolean", true))
        assertExpression("2 <= 1", Value("Boolean", false))
    }

    @Test
    fun operatorPrecedence() {
        assertExpression("1 + 2 * 2 + 1", Value("Integer", 6))
        assertExpression("3 + 2 > 3 + 1", Value("Boolean", true))
    }

    @Test
    fun forLoop() {
        assertExpression(
            """
            {
              let x = 0
              for i in 1..3 { x = x + i }
              x
            }
            """,
            Value("Integer", 6)
        )
    }

    @Test
    fun whileLoop() {
        assertExpression(
            """
            {
              let x = 0
              let i = 0
              while i < 3 {
                i = i + 1
                x = x + i
              }
              x
            }
            """,
            Value("Integer", 6)
        )
    }

    @Test
    fun functionCall() {
        defineFunction(
            """
            function add(a: Integer, b: Integer): Integer {
              a + b
            }
            """
        )
        assertExpression("add(5, 10)", Value("Integer", 15))
    }

    private fun assertExpression(code: String, expected: Value) {
        val parser = HappyParser(CommonTokenStream(HappyLexer(CharStreams.fromString(code))))
        val result = interpreter.visitExpression(parser.expression())
        assertEquals(expected, result)
    }

    private fun defineFunction(code: String) {
        val parser = HappyParser(CommonTokenStream(HappyLexer(CharStreams.fromString(code))))
        interpreter.visitFunction(parser.function())
    }
}