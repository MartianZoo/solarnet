package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.tfm.canon.Canon
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EnglishGlobalEventTextTest {
  private val english = English(Canon.classTable, TerraformingMarsDescribers.descriptions)

  @Test
  internal fun allGlobalEventTextMatchesCurrentSnapshot() {
    val current =
        EnglishTextData.parse(readEnglishCardText("english-global-event-text-current.tsv"))
    val published =
        EnglishTextData.parse(
            readEnglishCardText("english-global-event-published-wording-evidence.tsv")
        )
    val globalEvent = Canon.classTable.getClass(cn("GlobalEvent"))
    current.keys shouldBe
        Canon.classTable.allClassNames
            .map(Canon.classTable::getClass)
            .filter { !it.abstract && it.isSubtypeOf(globalEvent) }
            .map { it.className }
            .toSet()
    current.keys.toList() shouldBe published.keys.toList()
    current.forEach { (name, expected) ->
      withClue(name.toString()) {
        expected.englishName shouldBe published.getValue(name).englishName
        val actual = english.renderGlobalEvent(Canon.classTable.getClass(name))
        actual.text shouldBe expected.text
        countRenderedPetsFallbacks(actual.text) shouldBe actual.unresolved.size
      }
    }
  }

  @Test
  internal fun preservesCapOrderAndResourceRetention() {
    english.describe(parse<InstructionTree>("-2 MC. / (BuildingTag MAX 3 - Influence)")) shouldBe
        "Remove 2 M€ per building tag you have (max 3) in excess of influence, or as much as possible."
    english.describe(parse<InstructionTree>("-2 MC. / (BuildingTag - Influence) MAX 3")) shouldBe
        "Remove 2 M€ per building tag you have in excess of influence (max 3), or as much as possible."
    english.describe(parse<InstructionTree>("-Steel! / (Steel - Influence - 2)")) shouldBe
        "Remove all steel except 2 plus influence."
    english.describe(parse<InstructionTree>("-2 Steel! / Steel")) shouldBe
        "Remove 2 steel per steel you have."
    english.describe(parse<InstructionTree>("-Steel? / Steel")) shouldBe "[-Steel? / Steel]."
    english.describe(parse<InstructionTree>("2 MC / (BuildingTag - (Influence - 2))")) shouldBe
        "[2 MC / BuildingTag - (Influence - 2)]."
  }

  @Test
  internal fun distinguishesThresholdRewardsFromRepeatedSets() {
    english.describe(parse<InstructionTree>("5 MC / 3 (EarthTag OR Influence) MAX 1")) shouldBe
        "If you have at least 3 Earth tags and influence combined, gain 5 M€."
    english.describe(parse<InstructionTree>("5 MC / 3 (EarthTag OR Influence) MAX 2")) shouldBe
        "Gain 5 M€ per complete set of 3 Earth tags and influence combined (max 2)."
  }
}
