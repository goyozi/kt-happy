package happy.ast

import happy.*

data class TypeAnnotation(val name: String, val genericParameter: String?, override val loc: Loc) : AstNode {

    fun toType(): Type {
        var declaredType = typingScope.get(name)

        if (declaredType is EnumType && genericParameter != null) {
            // todo: should be a union ??
            declaredType = EnumType(declaredType.name, declaredType.types.map {
                if (it is GenericType) typingScope.get(genericParameter)
                else it
            }.toSet())
        }
        return declaredType
    }

    override fun eval() = Unit
}
