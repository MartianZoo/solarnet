package dev.martianzoo.tfm.script

import dev.martianzoo.script.ScriptSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class TfmActionCommandTest {
  @Test
  fun `tfm action selects the standard action and uses the requested card action`() {
    val repl = actionGame("PROD[Energy], AiCentral")

    val output = repl.command("tfm_action AiCentral 1")

    assertEquals(1, repl.gameplay.count("ActionUsedMarker<AiCentral>"), output.joinToString("\n"))
    assertEquals(2, repl.gameplay.count("ProjectCard"))
  }

  @Test
  fun `tfm action continues a use card action already underway`() {
    val repl = ScriptSession()
    repl.command("newgame BRP 2")
    repl.command("become Player1")
    repl.gameplay.godMode().manual("PROD[Energy], AiCentral")
    repl.command("auto none")
    repl.gameplay.godMode().beginManual("UseAction1<UseCardActionSA>")
    repl.command("auto safe")

    val output = repl.command("tfm_action AiCentral 1")

    assertEquals(1, repl.gameplay.count("ActionUsedMarker<AiCentral>"), output.joinToString("\n"))
  }

  @Test
  fun `tfm action forwards inline payment through an accept workflow`() {
    val repl = actionGame("WaterImportFromEuropa, 12")

    val output = repl.command("tfm_action WaterImportFromEuropa 1, 12")

    assertEquals(
        1,
        repl.gameplay.count("ActionUsedMarker<WaterImportFromEuropa>"),
        output.joinToString("\n"),
    )
    assertEquals(0, repl.gameplay.count("Megacredit"))
    assertEquals(0, repl.gameplay.count("Owed"))
    assertTrue(repl.command("tasks").any { "OceanTile" in it })
  }

  @Test
  fun `tfm action selects an alternative direct removal cost`() {
    val repl = actionGame("PROD[Energy], ElectroCatapult, Plant, Steel")

    val output = repl.command("tfm_action ElectroCatapult 1, 1 Steel")

    assertEquals(
        1,
        repl.gameplay.count("ActionUsedMarker<ElectroCatapult>"),
        output.joinToString("\n"),
    )
    assertEquals(1, repl.gameplay.count("Plant"))
    assertEquals(0, repl.gameplay.count("Steel"))
    assertEquals(7, repl.gameplay.count("Megacredit"))
  }

  @Test
  fun `tfm action binds a variable direct removal cost`() {
    val repl = actionGame("PowerInfrastructure, 5 Energy")

    val output = repl.command("tfm_action PowerInfrastructure 1, 5 Energy")

    assertEquals(
        1,
        repl.gameplay.count("ActionUsedMarker<PowerInfrastructure>"),
        output.joinToString("\n"),
    )
    assertEquals(0, repl.gameplay.count("Energy"))
    assertEquals(5, repl.gameplay.count("Megacredit"))
  }

  @Test
  fun `tfm action binds a multiplied variable direct removal cost`() {
    val repl = actionGame("EnergyMarket, 6", "BRPX")

    val output = repl.command("tfm_action EnergyMarket 1, 6")

    assertEquals(
        1,
        repl.gameplay.count("ActionUsedMarker<EnergyMarket>"),
        output.joinToString("\n"),
    )
    assertEquals(0, repl.gameplay.count("Megacredit"))
    assertEquals(3, repl.gameplay.count("Energy"))
  }

  @Test
  fun `tfm action verifies a fixed direct removal cost`() {
    val repl = actionGame("DevelopmentCenter, Energy")

    val output = repl.command("tfm_action DevelopmentCenter 1, 1 Energy")

    assertEquals(
        1,
        repl.gameplay.count("ActionUsedMarker<DevelopmentCenter>"),
        output.joinToString("\n"),
    )
    assertEquals(0, repl.gameplay.count("Energy"))
    assertEquals(1, repl.gameplay.count("ProjectCard"))
  }

  @Test
  fun `tfm action rolls back when a direct removal does not match the payment`() {
    val repl = actionGame("PROD[Energy], ElectroCatapult, Plant, Energy")

    val output = repl.command("tfm_action ElectroCatapult 1, 1 Energy")

    assertTrue(output.single().contains("does not narrow"), output.joinToString("\n"))
    assertEquals(0, repl.gameplay.count("ActionUsedMarker<ElectroCatapult>"))
    assertEquals(1, repl.gameplay.count("Plant"))
    assertEquals(1, repl.gameplay.count("Energy"))
  }

  @Test
  fun `tfm action rejects an invalid action number`() {
    val repl = ScriptSession()

    assertTrue(repl.command("tfm_action AiCentral 4").single().startsWith("Usage:"))
  }

  private fun actionGame(contents: String, options: String = "BRP"): ScriptSession {
    val repl = ScriptSession()
    repl.command("newgame $options 2")
    repl.command("become Player1")
    repl.gameplay.godMode().manual(contents)
    repl.command("phase Action")
    repl.gameplay.godMode().beginManual("NewTurn")
    return repl
  }
}
