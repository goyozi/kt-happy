package io.github.goyozi.kthappy

sealed interface Type {
    val name: String

    // todo: any handling
    fun assignableFrom(other: Type) = this == other
}

data class BuiltInType(override val name: String) : Type

data class DataType(override val name: String, val fields: Map<String, Type>) : Type

data class EnumType(override val name: String, val types: Set<Type>) : Type {
    override fun assignableFrom(other: Type) =
        if (other is EnumType) types.containsAll(other.types) else types.contains(other)
}

data class GenericType(override val name: String) : Type

data class FunctionType(override val name: String, val variants: Map<List<Type>, Type>) : Type

data class SymbolType(override val name: String) : Type

fun union(types: Set<Type>): Type {
    val flattenedTypes = types.flatMap { if (it is EnumType) it.types else setOf(it) }.toSet()
    return if (flattenedTypes.size == 1) return flattenedTypes.single()
    else EnumType("Inline", flattenedTypes)
}