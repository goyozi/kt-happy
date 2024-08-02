package happy.ast.expression.literal

import happy.Loc
import happy.SymbolType
import happy.ast.expression.Expression

data class SymbolLiteral(val label: String, override val loc: Loc) : Expression {

    override fun type() = SymbolType(label)

    override fun eval() = type()
}