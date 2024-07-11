package io.github.goyozi.kthappy

import HappyBaseVisitor
import HappyParser

class TypeChecker : HappyBaseVisitor<String>() {
    val scope = Scope<String>()
    val declaredTypes = mutableMapOf<String, DeclaredType>()
    val typeErrors = mutableListOf<TypeError>()

    override fun visitSourceFile(ctx: HappyParser.SourceFileContext): String {
        for (function in builtIns) {
            val functionType = "(" + function.value.arguments.joinToString(",") { it.second } + ")" + "->" + function.value.returnType
            scope.define(function.key, functionType)
        }

        ctx.importStatement().forEach(this::visitImportStatement)
        ctx.enum_().forEach(this::visitEnum)
        ctx.function().forEach(this::visitFunction)
        ctx.action().forEach(this::visitAction)
        return "None"
    }

    override fun visitImportStatement(ctx: HappyParser.ImportStatementContext): String {
        val path = ctx.paths.joinToString("/") { it.text } + ".happy"
        visitSourceFile(parseSourceFile(path))
        return "None"
    }

    override fun visitEnum(ctx: HappyParser.EnumContext): String {
        declaredTypes[ctx.name.text] = DeclaredType(ctx.name.text, ctx.typeOrSymbol().map { it.text })
        return "None"
    }

    override fun visitFunction(ctx: HappyParser.FunctionContext): String {
        val functionType = "(" + ctx.arguments.joinToString(",") { it.type.text } + ")" + "->" + ctx.returnType.text
        scope.define(ctx.ID(0).text, functionType)
        scope.enter()

        for (i in 0..<ctx.arguments.size) {
            scope.define(ctx.arguments[i].name.text, ctx.arguments[i].type.text)
        }

        val declaredReturnType = ctx.returnType.text
        val actualReturnType = visitExpression(ctx.expression())
        if (incompatibleTypes(declaredReturnType, actualReturnType)) {
            typeErrors.add(TypeError("${ctx.start.line}", declaredReturnType, actualReturnType))
        }

        return "None"
    }

    override fun visitStatement(ctx: HappyParser.StatementContext): String {
        if (ctx.variableDeclaration() != null) {
            val expressionType = if (ctx.variableDeclaration().expression() != null) visitExpression(
                ctx.variableDeclaration().expression()
            ) else null
            scope.define(
                ctx.variableDeclaration().ID(0).text,
                ctx.variableDeclaration().ID().getOrNull(1)?.text ?: expressionType!!
            )
            if (ctx.variableDeclaration().ID().size == 2 && expressionType != null && incompatibleTypes(
                    ctx.variableDeclaration().ID(1).text,
                    expressionType
                )
            ) {
                typeErrors.add(TypeError("${ctx.start.line}", ctx.variableDeclaration().ID(1).text, expressionType))
            }
        } else if (ctx.variableAssignment() != null) {
            val declaredType = scope.get(ctx.variableAssignment().ID().text)
            val expressionType = visitExpression(ctx.variableAssignment().expression())
            if (incompatibleTypes(declaredType, expressionType)) {
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
        scope.define(ctx.ID().text, "Integer")
        ctx.action().forEach(this::visitAction)
        return "None"
    }

    override fun visitComplexExpression(ctx: HappyParser.ComplexExpressionContext): String {
        return ctx.postfixExpression().accept(this)
    }

    override fun visitExpressionInBrackets(ctx: HappyParser.ExpressionInBracketsContext): String {
        return visitExpression(ctx.expression())
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

    override fun visitSymbol(ctx: HappyParser.SymbolContext): String {
        return ctx.SYMBOL().text
    }

    override fun visitFunctionCall(ctx: HappyParser.FunctionCallContext): String {
        val functionType = (ctx.parent as HappyParser.ComplexExpressionContext).expression().accept(this)
        val (argumentsInBrackets, returnType) = functionType.split("->")
        val arguments = argumentsInBrackets.drop(1).dropLast(1).split(",")

        for (i in arguments.indices) {
            val declaredArgumentType = arguments[i]
            val actualArgumentType = visitExpression(ctx.expression(i))
            if (incompatibleTypes(declaredArgumentType, actualArgumentType) && declaredArgumentType != "Any") {
                typeErrors.add(TypeError("${ctx.start.line}", declaredArgumentType, actualArgumentType))
            }
        }
        return returnType
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
        return ctx.accept(this) ?: throw Error("Unsupported expression: ${ctx.text}")
    }

    private fun incompatibleTypes(declaredType: String, expressionType: String): Boolean {
        val matchingTypes = declaredType == expressionType
        val withinEnum = declaredTypes[declaredType]?.values?.contains(expressionType) ?: false
        return !matchingTypes && !withinEnum
    }
}