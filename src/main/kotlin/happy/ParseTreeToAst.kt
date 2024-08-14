package happy

import HappyBaseVisitor
import HappyParser
import happy.ast.*
import happy.ast.expression.*
import happy.ast.expression.comparison.*
import happy.ast.expression.literal.BooleanLiteral
import happy.ast.expression.literal.IntLiteral
import happy.ast.expression.literal.StringLiteral
import happy.ast.expression.literal.SymbolLiteral
import happy.ast.expression.math.*
import happy.ast.statement.*
import org.antlr.v4.runtime.ParserRuleContext

class ParseTreeToAst : HappyBaseVisitor<AstNode>() {

    override fun visitSourceFile(ctx: HappyParser.SourceFileContext): SourceFile {
        return SourceFile(
            ctx.importStatement().map { visitImportStatement(it) },
            ctx.typeDeclaration().map { visitTypeDeclaration(it) },
            ctx.function().map { visitFunction(it) },
            ctx.statement().map { visitStatement(it) },
            ctx.loc
        )
    }

    override fun visitImportStatement(ctx: HappyParser.ImportStatementContext): ImportDeclaration {
        val path = ctx.paths.joinToString("/") { it.text } + ".happy"
        val sourceFile = parseSourceFile(path)
        sourceFiles[path] = visitSourceFile(sourceFile)
        return ImportDeclaration(path, ctx.symbols.map { it.text }, ctx.loc)
    }

    override fun visitTypeDeclaration(ctx: HappyParser.TypeDeclarationContext): TypeDeclaration {
        return super.visitTypeDeclaration(ctx) as TypeDeclaration
    }

    override fun visitData(ctx: HappyParser.DataContext): DataTypeDeclaration {
        return DataTypeDeclaration(
            ctx.name.text,
            ctx.keyType().associate { it.name.text to visitTypeSpec(it.typeSpec()) },
            ctx.loc
        )
    }

    override fun visitEnum(ctx: HappyParser.EnumContext): AstNode {
        return EnumTypeDeclaration(
            ctx.name.text,
            ctx.genericType?.text,
            ctx.typeOrSymbol().filter { it.typeSpec() != null }.map { visitTypeSpec(it.typeSpec()) },
            ctx.typeOrSymbol().filter { it.symbol() != null }.map { visitSymbol(it.symbol()) },
            ctx.loc
        )
    }

    override fun visitInterface(ctx: HappyParser.InterfaceContext): AstNode {
        return InterfaceDeclaration(ctx.name.text, ctx.sigs.map { visitFunctionSignature(it) }, ctx.loc)
    }

    override fun visitFunction(ctx: HappyParser.FunctionContext): FunctionDeclaration {
        return FunctionDeclaration(
            visitFunctionSignature(ctx.sig),
            ctx.statement().map { visitStatement(it) },
            visitExpression(ctx.expression()),
            ctx.loc
        )
    }

    override fun visitFunctionSignature(ctx: HappyParser.FunctionSignatureContext): FunctionSignature {
        return FunctionSignature(
            ctx.name.text,
            ctx.arguments.associate { it.name.text to visitTypeSpec(it.typeSpec()) },
            visitTypeSpec(ctx.returnType),
            ctx.loc
        )
    }

    override fun visitStatement(ctx: HappyParser.StatementContext): Statement = when {
        ctx.variableDeclaration() != null -> visitVariableDeclaration(ctx.variableDeclaration())
        ctx.variableAssignment() != null -> visitVariableAssignment(ctx.variableAssignment())
        ctx.whileLoop() != null -> visitWhileLoop(ctx.whileLoop())
        ctx.forLoop() != null -> visitForLoop(ctx.forLoop())
        ctx.expressionStatement() != null -> ExpressionStatement(visitExpression(ctx.expressionStatement().expression()), ctx.loc)
        else -> throw IllegalArgumentException(ctx.text)
    }

    override fun visitVariableDeclaration(ctx: HappyParser.VariableDeclarationContext): VariableDeclaration {
        return VariableDeclaration(
            ctx.ID().text,
            ctx.typeSpec()?.let { visitTypeSpec(it) },
            ctx.expression()?.let { visitExpression(it) }, ctx.loc
        )
    }

    override fun visitVariableAssignment(ctx: HappyParser.VariableAssignmentContext): VariableAssignment {
        return VariableAssignment(ctx.ID().text, visitExpression(ctx.expression()), ctx.loc)
    }

    override fun visitWhileLoop(ctx: HappyParser.WhileLoopContext): WhileLoop {
        return WhileLoop(visitExpression(ctx.expression()), ctx.statement().map { visitStatement(it) }, ctx.loc)
    }

    override fun visitForLoop(ctx: HappyParser.ForLoopContext): ForLoop {
        val range = (ctx.INTEGER_LITERAL(0).text.toInt())..(ctx.INTEGER_LITERAL(1).text.toInt())
        return ForLoop(ctx.ID().text, range, ctx.statement().map { visitStatement(it) }, ctx.loc)
    }

    override fun visitNegation(ctx: HappyParser.NegationContext): AstNode {
        return Negation(visitExpression(ctx.expression()), ctx.loc)
    }

