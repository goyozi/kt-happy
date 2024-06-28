package io.github.goyozi.kthappy

import HappyBaseVisitor

class HappyVisitor: HappyBaseVisitor<String>() {
    val scope = Scope()
    val functions = mutableMapOf<String, HappyParser.FunctionContext>()

    override fun visitSourceFile(ctx: HappyParser.SourceFileContext): String {
        ctx.function().forEach(this::visitFunction)
        ctx.statement().forEach(this::visitStatement)
        ctx.expressionStatement().map { it.expression() }.forEach(this::visitExpression)
        return "doesn't matter"
    }

    override fun visitFunction(ctx: HappyParser.FunctionContext): String {
        functions[ctx.ID(0).text] = ctx
        return "doesn't matter"
    }

    override fun visitStatement(ctx: HappyParser.StatementContext): String {
        scope.set(ctx.ID().text, visitExpression(ctx.expression()))
        return "doesn't matter"
    }

    override fun visitExpression(ctx: HappyParser.ExpressionContext): String {
        return if (ctx.NUMBER() != null) ctx.NUMBER().text
        else if (ctx.ID() != null) scope.get(ctx.ID().text)
        else if (ctx.STRING_LITERAL() != null) ctx.STRING_LITERAL().text.drop(1).dropLast(1)
        else if (ctx.call() != null) visitCall(ctx.call())
        else visitExpression(ctx.expression(0)) + visitExpression(ctx.expression(1))
    }

    override fun visitCall(ctx: HappyParser.CallContext): String {
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