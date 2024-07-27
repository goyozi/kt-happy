package io.github.goyozi.kthappy

import HappyBaseVisitor
import HappyParser
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.TerminalNode

class TypeChecker : HappyBaseVisitor<Type>() {
    val builtInTypes = setOf<Type>(any, integer, string, boolean)

    val scope = Scope<Type>()
    val typeErrors = mutableListOf<TypeError>()

    override fun visitSourceFile(ctx: HappyParser.SourceFileContext): Type {
        builtInTypes.forEach { scope.define(it.name, it) }
        builtIns.forEach { defineFunction(it.name, it) }

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
            ctx.sigs.map {
                val arguments = it.arguments.map {
                    DeclaredArgument(
                        typeSpecToType(it.typeSpec()),
                        it.name.text
                    )
                }
                OverloadedFunction(
                    it.name.text,
                    listOf(InterfaceFunction(it.name.text, arguments, typeSpecToType(it.returnType)))
                )
            }.toSet()
        )
        scope.define(ctx.name.text, interfaceType)
        interfaceTypes.put(ctx, interfaceType)
        interfaceType.completeFunctions().forEach { functionType ->
            // todo: unhack
            defineFunction(functionType.name, functionType.functions.single())
        }
        return nothing
    }

    override fun visitFunction(ctx: HappyParser.FunctionContext): Type {
        val arguments = ctx.sig.arguments.map {
            DeclaredArgument(typeSpecToType(it.typeSpec()), it.name.text)
        }
        val function = CustomFunction(arguments, typeSpecToType(ctx.sig.returnType), ctx)
        functions.put(ctx, function)
        defineFunction(function.name, function)
        return nothing
    }

    private fun defineFunction(name: String, impl: Function) {
        try {
            val function = scope.get(name) as OverloadedFunction // todo: check if it's a function
            val newFunction = OverloadedFunction(name, function.functions + listOf(impl))
            scope.assign(name, newFunction)
        } catch (_: IllegalStateException) {
            scope.define(name, OverloadedFunction(name, listOf(impl)))
        }
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
        checkType(declaredReturnType, actualReturnType, ctx)

        scope.leave()
    }

    override fun visitVariableDeclaration(ctx: HappyParser.VariableDeclarationContext): Type {
        if (ctx.typeSpec() != null) {
            val type = ctx.typeSpec().type.text
            try {
                scope.get(type)
            } catch (_: IllegalStateException) {
                typeErrors.add(UndeclaredType(type, ctx.loc))
                scope.define(ctx.ID().text, nothing)
                return nothing
            }
        }

        val expressionType = if (ctx.expression() != null) visitExpression(ctx.expression()) else null

        val declaredType =
            if (ctx.typeSpec() != null) typeSpecToType(ctx.typeSpec())
            else null

        scope.define(ctx.ID().text, declaredType ?: expressionType!!)

        if (declaredType != null && expressionType != null) {
            checkType(declaredType, expressionType, ctx)
        }
        return nothing
    }

    override fun visitVariableAssignment(ctx: HappyParser.VariableAssignmentContext): Type {
        val declaredType = scope.get(ctx.ID().text)
        val expressionType = visitExpression(ctx.expression())
        checkType(declaredType, expressionType, ctx)
        return nothing
    }

    override fun visitWhileLoop(ctx: HappyParser.WhileLoopContext): Type {
        // todo: clearly not testing separate scope
        ctx.action().forEach(this::visitAction)
        return nothing
    }

    override fun visitForLoop(ctx: HappyParser.ForLoopContext): Type {
        // todo: clearly not testing separate scope
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

    override fun visitBooleanLiteral(ctx: HappyParser.BooleanLiteralContext): Type {
        return boolean
    }

    override fun visitIntegerLiteral(ctx: HappyParser.IntegerLiteralContext): Type {
        return integer
    }

    override fun visitStringLiteral(ctx: HappyParser.StringLiteralContext): Type {
        return string
    }

    override fun visitMultiplicative(ctx: HappyParser.MultiplicativeContext): Type {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return integer
    }

    override fun visitAdditive(ctx: HappyParser.AdditiveContext): Type {
        val left = visitExpression(ctx.expression(0))
        val right = visitExpression(ctx.expression(1))
        return left
    }

    override fun visitComparison(ctx: HappyParser.ComparisonContext): Type {
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
        var functionType = (ctx.parent as HappyParser.ComplexExpressionContext).expression().accept(this)
        val arguments = mutableListOf<Type>()

        if (functionType is PreAppliedFunction) {
            arguments.add(functionType.firstArgument.type)
            functionType = scope.get(functionType.name)
        }

        if (functionType !is OverloadedFunction) {
            // todo: type error: not a function
            return nothing
        }

        ctx.expression().map { visitExpression(it) }.forEach(arguments::add)

        if (functionType.functions.size == 1) {
            val function = functionType.functions.single()
            val returnType = function.returnType

            for (i in function.arguments.indices) {
                val declaredArgumentType = function.arguments[i].type
                val actualArgumentType = arguments[i]
                checkType(declaredArgumentType, actualArgumentType, ctx)
            }

            argumentTypes.put(ctx, function.arguments.map { it.type })

            return returnType
        } else {
            val function = functionType.getVariant(arguments, scope) // todo: might fail badly!
            argumentTypes.put(ctx, function.arguments.map { it.type })
            return function.returnType
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
                checkType(declaredType, actualType, ctx)
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

            if (functionType !is OverloadedFunction)
                throw Error("${ctx.ID().text} is not a function: $functionType")

            // todo: proper overloading + tests
            return PreAppliedFunction(ctx.ID().text, ArgumentValue(target, Unit))
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

    private fun checkType(declaredType: Type, actualType: Type, ctx: ParserRuleContext) {
        if (incompatibleTypes(declaredType, actualType)) {
            typeErrors.add(IncompatibleType(declaredType, actualType, ctx.loc))
        }
    }

    private fun incompatibleTypes(declaredType: Type, expressionType: Type): Boolean {
        return !declaredType.assignableFrom(expressionType, scope)
    }
}