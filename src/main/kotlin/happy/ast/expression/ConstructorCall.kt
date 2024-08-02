package happy.ast.expression

import happy.*

data class ConstructorCall(
    val typeName: String,
    val assignments: Map<String, Expression>,
    override val loc: Loc
) : Expression {
    lateinit var type: DataType

    override fun type(): Type {
        type = try {
            typingScope.get(typeName) as DataType // todo: we actually have to check this
        } catch (_: IllegalStateException) {
            typeErrors.add(UndeclaredType(typeName, loc))
            return DataType(typeName, emptyMap())
        }

        for (field in type.fields.keys) {
            if (!assignments.containsKey(field)) {
                typeErrors.add(UninitializedField(field, type, loc))
            }
        }
        for (assignment in assignments) {
            val declaredType = type.fields[assignment.key]
            if (declaredType == null) {
                typeErrors.add(UndeclaredField(assignment.key, type, loc))
            } else {
                val actualType = assignment.value.type()
                checkType(declaredType, actualType, this)
            }
        }
        return type
    }

    override fun eval() = DataObject(type, assignments.mapValues { it.value.eval() })
}