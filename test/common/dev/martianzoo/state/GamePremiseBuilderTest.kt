package dev.martianzoo.state

import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test
import kotlin.test.assertSame

internal class GamePremiseBuilderTest {
  @Test
  internal fun moduleDefaultsFollowPlayerCountsAndOtherModulesWhileRespectingExclusions() {
    val catalog =
        testCatalog(
            """
            ABSTRACT CLASS Player : Owner, Actor { HAS =1 This }
            ABSTRACT CLASS Rules : System { autoSelectWhen = Requirement? }
            CLASS GroupRules : Rules { autoSelectWhen = HAS "2 Player" }
            CLASS ExtraRules : Rules { autoSelectWhen = HAS "GroupRules" }
            CLASS OptionalPiece
            """
                .trimIndent(),
            moduleSelections =
                mapOf(
                    cn("GroupRules") to emptySet(),
                    cn("ExtraRules") to setOf(ClassSelection(cn("OptionalPiece"))),
                ),
        )

    val group = catalog.gamePremise(GameConfig("", "Blue", "Red"))
    group.modules shouldBe setOf(cn("GroupRules"), cn("ExtraRules"))
    group.classTable.isInhabited(cn("OptionalPiece")) shouldBe true
    assertSame(
        catalog.classTable.getClass(cn("GroupRules")),
        group.classTable.getClass(cn("GroupRules")),
    )
    catalog.classTable.findClass(cn("Blue")) shouldBe null

    val excluded = catalog.gamePremise(GameConfig("-ExtraRules", "Blue", "Red"))
    excluded.modules shouldBe setOf(cn("GroupRules"))
    excluded.classTable.isInhabited(cn("OptionalPiece")) shouldBe false
    catalog.gamePremise(GameConfig("", "Blue")).modules shouldBe emptySet()
  }

  @Test
  internal fun cyclicModuleDefaultsAreRejected() {
    val catalog =
        testCatalog(
            """
            ABSTRACT CLASS Rules : System { autoSelectWhen = Requirement? }
            CLASS FirstRules : Rules { autoSelectWhen = HAS "MAX 0 SecondRules" }
            CLASS SecondRules : Rules { autoSelectWhen = HAS "MAX 0 FirstRules" }
            """
                .trimIndent(),
            moduleSelections =
                mapOf(cn("FirstRules") to emptySet(), cn("SecondRules") to emptySet()),
        )

    val failure =
        shouldThrow<InvalidPetDefinitionException> {
          catalog.gamePremise(GameConfig(""))
        }
    failure.message.orEmpty() shouldContain "Module defaults do not converge"
  }

  @Test
  internal fun compositionAndPlayerCatalogsRetainGenericModuleSelections() {
    val rules =
        testCatalog(
            """
            ABSTRACT CLASS Player : Owner, Actor { HAS =1 This }
            CLASS Rules : System
            CLASS SelectedPiece
            """
                .trimIndent(),
            moduleSelections = mapOf(cn("Rules") to setOf(ClassSelection(cn("SelectedPiece")))),
        )
    val catalog = Catalog(rules, testCatalog("CLASS OtherPiece"))
    val seated = catalog.withPlayers(2)
    assertSame(seated, catalog.withPlayers(2))
    val premise = seated.gamePremise(GameConfig("Rules, OtherPiece", "Player1", "Player2"))

    premise.classTable.isInhabited(cn("SelectedPiece")) shouldBe true
    premise.classTable.isInhabited(cn("OtherPiece")) shouldBe true
    catalog.classTable.findClass(cn("Player1")) shouldBe null
  }
}
