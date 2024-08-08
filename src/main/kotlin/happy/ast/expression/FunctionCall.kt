package happy.ast.expression

import happy.*
import happy.Function

data class FunctionCall(val target: Expression, val arguments: List<Expression>, override val loc: Loc) : Expression {
    private val iioTypes = mutableMapOf<Expression, InterfaceType>()

    private lateinit var resolvedCall: Function
    private lateinit var resolvedReturnType: Type
    private var objectArguments = mutableMapOf<String, Expression>()
    private var intArguments = mutableMapOf<String, Expression>()

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
            resolvedReturnType = function.returnType

            for (i in function.arguments.indices) {
                val declaredArgumentType = function.arguments[i].type
                val actualArgumentType = arguments[i]
                checkType(declaredArgumentType, actualArgumentType.type, this)
            }

            populateContext(function, arguments)
        } else {
            val function = functionType.getVariant(arguments.map { it.type }, typingScope) // todo: might fail badly!
            resolvedReturnType = function.returnType
            populateContext(function, arguments)
        }

        return resolvedReturnType
    }

    private fun populateContext(function: Function, arguments: MutableList<Argument>) {
        for (i in arguments.indices) {
            val declaredType = function.arguments[i].type
            val actualType = arguments[i].type
            val argumentExpression = arguments[i].value as Expression

            if (declaredType is InterfaceType && declaredType != actualType) {
                iioTypes[argumentExpression] = declaredType
            }

            if (declaredType == integer) {
                intArguments[function.arguments[i].name] = argumentExpression
            } else {
                objectArguments[function.arguments[i].name] = argumentExpression
            }
        }

        resolvedCall = function
    }

    override fun eval(): Any {
        if (resolvedReturnType == integer) return intEval()

        evalArgumentsAndEnterScope()
        val result = resolvedCall.invoke()
        scope.leave()
        return result
    }

    override fun intEval(): Int {
        evalArgumentsAndEnterScope()
        val result = resolvedCall.intInvoke()
        scope.leave()
        return result
    }

    private fun evalArgumentsAndEnterScope() {
        val functionScope = Layer(resolvedCall.parentScope)

        objectArguments.forEach { (name, expression) ->
            val iioType = iioTypes[expression]
            val argValue = if (iioType == null) {
                expression.eval()
            } else {
                toIIO(iioType, expression.eval())
            }
            functionScope.define(name, argValue)
        }

        intArguments.forEach { (name, expression) ->
            functionScope.define(name, expression.intEval())
        }

        scope.enterRaw(functionScope)
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