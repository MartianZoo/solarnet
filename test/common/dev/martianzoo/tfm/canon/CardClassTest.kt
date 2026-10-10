package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.data.ClassDeclaration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CardClassTest {
  @Test
  internal fun productionMetricsAreNotCopyableProductionBoxes() {
    val source =
        catalogWith(
            """
        CLASS MetricOnly : AutomatedCard {
          cost = 0
          This: Plant / PROD[Energy]
        }
        CLASS MixedProduction : AutomatedCard {
          cost = 0
          This: PROD[Energy], Plant / PROD[Heat]
        }
        """
        )
    cardProductionBoxes(source.card(cn("MetricOnly"))) shouldBe emptyList()
    cardProductionBoxes(source.card(cn("MixedProduction"))) shouldBe
        listOf(parse<Instruction>("PROD[Energy]"))
  }

  private val catalog: TfmCatalog by lazy {
    catalogWith(
        """
        CLASS ClassBackedExample : ActionCard, ActiveCard, ResourceCard<Class<Microbe>> {
          cost = 7
          requirement = HAS "3 OceanTile"

          HAS =1 EarthTag<This>, =1 BuildingTag<This>
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
  internal fun printedTagsIncludeInheritedCountsWithoutDuplicatingInheritedDeclarations() {
    val source =
        catalogWith(
            """
            ABSTRACT CLASS SciencePair : AutomatedCard { HAS =2 ScienceTag<This> }
            ABSTRACT CLASS FirstFamily : SciencePair
            ABSTRACT CLASS SecondFamily : SciencePair
            CLASS InheritedTags : FirstFamily, SecondFamily {
              cost = 0
              HAS =1 EarthTag<This>
            }
            """
        )

    cardTags(source.card(cn("InheritedTags")))
        .shouldContainExactlyInAnyOrder(cn("ScienceTag"), cn("ScienceTag"), cn("EarthTag"))
  }

  @Test
  internal fun nonEventCardsCannotCarryTheEventTag() {
    val invalid =
        catalogWith(
            """
            CLASS Mistagged : AutomatedCard {
              cost = 0
              HAS =1 EventTag<This>
            }
            """
        )

    shouldThrow<IllegalArgumentException> { validateCardClassification(invalid) }
  }

  @Test
  internal fun activeAndAutomatedRolesMustMatchPersistentBehavior() {
    val invalid =
        catalogWith(
            """
            CLASS Misclassified : AutomatedCard {
              cost = 0
              Generation: MC
            }
            """
        )

    shouldThrow<IllegalArgumentException> { validateCardClassification(invalid) }
  }

  @Test
  internal fun sourceOwnedPersistentComponentsMakeProjectCardsActive() {
    val valid =
        catalogWith(
            """
            ABSTRACT CLASS PersistentCapability<CardFront<@Player>> : Owned<@Player> {
              Generation: MC
            }

            CLASS ConcreteCapability : PersistentCapability

            CLASS ComponentBacked : ActiveCard {
              cost = 0
              HAS =1 ConcreteCapability<This>
            }

            CLASS EffectBacked : ActiveCard {
              cost = 0
              This:: ConcreteCapability<This>
            }

            CLASS OneTimeAutomatic : AutomatedCard {
              cost = 0
              This:: 2 MC
            }
            """
        )

    validateCardClassification(valid)
  }

  @Test
  internal fun cardboundCubesDoNotMakeAResourceCard() {
    val source =
        catalogWith(
            """
        CLASS DecoyAnimal : Cardbound
        CLASS DecoyBirds : ActionCard, ActiveCard {
          cost = 10
          HAS =1 AnimalTag<This>
          End: VictoryPoint / DecoyAnimal<This>
          -> DecoyAnimal<This>
        }
        """
        )

    cardResourceType(source.card(cn("DecoyBirds"))) shouldBe null
  }

  private fun catalogWith(source: String): TfmCatalog {
    val additions =
        object : TfmCatalog() {
          override val explicitClassDeclarations: Set<ClassDeclaration> =
              parseClasses(source.trimIndent()).toSet()
        }
    return TfmCatalog(Canon, additions)
  }
}
