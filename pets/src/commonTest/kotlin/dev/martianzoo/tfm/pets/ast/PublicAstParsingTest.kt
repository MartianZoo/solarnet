package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Expression.Refinement
import dev.martianzoo.pets.ast.FromExpression
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.ScaledExpression.Scalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PublicAstParsingTest {
  @Test
  internal fun publicParsingCoversStructuralAstKinds() {
    parse<FromExpression>("Foo FROM Bar").toExpression.toString() shouldBe "Foo"
    parse<Refinement>("(HAS? Foo)").forgiving shouldBe true
    parse<Property>("Owner.amount").propertyName.value shouldBe "amount"
    parse<Scalar>("2X") shouldBe XScalar(2)
  }
}
