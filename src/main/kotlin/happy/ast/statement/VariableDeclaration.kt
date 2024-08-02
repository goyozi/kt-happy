package happy.ast.statement

import happy.*
import happy.ast.TypeAnnotation
import happy.ast.expression.Expression

data class VariableDeclaration(
    val name: String,
    val typeAnnotation: TypeAnnotation?,
    val value: Expression?,
    override val loc: Loc
) : Statement {

    override fun typeCheck() {
        var declaredType: Type? = null

        if (typeAnnotation != null) {
            try {
                declaredType = typeAnnotation.toType()
            } catch (_: IllegalStateException) {
                typeErrors.add(UndeclaredType(typeAnnotation.name, loc))
                typingScope.define(name, nothing)
                return
            }
        }

        val expressionType = value?.type()

        typingScope.define(name, declaredType ?: expressionType!!)

        if (declaredType != null && expressionType != null) {
            checkType(declaredType, expressionType, this)
        }
    }

    override fun eval() {
        if (value != null) scope.define(name, value.eval())
    }
}