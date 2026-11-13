package dev.martianzoo.tfm.script

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class TfmPlayCommandTest {
  @Test
  internal fun `tfm play completes a card action that was already selected`() {
    val repl = ScriptSession()
    repl.command("newgame BRP 2")
    repl.agents[PLAYER1].doTask("-10 ProjectCard<Selecting>")
    repl.agents[PLAYER2].doTask("-10 ProjectCard<Selecting>")
    repl.command("auto safe")
    repl.command("become Player1")
    repl.agent.runOperation("ProjectCard, 4 MC")
    repl.command("phase Action")
    repl.command("turn")
    repl.agent.doTasks(
        "UseAction<PlayCardAction, Action1>",
        "PlayCard<Class<ProjectCard>, Class<Mine>, Hand>",
    )

    val result = repl.command("tfm_play Mine, 4 MC")

    assertEquals(1, repl.agent.count("Mine<Player1>"), result.joinToString("\n"))
    assertEquals(0, repl.agent.count("ProjectCard<Hand>"))
    assertEquals(0, repl.agent.count("Owed"))
  }

  @Test
  internal fun `tfm play uses the authorized source within an automatic solo game`() {
    val repl = ScriptSession()
    repl.newGame(
        "TerraformingMars, CorporateEraExpansion, ElysiumMap, PreludeExpansion, promos:cards",
        listOf("Me"),
        purple = true,
        soloLocations =
            listOf("Elysium_5_6", "Elysium_5_5", "Elysium_7_7", "Elysium_7_6").map(::cn),
    )
    repl.command("become Me")
    repl.agent.doTask("Ok") // keep all ten starting projects
    repl.agent.autoExecPolicy = NONE
    repl.agent.doTasks(
        "PlayCard<Class<CorporationCard>, Class<SaturnSystems>>",
        "SaturnSystems FROM CorporationCard",
        "42 MC",
        "PROD[Titanium]",
        "PROD[MC]",
        "30 Pay<> FROM MC",
        "10 ProjectCard<Hand FROM Selecting>",
    )
    repl.agent.autoExecPolicy = EAGER
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
  internal fun `tfm play pays with alternatives first and rolls back an incorrect remainder`() {
    val repl = ScriptSession()
    repl.command("newgame BRP 2")
    repl.agents[PLAYER1].doTask("-10 ProjectCard<Selecting>")
    repl.agents[PLAYER2].doTask("-10 ProjectCard<Selecting>")
    repl.command("auto safe")
    repl.command("become Player1")
    repl.agent.runOperation("ProjectCard, 2 Steel, 6 MC")
    repl.command("phase Action")
    repl.command("turn")

    val policy = repl.agent.autoExecPolicy
    val rejected = repl.command("tfm_play OlympusConference, 5 MC, 2 Steel")
    assertTrue(rejected.single().contains("no matching task"), rejected.joinToString("\n"))
    assertEquals(2, repl.agent.count("Steel"))
    assertEquals(6, repl.agent.count("MC"))
    assertEquals(0, repl.agent.count("Owed"))
    assertEquals(policy, repl.agent.autoExecPolicy)

    repl.command("tfm_play OlympusConference, 6 MC, 2 Steel")

    assertEquals(1, repl.agent.count("OlympusConference<Player1>"))
    assertEquals(0, repl.agent.count("Steel<Player1>"))
    assertEquals(0, repl.agent.count("Owed<Player1>"))
  }
}
