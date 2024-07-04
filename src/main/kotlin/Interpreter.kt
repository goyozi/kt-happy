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
        for (i in ctx.INTEGER_LITERAL(0).text.toInt()..ctx.INTEGER_LITERAL(1).text.toInt()) {
            scope.set(ctx.ID().text, Value("Integer", i))
            ctx.action().forEach(this::visitAction)
        }
        return none
    }

    override fun visitTrueLiteral(ctx: HappyParser.TrueLiteralContext): Value {
        return Value("Boolean", true)
    }

    override fun visitFalseLiteral(ctx: HappyParser.FalseLiteralContext): Value {
        return Value("Boolean", false)
    }

    override fun visitIntegerLiteral(ctx: HappyParser.IntegerLiteralContext): Value {
        return Value("Integer", ctx.INTEGER_LITERAL().text.toInt())
    }

    override fun visitStringLiteral(ctx: HappyParser.StringLiteralContext): Value {
        return Value("String", ctx.STRING_LITERAL().text.drop(1).dropLast(1))
    }

    override fun visitNegation(ctx: HappyParser.NegationContext): Value {
        return Value("Boolean", !(visitExpression(ctx.expression()).value as Boolean))
    }

    override fun visitUnaryMinus(ctx: HappyParser.UnaryMinusContext): Value {
        return Value("Integer", -(visitExpression(ctx.expression()).value as Int))
    }

    override fun visitMultiplication(ctx: HappyParser.MultiplicationContext): Value {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return Value("Integer", left.value as Int * right.value as Int)
    }

    override fun visitDivision(ctx: HappyParser.DivisionContext): Value {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return Value("Integer", left.value as Int / right.value as Int)
    }

    override fun visitModulus(ctx: HappyParser.ModulusContext): Value {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return Value("Integer", left.value as Int % right.value as Int)
    }

    override fun visitAddition(ctx: HappyParser.AdditionContext): Value {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return if (left.type == "Integer") Value("Integer", left.value as Int + right.value as Int)
        else Value("String", left.value as String + right.value.toString())
    }

    override fun visitSubtraction(ctx: HappyParser.SubtractionContext): Value {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return Value("Integer", left.value as Int - right.value as Int)
    }

    override fun visitGreaterThan(ctx: HappyParser.GreaterThanContext): Value {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return Value("Boolean", left.value as Int > right.value as Int)
    }

    override fun visitLessThan(ctx: HappyParser.LessThanContext): Value {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return Value("Boolean", (left.value as Int) < (right.value as Int))
    }

    override fun visitGreaterOrEqual(ctx: HappyParser.GreaterOrEqualContext): Value {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return Value("Boolean", (left.value as Int) >= (right.value as Int))
    }

    override fun visitLessOrEqual(ctx: HappyParser.LessOrEqualContext): Value {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return Value("Boolean", (left.value as Int) <= (right.value as Int))
    }

    override fun visitEqualTo(ctx: HappyParser.EqualToContext): Value {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return Value("Boolean", left.value == right.value)
    }

    override fun visitNotEqual(ctx: HappyParser.NotEqualContext): Value {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return Value("Boolean", left.value != right.value)
    }

    override fun visitIdentifier(ctx: HappyParser.IdentifierContext): Value {
        return scope.get(ctx.ID().text)
    }

    override fun visitFunctionCall(ctx: HappyParser.FunctionCallContext): Value {
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

    override fun visitDotCall(ctx: HappyParser.DotCallContext): Value {
        return (scope.get(ctx.ID(0).text).value as Map<String, Value>)[ctx.ID(1).text]
            ?: throw Error("${ctx.ID(0).text} does not have a member named ${ctx.ID(1).text}")
    }

    override fun visitIfExpression(ctx: HappyParser.IfExpressionContext): Value {
        val conditionMet = visitExpression(ctx.expression())
        return if (conditionMet.value as Boolean) visitExpressionBlock(ctx.expressionBlock(0))
        else if (ctx.ifExpression() != null) visitIfExpression(ctx.ifExpression())
        else visitExpressionBlock(ctx.expressionBlock(1))
    }

    override fun visitExpressionBlock(ctx: HappyParser.ExpressionBlockContext): Value {
        ctx.action().forEach(this::visitAction)
        return visitExpression(ctx.expression())
    }

    fun visitExpression(ctx: HappyParser.ExpressionContext): Value {
        return ctx.accept(this)
    }
}
