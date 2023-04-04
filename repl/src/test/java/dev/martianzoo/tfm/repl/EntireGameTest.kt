package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.ResourceUtils.lookUpProductionLevels
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Engine
import org.junit.jupiter.api.Test

class EntireGameTest {
  @Test
  fun fourWholeGenerations() {
    val game = Engine.newGame(GameSetup(Canon, "BREPT", 2))
    val p1 = InteractiveSession(game, PLAYER1)
    val p2 = InteractiveSession(game, PLAYER2)
    val engine = InteractiveSession(game)

    p1.execute("CorporationCard, LakefrontResorts, 3 BuyCard")
    p2.execute("CorporationCard, InterplanetaryCinematics, 8 BuyCard")

    p1.execute("2 PreludeCard, MartianIndustries, GalileanMining")
    p2.execute("2 PreludeCard, MiningOperations, UnmiContractor")

    engine.execute("ActionPhase")

    p1.execute("-30 THEN AsteroidMining")

    with(p2) {
      execute("-4 Steel THEN -1 THEN NaturalPreserve", "Tile044<E37>")
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
    p1.doTask("A", "3 BuyCard")
    p2.doTask("B", "2 BuyCard")
    engine.execute("ActionPhase")

    with(p2) {
      execute("UseAction1<ElectroCatapult>")
      dropTask("A")
      execute("-Steel THEN 7")
      execute("UseAction1<SpaceElevator>")
    }

    with(p1) {
      execute("-2 Steel THEN -14 THEN ResearchOutpost", "CityTile<E56>")
      execute("-13 Titanium THEN -1 THEN IoMiningIndustries")
    }

    with(p2) {
      execute("-Titanium THEN -1 THEN TransNeptuneProbe")
      execute("-1 THEN Hackers")
      doTask("B", "PROD[-2 Megacredit<P1>]")
    }

    with(p1) {
      execute("UseAction1<SellPatents>")
      doTask("Megacredit FROM ProjectCard")
    }

    with(p2) {
      execute("-4 Steel THEN -1 THEN SolarPower")
      execute(
          "UseAction1<UseStandardProject>",
          "UseAction1<CitySP>",
          "-25 THEN CityTile<E65> THEN PROD[1]")

      execute("PROD[-Plant, Energy]") // CORRECTION TODO WHY
    }

    engine.execute("ProductionPhase")

    // Stuff
    assertThat(engine.counts("Generation")).containsExactly(5)
    assertThat(engine.counts("OceanTile, OxygenStep, TemperatureStep")).containsExactly(0, 0, 0)

    with(p1) {
      assertThat(count("TerraformRating")).isEqualTo(20)

      val prods = lookUpProductionLevels(game.reader, player.expression)
      assertThat(prods.values).containsExactly(2, 2, 7, 0, 1, 0).inOrder()

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

      val prods2 = lookUpProductionLevels(game.reader, player.expression)
      assertThat(prods2.values).containsExactly(8, 6, 1, 0, 2, 0).inOrder()

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
    val eng = InteractiveSession(game)
    val p1 = eng.asPlayer(PLAYER1)
    val p2 = eng.asPlayer(PLAYER2)

    p1.execute("Turn", "InterplanetaryCinematics", "7 BuyCard")
    p2.execute("Turn", "PharmacyUnion", "5 BuyCard")

    eng.execute("ActionPhase")

    p1.execute(
        "Turn",
        "UseAction1<PlayCardFromHand>",
        "PlayCard<Class<MediaGroup>>",
        "6 Pay<Class<M>> FROM M")

    assertThat(p1.counts("Tag, BuildingTag, EarthTag, ProjectCard")).containsExactly(2, 1, 1, 6)
  }

  @Test
  fun ellieGame() {
    val game = Engine.newGame(GameSetup(Canon, "BRHXP", 2))
    val eng = InteractiveSession(game)
    val p1 = eng.asPlayer(PLAYER1)
    val p2 = eng.asPlayer(PLAYER2)

    p1.execute("Turn", "InterplanetaryCinematics", "7 BuyCard")
    p2.execute("Turn", "PharmacyUnion", "5 BuyCard")

    eng.execute("PreludePhase")

    p1.execute("Turn", "UnmiContractor")
    p1.execute("Turn", "CorporateArchives")

    p2.execute("Turn", "BiosphereSupport")
    p2.execute("Turn", "SocietySupport")

    eng.execute("ActionPhase")

    p1.playCard(6, "MediaGroup")
    p1.playCard(1, "Sabotage", "-7 Megacredit<P2>")

    p2.playCard(11, "Research")
    p2.playCard(9, "MartianSurvey", "Ok") // TODO huh?

    // TODO support TR abbreviation here
    p2.playCard(
        3,
        "SearchForLife",
        "PlayedEvent<Class<PharmacyUnion>> FROM PharmacyUnion THEN 3 TerraformRating")

    p2.useAction1("SearchForLife", "-1 THEN Ok") // TODO simplify

    eng.execute("ProductionPhase")
    p1.doTask("BuyCard")
    p2.doTask("3 BuyCard")
    eng.execute("ActionPhase")

    p2.execute("Turn", "UseAction1<SellPatents>", "Megacredit FROM ProjectCard")

    p2.playCard(15, "VestaShipyard", "Ok") // ?

    p1.playCard(23, "EarthCatapult")
    p1.execute(
        "Turn",
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

    p1.playCard(1, "GeothermalPower", "4 Pay<Class<S>> FROM S")
    p1.playCard(10, "MirandaResort", "Ok")
    p1.playCard(1, "Hackers", "PROD[-2 M<P2>]")
    p1.playCard(1, "MicroMills")

    eng.execute("ProductionPhase")
    p1.doTask("3 BuyCard")
    p2.doTask("BuyCard")
    eng.execute("ActionPhase")

    p1.useAction1("DevelopmentCenter")

    assertThat(eng.agent.tasks()).isEmpty()

    p1.playCard(1, "ImmigrantCity", "5 Pay<Class<S>> FROM S")
    // TODO place the city, then check tasks are empty down here instead

    assertThat(eng.count("PaymentMechanic")).isEqualTo(0)

    // Check counts, shared stuff first

    assertThat(eng.counts("Generation")).containsExactly(3)
    assertThat(eng.counts("OceanTile, OxygenStep, TemperatureStep")).containsExactly(0, 0, 0)

    with(p1) {
      assertThat(count("TerraformRating")).isEqualTo(23)

      val prods1 = lookUpProductionLevels(agent.reader, agent.player) // TODO
      assertThat(prods1.values).containsExactly(4, 0, 0, 0, 0, 1).inOrder()

      assertThat(counts("M, Steel, Titanium, Plant, Energy, Heat"))
          .containsExactly(22, 3, 0, 0, 0, 1)
          .inOrder()

      assertThat(counts("ProjectCard, CardFront, ActiveCard, AutomatedCard, PlayedEvent"))
          .containsExactly(6, 12, 5, 4, 1)

      // tag abbreviations
      assertThat(counts("Tag, BUT, SPT, SCT, POT, EAT, JOT, CIT"))
          .containsExactly(16, 5, 1, 3, 1, 4, 1, 1)
          .inOrder()

      assertThat(counts("CityTile, GreeneryTile, SpecialTile")).containsExactly(0, 0, 0).inOrder()
    }

    with(p2) {
      assertThat(count("TerraformRating")).isEqualTo(25)

      val prods2 = lookUpProductionLevels(agent.reader, agent.player) // TODO
      assertThat(prods2.values).containsExactly(-4, 0, 1, 3, 1, 1).inOrder()

      assertThat(counts("M, Steel, Titanium, Plant, Energy, Heat"))
          .containsExactly(18, 0, 1, 6, 1, 3)
          .inOrder()

      assertThat(counts("ProjectCard, CardFront, ActiveCard, AutomatedCard, PlayedEvent"))
          .containsExactly(9, 5, 1, 2, 2)

      assertThat(counts("Tag, SPT, SCT, JOT, PLT")).containsExactly(6, 1, 3, 1, 1).inOrder()

      assertThat(counts("CityTile, GreeneryTile, SpecialTile")).containsExactly(0, 0, 0).inOrder()
    }

    val cp = game.checkpoint()
    eng.execute("End")
    assertThat(eng.agent.tasks()).hasSize(1) // TODO fix that

    eng.game.eventLog.changesSince(cp).forEach(::println)

    // Not sure where this discrepancy comes from... expected P2 to be shorted 1 pt because event

    // 23 2 1 1 -1
    assertThat(p1.count("VictoryPoint")).isEqualTo(26)

    // 25 1 1 1 (but getting shorted for event card)
    assertThat(p2.count("VictoryPoint")).isEqualTo(27) // TODO 28
  }

  fun InteractiveSession.playCard(cost: Int, cardName: String, vararg tasks: String) {
    execute("Turn", "UseAction1<PlayCardFromHand>", "PlayCard<Class<$cardName>>")
    if (cost > 0) doTask("$cost Pay<Class<M>> FROM M")
    tasks.forEach(::doTask)
  }

  fun InteractiveSession.useAction1(cardName: String, vararg tasks: String) =
      execute(
          "Turn",
          "UseAction1<UseActionFromCard>",
          "UseAction1<$cardName> THEN ActionUsedMarker<$cardName>",
          *tasks)

  fun InteractiveSession.counts(s: String) = s.split(",").map(::count)
}
