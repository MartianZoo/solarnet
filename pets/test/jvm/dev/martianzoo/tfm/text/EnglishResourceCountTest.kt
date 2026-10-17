package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.canon.Canon
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class EnglishResourceCountTest {
  private val english = English(Canon.classTable, TerraformingMarsDescribers.descriptions)
  private val describers = Describers(Canon.classTable, TerraformingMarsDescribers.descriptions)

  @Test
  fun resourceGoalsKeepTheirEstablishedWording() {
    for ((name, expected) in
        mapOf(
            "Excentric" to "Most card resources.",
            "Trader" to "Requires 3 different types of card resources.",
            "Collector" to "Most different types of resources.",
        )) {
      english.renderGoal(Canon.classTable.getClass(cn(name))).text shouldBe expected
    }
    english.bottomText(Canon.classTable.getClass(cn("DiversitySupport"))) shouldBe
        "Requires that you have 9 different types of resources. Raise your terraform rating 1 step."
  }

  @Test
  fun cardResourceCountsKeepSpecificHoldersAndOwnersVisible() {
    for (selector in
        listOf(
            "CardResource<Pets>",
            "CardResource<CardFront(HAS SpaceTag)>",
            "CardResource<CardFront, Anyone>",
            "CardResource<ResourceHolder>",
        )) {
      lexicalizeRankedMetricPhrase(parse<Metric>(selector), describers).linearize() shouldContain
          "["
      english.describe(parse<Requirement>("3 Class<@CardResource>(HAS @${selector})")) shouldContain
          "["
    }
  }

  @Test
  fun resourceTypeExclusionsAreImplicitOnlyWhenVocabularySaysSo() {
    val requirement = parse<Requirement>("9 Class<@Resource>(HAS @Resource, NOT Class<RobotUnit>)")
    english.describe(requirement) shouldBe "Requires that you have 9 different types of resources."
    val withoutImplicitExclusion = TerraformingMarsDescribers.descriptions.toMutableMap()
    withoutImplicitExclusion[cn("Resource")] =
        withoutImplicitExclusion.getValue(cn("Resource")).copy(implicitKindExclusions = null)
    English(Canon.classTable, withoutImplicitExclusion).describe(requirement) shouldContain "["
    english.describe(
        parse<Requirement>("9 Class<@Resource>(HAS @Resource, NOT Class<Animal>)")
    ) shouldContain "["
    english.describe(
        parse<Requirement>(
            "9 Class<@Resource>(HAS @Resource, NOT Class<RobotUnit>, NOT Class<Animal>)"
        )
    ) shouldContain "["
    english.describe(
        parse<Requirement>("9 Class<@Resource>(HAS 2 @Resource, NOT Class<RobotUnit>)")
    ) shouldContain "["
  }
}
