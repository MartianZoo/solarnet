package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MartianMediaCenterTest : TfmSandboxTest() {
  @Test
  internal fun `Martian Media Center action requires and places an available owned delegate`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("$MartianMediaCenter")

    kim.cardAction1(MartianMediaCenter) { doTask("PartyDelegate<Greens>") }
        .expect("PartyDelegate<Greens>, -3 MC, 0 LobbyActionAvailable")
  }

  @Test
  internal fun `Martian Media Center may spend the last unplaced Lobby delegate`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("$MartianMediaCenter")
    kim.exMachina("6 PartyDelegate<Unity>")

    kim.cardAction1(MartianMediaCenter) { doTask("PartyDelegate<Greens>") }
        .expect("PartyDelegate<Greens>, -3 MC, -LobbyActionAvailable")
  }

  @Test
  internal fun `Martian Media Center action cannot be used without an available delegate`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("$MartianMediaCenter")
    kim.exMachina("7 PartyDelegate<Unity>")

    shouldThrow<DeadEndException> {
      kim.cardAction1(MartianMediaCenter) { doTask("PartyDelegate<Greens>") }
    }

    kim.count("MC") shouldBe 42
    kim.count("PartyDelegate<Greens>") shouldBe 0
  }
}
