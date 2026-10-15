package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MetricLexicalizationTest {
  private val describers = Describers(Canon.classTable, TerraformingMarsDescribers.descriptions)

  @Test
  internal fun totalizesAnUnsupportedCountExpressionAsAPhraseRefusal() {
    val expression = cn("Briber").expression
    val rendering =
        describers.lexicalizeCountedExpression(
            expression,
            count = null,
            possessorEstablished = false,
        )

    rendering.phrase.linearize() shouldBe "[Briber]"
    rendering.phrase.unresolved() shouldBe
        listOf(Unresolved(expression, RefusalReason.UNSUPPORTED_METRIC))
  }
}
