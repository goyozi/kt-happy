package happy.ast.expression

import happy.Loc
import happy.ast.TypeAnnotation

data class TypeCast(val value: Expression, val asType: TypeAnnotation, override val loc: Loc) : Expression {

    override fun type() = asType.toType()

    // todo: actual type cast?
    override fun eval() = value.eval()
}
