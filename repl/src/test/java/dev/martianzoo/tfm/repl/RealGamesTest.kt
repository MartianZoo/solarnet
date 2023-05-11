package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.Humanize.counts
import dev.martianzoo.tfm.engine.Humanize.playCard
import dev.martianzoo.tfm.engine.Humanize.production
import dev.martianzoo.tfm.engine.Humanize.startTurn
import dev.martianzoo.tfm.engine.Humanize.useCardAction
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.repl.TestHelpers.assertCounts
import dev.martianzoo.tfm.repl.TestHelpers.taskReasons
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

class RealGamesTest {
  @Test
  fun fourWholeGenerations() {
    val game = Engine.newGame(GameSetup(Canon, "BREPT", 2))
    val engine = game.session(ENGINE)
    val p1 = game.session(PLAYER1)
    val p2 = game.session(PLAYER2)

    fun areWeClear() = assertThat(game.tasks.toStrings()).isEmpty()

    p1.action("CorporationCard, LakefrontResorts")
    p1.action("3 BuyCard") // TODO join with prev line?
    p2.action("CorporationCard, InterplanetaryCinematics")
    p2.action("8 BuyCard")

    engine.action("PreludePhase")

    p1.action("2 PreludeCard, MartianIndustries, GalileanMining")
    p2.action("2 PreludeCard, MiningOperations, UnmiContractor")

    engine.action("ActionPhase")

    p1.action("-30, AsteroidMining")

    with(p2) {
      action("-4 Steel, -1, NaturalPreserve") { doFirstTask("NpTile<E37>") }
      action("-13 Steel, -1, SpaceElevator")
      action("UseAction1<SpaceElevator>")
      action("-2, InventionContest")
      action("-6, GreatEscarpmentConsortium") { doFirstTask("PROD[-Steel<Player1>]") }
    }

    engine.action("ProductionPhase, ResearchPhase") {
      p1.tryMatchingTask("4 BuyCard")
      p2.tryMatchingTask("1 BuyCard")
    }

    engine.action("ActionPhase")

    with(p2) {
      action("UseAction1<SpaceElevator>")
      action("-23, EarthCatapult")
      areWeClear()
    }

    with(p1) {
      action("-7 THEN TitaniumMine")
      execute("-9 THEN RoboticWorkforce", "@copyProductionBox(MartianIndustries)")
      action("-6 THEN Sponsors")
      areWeClear()
    }

    with(p2) {
      action("-5 Steel THEN IndustrialMicrobes")
      action("-Titanium THEN TechnologyDemonstration")
      execute("-1 THEN EnergyTapping", "PROD[-Energy<Player1>]")
      action("-2 Steel THEN BuildingIndustries")
      areWeClear()
    }

    engine.action("ProductionPhase")
    engine.action("ResearchPhase") {
      p1.tryMatchingTask("3 BuyCard")
      p2.tryMatchingTask("2 BuyCard")
    }
    engine.action("ActionPhase")
    areWeClear()

    p1.action("-2 THEN -1 Steel THEN Mine")

    with(p2) {
      action("UseAction1<SpaceElevator>")
      action("-5 THEN -5 Steel THEN ElectroCatapult")
      action("UseAction1<ElectroCatapult>") // TODO just one
      action("-Titanium THEN -7 THEN SpaceHotels")
      action("-6 THEN MarsUniversity")
      execute("-10 THEN ArtificialPhotosynthesis", "PROD[2 Energy]")
      action("-5 THEN BribedCommittee")
      areWeClear()
    }

    engine.action("ProductionPhase")
    engine.action("ResearchPhase") {
      p1.tryMatchingTask("3 BuyCard")
      p2.tryMatchingTask("2 BuyCard")
    }
    engine.action("ActionPhase")
    areWeClear()

    with(p2) {
      action("UseAction1<ElectroCatapult>")
      // execute("-Steel THEN 7")
      action("UseAction1<SpaceElevator>")
      areWeClear()
    }

    with(p1) {
      execute("-2 Steel THEN -14 THEN ResearchOutpost", "CityTile<E56>")
      action("-13 Titanium THEN -1 THEN IoMiningIndustries")
      areWeClear()
    }

    with(p2) {
      action("-Titanium THEN -1 THEN TransNeptuneProbe")
      execute("-1 THEN Hackers", "PROD[-2 Megacredit<Player1>]")
      areWeClear()
    }

    p1.execute("UseAction1<SellPatents>", "Megacredit FROM ProjectCard")

    with(p2) {
      action("-4 Steel THEN -1 THEN SolarPower")
      execute("UseAction1<CitySP>", "CityTile<E65>")
      action("PROD[-Plant, Energy]") // CORRECTION TODO WHY WHY
      areWeClear()
    }

    engine.action("ProductionPhase")

    // Stuff
    assertThat(engine.counts("Generation")).containsExactly(4)
    assertThat(engine.counts("OceanTile, OxygenStep, TemperatureStep")).containsExactly(0, 0, 0)

    with(p1) {
      assertThat(count("TerraformRating")).isEqualTo(20)

      assertThat(production().values).containsExactly(2, 2, 7, 0, 1, 0).inOrder()

      assertThat(counts("M, Steel, Titanium, Plant, Energy, Heat"))
          .containsExactly(34, 2, 8, 3, 1, 3)
          .inOrder()

      assertThat(counts("ProjectCard, CardFront, ActiveCard, AutomatedCard, PlayedEvent"))
          .containsExactly(5, 10, 1, 6, 0)

      // tag abbreviations
      assertThat(counts("BUT, SPT, SCT, POT, EAT, JOT, PLT, MIT, ANT, CIT"))
          .containsExactly(5, 2, 2, 0, 1, 3, 0, 0, 0, 1)
          .inOrder()

      assertThat(counts("CityTile, GreeneryTile, SpecialTile")).containsExactly(1, 0, 0).inOrder()
    }

    with(p2) {
      assertThat(count("TerraformRating")).isEqualTo(25)

      assertThat(production().values).containsExactly(8, 6, 1, 0, 2, 0).inOrder()

      assertThat(counts("M, Steel, Titanium, Plant, Energy, Heat"))
          .containsExactly(47, 6, 1, 1, 2, 3)
          .inOrder()

      assertThat(counts("ProjectCard, CardFront, ActiveCard, AutomatedCard, PlayedEvent"))
          .containsExactly(3, 17, 4, 10, 3)

      // tag abbreviations
      assertThat(counts("BUT, SPT, SCT, POT, EAT, JOT, PLT, MIT, ANT, CIT"))
          .containsExactly(9, 3, 4, 2, 3, 0, 0, 1, 0, 0)
          .inOrder()

      assertThat(counts("CityTile, GreeneryTile, SpecialTile")).containsExactly(1, 0, 1).inOrder()
    }
  }

