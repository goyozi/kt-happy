package io.github.goyozi.kthappy

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

data class InterfaceType(override val name: String, val functions: Set<FunctionType>) : Type {
    override fun assignableFrom(other: Type, scope: Scope<*>?) =
        super.assignableFrom(other, scope)
                || (scope != null && functions.all { presentIn(it, scope, other) })

    private fun presentIn(function: FunctionType, scope: Scope<*>, targetType: Type): Boolean {
        val scopeFunction = scope.get(function.name)

        // should the type be an exact match?
        // todo: check other args
        if (scopeFunction is FunctionType)
            return scopeFunction.variants.any { it.key.getOrNull(0) == targetType }

        if (scopeFunction is Function)
            return scopeFunction.variants.any { it.key.getOrNull(0) == targetType }

        return false
    }
}

data class GenericType(override val name: String) : Type

data class FunctionType(override val name: String, val variants: Map<List<Type>, Type>) : Type

data class SymbolType(override val name: String) : Type

fun union(types: Set<Type>): Type {
    val flattenedTypes = types.flatMap { if (it is EnumType) it.types else setOf(it) }.toSet()
    return if (flattenedTypes.size == 1) return flattenedTypes.single()
    else EnumType("Inline", flattenedTypes)
}