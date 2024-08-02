package happy.ast.expression

import happy.Loc
import happy.boolean

data class Negation(val expression: Expression, override val loc: Loc) : Expression {

    override fun type() = boolean

    override fun eval() = !(expression.eval() as Boolean)
}