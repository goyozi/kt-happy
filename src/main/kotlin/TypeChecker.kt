package io.github.goyozi.kthappy

import HappyBaseVisitor
import HappyParser

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
        scope.enter()

        for (i in 0..<ctx.arguments.size) {
            scope.set(ctx.arguments[i].name.text, ctx.arguments[i].type.text)
        }

        val declaredReturnType = ctx.returnType.text
        val actualReturnType = visitExpression(ctx.expression())
        if (declaredReturnType != actualReturnType) {
            typeErrors.add(TypeError("${ctx.start.line}", declaredReturnType, actualReturnType))
        }

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

    override fun visitIntegerLiteral(ctx: HappyParser.IntegerLiteralContext): String {
        return "Integer"
    }

    override fun visitStringLiteral(ctx: HappyParser.StringLiteralContext): String {
        return "String"
    }

    override fun visitMultiplication(ctx: HappyParser.MultiplicationContext): String {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return "Integer"
    }

    override fun visitDivision(ctx: HappyParser.DivisionContext): String {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return "Integer"
    }

    override fun visitModulus(ctx: HappyParser.ModulusContext): String {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return "Integer"
    }

    override fun visitAddition(ctx: HappyParser.AdditionContext): String {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return left
    }

    override fun visitSubtraction(ctx: HappyParser.SubtractionContext): String {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return "Integer"
    }

    override fun visitGreaterThan(ctx: HappyParser.GreaterThanContext): String {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return "Boolean"
    }

    override fun visitLessThan(ctx: HappyParser.LessThanContext): String {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return "Boolean"
    }

    override fun visitGreaterOrEqual(ctx: HappyParser.GreaterOrEqualContext): String {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return "Boolean"
    }

    override fun visitLessOrEqual(ctx: HappyParser.LessOrEqualContext): String {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return "Boolean"
    }

    override fun visitEqualTo(ctx: HappyParser.EqualToContext): String {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return "Boolean"
    }

    override fun visitNotEqual(ctx: HappyParser.NotEqualContext): String {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return "Boolean"
    }

    override fun visitIdentifier(ctx: HappyParser.IdentifierContext): String {
        return scope.get(ctx.ID().text)
    }

    override fun visitFunctionCall(ctx: HappyParser.FunctionCallContext): String {
        val function = functions[ctx.ID().text]
        if (function != null) {
            for (i in 0..<function.arguments.size) {
                val declaredArgumentType = function.arguments[i].type.text
                val actualArgumentType = visitExpression(ctx.expression(i))
                if (declaredArgumentType != actualArgumentType) {
                    typeErrors.add(TypeError("${ctx.start.line}", declaredArgumentType, actualArgumentType))
                }
            }
            return function.returnType.text
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

    override fun visitDotCall(ctx: HappyParser.DotCallContext): String {
        // need to keep track of data types
//        return (scope.get(ctx.ID(0).text).value as Map<String, Value>)[ctx.ID(1).text]
//            ?: throw Error("${ctx.ID(0).text} does not have a member named ${ctx.ID(1).text}")
        return "TODO"
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

    override fun visitExpressionBlock(ctx: HappyParser.ExpressionBlockContext): String {
        ctx.action().forEach(this::visitAction)
        return visitExpression(ctx.expression())
    }

    fun visitExpression(ctx: HappyParser.ExpressionContext): String {
        return ctx.accept(this)
    }
}