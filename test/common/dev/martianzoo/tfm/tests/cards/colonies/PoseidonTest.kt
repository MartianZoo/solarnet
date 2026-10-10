package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.Poseidon
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PoseidonTest : TfmSandboxTest() {
  @Test
  internal fun `Its free starting colony earns both Luna and Poseidon production bonuses`() {
    newTestGame(addOptions = "Luna", kimCorporation = Poseidon)

    kim.stdAction("RequiredActionsSignal") { doTask("Colony<Luna>") }
        .expect("Colony<Luna>, PROD[3 MC], 0 MC")
    kim.count("RequiredAction") shouldBe 0
  }
}
