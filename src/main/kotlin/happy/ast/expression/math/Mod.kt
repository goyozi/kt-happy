package happy.ast.expression.math

import happy.Loc
import happy.ast.expression.Expression
import happy.integer

data class Mod(val left: Expression, val right: Expression, override val loc: Loc) : Expression {

    override fun type() = integer.also {
        left.type()
        right.type()
    }

    override fun eval() = intEval()

    override fun intEval() = left.intEval() % right.intEval()
}