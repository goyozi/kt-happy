package happy.ast

import happy.Loc

data class FunctionSignature(
    val name: String,
    val arguments: Map<String, TypeAnnotation>,
    val returnType: TypeAnnotation,
    override val loc: Loc
) : AstNode {
    override fun eval() = Unit
}