package happy.ast.expression.math

import happy.Loc
import happy.ast.expression.Expression
import happy.integer

data class Multiplication(val left: Expression, val right: Expression, override val loc: Loc) : Expression {

    override fun type() = integer.also {
        left.type()
        right.type()
    }

    override fun eval() = left.eval() as Int * right.eval() as Int
}