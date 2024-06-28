package io.github.goyozi.kthappy

import HappyBaseVisitor

class HappyVisitor: HappyBaseVisitor<Int>() {
    val scope = Scope()
    val functions = mutableMapOf<String, HappyParser.FunctionContext>()

    override fun visitSourceFile(ctx: HappyParser.SourceFileContext): Int {
        ctx.function().forEach(this::visitFunction)
        ctx.statement().forEach(this::visitStatement)
        ctx.expression().forEach(this::visitExpression)
        return visitExpression(ctx.expression().last())
    }

    override fun visitFunction(ctx: HappyParser.FunctionContext): Int {
        functions[ctx.ID(0).text] = ctx
        return -1
    }

    override fun visitStatement(ctx: HappyParser.StatementContext): Int {
        scope.set(ctx.ID().text, visitExpression(ctx.expression()))
        return -1
    }

    override fun visitExpression(ctx: HappyParser.ExpressionContext): Int {
        return if (ctx.NUMBER() != null) ctx.NUMBER().text.toInt()
        else if (ctx.ID() != null) scope.get(ctx.ID().text)
        else if (ctx.call() != null) visitCall(ctx.call())
        else visitExpression(ctx.expression(0)) + visitExpression(ctx.expression(1))
    }

    override fun visitCall(ctx: HappyParser.CallContext): Int {
        scope.enter()
        val function = functions[ctx.ID().text]!!
        for (i in 1..<function.ID().size) {
             scope.set(function.ID(i).text, visitExpression(ctx.expression(i - 1)))
        }
        val result = visitExpression(function.expression())
        scope.leave()
        return result
    }
}