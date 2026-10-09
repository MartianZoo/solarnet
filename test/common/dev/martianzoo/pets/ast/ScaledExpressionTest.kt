package dev.martianzoo.pets.ast

import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.testRoundTrip
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

internal class ScaledExpressionTest {
  @Test
  internal fun testParse() {
    parse<ScaledExpression.Scalar>("2")
    parse<ScaledExpression.Scalar>("0")
    parse<ScaledExpression.Scalar>("X")
    parse<ScaledExpression.Scalar>("1X")
    parse<ScaledExpression.Scalar>("2X")

    testRoundTrip<ScaledExpression>("Foo")
    testRoundTrip<ScaledExpression>("0 Foo")
    testRoundTrip<ScaledExpression>("1 Foo", "Foo")
    testRoundTrip<ScaledExpression>("3 Foo")
    testRoundTrip<ScaledExpression>("1111 Foo")
    testRoundTrip<ScaledExpression>("X Foo")
    testRoundTrip<ScaledExpression>("2X Foo")
    testRoundTrip<ScaledExpression>("1111X Foo")
  }

  @Test
  internal fun denominationlessMoneyAmountsExplainHowToFixThem() {
    assertDenominationlessAmountRejected { parse<ScaledExpression>("2") }
    assertDenominationlessAmountRejected { parse<ScaledExpression>("X") }
    assertDenominationlessAmountRejected { parse<InstructionTree>("-5") }
    assertDenominationlessAmountRejected { parse<InstructionTree>("-X?") }
    assertDenominationlessAmountRejected { parse<InstructionTree>("PROD[1]") }
    assertDenominationlessAmountRejected { parse<Action>("2 -> Foo") }
    assertDenominationlessAmountRejected { parse<Effect>("Foo: 3") }
    assertDenominationlessAmountRejected { parse<Requirement>("MAX 4") }
    assertDenominationlessAmountRejected { parse<Requirement>("Foo(HAS 1)") }
    assertDenominationlessAmountRejected {
      Parsing.parseClasses("CLASS Foo { This: 3 }")
    }
  }

  private fun assertDenominationlessAmountRejected(block: () -> Unit) {
    val failure = assertFailsWith<PetSyntaxException>(block = block)
    assertContains(
        failure.message.orEmpty(),
        "money amounts must name `MC` explicitly",
    )
  }
}
