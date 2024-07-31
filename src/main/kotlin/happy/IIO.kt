package happy

data class IIO(val value: Any, val type: Type, val functions: Set<OverloadedFunction>) {

    fun getVariant(name: String, argTypes: List<Type>, scope: Scope<*>) =
        functions.single { it.name == name }.getVariant(listOf(type) + argTypes, scope)
}