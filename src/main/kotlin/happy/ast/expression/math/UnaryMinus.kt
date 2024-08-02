package happy.ast.expression.math

import happy.Loc
import happy.ast.expression.Expression
import happy.integer

data class UnaryMinus(val expression: Expression, override val loc: Loc) : Expression {

    override fun type() = integer.also {
        expression.type()
    }

    override fun eval() = -(expression.eval() as Int)
}