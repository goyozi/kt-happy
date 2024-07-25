package io.github.goyozi.kthappy

import HappyBaseVisitor
import HappyParser

class Interpreter : HappyBaseVisitor<Any>() {
    val scope = Scope<Any>()
    val functionParent = mutableMapOf<Function, Layer<Any>>()

    override fun visitSourceFile(ctx: HappyParser.SourceFileContext): Any {
        builtIns.forEach { defineFunction(it.name, it) }

        ctx.importStatement().forEach(this::visitImportStatement)
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
        return ctx.INTEGER_LITERAL().text.toInt()
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
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))

        if (left is String) return left + right

        return when (ctx.op.text) {
            "+" -> left as Int + right as Int
            else -> left as Int - right as Int
        }
    }

    override fun visitComparison(ctx: HappyParser.ComparisonContext): Any {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))

        return when (ctx.op.text) {
            "==" -> left == right
            "!=" -> left != right
            ">" -> left as Int > right as Int
            "<" -> (left as Int) < right as Int
            ">=" -> left as Int >= right as Int
            else -> left as Int <= right as Int
        }
    }

    override fun visitIdentifier(ctx: HappyParser.IdentifierContext): Any {
        return scope.get(ctx.ID().text)
    }

    override fun visitSymbol(ctx: HappyParser.SymbolContext): String {
        return ctx.SYMBOL().text.drop(1)
    }

    override fun visitFunctionCall(ctx: HappyParser.FunctionCallContext): Any {
        var function = (ctx.parent as HappyParser.ComplexExpressionContext).expression().accept(this)
        val arguments = mutableListOf<ArgumentValue>()

        if (function is PreAppliedFunction) {
            arguments.add(function.firstArgument)
            function = scope.get(function.name)
        }

        for (it in ctx.expression()) {
            arguments.add(ArgumentValue(expressionTypes.get(it), visitExpression(it)))
        }

        return (function as OverloadedFunction).invoke(arguments, this)
    }

    override fun visitConstructor(ctx: HappyParser.ConstructorContext): Any {
        return DataObject(
            expressionTypes.get(ctx),
            ctx.keyExpression().associate { it.ID().text to visitExpression(it.expression()) })
    }

    override fun visitDotCall(ctx: HappyParser.DotCallContext): Any {
        val targetExpression = (ctx.parent as HappyParser.ComplexExpressionContext).expression()
        val target = targetExpression.accept(this)

        if (target is DataObject && target.values.containsKey(ctx.ID().text)) {
            return target.values[ctx.ID().text]!!
        }

        val firstArgumentType = if (target is DataObject) target.type else expressionTypes.get(targetExpression)
            ?: throw Error("Unknown expression type: ${targetExpression.text}")

        return PreAppliedFunction(ctx.ID().text, ArgumentValue(firstArgumentType, target))
    }

    override fun visitTypeCast(ctx: HappyParser.TypeCastContext): Any {
        // TODO: actual type cast?
        return (ctx.parent as HappyParser.ComplexExpressionContext).expression().accept(this)
    }

    override fun visitIfExpression(ctx: HappyParser.IfExpressionContext): Any {
        val conditionMet = visitExpression(ctx.expression())
        return if (conditionMet as Boolean) visitExpressionBlock(ctx.expressionBlock(0))
        else if (ctx.ifExpression() != null) visitIfExpression(ctx.ifExpression())
        else visitExpressionBlock(ctx.expressionBlock(1))
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
