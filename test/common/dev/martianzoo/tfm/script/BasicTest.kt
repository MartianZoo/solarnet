package dev.martianzoo.tfm.script

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class BasicTest {
  @Test
  internal fun turmoilSetupSuppliesTheFullDeckBeforeAdminDraws() {
    val session = ScriptSession()
    session.newGame("BT", 2)
    assertTrue(session.game.reader.drawOrder.size > 2)
    assertEquals(session.game.reader.drawOrder.size, session.game.reader.drawOrder.toSet().size)
    assertEquals(2, session.count("GlobalEventDrawn"))

    session.newGame("TurmoilExpansion", listOf("Kim", "Stan"))
    assertTrue(session.game.reader.drawOrder.size > 2)
    assertEquals(session.game.reader.drawOrder.size, session.game.reader.drawOrder.toSet().size)
    assertEquals(2, session.count("GlobalEventDrawn"))
  }

  @Test
  internal fun quotedSoloConfigurationInPurpleModePlacesNeutralTiles() {
    val session = ScriptSession()
    val response = session.command("newgame \"TerraformingMars, ElysiumMap\" Me purple")
    assertEquals("Me", session.playerSnapshot().playerName, response.joinToString("\n"))
    assertEquals(ScriptSession.ScriptMode.PURPLE, session.mode)
    assertEquals(2, session.count("CityTile<SoloOpponent>"))
    assertEquals(2, session.count("GreeneryTile<SoloOpponent>"))
  }

  @Test
  internal fun invalidNewGameConfigurationIncludesUsage() {
    val response = ScriptSession().command("newgame B 0")

    assertEquals(2, response.size)
    assertTrue(response.last().startsWith("Usage: newgame"))
  }

  @Test
  internal fun playerSnapshotDefaultsToTheFirstConfiguredPlayer() {
    val session = ScriptSession()

    val created = session.command("newgame B 1")
    assertEquals("Me", session.playerSnapshot().playerName, created.joinToString("\n"))
    assertEquals(2, session.count("CityTile<SoloOpponent>"))
    assertEquals(2, session.count("GreeneryTile<SoloOpponent>"))

    session.command("newgame \"\" Blue Yellow")
    assertEquals("Blue", session.playerSnapshot().playerName)

    session.command("newgame B 6")
    assertEquals(6, session.playerCount)
  }
}
