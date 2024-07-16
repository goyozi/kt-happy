package io.github.goyozi.kthappy

import HappyBaseVisitor
import HappyParser

class TypeChecker : HappyBaseVisitor<String>() {
    val builtInTypes = setOf("Integer", "String", "Boolean")

    val scope = Scope<String>()
    val declaredTypes = mutableMapOf<String, DeclaredType>()
    val typeErrors = mutableListOf<TypeError>()

    override fun visitSourceFile(ctx: HappyParser.SourceFileContext): String {
        for (function in builtIns) {
            val functionType = "(" + function.value.arguments.joinToString(",") { it.second } + ")" + "->" + function.value.returnType
            scope.define(function.key, functionType)
        }

        ctx.importStatement().forEach(this::visitImportStatement)
        ctx.data().forEach(this::visitData)
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

    override fun visitData(ctx: HappyParser.DataContext): String {
        val declaredType = DeclaredType(
            ctx.name.text,
            fields = ctx.keyType().associate { it.name.text to it.type.type.text })

        declaredTypes[ctx.name.text] = declaredType

        for (type in declaredType.fields.values) {
            if (!builtInTypes.contains(type) && declaredTypes[type] == null) {
                typeErrors.add(UndeclaredType(type, ctx.loc))
            }
        }

        return "None"
    }

    override fun visitEnum(ctx: HappyParser.EnumContext): String {
        declaredTypes[ctx.name.text] = DeclaredType(
            ctx.name.text,
            ctx.typeOrSymbol().map { it.text }.filter { it !== ctx.genericType?.text })
        return "None"
    }

    override fun visitFunction(ctx: HappyParser.FunctionContext): String {
        val functionType = "(" + ctx.arguments.joinToString(",") { it.type.text } + ")" + "->" + ctx.returnType.text
        scope.define(ctx.ID().text, functionType)
        scope.enter()

        for (i in 0..<ctx.arguments.size) {
            scope.define(ctx.arguments[i].name.text, ctx.arguments[i].type.text)
        }

        val declaredReturnType = ctx.returnType.text
        val actualReturnType = visitExpression(ctx.expression())
        if (incompatibleTypes(declaredReturnType, actualReturnType)) {
            typeErrors.add(IncompatibleType(declaredReturnType, actualReturnType, ctx.loc))
        }

        return "None"
    }

    override fun visitStatement(ctx: HappyParser.StatementContext): String {
        if (ctx.variableDeclaration() != null) {
            val expressionType = if (ctx.variableDeclaration().expression() != null) visitExpression(
                ctx.variableDeclaration().expression()
            ) else null
            scope.define(
                ctx.variableDeclaration().ID().text,
                ctx.variableDeclaration().typeSpec()?.text ?: expressionType!!
            )
            if (ctx.variableDeclaration().typeSpec() != null && expressionType != null && incompatibleTypes(
                    ctx.variableDeclaration().typeSpec().text,
                    expressionType
                )
            ) {
                typeErrors.add(IncompatibleType(ctx.variableDeclaration().typeSpec().text, expressionType, ctx.loc))
            }
        } else if (ctx.variableAssignment() != null) {
            val declaredType = scope.get(ctx.variableAssignment().ID().text)
            val expressionType = visitExpression(ctx.variableAssignment().expression())
            if (incompatibleTypes(declaredType, expressionType)) {
                typeErrors.add(IncompatibleType(declaredType, expressionType, ctx.loc))
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

    override fun visitTrueLiteral(ctx: HappyParser.TrueLiteralContext): String {
        return "Boolean"
    }

    override fun visitFalseLiteral(ctx: HappyParser.FalseLiteralContext): String {
        return "Boolean"
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

        if (arguments == listOf("")) return returnType

        for (i in arguments.indices) {
            val declaredArgumentType = arguments[i]
            val actualArgumentType = visitExpression(ctx.expression(i))
            if (incompatibleTypes(declaredArgumentType, actualArgumentType) && declaredArgumentType != "Any") {
                typeErrors.add(IncompatibleType(declaredArgumentType, actualArgumentType, ctx.loc))
            }
        }
        return returnType
    }

    override fun visitConstructor(ctx: HappyParser.ConstructorContext): String {
        val dataType = declaredTypes[ctx.ID().text]
        if (dataType == null) {
            typeErrors.add(UndeclaredType(ctx.ID().text, ctx.loc))
            return ctx.ID().text
        }
        for (field in dataType.fields.keys) {
            if (ctx.keyExpression().map { it.ID().text }.find { it == field } == null) {
                typeErrors.add(UninitializedField(field, dataType.name, ctx.loc))
            }
        }
        for (keyExpr in ctx.keyExpression()) {
            val declaredType = dataType.fields[keyExpr.ID().text]
            if (declaredType == null) {
                typeErrors.add(UndeclaredField(keyExpr.ID().text, dataType.name, ctx.loc))
            } else {
                val actualType = visitExpression(keyExpr.expression())
                if (incompatibleTypes(declaredType, actualType)) {
                    typeErrors.add(IncompatibleType(declaredType, actualType, ctx.loc))
                }
            }
        }
        return ctx.ID().text
    }

    override fun visitDotCall(ctx: HappyParser.DotCallContext): String {
        val target = (ctx.parent as HappyParser.ComplexExpressionContext).expression().accept(this)
        val dataType = declaredTypes[target]
        if (dataType != null) {
            val fieldType = dataType.fields[ctx.ID().text]
            if (fieldType != null) return fieldType
        }

        try {
            val functionType = scope.get(ctx.ID().text)
            val (argumentsInBrackets, returnType) = functionType.split("->")
            val arguments = argumentsInBrackets.drop(1).dropLast(1).split(",")
            return "(" + arguments.drop(1).joinToString(",") + ")" + "->" + returnType
        } catch (_: IllegalStateException) {
            typeErrors.add(UndeclaredField(ctx.ID().text, target, ctx.loc))
            return "Unknown"
        }
    }

    override fun visitTypeCast(ctx: HappyParser.TypeCastContext): String {
        return ctx.typeSpec().text
    }

    override fun visitIfExpression(ctx: HappyParser.IfExpressionContext): String {
        val conditionMet = visitExpression(ctx.expression())
        // condition met must be boolean
        val ifType = visitExpressionBlock(ctx.expressionBlock(0))
        val elseType = if (ctx.ifExpression() != null) visitIfExpression(ctx.ifExpression())
        else visitExpressionBlock(ctx.expressionBlock(1))
        return if (ifType == elseType) ifType else "$ifType|$elseType"
    }

    override fun visitMatchExpression(ctx: HappyParser.MatchExpressionContext): String {
        val resultTypes = mutableSetOf<String>()
        for (patternValue in ctx.patternValue()) {
            resultTypes.add(visitExpression(patternValue.value))
        }
        resultTypes.add(visitExpression(ctx.matchElse().expression()))
        return resultTypes.joinToString("|")
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
        val allTypes = expressionType.split("|")
        val nonGenericType = declaredType.replaceAfter("<", "").replace("<", "")
        val withinEnum = allTypes.all { (declaredTypes[nonGenericType]?.values?.contains(it) ?: false) || declaredType.endsWith("<$it>") }
        return !matchingTypes && !withinEnum
    }
}