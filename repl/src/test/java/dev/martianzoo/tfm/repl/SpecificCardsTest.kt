package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.UserException.InvalidReificationException
import dev.martianzoo.tfm.api.UserException.RequirementException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.Exceptions.LimitsException
import dev.martianzoo.tfm.engine.Humanizing.counts
import dev.martianzoo.tfm.engine.Humanizing.playCard
import dev.martianzoo.tfm.engine.Humanizing.production
import dev.martianzoo.tfm.engine.Humanizing.stdProject
import dev.martianzoo.tfm.engine.Humanizing.turn
import dev.martianzoo.tfm.engine.Humanizing.useCardAction
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.repl.TestHelpers.assertCounts
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SpecificCardsTest {

  @Test
  fun localHeatTrapping() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    val p1 = game.asPlayer(PLAYER1).session()

    p1.execute("4 Heat, 3 ProjectCard, Pets")
    assertThat(p1.counts("Card, Heat, CardBack, CardFront, Animal, PlayedEvent"))
        .containsExactly(3, 4, 2, 1, 1, 0)
        .inOrder()

    val cp1 = p1.game.checkpoint()
    p1.execute("LocalHeatTrapping")
    assertThat(p1.counts("Card, Heat, CardBack, CardFront, Animal, PlayedEvent"))
        .containsExactly(3, 4, 1, 1, 1, 1)
        .inOrder()

    val tasks = p1.agent.tasks()
    assertThat(tasks.values.any { it.whyPending == "can't gain/remove 5 instances, only 4" })
    p1.game.rollBack(cp1)

    p1.execute("2 Heat")
    assertThat(p1.counts("Card, Heat, CardBack, CardFront, Animal, PlayedEvent"))
        .containsExactly(3, 6, 2, 1, 1, 0)
        .inOrder()

    val nextTask = p1.execute("LocalHeatTrapping").tasksSpawned.single()
    assertThat(p1.counts("Card, Heat, CardBack, CardFront, Animal, PlayedEvent"))
        .containsExactly(3, 1, 1, 1, 1, 1)
        .inOrder()

    val cp2 = p1.game.checkpoint()
    assertThrows<Exception>("2") { p1.doTask(nextTask, "2 Animal") }
    assertThat(p1.game.checkpoint()).isEqualTo(cp2)

    assertThrows<Exception>("3") { p1.doTask(nextTask, "2 Animal<Fish>") }
    assertThat(p1.game.checkpoint()).isEqualTo(cp2)
    assertThat(p1.count("Animal")).isEqualTo(1)

    p1.doTask(nextTask, "4 Plant")
    assertThat(p1.counts("Card, Heat, Plant, Animal")).containsExactly(3, 1, 4, 1).inOrder()
    p1.game.rollBack(cp2)

    p1.doTask(nextTask, "2 Animal<Pets>")
    assertThat(p1.counts("Card, Heat, Plant, Animal")).containsExactly(3, 1, 0, 3).inOrder()
    p1.game.rollBack(cp2)
  }

  @Test
  fun manutech() {
    val game = Engine.newGame(GameSetup(Canon, "BMV", 2))
    val p1 = game.asPlayer(PLAYER1).session()

    p1.execute("CorporationCard, Manutech")
    assertThat(p1.count("Production<Class<S>>")).isEqualTo(1)
    assertThat(p1.count("Steel")).isEqualTo(1)

    p1.execute("PROD[8, 6T, 7P, 5E, 3H]")

    // Being very lazy and depending on declaration order!
    assertThat(p1.production().values).containsExactly(8, 1, 6, 7, 5, 3).inOrder()
    assertThat(p1.counts("M, S, T, P, E, H")).containsExactly(43, 1, 6, 7, 5, 3)

    p1.execute("-7 Plant")
    p1.execute("ProjectCard")
    p1.execute("Moss")
    assertThat(p1.game.tasks.isEmpty()).isTrue()

    assertThat(p1.production().values).containsExactly(8, 1, 6, 8, 5, 3).inOrder()
    assertThat(p1.counts("M, S, T, P, E, H")).containsExactly(43, 1, 6, 0, 5, 3)
  }

  @Test
  fun sulphurEatingBacteria() {
    val game = Engine.newGame(GameSetup(Canon, "BMV", 2))
    val p1 = game.asPlayer(PLAYER1).session()

    p1.execute("ProjectCard THEN SulphurEatingBacteria")
    p1.execute("UseAction1<SulphurEatingBacteria>")
    assertThat(p1.count("Microbe")).isEqualTo(1)

    p1.execute("UseAction2<SulphurEatingBacteria>", "-Microbe<SulphurEatingBacteria> THEN 3")
    assertThat(p1.count("Microbe")).isEqualTo(0)

    p1.execute("4 UseAction1<SulphurEatingBacteria>")
    assertThat(p1.count("Microbe")).isEqualTo(4)

    p1.execute("UseAction2<SulphurEatingBacteria>")

    fun assertTaskFails(task: String, desc: String) =
        assertThrows<Exception>(desc) { p1.doTask("A", task) }

    // Make sure these task attempts *don't* work

    assertTaskFails("-Microbe<C251> THEN 4", "greed")
    assertTaskFails("-Microbe<C251> THEN 2", "shortchanged")
    assertTaskFails("-Microbe<C251>", "no get paid")
    assertTaskFails("-3 Microbe THEN 9", "which microbe")
    assertTaskFails("-5 Microbe<C251> THEN 15", "more than have")
    assertTaskFails("-0 Microbe<C251> THEN 0", "x can't be zero")
    assertTaskFails("-3 Resource<C251> THEN 9", "what kind")
    assertTaskFails("9 THEN -3 Microbe<C251>", "out of order")
    assertTaskFails("2 Microbe<C251> THEN -6", "inverse")

    assertThat(p1.count("Microbe")).isEqualTo(4)

    p1.doTask("-3 Microbe<C251> THEN 9")
    assertThat(p1.count("Microbe")).isEqualTo(1)
  }

  @Test
  fun unmi() {
    val game = Engine.newGame(GameSetup(Canon, "BM", 2))
    val eng = game.asPlayer(ENGINE).session()
    val p1 = game.asPlayer(PLAYER1).session()

    p1.turn("UnitedNationsMarsInitiative", "5 BuyCard")
    p1.assertCounts(25 to "Megacredit", 20 to "TR")

    eng.execute("ActionPhase")

    val cp = game.checkpoint()
    p1.useCardAction(1, "UnitedNationsMarsInitiative")

    // Can't use UNMI action yet - fail, don't no-op, per https://boardgamegeek.com/thread/2525032
    assertThrows<RequirementException> { p1.doTask("-3 THEN HasRaisedTr: TR!") }
    game.rollBack(cp)

    // Do anything that raises TR
    p1.stdProject("AsteroidSP")
    p1.assertCounts(11 to "Megacredit", 21 to "TR")

    // Now we can use UNMI
    p1.useCardAction(1, "UnitedNationsMarsInitiative")
    p1.assertCounts(8 to "Megacredit", 22 to "TR")

    // Can't use it twice tho
    assertThrows<LimitsException> { p1.useCardAction(1, "UnitedNationsMarsInitiative") }
    p1.assertCounts(8 to "Megacredit", 22 to "TR")
  }

  @Test
  fun unmiOutOfOrder() {
    val game = Engine.newGame(GameSetup(Canon, "BM", 2))
    val eng = game.asPlayer(ENGINE).session()
    val p1 = eng.asPlayer(PLAYER1)

    p1.assertCounts(0 to "Megacredit", 20 to "TR")

    // Do anything that raises TR, even before UNMI is played
    p1.execute("TemperatureStep")
    p1.assertCounts(0 to "Megacredit", 21 to "TR")

    p1.turn("UnitedNationsMarsInitiative", "5 BuyCard")
    p1.assertCounts(25 to "Megacredit", 21 to "TR")

    eng.execute("ActionPhase")
    p1.useCardAction(1, "UnitedNationsMarsInitiative")
    p1.assertCounts(22 to "Megacredit", 22 to "TR")
  }

  @Test
  fun aiCentral() {
    val game = Engine.newGame(GameSetup(Canon, "BRM", 2))
    val eng = game.asPlayer(ENGINE).session()
    val p1 = eng.asPlayer(PLAYER1)

    eng.execute("ActionPhase")
    p1.execute("5 ProjectCard, 100, Steel")

    p1.playCard(3, "SearchForLife")
    p1.playCard(9, "InventorsGuild")

    var cp = game.checkpoint()
    p1.turn("UseAction1<PlayCardFromHand>")
    assertThrows<RequirementException> { p1.doTask("PlayCard<Class<AiCentral>>") }
    game.rollBack(cp)

    p1.playCard(16, "DesignedMicroorganisms")

    // Now I do have the 3 science tags, but not the energy production
    cp = game.checkpoint()
    p1.playCard(19, "AiCentral", "Pay<Class<S>> FROM S")
    assertThrows<LimitsException> { p1.doTask("PROD[-Energy]") }
    game.rollBack(cp)

    // Give energy prod and try again - success
    p1.execute("PROD[E]")
    p1.playCard(19, "AiCentral", "Pay<Class<S>> FROM S")
    p1.assertCounts(0 to "Production<Class<Energy>>")

    // Use the action
    p1.assertCounts(1 to "ProjectCard")
    p1.useCardAction(1, "AiCentral")
    p1.assertCounts(3 to "ProjectCard")

    // Can't use it again
    cp = game.checkpoint()
    assertThrows<LimitsException> { p1.useCardAction(1, "AiCentral") }
    game.rollBack(cp)

    // Next gen we can again
    eng.execute("Generation")
    p1.useCardAction(1, "AiCentral")
    p1.assertCounts(5 to "ProjectCard")
  }

  @Test
  fun ceosFavoriteProject() {
    val game = Engine.newGame(GameSetup(Canon, "CVERB", 2))
    val p1 = game.asPlayer(PLAYER1).session()

    p1.execute("10 ProjectCard, ForcedPrecipitation")

    // We can't CEO's onto an empty card
    p1.execute("CeosFavoriteProject")
    assertThrows<InvalidReificationException> { p1.doTask("Floater<ForcedPrecipitation>") }
    p1.assertCounts(0 to "Floater")

    // But if we *manually* add a floater first...
    p1.execute("Floater<ForcedPrecipitation>")
    p1.assertCounts(1 to "Floater")

    // Now we can complete that task from before
    p1.doTask("Floater<ForcedPrecipitation>")
    p1.assertCounts(2 to "Floater")
  }

  @Test
  fun airScrappingExpedition() {
    val game = Engine.newGame(GameSetup(Canon, "CVERB", 2))
    val p1 = game.asPlayer(PLAYER1).session()

    p1.execute("10 ProjectCard, ForcedPrecipitation")
    p1.execute("AtmoCollectors", "2 Floater<AtmoCollectors>")
    p1.assertCounts(2 to "Floater")

    p1.execute("AirScrappingExpedition")

    // We can't put air-scrap floaters onto a non-Venus card
    assertThrows<InvalidReificationException> { p1.doTask("3 Floater<AtmoCollectors>") }
    p1.assertCounts(2 to "Floater")

    // But we can on a Venus card
    p1.execute("3 Floater<ForcedPrecipitation>")
    p1.assertCounts(5 to "Floater")
  }

  @Test
  fun communityServices() {
    val game = Engine.newGame(GameSetup(Canon, "CVERB", 2))
    val p1 = game.asPlayer(PLAYER1).session()

    p1.execute("10 ProjectCard, ForcedPrecipitation")
    p1.execute("AtmoCollectors", "2 Floater<AtmoCollectors>")
    p1.execute("Airliners", "2 Floater<AtmoCollectors>")
    assertThat(p1.production()[cn("Megacredit")]).isEqualTo(2)

    p1.execute("CommunityServices") // should be 3 tagless cards (Atmo, Airl, Comm)
    assertThat(p1.production()[cn("Megacredit")]).isEqualTo(5)
  }

  @Test
  fun elCheapo() {
    val game = Engine.newGame(GameSetup(Canon, "BRMVPCX", 2))
    val eng = game.asPlayer(ENGINE).session()
    val p1 = game.asPlayer(PLAYER1).session()

    eng.execute("ActionPhase")

    p1.execute("CorporationCard, 12 ProjectCard, Phobolog, Steel") // -1

    p1.execute("AntiGravityTechnology") // -5
    p1.execute("EarthCatapult")
    p1.execute("ResearchOutpost", "CityTile<M33>")

    p1.execute("MassConverter") // -12
    p1.execute("QuantumExtractor")
    p1.execute("Shuttles")
    p1.execute("SpaceStation")
    p1.execute("WarpDrive")

    p1.execute("AdvancedAlloys") // -4
    p1.execute("MercurianAlloys")
    p1.execute("RegoPlastics")

    p1.playCard(0, "SpaceElevator")
    p1.doTask("Pay<Class<T>> FROM T") // -3
    p1.doTask("Pay<Class<S>> FROM S") // -2

    assertThat(p1.has("SpaceElevator")).isTrue()
    assertThat(p1.count("M")).isEqualTo(23)
  }

  @Test
  fun doubleDown() {
    val game = Engine.newGame(GameSetup(Canon, "BRHXP", 2))
    val eng = game.asPlayer(ENGINE).session()
    val p1 = eng.asPlayer(PLAYER1)
    val p2 = eng.asPlayer(Player.PLAYER2)

    p1.turn("InterplanetaryCinematics", "7 BuyCard")
    p2.turn("PharmacyUnion", "5 BuyCard")

    eng.execute("PreludePhase")

    p1.turn("UnmiContractor")
    p1.turn("CorporateArchives")

    with(p2) {
      turn("BiosphereSupport")
      assertThat(production().values).containsExactly(-1, 0, 0, 2, 0, 0).inOrder()

      turn("DoubleDown")
      assertThrows<Exception>("1") { doTask("@copyPrelude(MartianIndustries)") }
      assertThrows<Exception>("2") { doTask("@copyPrelude(UnmiContractor)") }
      assertThrows<Exception>("3") { doTask("@copyPrelude(PharmacyUnion)") }
      assertThrows<Exception>("4") { doTask("@copyPrelude(DoubleDown)") }

      doTask("@copyPrelude(BiosphereSupport)")
      assertThat(production().values).containsExactly(-2, 0, 0, 4, 0, 0).inOrder()
    }
  }
}
