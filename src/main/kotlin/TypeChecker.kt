package io.github.goyozi.kthappy

import HappyBaseVisitor
import HappyParser
import org.antlr.v4.runtime.tree.TerminalNode

class TypeChecker : HappyBaseVisitor<Type>() {
    val builtInTypes = setOf<Type>(any, integer, string, boolean)

    val scope = Scope<Type>()
    val typeErrors = mutableListOf<TypeError>()

    override fun visitSourceFile(ctx: HappyParser.SourceFileContext): Type {
        for (type in builtInTypes) {
            scope.define(type.name, type)
        }

        for (function in builtIns) {
            val functionType = FunctionType(
                function.key,
                mapOf(function.value.arguments.map { it.second } to function.value.returnType)
            )
            scope.define(function.key, functionType)
        }

        ctx.importStatement().forEach(this::visitImportStatement)
        ctx.data().forEach(this::visitData)
        ctx.enum_().forEach(this::visitEnum)
        ctx.interface_().forEach(this::visitInterface)
        ctx.function().forEach(this::visitFunction)
        ctx.action().forEach(this::visitAction)
        ctx.function().forEach(this::visitFunctionBody)
        return nothing
    }

    override fun visitImportStatement(ctx: HappyParser.ImportStatementContext): Type {
        val path = ctx.paths.joinToString("/") { it.text } + ".happy"
        val currentScope = scope.stack.last()
        scope.enter(Layer())
        val sourceFile = parseSourceFile(path)
        visitSourceFile(sourceFile)
        sourceFileTrees[path] = sourceFile
        for (symbol in ctx.symbols) {
            currentScope.bindings[symbol.text] = scope.get(symbol.text)
        }
        scope.leave()
        return nothing
    }

    override fun visitData(ctx: HappyParser.DataContext): Type {
        val declaredType = DataType(
            ctx.name.text,
            ctx.keyType().associate {
                it.name.text to try {
                    // todo: generics
                    scope.get(it.type.type.text)
                } catch (_: IllegalStateException) {
                    typeErrors.add(UndeclaredType(it.type.type.text, ctx.loc))
                    nothing
                }
            })

        scope.define(ctx.name.text, declaredType)
        return nothing
    }

    override fun visitEnum(ctx: HappyParser.EnumContext): Type {
        val typeSet = ctx.typeOrSymbol()
            .map {
                if (it.SYMBOL() != null) symbolToType(it.SYMBOL())
                else if (it.ID().text == ctx.genericType?.text) GenericType(it.ID().text)
                else idToType(it.ID())
            }
            .toSet()

        scope.define(ctx.name.text, EnumType(ctx.name.text, typeSet))
        return nothing
    }

    override fun visitInterface(ctx: HappyParser.InterfaceContext): Type {
        val interfaceType = InterfaceType(
            ctx.name.text,
            ctx.sigs.map { FunctionType(it.name.text, mapOf(it.arguments.map { typeSpecToType(it.typeSpec()) } to typeSpecToType(it.returnType))) }.toSet()
        )
        scope.define(ctx.name.text, interfaceType)
        interfaceType.functions.forEach { functionType ->
            scope.define(functionType.name, FunctionType(functionType.name, functionType.variants.mapKeys { listOf(interfaceType) + it.key }))
        }
        return nothing
    }

    override fun visitFunction(ctx: HappyParser.FunctionContext): Type {
        val argumentsToVariant =
            ctx.sig.arguments.map { typeSpecToType(it.typeSpec()) } to typeSpecToType(ctx.sig.returnType)
        argumentTypes.put(ctx, argumentsToVariant.first)
        try {
            val existingFunction = scope.get(ctx.sig.ID().text) as FunctionType // todo: might not be the case
            val newFunction = FunctionType(existingFunction.name, existingFunction.variants + mapOf(argumentsToVariant))
            scope.assign(ctx.sig.name.text, newFunction)
        } catch (_: IllegalStateException) {
            val functionType = FunctionType(
                ctx.sig.name.text,
                // todo: test with return being generic type
                mapOf(argumentsToVariant)
            )
            scope.define(ctx.sig.name.text, functionType)
        }
        return nothing
    }

    private fun visitFunctionBody(ctx: HappyParser.FunctionContext) {
        scope.enter()

        for (i in 0..<ctx.sig.arguments.size) {
            // todo: test with arguments being generic types
            scope.define(ctx.sig.arguments[i].name.text, typeSpecToType(ctx.sig.arguments[i].type))
        }

        // todo: test that we evaluate the actions
        ctx.action().forEach(this::visitAction)

        val declaredReturnType = typeSpecToType(ctx.sig.returnType)
        val actualReturnType = visitExpression(ctx.expression())
        if (incompatibleTypes(declaredReturnType, actualReturnType)) {
            typeErrors.add(IncompatibleType(declaredReturnType, actualReturnType, ctx.loc))
        }

        scope.leave()
    }

