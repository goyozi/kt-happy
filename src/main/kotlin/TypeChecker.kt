package io.github.goyozi.kthappy

import HappyBaseVisitor
import HappyParser
import kotlin.math.exp

class TypeChecker : HappyBaseVisitor<String>() {
    val scope = Scope<String>()
    val functions = mutableMapOf<String, HappyParser.FunctionContext>()
    val typeErrors = mutableListOf<TypeError>()

    override fun visitSourceFile(ctx: HappyParser.SourceFileContext): String {
        ctx.function().forEach(this::visitFunction)
        ctx.action().forEach(this::visitAction)
        return "None"
    }

    override fun visitFunction(ctx: HappyParser.FunctionContext): String {
        functions[ctx.ID(0).text] = ctx
        return "None"
    }

    override fun visitStatement(ctx: HappyParser.StatementContext): String {
        if (ctx.variableDeclaration() != null) {
            val expressionType = visitExpression(ctx.variableDeclaration().expression())
            scope.set(
                ctx.variableDeclaration().ID(0).text,
                ctx.variableDeclaration().ID().getOrNull(1)?.text ?: expressionType
            )
            if (ctx.variableDeclaration().ID().size == 2 && ctx.variableDeclaration().ID(1).text != expressionType) {
                typeErrors.add(TypeError("${ctx.start.line}", ctx.variableDeclaration().ID(1).text, expressionType))
            }
        } else if (ctx.variableAssignment() != null) {
            val declaredType = scope.get(ctx.variableAssignment().ID().text)
            val expressionType = visitExpression(ctx.variableAssignment().expression())
            if (declaredType != expressionType) {
                typeErrors.add(TypeError("${ctx.start.line}", declaredType, expressionType))
            }
        } else if (ctx.whileLoop() != null) {
            ctx.whileLoop().action().forEach(this::visitAction)
        } else if (ctx.forLoop() != null) {
            visitForLoop(ctx.forLoop())
        } else {
            throw Error("Unimplemented statement: ${ctx.text}")
        }
        return "None"
    }

    override fun visitForLoop(ctx: HappyParser.ForLoopContext): String {
        scope.set(ctx.ID().text, "Integer")
        ctx.action().forEach(this::visitAction)
        return "None"
    }

    override fun visitExpression(ctx: HappyParser.ExpressionContext): String {
        return if (ctx.NUMBER() != null) "Integer"
        else if (ctx.ID() != null) scope.get(ctx.ID().text)
        else if (ctx.STRING_LITERAL() != null) "String"
        else if (ctx.call() != null) visitCall(ctx.call())
        else if (ctx.PLUS() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            // check if left and right are the same
            left
        } else if (ctx.MINUS() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            // check if left and right are the same
            left
        } else if (ctx.TIMES() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            // check if left and right are the same
            left
        } else if (ctx.DIV() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            // check if left and right are the same
            left
        } else if (ctx.MOD() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            // check if left and right are the same
            left
        } else if (ctx.GT() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            // check if left and right are numbers
            "Boolean"
        } else if (ctx.LT() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            // check if left and right are numbers
            "Boolean"
        } else if (ctx.GT_EQ() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            // check if left and right are numbers
            "Boolean"
        } else if (ctx.LT_EQ() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            // check if left and right are numbers
            "Boolean"
        } else if (ctx.EQ() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            // check if left and right are the same
            "Boolean"
        } else if (ctx.NOT_EQ() != null) {
            val left = visitExpression(ctx.expression(0))
            val right = visitExpression(ctx.expression(1))
            // check if left and right are the same
            "Boolean"
        } else if (ctx.ifExpression() != null) {
            visitIfExpression(ctx.ifExpression())
        } else if (ctx.constructor() != null) {
            visitConstructor(ctx.constructor())
            // check if type exists and if field types match
        } else if (ctx.dotAccess() != null) {
            visitDotAccess(ctx.dotAccess())
        } else if (ctx.expressionBlock() != null) {
            visitExpressionBlock(ctx.expressionBlock())
        } else {
            throw Error("Unimplemented expression: " + ctx.text)
        }
    }

    override fun visitExpressionBlock(ctx: HappyParser.ExpressionBlockContext): String {
        ctx.action().forEach(this::visitAction)
        return visitExpression(ctx.expression())
    }

    override fun visitIfExpression(ctx: HappyParser.IfExpressionContext): String {
        val conditionMet = visitExpression(ctx.expression())
        // condition met must be boolean
        val ifType = visitExpressionBlock(ctx.expressionBlock(0))
        val elseType = if (ctx.ifExpression() != null) visitIfExpression(ctx.ifExpression())
        else visitExpressionBlock(ctx.expressionBlock(1))
        // if type must match else type
        return ifType
    }

    override fun visitCall(ctx: HappyParser.CallContext): String {
//        scope.enter()

        val function = functions[ctx.ID().text]
        if (function != null) {
//            for (i in 1..<function.ID().size) {
//                scope.set(function.ID(i).text, visitExpression(ctx.expression(i - 1)))
//            }
//            function.action().forEach(this::visitAction)
//            val result = visitExpression(function.expression())
//            scope.leave()
            return function.ID().last().text
        }

        val builtInFunction = builtIns[ctx.ID().text]
        if (builtInFunction != null) {
//            for (i in 0..<builtInFunction.arguments.size) {
//                scope.set(builtInFunction.arguments[i].first, visitExpression(ctx.expression(i)))
//            }
//            val result = builtInFunction.implementation(scope)
//            scope.leave()
            return builtInFunction.returnType
        }

        throw Error("Undeclared function: ${ctx.ID().text}")
    }

    override fun visitConstructor(ctx: HappyParser.ConstructorContext): String {
        return ctx.ID().text
    }

    override fun visitDotAccess(ctx: HappyParser.DotAccessContext): String {
        // need to keep track of data types
//        return (scope.get(ctx.ID(0).text).value as Map<String, Value>)[ctx.ID(1).text]
//            ?: throw Error("${ctx.ID(0).text} does not have a member named ${ctx.ID(1).text}")
        return "TODO"
    }
}