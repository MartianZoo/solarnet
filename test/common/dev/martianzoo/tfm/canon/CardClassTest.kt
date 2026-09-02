package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.ClassDeclaration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CardClassTest {
  private val catalog: TfmCatalog by lazy {
    catalogWith(
        """
        CLASS ClassBackedExample : ActionCard, ActiveCard<Class<ProjectCard>>, ResourceCard<Class<Microbe>> {
          cost = 7
          requirement = HAS "3 OceanTile"

          This:: EarthTag<This>, BuildingTag<This>
          This: 2 MC
          End: VictoryPoint

          Plant -> Microbe<This>
        }

        CLASS UnrelatedHelper
        """
    )
  }

  @Test
  internal fun cardSemanticsComeEntirelyFromLoadedPetsClasses() {
    val card = catalog.card(cn("ClassBackedExample"))

    cardBack(card)?.className shouldBe TfmClasses.PROJECT_CARD
    cardCost(card) shouldBe 7
    cardRequirement(card).toString() shouldBe "3 OceanTile"
    cardTags(card).elements.shouldContainExactlyInAnyOrder(cn("EarthTag"), cn("BuildingTag"))
    cardImmediate(card).toString() shouldBe "2 MC"
    cardActions(card).map { it.toString() }.shouldContainExactly("Plant -> Microbe<This>")
    cardEffects(card).map { it.toString() }.shouldContainExactly("End: VictoryPoint")
    cardResourceType(card) shouldBe cn("Microbe")
  }

  @Test
  internal fun concreteCardFrontSubclassesAreTheCardRegistry() {
    catalog.cards.map { it.className }.contains(cn("ClassBackedExample")) shouldBe true
    catalog.cards.map { it.className }.contains(cn("UnrelatedHelper")) shouldBe false
  }

  @Test
  internal fun nonEventCardsCannotCarryTheEventTag() {
    val invalid =
        catalogWith(
            """
            CLASS Mistagged : AutomatedCard<Class<ProjectCard>> {
              cost = 0
              This:: EventTag<This>
            }
            """
        )

    shouldThrow<IllegalArgumentException> { invalid.classTable }
  }

  @Test
  internal fun activeAndAutomatedRolesMustMatchPersistentBehavior() {
    val invalid =
        catalogWith(
            """
            CLASS Misclassified : AutomatedCard<Class<ProjectCard>> {
              cost = 0
              Generation: MC
            }
            """
        )

    shouldThrow<IllegalArgumentException> { invalid.classTable }
  }

  @Test
  internal fun sourceOwnedPersistentComponentsMakeProjectCardsActive() {
    val valid =
        catalogWith(
            """
            ABSTRACT CLASS PersistentCapability<CardFront<Player>> : Owned<Player> {
              Generation: MC
            }

            CLASS ConcreteCapability : PersistentCapability

            CLASS ComponentBacked : ActiveCard<Class<ProjectCard>> {
              cost = 0
              This:: ConcreteCapability<This>
            }

            CLASS OneTimeAutomatic : AutomatedCard<Class<ProjectCard>> {
              cost = 0
              This:: 2 MC
            }
            """
        )

    valid.classTable
  }

  private fun catalogWith(source: String): TfmCatalog {
    val additions =
        object : TfmCatalog() {
          override val explicitClassDeclarations: Set<ClassDeclaration> =
              parseClasses(source.trimIndent()).toSet()
        }
    return TfmCatalog.compose(Canon, additions)
  }
}
