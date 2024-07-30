package io.github.goyozi.kthappy

import HappyBaseVisitor
import HappyParser

class Interpreter : HappyBaseVisitor<Any>() {
    val scope = Scope<Any>()
    val functionParent = mutableMapOf<Function, Layer<Any>>()

    override fun visitSourceFile(ctx: HappyParser.SourceFileContext): Any {
        builtIns.forEach { defineFunction(it.name, it) }

        ctx.importStatement().forEach(this::visitImportStatement)
        ctx.interface_().forEach(this::visitInterface)
        ctx.function().forEach(this::visitFunction)
        ctx.action().forEach(this::visitAction)
        return Unit
    }

    override fun visitImportStatement(ctx: HappyParser.ImportStatementContext): Any {
        val path = ctx.paths.joinToString("/") { it.text } + ".happy"
        val currentScope = scope.stack.last()
        scope.enter(Layer())
        visitSourceFile(sourceFileTrees[path]!!)
        for (symbol in ctx.symbols) {
            currentScope.bindings[symbol.text] = scope.get(symbol.text)
        }
        scope.leave()
        return Unit
    }

    override fun visitInterface(ctx: HappyParser.InterfaceContext): Any {
        val interfaceType = interfaceTypes.get(ctx)
        interfaceType.completeFunctions().forEach { functionType ->
            // todo: unhack
            defineFunction(functionType.name, functionType.functions.single())
        }
        return Unit
    }

    override fun visitFunction(ctx: HappyParser.FunctionContext): Any {
        val impl = functions.get(ctx)
        defineFunction(ctx.sig.name.text, impl)
        functionParent[impl] = scope.stack.last()
        return Unit
    }

    private fun defineFunction(name: String, impl: Function) {
        try {
            val function = scope.get(name) as OverloadedFunction // todo: might blow up
            val newFunction = OverloadedFunction(name, function.functions + listOf(impl))
            scope.assign(name, newFunction)
        } catch (_: IllegalStateException) {
            scope.define(name, OverloadedFunction(name, listOf(impl)))
        }
    }

    override fun visitVariableDeclaration(ctx: HappyParser.VariableDeclarationContext): Any {
        scope.define(ctx.ID().text, visitExpression(ctx.expression()))
        return Unit
    }

    override fun visitVariableAssignment(ctx: HappyParser.VariableAssignmentContext): Any {
        scope.assign(ctx.ID().text, visitExpression(ctx.expression()))
        return Unit
    }

    override fun visitWhileLoop(ctx: HappyParser.WhileLoopContext): Any {
        scope.enter()
        while (visitExpression(ctx.expression()) == true) ctx.action().forEach(this::visitAction)
        scope.leave()
        return Unit
    }

    override fun visitForLoop(ctx: HappyParser.ForLoopContext): Any {
        scope.enter()
        for (i in ctx.INTEGER_LITERAL(0).text.toInt()..ctx.INTEGER_LITERAL(1).text.toInt()) {
            scope.define(ctx.ID().text, i)
            ctx.action().forEach(this::visitAction)
        }
        scope.leave()
        return Unit
    }

    override fun visitComplexExpression(ctx: HappyParser.ComplexExpressionContext): Any {
        return ctx.postfixExpression().accept(this)
    }

    override fun visitExpressionInBrackets(ctx: HappyParser.ExpressionInBracketsContext): Any {
        return visitExpression(ctx.expression())
    }

    override fun visitBooleanLiteral(ctx: HappyParser.BooleanLiteralContext): Any {
        return ctx.text.toBoolean()
    }

    override fun visitIntegerLiteral(ctx: HappyParser.IntegerLiteralContext): Any {
        return literals.get(ctx)
    }

    override fun visitStringLiteral(ctx: HappyParser.StringLiteralContext): Any {
        return ctx.STRING_LITERAL().text.drop(1).dropLast(1)
    }

    override fun visitNegation(ctx: HappyParser.NegationContext): Any {
        return !(visitExpression(ctx.expression()) as Boolean)
    }

    override fun visitUnaryMinus(ctx: HappyParser.UnaryMinusContext): Any {
        return -(visitExpression(ctx.expression()) as Int)
    }

    override fun visitMultiplicative(ctx: HappyParser.MultiplicativeContext): Any {
        val left = visitExpression(ctx.expression(0)) as Int
        val right = visitExpression(ctx.expression(1)) as Int
        return when (ctx.op.text) {
            "*" -> left * right
            "/" -> left / right
            else -> left % right
        }
    }

    override fun visitAdditive(ctx: HappyParser.AdditiveContext): Any {
        val left = visitExpression(ctx.left)
        val right = visitExpression(ctx.right)
        return binaryOps.get(ctx)(left, right)
    }

    override fun visitComparison(ctx: HappyParser.ComparisonContext): Any {
        val left = visitExpression(ctx.left)
        val right = visitExpression(ctx.right)
        return binaryOps.get(ctx)(left, right)
    }

    override fun visitIdentifier(ctx: HappyParser.IdentifierContext): Any {
        return scope.get(identifiers.get(ctx))
    }

    override fun visitSymbol(ctx: HappyParser.SymbolContext): String {
        return ctx.SYMBOL().text.drop(1)
    }

    override fun visitFunctionCall(ctx: HappyParser.FunctionCallContext): Any {
        val argumentExpressions = resolvedArgumentExpressions.get(ctx)
        val arguments = Array<Any>(argumentExpressions.size) {}

        for (it in arguments.indices) {
            val expression = argumentExpressions[it]
            val iioType = iioTypes.get(expression)
            if (iioType == null) {
                arguments[it] = visitExpression(expression)
            } else {
                arguments[it] = toIIO(iioType, visitExpression(expression))
            }
        }

        return resolvedFunctionCalls.get(ctx).invoke(arguments, this)
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

    override fun visitConstructor(ctx: HappyParser.ConstructorContext): Any {
        return DataObject(
            expressionTypes.get(ctx),
            ctx.keyExpression().associate { it.ID().text to visitExpression(it.expression()) })
    }

    override fun visitDotCall(ctx: HappyParser.DotCallContext): Any {
        val targetExpression = (ctx.parent as HappyParser.ComplexExpressionContext).expression()
        val target = targetExpression.accept(this) as DataObject
        return target.values[ctx.ID().text]!! // todo: proper error message
    }

    override fun visitTypeCast(ctx: HappyParser.TypeCastContext): Any {
        // TODO: actual type cast?
        return (ctx.parent as HappyParser.ComplexExpressionContext).expression().accept(this)
    }

    override fun visitIfExpression(ctx: HappyParser.IfExpressionContext): Any {
        val conditionMet = visitExpression(ctx.condition)
        return if (conditionMet as Boolean) visitExpressionBlock(ctx.ifTrue)
        else if (ctx.elseIf != null) visitIfExpression(ctx.elseIf)
        else visitExpressionBlock(ctx.ifFalse)
    }

    override fun visitMatchExpression(ctx: HappyParser.MatchExpressionContext): Any {
        val matchValue = visitExpression(ctx.expression())
        for (patternValue in ctx.patternValue()) {
            if (matchValue == visitExpression(patternValue.pattern)) {
                return visitExpression(patternValue.value)
            }
        }
        return visitExpression(ctx.matchElse().expression())
    }

    override fun visitExpressionBlock(ctx: HappyParser.ExpressionBlockContext): Any {
        scope.enter()
        ctx.action().forEach(this::visitAction)
        return visitExpression(ctx.expression()).also { scope.leave() }
    }

    fun visitExpression(ctx: HappyParser.ExpressionContext): Any {
        return ctx.accept(this)
    }
}
