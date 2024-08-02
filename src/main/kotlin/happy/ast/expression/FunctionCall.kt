package happy.ast.expression

import happy.*
import happy.Function

data class FunctionCall(val target: Expression, val arguments: List<Expression>, override val loc: Loc) : Expression {
    private val iioTypes = mutableMapOf<Expression, InterfaceType>()

    private lateinit var resolvedCall: Function
    private lateinit var resolvedArguments: List<Expression>

    override fun type(): Type {
        var functionType = target.type()
        val arguments = mutableListOf<Argument>()

        if (functionType is PreAppliedFunction) {
            arguments.add(functionType.firstArgument)
            functionType = typingScope.get(functionType.name)
        }

        if (functionType !is OverloadedFunction) {
            // todo: type error: not a function
            return nothing
        }

        this.arguments.map { Argument(it, it.type()) }.forEach(arguments::add)

        if (functionType.functions.size == 1) {
            val function = functionType.functions.single()
            val returnType = function.returnType

            for (i in function.arguments.indices) {
                val declaredArgumentType = function.arguments[i].type
                val actualArgumentType = arguments[i]
                checkType(declaredArgumentType, actualArgumentType.type, this)
            }

            populateContext(function, arguments)

            return returnType
        } else {
            val function = functionType.getVariant(arguments.map { it.type }, typingScope) // todo: might fail badly!
            populateContext(function, arguments)
            return function.returnType
        }
    }

    private fun populateContext(function: Function, arguments: MutableList<Argument>) {
        for (i in arguments.indices) {
            val declaredType = function.arguments[i].type
            val actualType = arguments[i].type
            if (declaredType is InterfaceType && declaredType != actualType) {
                iioTypes[arguments[i].value as Expression] = declaredType
            }
        }

        resolvedCall = function
        resolvedArguments = arguments.map { it.value } as List<Expression>
    }

    override fun eval(): Any {
        val arguments = Array<Any>(resolvedArguments.size) {}

        for (it in arguments.indices) {
            val expression = resolvedArguments[it]
            val iioType = iioTypes[expression]
            if (iioType == null) {
                arguments[it] = expression.eval()
            } else {
                arguments[it] = toIIO(iioType, expression.eval())
            }
        }

        return resolvedCall.invoke(arguments)
    }

    private fun toIIO(argumentType: InterfaceType, argumentValue: Any): IIO {
        val value = argumentValue as DataObject // for simplicity
        val boundFunctions = argumentType.completeFunctions(value.type)
            .asSequence()
            .flatMap { it.functions }
            .map { (scope.get(it.name) as OverloadedFunction).getStaticVariant(it.arguments.map { it.type }, scope) }
            .groupBy { it.name }
            .map { OverloadedFunction(it.key, it.value) }
            .toSet()
        return IIO(value, value.type, boundFunctions)
    }
}