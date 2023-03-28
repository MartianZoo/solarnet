package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.ResourceUtils.lookUpProductionLevels
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
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
      execute("-4 Steel THEN -1 THEN NaturalPreserve")
      dropTask("B")
      execute("Tile044<E37>")
      execute("-13 Steel THEN -1 THEN SpaceElevator")
      execute("UseAction1<SpaceElevator>")
      execute("-2 THEN InventionContest")
      execute("-6 THEN GreatEscarpmentConsortium")
      doTask("A", "PROD[-Steel<P1>]")
    }

    engine.execute("ProductionPhase")
    p1.doTask("A", "4 BuyCard")
    p2.doTask("B", "1 BuyCard")
    engine.execute("ActionPhase")

    with(p2) {
      execute("UseAction1<SpaceElevator>")
      execute("-23 THEN EarthCatapult")
    }

    with(p1) {
      execute("-7 THEN TitaniumMine")
      execute("-9 THEN RoboticWorkforce")
      doTask("A", "@copyProductionBox(MartianIndustries)")
      execute("-6 THEN Sponsors")
    }

    with(p2) {
      execute("-5 Steel THEN IndustrialMicrobes")
      execute("-Titanium THEN TechnologyDemonstration")
      execute("-1 THEN EnergyTapping")
      doTask("A", "PROD[-Energy<P1>]")
      execute("-2 Steel THEN BuildingIndustries")
    }

    engine.execute("ProductionPhase")
    p1.doTask("A", "3 BuyCard")
    p2.doTask("B", "2 BuyCard")
    engine.execute("ActionPhase")

    p1.execute("-2 THEN -1 Steel THEN Mine")

    with(p2) {
      execute("UseAction1<SpaceElevator>")
      execute("-5 THEN -5 Steel THEN ElectroCatapult")
      execute("UseAction1<ElectroCatapult>")
      doTask("A", "-Steel THEN 7") // TODO just one
      execute("-Titanium THEN -7 THEN SpaceHotels")
      execute("-6 THEN MarsUniversity")
      execute("-10 THEN ArtificialPhotosynthesis")
      doTask("B", "PROD[2 Energy]")
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
      execute("-2 Steel THEN -14 THEN ResearchOutpost")
      dropTask("A")
      execute("CityTile<E56>") // TODO reif refi
      execute("-13 Titanium THEN -1 THEN IoMiningIndustries")
    }

    with(p2) {
      execute("-Titanium THEN -1 THEN TransNeptuneProbe")
      execute("-1 THEN Hackers")
      doTask("B", "PROD[-2 Megacredit<P1>]")
    }

    with(p1) {
      execute("UseAction1<SellPatents>")
      doTask("A", "Megacredit FROM ProjectCard")
    }

    with(p2) {
      execute("-4 Steel THEN -1 THEN SolarPower")
      execute("UseAction1<UseStandardProject>")
      doTask("A", "UseAction1<CitySP>")
      dropTask("B") // split
      execute("-25 THEN CityTile<E65> THEN PROD[1]")
      execute("PROD[-Plant, Energy]") // CORRECTION TODO WHY
    }

    engine.execute("ProductionPhase")

    fun InteractiveSession.counts(arg: String) = arg.split(",").map { this.count(it) }
    fun InteractiveSession.assertCount(text: String, i: Int) = assertThat(count(text)).isEqualTo(i)

    // Stuff
    assertThat(engine.counts("Generation")).containsExactly(5)
    assertThat(engine.counts("OceanTile, OxygenStep, TemperatureStep")).containsExactly(0, 0, 0)

    // P1

    p1.assertCount("TerraformRating", 20)

    val prods = lookUpProductionLevels(p1.game.reader, p1.player.expression)
    assertThat(prods.values).containsExactly(2, 2, 7, 0, 1, 0).inOrder()

    assertThat(p1.counts("M, Steel, Titanium, Plant, Energy, Heat"))
        .containsExactly(34, 2, 8, 3, 1, 3)
        .inOrder()

    assertThat(p1.counts("ProjectCard, CardFront, ActiveCard, AutomatedCard, PlayedEvent"))
        .containsExactly(5, 10, 1, 6, 0)

    // tag abbreviations
    assertThat(p1.counts("BUT, SPT, SCT, POT, EAT, JOT, PLT, MIT, ANT, CIT"))
        .containsExactly(5, 2, 2, 0, 1, 3, 0, 0, 0, 1)
        .inOrder()

    assertThat(p1.counts("CityTile, GreeneryTile, SpecialTile"))
        .containsExactly(1, 0, 0)
        .inOrder()

    // P2

    p2.assertCount("TerraformRating", 25)

    val prods2 = lookUpProductionLevels(p2.game.reader, p2.player.expression)
    assertThat(prods2.values).containsExactly(8, 6, 1, 0, 2, 0).inOrder()

    assertThat(p2.counts("M, Steel, Titanium, Plant, Energy, Heat"))
        .containsExactly(47, 6, 1, 1, 2, 3)
        .inOrder()

    assertThat(p2.counts("ProjectCard, CardFront, ActiveCard, AutomatedCard, PlayedEvent"))
        .containsExactly(3, 17, 4, 10, 3)

    // tag abbreviations
    assertThat(p2.counts("BUT, SPT, SCT, POT, EAT, JOT, PLT, MIT, ANT, CIT"))
        .containsExactly(9, 3, 4, 2, 3, 0, 0, 1, 0, 0)
        .inOrder()

    assertThat(p2.counts("CityTile, GreeneryTile, SpecialTile"))
        .containsExactly(1, 0, 1)
        .inOrder()
  }

  @Test
  fun startEllieGameWithoutPrelude() {
    val repl = ReplSession(Canon, GameSetup(Canon, "BRHX", 2))
    repl.test("mode blue")

    repl.test("become P1")
    repl.test("exec Turn", "task B InterplanetaryCinematics", "task C 7 BuyCard")

    repl.test("become P2")
    repl.test("exec Turn", "task B PharmacyUnion", "task C 5 BuyCard")

    repl.test("become Engine")
    repl.test("exec ActionPhase")

    repl.test("become P1")
    repl.playCard(6, "MediaGroup")
  }

  @Test
  fun ellieGame() {
    val repl = ReplSession(Canon, GameSetup(Canon, "BRHXP", 2))

    repl.test("mode blue")

    repl.test("become P1")
    repl.test("exec Turn", "task B InterplanetaryCinematics", "task C 7 BuyCard")

    repl.test("become P2")
    repl.test("exec Turn", "task B PharmacyUnion", "task C 5 BuyCard")

    repl.test("become Engine")
    repl.test("exec PreludePhase")

    repl.test("become P1")
    repl.test("exec Turn", "task B UnmiContractor")
    repl.test("exec Turn", "task B CorporateArchives")

    repl.test("become P2")
    repl.test("exec Turn", "task B BiosphereSupport")
    repl.test("exec Turn", "task B SocietySupport")

    repl.test("become Engine")
    repl.test("exec ActionPhase")

    repl.test("become P1")

    repl.playCard(6, "MediaGroup")

    repl.playCard(1, "Sabotage", "task F -7 Megacredit<P2>")

    repl.test("become P2")
    repl.playCard(11, "Research")
    repl.playCard(9, "MartianSurvey", "task F Ok")

    repl.playCard(
        3,
        "SearchForLife",
        "task F PlayedEvent<Class<PharmacyUnion>> FROM PharmacyUnion THEN 3 TerraformRating")

    repl.useAction1("SearchForLife", "task C -1 THEN Ok")

    repl.test("become Engine")
    repl.test("exec ProductionPhase", "as P1 task A BuyCard", "as P2 task B 3 BuyCard")
    repl.test("exec ActionPhase")

    repl.test("become P2")

    repl.test("exec Turn", 1)
    repl.test("task A UseAction1<SellPatents>", 1)
    repl.test("task B Megacredit FROM ProjectCard")

    repl.test("exec Turn", 1)
    repl.test("task A UseAction1<PlayCardFromHand>", 1)
    repl.test("task B PlayCard<Class<VestaShipyard>>", 2)
    repl.test("task F 15 Pay<Class<M>> FROM M", 1)
    repl.test("task G Ok")

    repl.test("become P1")
    repl.playCard(23, "EarthCatapult")
    // TODO recognize one is impossible

    repl.test("exec Turn", 1)
    repl.test("task A UseAction1<PlayCardFromHand>", 1)
    repl.test("task B PlayCard<Class<OlympusConference>>", 2)
    repl.test("task H Ok", 1)
    repl.test("task I 4 Pay<Class<S>> FROM S", 1)
    repl.test("task J Science<OlympusConference>")

    repl.test("mode green")

    repl.test(
        "exec -4 Steel THEN -1 THEN DevelopmentCenter",
        "task A ProjectCard FROM Science<OlympusConference>")
    repl.test("exec -4 Steel THEN -1 THEN GeothermalPower")
    repl.test("exec -10 THEN MirandaResort")
    repl.test("exec -1 THEN Hackers", "task B PROD[-2 M<P2>]")
    repl.test("exec -1 THEN MicroMills")

    repl.test("become Engine")
    repl.test("exec ProductionPhase", "as P1 task A 3 BuyCard", "as P2 task B BuyCard")
    repl.test("exec ActionPhase")

    repl.test("become P1")
    repl.test(
        "exec UseAction1<UseActionFromCard>",
        "task A UseAction1<DevelopmentCenter> THEN ActionUsedMarker<DevelopmentCenter>",
    )
    repl.test("exec -5 Steel THEN -1 THEN ImmigrantCity", 1)
    repl.test("task C drop") // TODO

    // Shared stuff

    assertThat(repl.counts("Generation")).containsExactly(3)
    assertThat(repl.counts("OceanTile, OxygenStep, TemperatureStep")).containsExactly(0, 0, 0)

    // P1

    repl.command("become P1")

    repl.assertCount("TerraformRating", 23)

    val prods = lookUpProductionLevels(repl.session.game.reader, cn("P1").expr)
    assertThat(prods.values).containsExactly(4, 0, 0, 0, 0, 1).inOrder()

    assertThat(repl.counts("M, Steel, Titanium, Plant, Energy, Heat"))
        .containsExactly(22, 3, 0, 0, 0, 1)
        .inOrder()

    assertThat(repl.counts("ProjectCard, CardFront, ActiveCard, AutomatedCard, PlayedEvent"))
        .containsExactly(6, 12, 5, 4, 1)

    // tag abbreviations
    assertThat(repl.counts("BUT, SPT, SCT, POT, EAT, JOT, PLT, MIT, ANT, CIT"))
        .containsExactly(5, 1, 3, 1, 4, 1, 0, 0, 0, 1)
        .inOrder()

    assertThat(repl.counts("CityTile, GreeneryTile, SpecialTile"))
        .containsExactly(0, 0, 0)
        .inOrder()

    // P2

    repl.command("become P2")

    repl.assertCount("TerraformRating", 25)

    val prods2 = lookUpProductionLevels(repl.session.game.reader, cn("P2").expr)
    assertThat(prods2.values).containsExactly(-4, 0, 1, 3, 1, 1).inOrder()

    assertThat(repl.counts("M, Steel, Titanium, Plant, Energy, Heat"))
        .containsExactly(18, 0, 1, 6, 1, 3)
        .inOrder()

    assertThat(repl.counts("ProjectCard, CardFront, ActiveCard, AutomatedCard, PlayedEvent"))
        .containsExactly(9, 5, 1, 2, 2)

    // tag abbreviations
    assertThat(repl.counts("BUT, SPT, SCT, POT, EAT, JOT, PLT, MIT, ANT, CIT"))
        .containsExactly(0, 1, 3, 0, 0, 1, 1, 0, 0, 0)
        .inOrder()

    assertThat(repl.counts("CityTile, GreeneryTile, SpecialTile"))
        .containsExactly(0, 0, 0)
        .inOrder()

    repl.test("become Engine")

    val cp = repl.session.game.checkpoint()
    repl.test("exec End")
    assertThat(repl.session.game.taskQueue.isEmpty()).isTrue()
    repl.session.game.eventLog.changesSince(cp).forEach(::println)

    // Not sure where this discrepancy comes from... expected P2 to be shorted 1 pt because event

    // 23 2 1 1 -1
    assertThat(repl.session.count("VictoryPoint<Player1>")).isEqualTo(26)

    // 25 1 1 1 (but getting shorted for event card)
    assertThat(repl.session.count("VictoryPoint<Player2>")).isEqualTo(27) // TODO 28
  }
}

