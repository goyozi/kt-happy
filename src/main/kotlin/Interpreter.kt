package io.github.goyozi.kthappy

import HappyBaseVisitor
import HappyParser
import org.antlr.v4.runtime.tree.ParseTreeProperty

class Interpreter : HappyBaseVisitor<Any>() {
    val scope = Scope<Any>()
    val functionParent = ParseTreeProperty<Layer<Any>>()

    override fun visitSourceFile(ctx: HappyParser.SourceFileContext): Any {
        ctx.importStatement().forEach(this::visitImportStatement)
        ctx.function().forEach(this::visitFunction)
        ctx.action().forEach(this::visitAction)
        return Unit
    }

    override fun visitImportStatement(ctx: HappyParser.ImportStatementContext): Any {
        val path = ctx.paths.joinToString("/") { it.text } + ".happy"
        val currentScope = scope.stack.last()
        scope.enter(Layer())
        visitSourceFile(parseSourceFile(path))
        for (symbol in ctx.symbols) {
            currentScope.bindings[symbol.text] = scope.get(symbol.text)
        }
        scope.leave()
        return Unit
    }

    override fun visitFunction(ctx: HappyParser.FunctionContext): Any {
        scope.define(ctx.name.text, ctx)
        functionParent.put(ctx, scope.stack.last())
        return Unit
    }

    override fun visitStatement(ctx: HappyParser.StatementContext): Any {
        if (ctx.variableDeclaration() != null) {
            scope.define(ctx.variableDeclaration().ID(0).text, visitExpression(ctx.variableDeclaration().expression()))
        } else if (ctx.variableAssignment() != null) {
            scope.assign(ctx.variableAssignment().ID().text, visitExpression(ctx.variableAssignment().expression()))
        } else if (ctx.whileLoop() != null) {
            scope.enter()
            while (visitExpression(ctx.whileLoop().expression()) == true) ctx.whileLoop().action()
                .forEach(this::visitAction)
            scope.leave()
        } else if (ctx.forLoop() != null) {
            visitForLoop(ctx.forLoop())
        } else {
            throw Error("Unimplemented statement: ${ctx.text}")
        }
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

    override fun visitTrueLiteral(ctx: HappyParser.TrueLiteralContext): Any {
        return true
    }

    override fun visitFalseLiteral(ctx: HappyParser.FalseLiteralContext): Any {
        return false
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

    override fun visitMultiplication(ctx: HappyParser.MultiplicationContext): Any {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return left as Int * right as Int
    }

    override fun visitDivision(ctx: HappyParser.DivisionContext): Any {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return left as Int / right as Int
    }

    override fun visitModulus(ctx: HappyParser.ModulusContext): Any {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return left as Int % right as Int
    }

    override fun visitAddition(ctx: HappyParser.AdditionContext): Any {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return if (left is Int) left + right as Int
        else left as String + right.toString()
    }

    override fun visitSubtraction(ctx: HappyParser.SubtractionContext): Any {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return left as Int - right as Int
    }

    override fun visitGreaterThan(ctx: HappyParser.GreaterThanContext): Any {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return left as Int > right as Int
    }

    override fun visitLessThan(ctx: HappyParser.LessThanContext): Any {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return (left as Int) < (right as Int)
    }

    override fun visitGreaterOrEqual(ctx: HappyParser.GreaterOrEqualContext): Any {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return (left as Int) >= (right as Int)
    }

    override fun visitLessOrEqual(ctx: HappyParser.LessOrEqualContext): Any {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return (left as Int) <= (right as Int)
    }

    override fun visitEqualTo(ctx: HappyParser.EqualToContext): Any {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return left == right
    }

    override fun visitNotEqual(ctx: HappyParser.NotEqualContext): Any {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return left != right
    }

    override fun visitIdentifier(ctx: HappyParser.IdentifierContext): Any {
        return builtIns[ctx.ID().text] ?: scope.get(ctx.ID().text)
    }

    override fun visitFunctionCall(ctx: HappyParser.FunctionCallContext): Any {
        val function = (ctx.parent as HappyParser.ComplexExpressionContext).expression().accept(this)
        if (function is HappyParser.FunctionContext) {
            scope.enter(functionParent.get(function))
            for (i in 0..<function.arguments.size) {
                scope.define(function.arguments[i].name.text, visitExpression(ctx.expression(i)))
            }
            function.action().forEach(this::visitAction)
            val result = visitExpression(function.expression())
            scope.leave()
            return result
        }

        scope.enter()
        val builtInFunction = function as BuiltInFunction
        for (i in 0..<builtInFunction.arguments.size) {
            scope.define(builtInFunction.arguments[i].first, visitExpression(ctx.expression(i)))
        }
        val result = builtInFunction.implementation(scope)
        scope.leave()
        return result
    }

    override fun visitConstructor(ctx: HappyParser.ConstructorContext): Any {
        return ctx.keyExpression().associate { it.ID().text to visitExpression(it.expression()) }
    }

    override fun visitDotCall(ctx: HappyParser.DotCallContext): Any {
        val target = (ctx.parent as HappyParser.ComplexExpressionContext).expression().accept(this)
        return (target as Map<String, Any>)[ctx.ID().text]
            ?: throw Error("$target does not have a member named ${ctx.ID().text}")
    }

    override fun visitIfExpression(ctx: HappyParser.IfExpressionContext): Any {
        val conditionMet = visitExpression(ctx.expression())
        return if (conditionMet as Boolean) visitExpressionBlock(ctx.expressionBlock(0))
        else if (ctx.ifExpression() != null) visitIfExpression(ctx.ifExpression())
        else visitExpressionBlock(ctx.expressionBlock(1))
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
