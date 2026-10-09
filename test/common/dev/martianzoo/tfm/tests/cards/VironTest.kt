package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class VironTest : TfmSandboxTest() {
  @Test
  internal fun `Can repeat an action used earlier in the generation`() {
    initializeGame()
    kim.cardAction1(AtmoCollectors)
    kim.cardAction1(Viron) { doTask("UseAction<$AtmoCollectors, Action1>") }.expect("Floater")
  }

  @Test
  internal fun `Can choose a different action on the previously used card`() {
    initializeGame()

    kim.cardAction1(AtmoCollectors)

    kim.cardAction1(Viron) {
          doTask("UseAction<$AtmoCollectors, Action2>")
          doTask("2 Titanium")
        }
        .expect("-Floater, 2 Titanium")
  }

  @Test
  internal fun `Cannot use Viron to repeat Viron's own action`() {
    initializeGame()
    kim.cardAction1(AtmoCollectors)
    kim.cardAction1(Viron) {
      shouldThrow<NarrowingException> { doTask("UseAction<$Viron, Action1>") }
      abort()
    }
  }

  @Test
  internal fun `Cannot choose an action card that has not been used`() {
    initializeGame()
    kim.exMachina("$ExtractorBalloons")
    kim.cardAction1(AtmoCollectors)

    kim.cardAction1(Viron) {
      shouldThrow<NarrowingException> { doTask("UseAction<$ExtractorBalloons, Action1>") }
      abort()
    }
  }

  @Test
  internal fun `Cannot repeat another player's action`() {
    newTestGame(kimCorporation = Viron)
    kim.exMachina("$ExtractorBalloons")
    stan.exMachina("$AtmoCollectors, Floater<$AtmoCollectors>")
    kim.cardAction1(ExtractorBalloons)
    stan.cardAction1(AtmoCollectors)

    kim.cardAction1(Viron) {
      shouldThrow<NarrowingException> { doTask("UseAction<$AtmoCollectors<Stan>, Action1>") }
      abort()
    }
  }

  @Test
  internal fun `Repeats an action on another corporation`() {
    newTestGame(kimCorporation = Viron)
    kim.exMachina("$Celestic")
    kim.cardAction1(Celestic) { addCardResources(Celestic) }

    kim.cardAction1(Viron) {
          doTask("UseAction<$Celestic, Action1>")
          addCardResources(Celestic)
        }
        .expect("Floater<$Celestic>")
  }

  private fun initializeGame() {
    newTestGame(kimCorporation = Viron)
    kim.exMachina("$AtmoCollectors, Floater<$AtmoCollectors>")
  }
}