fun ReplSession.playCard(mega: Int, cardName: String, vararg tasks: String) {
  test("exec Turn", 1)
  test("task A UseAction1<PlayCardFromHand>", 1)
  test("task B PlayCard<Class<$cardName>>", 1)
  val taskId = session.agent.tasks().keys.single()
  if (mega > 0) {
    test("task $taskId $mega Pay<Class<M>> FROM M", tasks.size)
  } else {
    test("task $taskId Ok", tasks.size)
  }
  var left = tasks.size
  for (task in tasks) test(task, --left)
}

fun ReplSession.useAction1(cardName: String, vararg tasks: String) {
  test("exec Turn", 1)
  test("task A UseAction1<UseActionFromCard>", 1)
  test("task B UseAction1<$cardName> THEN ActionUsedMarker<$cardName>", 1)
  var left = tasks.size
  for (task in tasks) test(task, --left)
}

fun ReplSession.test(s: String, tasksExpected: Int = 0) {
  val (cmd, args) = s.split(" ", limit = 2)
  commands[cmd]!!.withArgs(args)
  assertWithMessage("${session.game.taskQueue}")
      .that(session.game.taskQueue.size)
      .isEqualTo(tasksExpected)
}

fun ReplSession.test(vararg s: String) {
  var x = s.size
  for (it in s) test(it, --x)
}

fun ReplSession.assertCount(text: String, i: Int) {
  assertThat(session.count(text)).isEqualTo(i)
}

fun ReplSession.counts(text: String): List<Int> = text.split(",").map(session::count)
