package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilSolarPhaseTest : CardTest() {
  @Test
  internal fun `solar turmoil waits for the current event before government and changing times`() {
    newGame(TurmoilExpansion)
    admin.manual(
        "Current<Class<AquiferReleasedByPublicCouncil>> " +
            "FROM Coming<Class<AquiferReleasedByPublicCouncil>>"
    )
    admin.manual("Coming<Class<DryDeserts>> FROM Distant<Class<DryDeserts>>")
    admin.manual("RevealDistantEvent") { doTask("CelebrityLeaders") }

    TfmWorkflow.Manual(game).solarPhase()

    p1.count("TerraformRating") shouldBe 19
    requireP2().count("TerraformRating") shouldBe 19
    admin.count("Ruling<Greens>") shouldBe 1
    admin.count("Current<Class<AquiferReleasedByPublicCouncil>>") shouldBe 1

    p1.doTask("OceanTile<Tharsis_1_2> BY Admin")

    admin.count("Ruling<MarsFirst>") shouldBe 1
    admin.count("AquiferReleasedByPublicCouncil") shouldBe 0
    admin.count("Current<Class<DryDeserts>>") shouldBe 1
    admin.count("Coming<Class<CelebrityLeaders>>") shouldBe 1
    admin.count("Distant") shouldBe 0

    admin.doTask("Diversity")

    admin.count("Distant<Class<Diversity>>") shouldBe 1
  }

  @Test
  internal fun `terraform rating revision precedes the current global event`() {
    newGame(TurmoilExpansion)
    p1.manual("10 MC")
    admin.manual("RedInfluence, Current<Class<RedInfluence>>")

    TfmWorkflow.Manual(game).solarPhase()

    p1.count("TerraformRating") shouldBe 20
    p1.count("MC") shouldBe 7
  }
}
