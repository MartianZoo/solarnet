package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.api.Exceptions.AbstractException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.ResearchNetwork
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

private val fakeEstablishedMethods = cn("FakeEstablishedMethods")

private val fakeEstablishedMethodsDefinition =
    parseOneLinerClass(
        "CLASS FakeEstablishedMethods : CardFront<Class<PreludeCard>> { cost = 0; This: 30 MC, UseAction<StandardAction>!, UseAction<StandardAction>! }"
    )

internal class FakeEstablishedMethodsBugsTest :
    CardTest(additionalClassDeclarations = setOf(fakeEstablishedMethodsDefinition)) {
  @Test
  internal fun `Established Methods without its note dead-ends when no second project is affordable`() {
    newGame(PreludeExpansion)
    p1.phase("Prelude")
    p1.manual("PreludeCard")

    val deadEnd =
        shouldThrow<AbstractException> {
          p1.playPrelude(fakeEstablishedMethods) {
            p1.manual("-20 MC")
            doTask("UseAction<GreenerySP, First>")
            p1.autoExecNow()
          }
        }
    deadEnd.message shouldContain "FakeEstablishedMethods"
  }

  @Test
  internal fun `Nested standard projects preserve pending payments offer positions and wild tags`() {
    newGame(PreludeExpansion)
    p1.manual("2 PreludeCard")
    engine.phase("Prelude")
    p1.startTurn()

    p1.playPrelude(ResearchNetwork)
    p1.startTurn()
    p1.assignWildTag(ResearchNetwork, "PowerTag")
    p1.playPrelude(fakeEstablishedMethods) {
      val offers = standardActionOfferIds()
      offers.size shouldBe 2

      repeat(2) { projectIndex ->
        doTask("UseAction<PowerPlantSP, First>")

        tasks
            .extract { it }
            .any { "Pay" in "${it.instruction}" && "MC" in "${it.instruction}" } shouldBe true
        p1.count("Owed<>") shouldBe 11
        p1.count("WildTagUse<$ResearchNetwork>") shouldBe 1

        p1.pay(11)

        p1.count("WildTagUse<$ResearchNetwork>") shouldBe 1
        standardActionOfferIds() shouldBe offers.drop(projectIndex + 1)
      }
    }

    p1.count("WildTagUse") shouldBe 0
  }

  private fun standardActionOfferIds() =
      game.tasks
          .extract { it }
          .filter {
            val instruction = it.instruction.toString()
            it.assignee == p1.actor && "UseAction" in instruction && "StandardAction" in instruction
          }
          .map { it.id }
}
