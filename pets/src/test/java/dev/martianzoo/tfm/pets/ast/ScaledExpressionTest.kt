package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.testRoundTrip
import org.junit.jupiter.api.Test

class ScaledExpressionTest {
  @Test
  fun testParse() {
    parse(ScaledExpression.scalar(), "2")
    parse(ScaledExpression.scalar(), "0")
    parse(ScaledExpression.scalar(), "X")
    parse(ScaledExpression.scalar(), "1X")
    parse(ScaledExpression.scalar(), "2X")

    testRoundTrip<ScaledExpression>("Foo")
    testRoundTrip<ScaledExpression>("0 Foo")
    testRoundTrip<ScaledExpression>("1 Foo", "Foo")
    testRoundTrip<ScaledExpression>("3 Foo")
    testRoundTrip<ScaledExpression>("1111 Foo")
    testRoundTrip<ScaledExpression>("X Foo")
    testRoundTrip<ScaledExpression>("2X Foo")
    testRoundTrip<ScaledExpression>("1111X Foo")
  }
}
