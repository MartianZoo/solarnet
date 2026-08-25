package dev.martianzoo.tfm.script

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Gameplay
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test
import kotlin.test.assertEquals

internal class StinaScriptTest {
  @Test
  internal fun `Stina Saturn Systems game`() {
    val repl = ScriptSession()
    val script =
        """
        // Stina's Saturn Systems solo game
        newgame "TerraformingMars, CorporateEraExpansion, ElysiumMap, PreludeExpansion" Me purple

        // Neutral tiles for solo setup
        task CityTile<Elysium_5_6, SoloOpponent>
        task GreeneryTile<Elysium_5_5, SoloOpponent>
        task CityTile<Elysium_7_7, SoloOpponent>
        task GreeneryTile<Elysium_7_6, SoloOpponent>

        become Me

        tfm_play SaturnSystems
        task Ok
        task 30 Pay<Class<Megacredit>> FROM Megacredit

        tfm_play Biolab
        tfm_play AcquiredSpaceAgency

        tfm_play EarthOffice, 1
        tfm_play MediaGroup, 3

        tfm_play InvestmentLoan
        task Ok

        tfm_play IndenturedWorkers

        tfm_play EarthCatapult, 12

        tfm_play HiredRaiders
        task 2 Steel<Me> FROM Steel<SoloOpponent>
        task Ok

        tfm_play OlympusConference, 2 Steel, 1

        tfm_play AdvancedAlloys, 7
        task ProjectCard FROM Science<OlympusConference>

        tfm_play MineralDeposit, 3

        tfm_play ResearchOutpost, 5 Steel, 1
        task CityTile<Elysium_9_7>

        tfm_play InventionContest
        task Ok
        task ProjectCard FROM Science<OlympusConference>

        tfm_play BusinessContacts, 1

        tfm_play QuantumExtractor, 10

        tfm_play SpaceStation, 1 Titanium, 1

        tfm_play OptimalAerobraking
        task 1 Ok
        task Ok

        tfm_play TechnologyDemonstration
        task 1 Ok
        task 1 Ok
        task ProjectCard FROM Science<OlympusConference>

        tfm_play ImportOfAdvancedGhg
        task 1 Ok
        task Ok

        tfm_play ImportedGhg
        task 1 Ok
        task Ok

        tfm_play MassConverter, 5

        tfm_play TowingAComet, 3 Titanium, 2
        task OceanTile<Elysium_1_2>

        tfm_play AdaptationTechnology, 9
        task Science<OlympusConference>!

        tfm_play SpecialDesign, 1
        task ProjectCard FROM Science<OlympusConference>

        tfm_play Shuttles, 1
        """
            .trimIndent()

    script
        .lineSequence()
        .map { it.substringBefore("//").trim() }
        .filter(String::isNotEmpty)
        .forEach(repl::command)

    val p1 = repl.game.tfm(PLAYER1)
    p1.assertResources(m = 9, s = 0, t = 3, p = 2, e = 0, h = 15)
    p1.assertProduction(m = 2, s = 0, t = 1, p = 1, e = 9, h = 3)
    p1.assertCounts(
        16 to "TR",
        2 to "ProjectCard",
        26 to "CardFront OR PlayedEvent",
        12 to "ActiveCard",
        0 to "AutomatedCard",
        11 to "PlayedEvent",
    )
    p1.assertTags(but = 2, spt = 3, sct = 7, pot = 2, eat = 4, jot = 1, cit = 1)
    p1.assertCounts(
        1 to "CityTile",
        0 to "GreeneryTile",
        0 to "SpecialTile",
        1 to "Generation",
        0 to "TemperatureStep",
        1 to "OxygenStep",
        1 to "OceanTile",
        1 to "Science",
    )
  }

  // Full-game counterparts live in
  // engine/src/commonTest/kotlin/dev/martianzoo/tfm/engine/games/AbstractFullGameTest.kt.
  private fun Gameplay.assertCounts(vararg expected: Pair<Int, String>) {
    expected.forEach { (count, metric) -> assertEquals(count, count(metric), metric) }
  }

  private fun Gameplay.assertResources(m: Int, s: Int, t: Int, p: Int, e: Int, h: Int) {
    assertCounts(m to "M", s to "S", t to "T", p to "P", e to "E", h to "H")
  }

  private fun TfmGameplay.assertProduction(m: Int, s: Int, t: Int, p: Int, e: Int, h: Int) {
    assertEquals(m, production(cn("M")), "M production")
    assertEquals(s, production(cn("S")), "S production")
    assertEquals(t, production(cn("T")), "T production")
    assertEquals(p, production(cn("P")), "P production")
    assertEquals(e, production(cn("E")), "E production")
    assertEquals(h, production(cn("H")), "H production")
  }

  private fun Gameplay.assertTags(
      but: Int = 0,
      spt: Int = 0,
      sct: Int = 0,
      pot: Int = 0,
      eat: Int = 0,
      jot: Int = 0,
      vet: Int = 0,
      plt: Int = 0,
      mit: Int = 0,
      ant: Int = 0,
      cit: Int = 0,
  ) {
    assertCounts(
        but to "BuildingTag",
        spt to "SpaceTag",
        sct to "ScienceTag",
        pot to "PowerTag",
        eat to "EarthTag",
        jot to "JovianTag",
        plt to "PlantTag",
        mit to "MicrobeTag",
        ant to "AnimalTag",
        cit to "CityTag",
        vet to "VenusTag",
    )
  }
}
