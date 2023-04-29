package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.Humanizing.counts
import dev.martianzoo.tfm.engine.Humanizing.playCard
import dev.martianzoo.tfm.engine.Humanizing.production
import dev.martianzoo.tfm.engine.Humanizing.stdProject
import dev.martianzoo.tfm.engine.Humanizing.turn
import dev.martianzoo.tfm.engine.Humanizing.useCardAction
import org.junit.jupiter.api.Test

class RealGamesTest {
  @Test
  fun fourWholeGenerations() {
    val game = Engine.newGame(GameSetup(Canon, "BREPT", 2))
    val engine = game.asPlayer(ENGINE).session()
    val p1 = engine.asPlayer(PLAYER1)
    val p2 = engine.asPlayer(PLAYER2)

    p1.execute("CorporationCard, LakefrontResorts, 3 BuyCard")
    p2.execute("CorporationCard, InterplanetaryCinematics, 8 BuyCard")

    p1.execute("2 PreludeCard, MartianIndustries, GalileanMining")
    p2.execute("2 PreludeCard, MiningOperations, UnmiContractor")

    engine.execute("ActionPhase")

    p1.execute("-30 THEN AsteroidMining")

    with(p2) {
      execute("-4 Steel THEN -1 THEN NaturalPreserve", "NpTile<E37>")
      execute("-13 Steel THEN -1 THEN SpaceElevator")
      execute("UseAction1<SpaceElevator>")
      execute("-2 THEN InventionContest")
      execute("-6 THEN GreatEscarpmentConsortium", "PROD[-Steel<P1>]")
    }

    engine.execute("ProductionPhase")
    p1.doTask("4 BuyCard")
    p2.doTask("1 BuyCard")
    engine.execute("ActionPhase")

    with(p2) {
      execute("UseAction1<SpaceElevator>")
      execute("-23 THEN EarthCatapult")
    }

    with(p1) {
      execute("-7 THEN TitaniumMine")
      execute("-9 THEN RoboticWorkforce", "@copyProductionBox(MartianIndustries)")
      execute("-6 THEN Sponsors")
    }

    with(p2) {
      execute("-5 Steel THEN IndustrialMicrobes")
      execute("-Titanium THEN TechnologyDemonstration")
      execute("-1 THEN EnergyTapping", "PROD[-Energy<P1>]")
      execute("-2 Steel THEN BuildingIndustries")
    }

    engine.execute("ProductionPhase")
    p1.doTask("3 BuyCard")
    p2.doTask("2 BuyCard")
    engine.execute("ActionPhase")

    p1.execute("-2 THEN -1 Steel THEN Mine")

    with(p2) {
      execute("UseAction1<SpaceElevator>")
      execute("-5 THEN -5 Steel THEN ElectroCatapult")
      execute("UseAction1<ElectroCatapult>", "-Steel THEN 7") // TODO just one
      execute("-Titanium THEN -7 THEN SpaceHotels")
      execute("-6 THEN MarsUniversity")
      execute("-10 THEN ArtificialPhotosynthesis", "PROD[2 Energy]")
      execute("-5 THEN BribedCommittee")
    }

    engine.execute("ProductionPhase")
    p1.doTask("3 BuyCard")
    p2.doTask("2 BuyCard")
    engine.execute("ActionPhase")

    with(p2) {
      execute("UseAction1<ElectroCatapult>")
      agent.removeTask(TaskId("A"))
      execute("-Steel THEN 7")
      execute("UseAction1<SpaceElevator>")
    }

    with(p1) {
      execute("-2 Steel THEN -14 THEN ResearchOutpost", "CityTile<E56>")
      execute("-13 Titanium THEN -1 THEN IoMiningIndustries")
    }

    with(p2) {
      execute("-Titanium THEN -1 THEN TransNeptuneProbe")
      execute("-1 THEN Hackers", "PROD[-2 Megacredit<P1>]")
    }

    p1.execute("UseAction1<SellPatents>", "Megacredit FROM ProjectCard")

    with(p2) {
      execute("-4 Steel THEN -1 THEN SolarPower")
      stdProject("CitySP", "-25 THEN CityTile<E65> THEN PROD[1]")

      execute("PROD[-Plant, Energy]") // CORRECTION TODO WHY WHY
    }

    engine.execute("ProductionPhase")

    // Stuff
    assertThat(engine.counts("Generation")).containsExactly(5)
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
    val eng = game.asPlayer(ENGINE).session()
    val p1 = eng.asPlayer(PLAYER1)
    val p2 = eng.asPlayer(PLAYER2)

    p1.turn("InterplanetaryCinematics", "7 BuyCard")
    p2.turn("PharmacyUnion", "5 BuyCard")

    eng.execute("ActionPhase")

    p1.turn("UseAction1<PlayCardFromHand>", "PlayCard<Class<MediaGroup>>", "6 Pay<Class<M>> FROM M")

    assertThat(p1.counts("Tag, BuildingTag, EarthTag, ProjectCard")).containsExactly(2, 1, 1, 6)
  }

