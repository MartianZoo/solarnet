package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class RedAppeasementTest : TfmGameplayTest() {
  @Test
  internal fun `Cannot be played after another player has passed`() {
    newTestGame("RedAppeasement, TurmoilExpansion", playerCount = 2)
    kim.turn {
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Reds>") }
      stdAction("LobbyAction", 2, payment = { pay(5) }) { doTask("PartyDelegate<Reds>") }
    }
    stan.pass()

    shouldThrow<RequirementException> { kim.playProject(RedAppeasement, 0) }
  }

  @Test
  internal fun `Passes its owner so the next players pass ends the generation`() {
    newTestGame("RedAppeasement, TurmoilExpansion", playerCount = 2)
    kim.turn {
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Reds>") }
      stdAction("LobbyAction", 2, payment = { pay(5) }) { doTask("PartyDelegate<Reds>") }
    }
    stan.turn { sellPatents(1) }

    kim.playProject(RedAppeasement, 0).expect("PROD[2 MC], Pass, 0 Pass<Stan>")
    stan.pass()

    admin.count("VenusSolarPhase") shouldBe 1
  }
}
