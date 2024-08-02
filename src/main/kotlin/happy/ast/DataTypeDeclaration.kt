package happy.ast

import happy.*

data class DataTypeDeclaration(
    val name: String,
    val fields: Map<String, TypeAnnotation>,
    override val loc: Loc
) : TypeDeclaration {

    override fun declareType() {
        val declaredType = DataType(
            name,
            fields.mapValues {
                try {
                    it.value.toType()
                } catch (_: IllegalStateException) {
                    typeErrors.add(UndeclaredType(it.value.name, loc))
                    nothing
                }
            })

        typingScope.define(name, declaredType)
    }

    override fun eval() = Unit
}