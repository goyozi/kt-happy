package happy.ast

import happy.EnumType
import happy.GenericType
import happy.Loc
import happy.ast.expression.literal.SymbolLiteral
import happy.typingScope

data class EnumTypeDeclaration(
    val name: String,
    val genericParameter: String?,
    val subtypes: List<TypeAnnotation>,
    val symbols: List<SymbolLiteral>,
    override val loc: Loc
) : TypeDeclaration {

    override fun declareType() {
        val subtypeSet = subtypes
            .map {
                if (it.name == genericParameter) GenericType(it.name)
                else it.toType()
            }
            .toSet()
        val symbolSet = symbols.map { it.type() }

        typingScope.define(name, EnumType(name, subtypeSet + symbolSet))
    }

    override fun eval() = Unit
}