  @Test
  fun startOfEllieGameNoPrelude() {
    val game = Engine.newGame(GameSetup(Canon, "BRHX", 2))
    val eng = game.session(ENGINE)
    val p1 = game.session(PLAYER1)
    val p2 = game.session(PLAYER2)

    p1.action("NewTurn") {
      tryMatchingTask("InterplanetaryCinematics")
      assertCounts(30 to "M", 1 to "BuildingTag", 0 to "ProjectCard")
      tryMatchingTask("7 BuyCard")
      assertCounts(9 to "M", 1 to "BuildingTag", 7 to "ProjectCard")
    }

    p2.action("NewTurn") {
      doFirstTask("PharmacyUnion")
      doFirstTask("5 BuyCard")
      assertThat(taskReasons()).isEmpty()
    }

    eng.action("ActionPhase")

    p1.action("NewTurn") {
      doFirstTask("UseAction1<PlayCardFromHand>")
      doFirstTask("PlayCard<Class<MediaGroup>>")
      assertCounts(9 to "M", 1 to "BuildingTag", 0 to "EarthTag", 7 to "ProjectCard", 6 to "Owed")
      doFirstTask("6 Pay<Class<M>> FROM M")
      assertCounts(3 to "M", 1 to "BuildingTag", 1 to "EarthTag", 6 to "ProjectCard")
    }
  }

