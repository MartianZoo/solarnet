package dev.martianzoo.tfm.text

import dev.martianzoo.catalog.displayName
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EnglishGoalTextTest {
  private val current = EnglishTextData.parse(readEnglishCardText("english-goal-text-current.tsv"))
  private val english = English(Canon.classTable, TerraformingMarsDescribers.descriptions)

  @Test
  internal fun allGoalTextMatchesCurrentSnapshot() {
    val milestone = Canon.classTable.getClass(cn("Milestone"))
    val award = Canon.classTable.getClass(cn("Award"))
    current.keys shouldBe
        Canon.classTable.allClassNames
            .map(Canon.classTable::getClass)
            .filter { !it.abstract && (it.isSubtypeOf(milestone) || it.isSubtypeOf(award)) }
            .map { it.className }
            .toSet()
    assertSoftly {
      current.forEach { (className, expected) ->
        withClue(className.toString()) {
          expected.englishName shouldBe displayName(Canon, className)
          val rendering = english.renderGoal(Canon.classTable.getClass(className))
          rendering.text shouldBe expected.text
          countRenderedPetsFallbacks(rendering.text) shouldBe rendering.unresolved.size
        }
      }
    }
  }

  @Test
  internal fun claimPaymentIsSeparateFromTheMilestoneRequirement() {
    val additions =
        object : TfmCatalog() {
          override val explicitClassDeclarations: Set<ClassDeclaration> =
              parseClasses(
                      """
          CLASS ExtraClaimCost : Milestone {
            requirement = HAS "This"
            This: -7 MC
          }
          """
                  )
                  .toSet()
        }
    val catalog = TfmCatalog(Canon, additions)
    val english = English(catalog.classTable, TerraformingMarsDescribers.descriptions)
    val goal = catalog.classTable.getClass(cn("ExtraClaimCost"))
    english.renderGoal(goal).text shouldBe "[ExtraClaimCost]."
    english.describe(goal.declaration.authoredEffects.single().untransformed.instruction) shouldBe
        "Lose 7 M€."
  }
}
