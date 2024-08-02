package happy.ast.expression

import happy.Loc

data class BracketExpression(val inner: Expression, override val loc: Loc) : Expression {

    override fun type() = inner.type()

    override fun eval() = inner.eval()
}