  @Test
  fun ellieGame() {
    val game = Engine.newGame(GameSetup(Canon, "BRHXP", 2))
    val eng = game.session(ENGINE)
    val p1 = game.session(PLAYER1)
    val p2 = game.session(PLAYER2)

    // Let's play our corporations

    p1.action("NewTurn") {
      tryMatchingTask("InterplanetaryCinematics")
      assertCounts(30 to "M", 1 to "BuildingTag", 0 to "ProjectCard")
      tryMatchingTask("7 BuyCard")
      assertCounts(9 to "M", 1 to "BuildingTag", 7 to "ProjectCard")
    }

    p2.action("NewTurn", "PharmacyUnion", "5 BuyCard")

    // Let's play our preludes

    eng.action("PreludePhase")

    p1.action("NewTurn", "UnmiContractor")
    p1.action("NewTurn", "CorporateArchives")

    p1.action("NewTurn", "BiosphereSupport")
    p1.action("NewTurn", "SocietySupport")

    // Action!

    eng.action("ActionPhase")

    p1.playCard("MediaGroup", 6)
    p1.playCard("Sabotage", 1)

    // p1.tryMatchingTask("-7 Megacredit<Player2>") TODO playCard is over-Ok'ing

    p2.playCard("Research", 11)
    p2.playCard("MartianSurvey", 9)

    p1.startTurn("Pass")

    p2.startTurn("UseAction1<PlayCardFromHand>", "PlayCard<Class<SearchForLife>>")
    p2.tryMatchingTask("3 Pay<Class<M>> FROM M")
    p2.tryMatchingTask("PlayedEvent<Class<PharmacyUnion>> FROM PharmacyUnion THEN 3 TR")

    p2.useCardAction(1, "SearchForLife", "Ok") // TODO wha?
    p2.startTurn("Pass")

    // Generation 2

    eng.action("ProductionPhase")
    eng.action("ResearchPhase") {
      p1.tryMatchingTask("BuyCard")
      p2.tryMatchingTask("3 BuyCard")
    }
    eng.action("ActionPhase")

    p2.startTurn("UseAction1<SellPatents>", "Megacredit FROM ProjectCard")
    p2.playCard("VestaShipyard", 15)
    p2.startTurn("Pass")

    with(p1) {
      playCard("EarthCatapult", 23)
      playCard("OlympusConference", steel = 4)

      playCard("DevelopmentCenter", 1, steel = 4)
      tryMatchingTask("ProjectCard FROM Science<OlympusConference>")

      playCard("GeothermalPower", 1, steel = 4)

      playCard("MirandaResort", 10)
      playCard("Hackers", 1)
      tryMatchingTask("PROD[-2 M<Player2>]")
      playCard("MicroMills", 1)
      startTurn("Pass")
    }

    // Generation 2

    eng.action("ProductionPhase")
    eng.action("ResearchPhase") {
      p1.tryMatchingTask("3 BuyCard")
      p2.tryMatchingTask("BuyCard")
    }
    eng.action("ActionPhase")

    p1.useCardAction(1, "DevelopmentCenter")
    p1.playCard("ImmigrantCity", 1, steel = 5)
    p1.tryMatchingTask("CityTile<Hellas_9_7>")
    p1.tryMatchingTask("OceanTile<Hellas_5_6>")
    assertThat(eng.count("PaymentMechanic")).isEqualTo(0)

    // Check counts, shared stuff first

    assertThat(eng.counts("Generation")).containsExactly(3)
    assertThat(eng.counts("OceanTile, OxygenStep, TemperatureStep")).containsExactly(1, 0, 0)

    return // TODO get the rest working

    with(p1) {
      assertThat(count("TerraformRating")).isEqualTo(24)

      assertThat(production().values).containsExactly(5, 0, 0, 0, 0, 1).inOrder()

      assertThat(counts("M, S, T, P, E, H")).containsExactly(16, 3, 0, 0, 0, 1).inOrder()

      assertThat(counts("ProjectCard, CardFront, ActiveCard, AutomatedCard, PlayedEvent"))
          .containsExactly(7, 12, 5, 4, 1)

      // tag abbreviations
      assertThat(counts("Tag, BUT, SPT, SCT, POT, EAT, JOT, CIT"))
          .containsExactly(16, 5, 1, 3, 1, 4, 1, 1)
          .inOrder()

      assertThat(counts("CityTile, GreeneryTile, SpecialTile")).containsExactly(1, 0, 0).inOrder()
    }

    with(p2) {
      assertThat(count("TerraformRating")).isEqualTo(25)

      assertThat(production().values).containsExactly(-4, 0, 1, 3, 1, 1).inOrder()

      assertThat(counts("M, S, T, P, E, H")).containsExactly(18, 0, 1, 6, 1, 3).inOrder()

      assertThat(counts("ProjectCard, CardFront, ActiveCard, AutomatedCard, PlayedEvent"))
          .containsExactly(9, 5, 1, 2, 2)

      assertThat(counts("Tag, SPT, SCT, JOT, PLT")).containsExactly(6, 1, 3, 1, 1).inOrder()

      assertThat(counts("CityTile, GreeneryTile, SpecialTile")).containsExactly(0, 0, 0).inOrder()
    }

    // To check VPs we have to fake the game ending

    eng.action("End")
    assertThat(eng.tasks).isEmpty()

    // Not sure where this discrepancy comes from... expected P2 to be shorted 1 pt because event

    // 23 2 1 1 -1 / 25 1 1 1
    eng.assertCounts(26 to "VP<P1>", 28 to "VP<P2>")
  }
}
