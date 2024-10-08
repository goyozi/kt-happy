package happy

sealed interface Type {
    val name: String

    fun assignableFrom(other: Type, scope: Scope<*>? = null) = this == any || this == other
}

data class BuiltInType(override val name: String) : Type

data class DataType(override val name: String, val fields: Map<String, Type>) : Type

data class EnumType(override val name: String, val types: Set<Type>) : Type {
    override fun assignableFrom(other: Type, scope: Scope<*>?) =
        super.assignableFrom(other, scope)
                || if (other is EnumType) types.containsAll(other.types) else types.contains(other)
}

data class InterfaceType(override val name: String, val functions: Set<OverloadedFunction>) : Type {
    override fun assignableFrom(other: Type, scope: Scope<*>?) =
        super.assignableFrom(other, scope)
                || (scope != null && functions.all { presentIn(it, scope, other) })

    private fun presentIn(function: OverloadedFunction, scope: Scope<*>, targetType: Type): Boolean {
        val scopeFunction = scope.get(function.name) as OverloadedFunction
        // should the type be an exact match?
        // todo: check other args
        return scopeFunction.functions.any { it.arguments.getOrNull(0)?.type == targetType }
    }

    fun completeFunctions(type: Type = this) = functions.map { of ->
        OverloadedFunction(
            of.name,
            of.functions.map { f ->
                InterfaceFunction(
                    f.name,
                    listOf(Parameter("self", type)) + f.arguments,
                    f.returnType
                )
            }
        )
    }
}

data class InterfaceFunction(
    override val name: String,
    override val arguments: List<Parameter>,
    override val returnType: Type
) : Function {
    override lateinit var parentScope: Layer<Any>

    override fun invoke(): Any {
        val resolved = (scope.get(arguments[0].name) as IIO)
            .getVariant(name, this.arguments.map { it.type }.drop(1), scope)
        return resolved.invoke()
    }
}

data class GenericType(override val name: String) : Type

data class SymbolType(override val name: String) : Type

fun union(types: Set<Type>): Type {
    val flattenedTypes = types.flatMap { if (it is EnumType) it.types else setOf(it) }.toSet()
    return if (flattenedTypes.size == 1) return flattenedTypes.single()
    else EnumType("Inline", flattenedTypes)
}