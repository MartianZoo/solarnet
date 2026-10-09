package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.Cimmeria
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.Utopia
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CapitalTest : CardTest() {
  @Test
  internal fun `Scores adjacent oceans`() {
    newGame()
    p1.runOperation("PROD[2 Energy], OceanTile<Tharsis_3_2>, OceanTile<Tharsis_4_3>")
    admin.runOperation("OceanTile<Tharsis_6_8>, OceanTile<Tharsis_9_9>")

    p1.runOperation("$Capital") { placeTile(3, 3) }

    admin.runOperation("End FROM Phase")
    p1.assertCounts(24 to "VictoryPoint")
  }

  @Test
  internal fun `Counts as a special tile toward the Manager milestone`() {
    newGame(Utopia)
    p1.runOperation("8 MC, PROD[2 Energy]")
    p1.runOperation(
        "EcologicalZone_SpecialTile<Utopia_2_2>, NaturalPreserve_SpecialTile<Utopia_3_3>"
    )
    p1.runOperation("$Capital") { placeTile(1, 1) }
    admin.phase("Action")

    p1.claimMilestone(cn("Manager")).expect("Manager")
  }

  @Test
  internal fun `Capital counts for Founder as well as city effects`() {
    newGame(
        Cimmeria,
        PreludeExpansion,
        CorporateEraExpansion,
        startingProjects = listOf(0, 1),
    )
    val p2 = requireP2()
    p1.playCorp(TharsisRepublic, 0)
    p2.playCorp(CrediCor)
    p2.runOperation("72 MC")
    admin.phase("Prelude")
    p2.playPrelude(PowerGeneration)
    admin.phase("Action")
    p1.stdAction("DoRequiredActionsAction") { placeTile(1, 2) }
    p1.stdProject("GreeneryProject") { placeTile(2, 2) }
    p2.stdProject("AquiferProject") { placeTile(1, 1) }
    p2.stdProject("AquiferProject") { placeTile(2, 1) }
    p2.stdProject("AquiferProject") { placeTile(1, 5) }
    p2.stdProject("AquiferProject") { placeTile(2, 6) }
    p2.playProject(Capital, 26) { placeTile(3, 3) }.expect("PROD[MC<Player1>]")
    p1.fundAward(cn("Founder"), 8)

    admin.runOperation("End FROM Phase")
    p1.count("FirstPlace<Founder>") shouldBe 1
    p2.count("FirstPlace<Founder>") shouldBe 0
  }
}
