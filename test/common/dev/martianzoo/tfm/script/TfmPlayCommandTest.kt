package dev.martianzoo.tfm.script

import kotlin.test.Test
import kotlin.test.assertEquals

internal class TfmPlayCommandTest {
  @Test
  internal fun `tfm play uses the authorized source within the automatic solo workflow`() {
    val repl = ScriptSession()
    repl.command(
        "newgame \"TerraformingMars, CorporateEraExpansion, ElysiumMap, " +
            "PreludeExpansion, PromoCardPack\" Me purple"
    )
    repl.command("task CityTile<Elysium_5_6, SoloOpponent>")
    repl.command("task GreeneryTile<Elysium_5_5, SoloOpponent>")
    repl.command("task CityTile<Elysium_7_7, SoloOpponent>")
    repl.command("task GreeneryTile<Elysium_7_6, SoloOpponent>")
    repl.command("become Me")
    repl.command("task Ok")
    repl.command("tfm_play SaturnSystems")
    repl.command("task Ok")
    repl.command("task 30 Pay<Class<MC>> FROM MC")
    repl.command("tfm_play NewPartner")
    repl.command("tfm_play Donation")
    assertEquals(1, repl.agent.count("PreludeCard<Hand>"))
    assertEquals(0, repl.agent.count("PreludeCard<Selecting>"))
    repl.command("tfm_play AcquiredSpaceAgency")

    repl.command("tfm_play EarthOffice, 1 MC")

    assertEquals(1, repl.agent.count("ActionPhase"))
    assertEquals(1, repl.agent.count("EarthOffice<Me>"))
  }

  @Test
  internal fun `tfm play selects the play card action and forwards inline payment`() {
    val repl = ScriptSession()
    repl.command("newgame BRP 2")
    repl.command("auto safe")
    repl.command("become Player1")
    repl.command("phase Corporation")
    repl.command("turn")
    repl.command("tfm_play SaturnSystems")
    repl.command("task Ok")
    repl.command("task 30 Pay<Class<MC>> FROM MC")
    repl.command("exec 2 Steel")
    repl.command("phase Action")
    repl.command("turn")

    repl.command("tfm_play OlympusConference, 2 Steel, 6 MC")

    assertEquals(1, repl.agent.count("OlympusConference<Player1>"))
    assertEquals(0, repl.agent.count("Steel<Player1>"))
    assertEquals(0, repl.agent.count("Owed<Player1>"))
  }
}
