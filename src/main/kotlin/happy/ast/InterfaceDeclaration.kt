package happy.ast

import happy.*
import happy.Function

data class InterfaceDeclaration(
    val name: String,
    val signatures: List<FunctionSignature>,
    override val loc: Loc
) : TypeDeclaration {
    lateinit var type: InterfaceType
    private val completeFunctions = mutableListOf<Function>()

    override fun declareType() {
        val interfaceType = InterfaceType(
            name,
            signatures.map {
                val arguments = it.arguments.map { arg ->
                    Parameter(arg.key, arg.value.toType())
                }
                OverloadedFunction(
                    it.name,
                    listOf(InterfaceFunction(it.name, arguments, it.returnType.toType()))
                )
            }.toSet()
        )
        typingScope.define(name, interfaceType)

        type = interfaceType

        interfaceType.completeFunctions().forEach { functionType ->
            // todo: unhack
            defineFunctionType(functionType.name, functionType.functions.single())
            completeFunctions.add(functionType.functions.single())
        }
    }

    override fun eval() {
        completeFunctions.forEach { function ->
            // todo: unhack
            function.parentScope = scope.stack.last()
            defineFunction(function.name, function)
        }
    }
}