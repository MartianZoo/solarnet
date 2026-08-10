package dev.martianzoo.tfm.script

import dev.martianzoo.script.ScriptSession
import kotlin.test.Test
import kotlin.test.assertEquals

internal class TfmPlayCommandTest {
  @Test
  fun `tfm play works within the automatic solo workflow`() {
    val repl = ScriptSession()
    repl.command("newgame BERP 1 purple")
    repl.command("task CityTile<Elysium_5_6, SoloOpponent>")
    repl.command("task GreeneryTile<Elysium_5_5, SoloOpponent>")
    repl.command("task CityTile<Elysium_7_7, SoloOpponent>")
    repl.command("task GreeneryTile<Elysium_7_6, SoloOpponent>")
    repl.command("become P1")
    repl.command("tfm_play SaturnSystems")
    repl.command("task 10 BuyCard")
    repl.command("tfm_play Biolab")
    repl.command("tfm_play AcquiredSpaceAgency")

    repl.command("tfm_play EarthOffice, 1")

    assertEquals(1, repl.gameplay.count("ActionPhase"))
    assertEquals(1, repl.gameplay.count("EarthOffice<P1>"))
  }

  @Test
  fun `tfm play selects the play card action and forwards inline payment`() {
    val repl = ScriptSession()
    repl.command("newgame BERP 2")
    repl.command("auto safe")
    repl.command("become P1")
    repl.command("phase Corporation")
    repl.command("turn")
    repl.command("tfm_play SaturnSystems")
    repl.command("task 10 BuyCard")
    repl.command("exec 2 Steel")
    repl.command("phase Action")
    repl.command("turn")

    repl.command("tfm_play OlympusConference, 2 Steel, 6")

    assertEquals(1, repl.gameplay.count("OlympusConference<P1>"))
    assertEquals(0, repl.gameplay.count("Steel<P1>"))
    assertEquals(0, repl.gameplay.count("Owed<P1>"))
  }
}
