package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import kotlin.test.Test

class VitorTest : CardTest() {
  @Test
  fun worksInSoloWithoutAwardFunding() {
    val setup = Canon.fromOptionCodes("BRMSP", 1)
    val game = newBareGame(setup)
    val workflow = TfmWorkflow.Manual(game, setup)
    workflow.setupPhase()
    engine.doFirstTask("CityTile<Tharsis_4_1, Opponent>")
    engine.doTask("GreeneryTile<Tharsis_5_1, Opponent>")
    engine.doFirstTask("CityTile<Tharsis_2_2, Opponent>")
    engine.doTask("GreeneryTile<Tharsis_2_3, Opponent>")
    workflow.corporationPhase()

    val p1 = game.tfm(PLAYER1)
    p1.playCorp("Vitor", 5).expect("5 ProjectCard, 33")
    // Even after Vitor's mandated action is implemented, it should not exist in solo mode.
    p1.assertCounts(0 to "Mandate")

    p1.manual("SearchForLife").expect("3")
    p1.manual("Mine")
    p1.manual("BribedCommittee")
    p1.assertCounts(36 to "M")
  }
}