    override fun visitStatement(ctx: HappyParser.StatementContext): Type {
        if (ctx.variableDeclaration() != null) {
            if (ctx.variableDeclaration().typeSpec() != null) {
                val type = ctx.variableDeclaration().typeSpec().type.text
                try {
                    scope.get(type)
                } catch (_: IllegalStateException) {
                    typeErrors.add(UndeclaredType(type, ctx.loc))
                    scope.define(ctx.variableDeclaration().ID().text, nothing)
                    return nothing
                }
            }

            val expressionType = if (ctx.variableDeclaration().expression() != null) visitExpression(
                ctx.variableDeclaration().expression()
            ) else null

            val declaredType =
                if (ctx.variableDeclaration().typeSpec() != null) typeSpecToType(ctx.variableDeclaration().typeSpec())
                else null

            scope.define(ctx.variableDeclaration().ID().text, declaredType ?: expressionType!!)

            if (declaredType != null && expressionType != null && incompatibleTypes(declaredType, expressionType)) {
                typeErrors.add(IncompatibleType(declaredType, expressionType, ctx.loc))
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
        return nothing
    }

    private fun typeSpecToType(typeSpec: HappyParser.TypeSpecContext): Type {
        var declaredType = typeSpec.type.text.let { scope.get(it) }

        if (declaredType is EnumType && typeSpec.genericType != null) {
            // todo: should be a union ??
            declaredType = EnumType(declaredType.name, declaredType.types.map {
                if (it is GenericType) scope.get(typeSpec.genericType.text)
                else it
            }.toSet())
        }
        return declaredType
    }

    override fun visitForLoop(ctx: HappyParser.ForLoopContext): Type {
        scope.define(ctx.ID().text, integer)
        ctx.action().forEach(this::visitAction)
        return nothing
    }

    override fun visitComplexExpression(ctx: HappyParser.ComplexExpressionContext): Type {
        return ctx.postfixExpression().accept(this)
    }

    override fun visitExpressionInBrackets(ctx: HappyParser.ExpressionInBracketsContext): Type {
        return visitExpression(ctx.expression())
    }

    override fun visitTrueLiteral(ctx: HappyParser.TrueLiteralContext): Type {
        return boolean
    }

    override fun visitFalseLiteral(ctx: HappyParser.FalseLiteralContext): Type {
        return boolean
    }

    override fun visitIntegerLiteral(ctx: HappyParser.IntegerLiteralContext): Type {
        return integer
    }

    override fun visitStringLiteral(ctx: HappyParser.StringLiteralContext): Type {
        return string
    }

    override fun visitMultiplication(ctx: HappyParser.MultiplicationContext): Type {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return integer
    }

    override fun visitDivision(ctx: HappyParser.DivisionContext): Type {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return integer
    }

    override fun visitModulus(ctx: HappyParser.ModulusContext): Type {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return integer
    }

    override fun visitAddition(ctx: HappyParser.AdditionContext): Type {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return left
    }

    override fun visitSubtraction(ctx: HappyParser.SubtractionContext): Type {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return integer
    }

    override fun visitGreaterThan(ctx: HappyParser.GreaterThanContext): Type {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return boolean
    }

    override fun visitLessThan(ctx: HappyParser.LessThanContext): Type {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return boolean
    }

    override fun visitGreaterOrEqual(ctx: HappyParser.GreaterOrEqualContext): Type {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return boolean
    }

    override fun visitLessOrEqual(ctx: HappyParser.LessOrEqualContext): Type {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return boolean
    }

    override fun visitEqualTo(ctx: HappyParser.EqualToContext): Type {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return boolean
    }

    override fun visitNotEqual(ctx: HappyParser.NotEqualContext): Type {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return boolean
    }

    override fun visitIdentifier(ctx: HappyParser.IdentifierContext): Type {
        return idToType(ctx.ID())
    }

    private fun idToType(id: TerminalNode) = try {
        scope.get(id.text)
    } catch (_: IllegalStateException) {
        // type error: unknown identifier, also do it without an exception
        nothing
    }

    override fun visitSymbol(ctx: HappyParser.SymbolContext): Type {
        return symbolToType(ctx.SYMBOL())
    }

    private fun symbolToType(symbol: TerminalNode) = SymbolType(symbol.text)

    override fun visitFunctionCall(ctx: HappyParser.FunctionCallContext): Type {
        val functionType = (ctx.parent as HappyParser.ComplexExpressionContext).expression()
            .accept(this)

        if (functionType !is FunctionType) {
            // todo: type error: not a function
            return nothing
        }

        if (functionType.variants.size == 1) {
            val arguments = functionType.variants.keys.single()
            val returnType = functionType.variants.values.single()

            for (i in arguments.indices) {
                val declaredArgumentType = arguments[i]
                val actualArgumentType = visitExpression(ctx.expression(i))
                if (incompatibleTypes(declaredArgumentType, actualArgumentType) && declaredArgumentType != any) {
                    typeErrors.add(IncompatibleType(declaredArgumentType, actualArgumentType, ctx.loc))
                }
            }
            return returnType
        } else {
            val actualArguments = ctx.expression().map { visitExpression(it) }
            val returnType = functionType.variants[actualArguments]!! // todo: might fail badly!
            return returnType
        }
    }

    override fun visitConstructor(ctx: HappyParser.ConstructorContext): Type {
        val dataType = try {
            scope.get(ctx.ID().text) as DataType // todo: we actually have to check this
        } catch (_: IllegalStateException) {
            typeErrors.add(UndeclaredType(ctx.ID().text, ctx.loc))
            return DataType(ctx.ID().text, emptyMap())
        }
        for (field in dataType.fields.keys) {
            if (ctx.keyExpression().map { it.ID().text }.find { it == field } == null) {
                typeErrors.add(UninitializedField(field, dataType, ctx.loc))
            }
        }
        for (keyExpr in ctx.keyExpression()) {
            val declaredType = dataType.fields[keyExpr.ID().text]
            if (declaredType == null) {
                typeErrors.add(UndeclaredField(keyExpr.ID().text, dataType, ctx.loc))
            } else {
                val actualType = visitExpression(keyExpr.expression())
                if (incompatibleTypes(declaredType, actualType)) {
                    typeErrors.add(IncompatibleType(declaredType, actualType, ctx.loc))
                }
            }
        }
        return dataType
    }

    override fun visitDotCall(ctx: HappyParser.DotCallContext): Type {
        val targetExpression = (ctx.parent as HappyParser.ComplexExpressionContext).expression()
        val target = targetExpression.accept(this).also { expressionTypes.put(targetExpression, it) }
        val dataType = scope.get(target.name)
        if (dataType is DataType) {
            val fieldType = dataType.fields[ctx.ID().text]
            if (fieldType != null) return fieldType
        }

        try {
            val functionType = scope.get(ctx.ID().text)

            if (functionType !is FunctionType)
                throw Error("${ctx.ID().text} is not a function: $functionType")

            // todo: proper overloading + tests
            val matchingVariants = functionType.variants.filterKeys { it.getOrNull(0) == target }
            val arguments = matchingVariants.keys.single()
            val variant = matchingVariants.values.single()
            return FunctionType(ctx.ID().text, mapOf(arguments.drop(1) to variant))
        } catch (_: IllegalStateException) {
            typeErrors.add(UndeclaredField(ctx.ID().text, target, ctx.loc))
            return nothing
        }
    }

    override fun visitTypeCast(ctx: HappyParser.TypeCastContext): Type {
        // todo: test
        return typeSpecToType(ctx.typeSpec())
    }

    override fun visitIfExpression(ctx: HappyParser.IfExpressionContext): Type {
        val conditionMet = visitExpression(ctx.expression())
        // condition met must be boolean
        val ifType = visitExpressionBlock(ctx.expressionBlock(0))
        val elseType = if (ctx.ifExpression() != null) visitIfExpression(ctx.ifExpression())
        else visitExpressionBlock(ctx.expressionBlock(1))
        return union(setOf(ifType, elseType))
    }

    override fun visitMatchExpression(ctx: HappyParser.MatchExpressionContext): Type {
        val resultTypes = mutableSetOf<Type>()
        for (patternValue in ctx.patternValue()) {
            resultTypes.add(visitExpression(patternValue.value))
        }
        resultTypes.add(visitExpression(ctx.matchElse().expression()))
        return union(resultTypes)
    }

    override fun visitExpressionBlock(ctx: HappyParser.ExpressionBlockContext): Type {
        scope.enter()
        ctx.action().forEach(this::visitAction)
        return visitExpression(ctx.expression()).also { scope.leave() }
    }

    fun visitExpression(ctx: HappyParser.ExpressionContext): Type {
        return ctx.accept(this)
            ?.also { expressionTypes.put(ctx, it) }
            ?: throw Error("Unsupported expression: ${ctx.text}")
    }

    private fun incompatibleTypes(declaredType: Type, expressionType: Type): Boolean {
        return !declaredType.assignableFrom(expressionType, scope)
    }
}