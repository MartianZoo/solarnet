package dev.martianzoo.tfm.text

import dev.martianzoo.tfm.canon.Canon
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EnglishGoalTextTest {
  private val targets =
      EnglishGoalTextData.parse(readEnglishCardText("english-goal-text-goals.tsv"))
  private val current =
      EnglishGoalTextData.parse(readEnglishCardText("english-goal-text-current.tsv"))
  private val english = English(Canon.classTable, TerraformingMarsDescribers.descriptions)

  @Test
  internal fun allGoalTextMatchesCurrentSnapshot() {
    current.keys shouldBe targets.keys
    current.forEach { (className, expected) ->
      withClue(className.toString()) {
        expected.englishName shouldBe targets.getValue(className).englishName
        val rendering = english.renderGoal(Canon.classTable.getClass(className))
        rendering.text shouldBe expected.text
        countRenderedPetsFallbacks(rendering.text) shouldBe rendering.unresolved.size
      }
    }
  }
}
