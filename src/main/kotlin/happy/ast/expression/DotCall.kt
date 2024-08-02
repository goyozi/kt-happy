package happy.ast.expression

import happy.*

data class DotCall(val target: Expression, val id: String, override val loc: Loc) : Expression {

    override fun type(): Type {
        val targetType = target.type()
        val dataType = typingScope.get(targetType.name)
        if (dataType is DataType) {
            val fieldType = dataType.fields[id]
            if (fieldType != null) return fieldType
        }

        try {
            val functionType = typingScope.get(id)

            if (functionType !is OverloadedFunction)
                throw Error("$id is not a function: $functionType")

            // todo: proper overloading + tests
            return PreAppliedFunction(id, Argument(target, targetType))
        } catch (_: IllegalStateException) {
            typeErrors.add(UndeclaredField(id, targetType, loc))
            return nothing
        }
    }

    override fun eval(): Any {
        return (target.eval() as DataObject).values[id]!! // todo: proper error message
    }
}