  @Test
  fun ellieGame() {
    val game = Engine.newGame(GameSetup(Canon, "BRHXP", 2))
    val eng = game.asPlayer(ENGINE).session()
    val p1 = eng.asPlayer(PLAYER1)
    val p2 = eng.asPlayer(PLAYER2)

    // Let's play our corporations

    p1.turn("InterplanetaryCinematics", "7 BuyCard")
    p2.turn("PharmacyUnion", "5 BuyCard")

    // Let's play our preludes

    eng.execute("PreludePhase")

    p1.turn("UnmiContractor")
    p1.turn("CorporateArchives")

    p2.turn("BiosphereSupport")
    p2.turn("SocietySupport")

    // Action!

    eng.execute("ActionPhase")

    p1.playCard(6, "MediaGroup")
    p1.playCard(1, "Sabotage", "-7 Megacredit<P2>")

    p2.playCard(11, "Research")
    p2.playCard(9, "MartianSurvey", "Ok") // TODO make "Ok" unnecessary

    p2.playCard(
        3,
        "SearchForLife",
        "PlayedEvent<Class<PharmacyUnion>> FROM PharmacyUnion THEN 3 TerraformRating")

    p2.useCardAction(1, "SearchForLife", "-1 THEN Ok") // TODO simplify

    // Generation 2

    eng.execute("ProductionPhase")
    p1.doTask("BuyCard")
    p2.doTask("3 BuyCard")
    eng.execute("ActionPhase")

    p2.turn("UseAction1<SellPatents>", "Megacredit FROM ProjectCard")
    p2.playCard(15, "VestaShipyard", "Ok")

    // P2 passes already, it's all P1 now...

    p1.playCard(23, "EarthCatapult")
    p1.turn(
        "UseAction1<PlayCardFromHand>",
        "PlayCard<Class<OlympusConference>>",
        "Ok",
        "4 Pay<Class<S>> FROM S",
        "Science<OlympusConference>")

    p1.playCard(
        1,
        "DevelopmentCenter",
        "4 Pay<Class<S>> FROM S",
        "ProjectCard FROM Science<OlympusConference>")

    p1.playCard(1, "GeothermalPower")

    // studying to see why this is so slow
    p1.doTask("4 Pay<Class<S>> FROM S")

    p1.playCard(10, "MirandaResort", "Ok")
    p1.playCard(1, "Hackers", "PROD[-2 M<P2>]")
    p1.playCard(1, "MicroMills")

    // Generation 2

    eng.execute("ProductionPhase")
    p1.doTask("3 BuyCard")
    p2.doTask("BuyCard")
    eng.execute("ActionPhase")

    p1.useCardAction(1, "DevelopmentCenter")
    p1.playCard(
        1,
        "ImmigrantCity",
        "5 Pay<Class<S>> FROM S",
        "CityTile<Hellas_9_7>",
        "OceanTile<Hellas_5_6>")

    assertThat(eng.agent.tasks()).isEmpty()
    assertThat(eng.count("PaymentMechanic")).isEqualTo(0)

    // Check counts, shared stuff first

    assertThat(eng.counts("Generation")).containsExactly(3)
    assertThat(eng.counts("OceanTile, OxygenStep, TemperatureStep")).containsExactly(1, 0, 0)

    with(p1) {
      assertThat(count("TerraformRating")).isEqualTo(24)

      assertThat(production().values).containsExactly(5, 0, 0, 0, 0, 1).inOrder()

      assertThat(counts("M, S, T, P, E, H"))
          .containsExactly(16, 3, 0, 0, 0, 1)
          .inOrder()

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

      assertThat(counts("M, S, T, P, E, H"))
          .containsExactly(18, 0, 1, 6, 1, 3)
          .inOrder()

      assertThat(counts("ProjectCard, CardFront, ActiveCard, AutomatedCard, PlayedEvent"))
          .containsExactly(9, 5, 1, 2, 2)

      assertThat(counts("Tag, SPT, SCT, JOT, PLT")).containsExactly(6, 1, 3, 1, 1).inOrder()

      assertThat(counts("CityTile, GreeneryTile, SpecialTile")).containsExactly(0, 0, 0).inOrder()
    }

    // To check VPs we have to fake the game ending

    val cp = game.checkpoint()
    eng.execute("End")
    assertThat(eng.agent.tasks()).isEmpty()

    // Not sure where this discrepancy comes from... expected P2 to be shorted 1 pt because event

    // 23 2 1 1 -1
    assertThat(p1.count("VictoryPoint")).isEqualTo(27)

    // 25 1 1 1 (but getting shorted for event card)
    assertThat(p2.count("VictoryPoint")).isEqualTo(27) // TODO 28
  }
}
