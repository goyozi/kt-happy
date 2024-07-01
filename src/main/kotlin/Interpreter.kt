package io.github.goyozi.kthappy

import HappyBaseVisitor
import HappyParser

class Interpreter: HappyBaseVisitor<Value>() {
    val scope = Scope<Value>()
    val functions = mutableMapOf<String, HappyParser.FunctionContext>()

    override fun visitSourceFile(ctx: HappyParser.SourceFileContext): Value {
        ctx.function().forEach(this::visitFunction)
        ctx.action().forEach(this::visitAction)
        return none
    }

    override fun visitFunction(ctx: HappyParser.FunctionContext): Value {
        functions[ctx.name.text] = ctx
        return none
    }

    override fun visitStatement(ctx: HappyParser.StatementContext): Value {
        if (ctx.variableDeclaration() != null) {
            scope.set(ctx.variableDeclaration().ID(0).text, visitExpression(ctx.variableDeclaration().expression()))
        } else if (ctx.variableAssignment() != null) {
            scope.set(ctx.variableAssignment().ID().text, visitExpression(ctx.variableAssignment().expression()))
        } else if (ctx.whileLoop() != null) {
            while (visitExpression(ctx.whileLoop().expression()).value == true) ctx.whileLoop().action().forEach(this::visitAction)
        } else if (ctx.forLoop() != null) {
            visitForLoop(ctx.forLoop())
        } else {
            throw Error("Unimplemented statement: ${ctx.text}")
        }
        return none
    }

    override fun visitForLoop(ctx: HappyParser.ForLoopContext): Value {
        for (i in ctx.NUMBER(0).text.toInt()..ctx.NUMBER(1).text.toInt()) {
            scope.set(ctx.ID().text, Value("Integer", i))
            ctx.action().forEach(this::visitAction)
        }
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
            else Value("String", left.value as String + right.value.toString())
        } else if (ctx.MINUS() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            Value("Integer", left.value as Int - right.value as Int)
        } else if (ctx.TIMES() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            Value("Integer", left.value as Int * right.value as Int)
        } else if (ctx.DIV() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            Value("Integer", left.value as Int / right.value as Int)
        } else if (ctx.MOD() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            Value("Integer", left.value as Int % right.value as Int)
        } else if (ctx.GT() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            Value("Boolean", left.value as Int > right.value as Int)
        } else if (ctx.LT() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            Value("Boolean", (left.value as Int) < (right.value as Int))
        } else if (ctx.GT_EQ() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            Value("Boolean", (left.value as Int) >= (right.value as Int))
        } else if (ctx.LT_EQ() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            Value("Boolean", (left.value as Int) <= (right.value as Int))
        } else if (ctx.EQ() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            Value("Boolean", left.value == right.value)
        } else if (ctx.NOT_EQ() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            Value("Boolean", left.value != right.value)
        } else if (ctx.ifExpression() != null) {
            visitIfExpression(ctx.ifExpression())
        } else if (ctx.constructor() != null) {
            visitConstructor(ctx.constructor())
        } else if (ctx.dotAccess() != null) {
            visitDotAccess(ctx.dotAccess())
        } else if (ctx.expressionBlock() != null) {
            visitExpressionBlock(ctx.expressionBlock())
        } else {
            throw Error("Unimplemented expression: " + ctx.text)
        }
    }

    override fun visitExpressionBlock(ctx: HappyParser.ExpressionBlockContext): Value {
        ctx.action().forEach(this::visitAction)
        return visitExpression(ctx.expression())
    }

    override fun visitIfExpression(ctx: HappyParser.IfExpressionContext): Value {
        val conditionMet = visitExpression(ctx.expression())
        return if (conditionMet.value as Boolean) visitExpressionBlock(ctx.expressionBlock(0))
        else if (ctx.ifExpression() != null) visitIfExpression(ctx.ifExpression())
        else visitExpressionBlock(ctx.expressionBlock(1))
    }

    override fun visitCall(ctx: HappyParser.CallContext): Value {
        scope.enter()

        val function = functions[ctx.ID().text]
        if (function != null) {
            for (i in 0..<function.arguments.size) {
                scope.set(function.arguments[i].name.text, visitExpression(ctx.expression(i)))
            }
            function.action().forEach(this::visitAction)
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

        throw Error("Undeclared function: ${ctx.ID().text}")
    }

    override fun visitConstructor(ctx: HappyParser.ConstructorContext): Value {
        return Value(ctx.ID().text, ctx.keyExpression().associate { it.ID().text to visitExpression(it.expression()) })
    }

    override fun visitDotAccess(ctx: HappyParser.DotAccessContext): Value {
        return (scope.get(ctx.ID(0).text).value as Map<String, Value>)[ctx.ID(1).text]
            ?: throw Error("${ctx.ID(0).text} does not have a member named ${ctx.ID(1).text}")
    }
}