package io.github.goyozi.kthappy

import HappyBaseVisitor
import HappyParser

class HappyVisitor: HappyBaseVisitor<Value>() {
    val scope = Scope()
    val functions = mutableMapOf<String, HappyParser.FunctionContext>()

    override fun visitSourceFile(ctx: HappyParser.SourceFileContext): Value {
        ctx.function().forEach(this::visitFunction)
        ctx.statement().forEach(this::visitStatement)
        ctx.expressionStatement().map { it.expression() }.forEach(this::visitExpression)
        return none
    }

    override fun visitFunction(ctx: HappyParser.FunctionContext): Value {
        functions[ctx.ID(0).text] = ctx
        return none
    }

    override fun visitStatement(ctx: HappyParser.StatementContext): Value {
        scope.set(ctx.ID().text, visitExpression(ctx.expression()))
        return none
    }

    override fun visitExpression(ctx: HappyParser.ExpressionContext): Value {
        return if (ctx.NUMBER() != null) Value("Integer", ctx.NUMBER().text.toInt())
        else if (ctx.ID() != null) scope.get(ctx.ID().text)
        else if (ctx.STRING_LITERAL() != null) Value("String", ctx.STRING_LITERAL().text.drop(1).dropLast(1))
        else if (ctx.call() != null) visitCall(ctx.call())
        else if (ctx.PLUS() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            if (left.type == "Integer") Value("Integer", left.value as Int + right.value as Int)
            else Value("String", left.value as String + right.value as String)
        } else if (ctx.GT() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            Value("Boolean", left.value as Int > right.value as Int)
        } else if (ctx.EQ() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            Value("Boolean", left.value == right.value)
        } else if (ctx.ifExpression() != null) {
            visitIfExpression(ctx.ifExpression())
        } else {
            throw Error("Unimplemented expression: " + ctx.text)
        }
    }

    override fun visitExpressionBlock(ctx: HappyParser.ExpressionBlockContext): Value {
        for (expressionStatement in ctx.expressionStatement()) visitExpression(expressionStatement.expression())
        return visitExpression(ctx.expression())
    }

    override fun visitIfExpression(ctx: HappyParser.IfExpressionContext): Value {
        val conditionMet = visitExpression(ctx.expression())
        return if (conditionMet.value as Boolean) visitExpressionBlock(ctx.expressionBlock(0))
        else visitExpressionBlock(ctx.expressionBlock(1))
    }

    override fun visitCall(ctx: HappyParser.CallContext): Value {
        scope.enter()

        val function = functions[ctx.ID().text]
        if (function != null) {
            for (i in 1..<function.ID().size) {
                scope.set(function.ID(i).text, visitExpression(ctx.expression(i - 1)))
            }
            val result = visitExpression(function.expression())
            scope.leave()
            return result
        }

        val builtInFunction = builtIns[ctx.ID().text]
        if (builtInFunction != null) {
            for (i in 0..<builtInFunction.arguments.size) {
                scope.set(builtInFunction.arguments[i].first, visitExpression(ctx.expression(i)))
            }
            val result = builtInFunction.implementation(scope)
            scope.leave()
            return result
        }

        throw IllegalStateException("Undeclared function: ${ctx.ID().text}")
    }
}