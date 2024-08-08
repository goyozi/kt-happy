package happy.ast.expression.comparison

import happy.Loc
import happy.ast.expression.Expression
import happy.boolean

data class LesserEqual(val left: Expression, val right: Expression, override val loc: Loc) : Expression {

    override fun type() = boolean.also {
        left.type()
        right.type()
    }

    override fun eval() = left.intEval() <= right.intEval()
}