    override fun visitComparison(ctx: HappyParser.ComparisonContext): AstNode {
        return when (ctx.op.text) {
            ">" -> GreaterThan(visitExpression(ctx.left), visitExpression(ctx.right), ctx.loc)
            ">=" -> GreaterEqual(visitExpression(ctx.left), visitExpression(ctx.right), ctx.loc)
            "<" -> LesserThan(visitExpression(ctx.left), visitExpression(ctx.right), ctx.loc)
            "<=" -> LesserEqual(visitExpression(ctx.left), visitExpression(ctx.right), ctx.loc)
            "==" -> Equal(visitExpression(ctx.left), visitExpression(ctx.right), ctx.loc)
            "!=" -> NotEqual(visitExpression(ctx.left), visitExpression(ctx.right), ctx.loc)
            else -> throw IllegalArgumentException(ctx.op.text)
        }
    }

    override fun visitUnaryMinus(ctx: HappyParser.UnaryMinusContext): AstNode {
        return UnaryMinus(visitExpression(ctx.expression()), ctx.loc)
    }

    override fun visitConstructor(ctx: HappyParser.ConstructorContext): AstNode {
        return ConstructorCall(
            ctx.ID().text,
            ctx.keyExpression().associate { it.ID().text to visitExpression(it.expression()) }, ctx.loc
        )
    }

    override fun visitComplexExpression(ctx: HappyParser.ComplexExpressionContext): AstNode {
        return ctx.postfixExpression().accept(this)
    }

    override fun visitMultiplicative(ctx: HappyParser.MultiplicativeContext): AstNode {
        return when (ctx.op.text) {
            "*" -> Multiplication(visitExpression(ctx.left), visitExpression(ctx.right), ctx.loc)
            "/" -> Division(visitExpression(ctx.left), visitExpression(ctx.right), ctx.loc)
            "%" -> Mod(visitExpression(ctx.left), visitExpression(ctx.right), ctx.loc)
            else -> throw IllegalArgumentException(ctx.op.text)
        }
    }

    override fun visitAdditive(ctx: HappyParser.AdditiveContext): AstNode {
        return when (ctx.op.text) {
            "+" -> Addition(visitExpression(ctx.left), visitExpression(ctx.right), ctx.loc)
            "-" -> Subtraction(visitExpression(ctx.left), visitExpression(ctx.right), ctx.loc)
            else -> throw IllegalArgumentException(ctx.op.text)
        }
    }

    override fun visitExpressionInBrackets(ctx: HappyParser.ExpressionInBracketsContext): AstNode {
        return BracketExpression(visitExpression(ctx.expression()), ctx.loc)
    }

    override fun visitBooleanLiteral(ctx: HappyParser.BooleanLiteralContext): AstNode {
        return BooleanLiteral(ctx.text.toBoolean(), ctx.loc)
    }

    override fun visitIntegerLiteral(ctx: HappyParser.IntegerLiteralContext): AstNode {
        return IntLiteral(ctx.text.toInt(), ctx.loc)
    }

    override fun visitStringLiteral(ctx: HappyParser.StringLiteralContext): AstNode {
        return StringLiteral(ctx.text.drop(1).dropLast(1), ctx.loc)
    }

    override fun visitIdentifier(ctx: HappyParser.IdentifierContext): AstNode {
        return IdExpression(ctx.text, ctx.loc)
    }

    override fun visitSymbol(ctx: HappyParser.SymbolContext): SymbolLiteral {
        return SymbolLiteral(ctx.SYMBOL().text, ctx.loc)
    }

    override fun visitFunctionCall(ctx: HappyParser.FunctionCallContext): AstNode {
        return FunctionCall(
            visitExpression((ctx.parent as HappyParser.ComplexExpressionContext).expression()),
            ctx.expression().map { visitExpression(it) },
            ctx.loc
        )
    }

    override fun visitDotCall(ctx: HappyParser.DotCallContext): AstNode {
        return DotCall(
            visitExpression((ctx.parent as HappyParser.ComplexExpressionContext).expression()),
            ctx.ID().text,
            ctx.loc
        )
    }

    override fun visitTypeCast(ctx: HappyParser.TypeCastContext): AstNode {
        return TypeCast(
            visitExpression((ctx.parent as HappyParser.ComplexExpressionContext).expression()),
            visitTypeSpec(ctx.typeSpec()),
            ctx.loc
        )
    }

    override fun visitIfExpression(ctx: HappyParser.IfExpressionContext): IfExpression {
        return IfExpression(
            visitExpression(ctx.expression()),
            visitExpressionBlock(ctx.ifTrue),
            ctx.ifFalse?.let { visitExpressionBlock(it) } ?: visitIfExpression(ctx.elseIf),
            ctx.loc)
    }

    override fun visitMatchExpression(ctx: HappyParser.MatchExpressionContext): AstNode {
        return MatchExpression(
            visitExpression(ctx.expression()),
            ctx.patternValue().map { visitExpression(it.pattern) to visitExpression(it.value) },
            visitExpression(ctx.matchElse().expression()),
            ctx.loc
        )
    }

    override fun visitExpressionBlock(ctx: HappyParser.ExpressionBlockContext): ExpressionBlock {
        return ExpressionBlock(ctx.statement().map { visitStatement(it) }, visitExpression(ctx.expression()), ctx.loc)
    }

    override fun visitTypeSpec(ctx: HappyParser.TypeSpecContext): TypeAnnotation {
        return TypeAnnotation(ctx.type.text, ctx.genericType?.text, ctx.loc)
    }

    fun visitExpression(ctx: HappyParser.ExpressionContext): Expression {
        return ctx.accept(this) as Expression
    }

    val ParserRuleContext.loc get() = Loc(start.line, start.charPositionInLine, stop.line, stop.charPositionInLine)
}