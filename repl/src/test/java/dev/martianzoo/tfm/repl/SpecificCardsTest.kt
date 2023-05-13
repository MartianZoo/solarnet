package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.Exceptions.AbstractException
import dev.martianzoo.tfm.api.Exceptions.DependencyException
import dev.martianzoo.tfm.api.Exceptions.LimitsException
import dev.martianzoo.tfm.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.api.Exceptions.RequirementException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Game
import dev.martianzoo.tfm.engine.Humanize.cardAction
import dev.martianzoo.tfm.engine.Humanize.playCard
import dev.martianzoo.tfm.engine.Humanize.playCorp
import dev.martianzoo.tfm.engine.Humanize.production
import dev.martianzoo.tfm.engine.Humanize.startTurn
import dev.martianzoo.tfm.engine.Humanize.stdAction
import dev.martianzoo.tfm.engine.Humanize.useCardAction
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.repl.TestHelpers.assertCounts
import dev.martianzoo.tfm.repl.TestHelpers.taskReasons
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SpecificCardsTest {
  @Test
  fun localHeatTrapping_plants() {
    val game = Game.create(Canon.SIMPLE_GAME)
    val p1 = game.session(PLAYER1)

    // Set up
    p1.action("4 Heat, 3 ProjectCard, Pets") {
      assertCounts(0 to "Plant", 4 to "Heat", 1 to "Animal")
      assertCounts(3 to "Card", 2 to "CardBack", 1 to "CardFront", 0 to "PlayedEvent")
    }

    p1.action("LocalHeatTrapping") {
      // The card is played but nothing else
      assertCounts(3 to "Card", 1 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
      assertCounts(0 to "Plant", 4 to "Heat", 1 to "Animal")

      // And for the expected reasons
      assertThat(taskReasons())
          .containsExactly(
              "When gaining null and removing [Heat<Player1>]: can do only 4 of 5 required",
              "choice required in: `4 Plant<Player1>! OR 2 Animal<Player1>.`",
          )

      rollItBack()
    }

    // NOW we have enough heat
    p1.action("2 Heat") {
      assertCounts(3 to "Card", 2 to "CardBack", 1 to "CardFront", 0 to "PlayedEvent")
      assertCounts(0 to "Plant", 6 to "Heat", 1 to "Animal")
    }

    p1.action("LocalHeatTrapping") {
      // The heat is gone
      assertCounts(3 to "Card", 1 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
      assertCounts(0 to "Plant", 1 to "Heat", 1 to "Animal")

      assertThat(taskReasons())
          .containsExactly("choice required in: `4 Plant<Player1>! OR 2 Animal<Player1>.`")

      doFirstTask("4 Plant")
      assertCounts(3 to "Card", 1 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
      assertCounts(4 to "Plant", 1 to "Heat", 1 to "Animal")
    }
  }

  @Test
  fun localHeatTrapping_pets() {
    val game = Game.create(Canon.SIMPLE_GAME)
    val p1 = game.session(PLAYER1)

    // Set up
    p1.action("6 Heat, 3 ProjectCard, Pets") {
      assertCounts(3 to "Card", 2 to "CardBack", 1 to "CardFront", 0 to "PlayedEvent")
      assertCounts(0 to "Plant", 6 to "Heat", 1 to "Animal")
    }

    p1.action("LocalHeatTrapping") {
      // The card is played and the heat is gone
      assertCounts(3 to "Card", 1 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
      assertCounts(0 to "Plant", 1 to "Heat", 1 to "Animal")

      assertThrows<AbstractException>("1") { doFirstTask("2 Animal") }
      // no effect, but the task has been prepared if it wasn't already....
      assertCounts(3 to "Card", 1 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
      assertCounts(0 to "Plant", 1 to "Heat", 1 to "Animal")

      // card I don't have
      assertThrows<DependencyException>("2") { doFirstTask("2 Animal<Fish>") }

      // but this should work
      doFirstTask("2 Animal<Pets>")
      assertCounts(3 to "Card", 1 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
      assertCounts(0 to "Plant", 1 to "Heat", 3 to "Animal")
    }
  }

  @Test
  fun manutech() {
    val game = Game.create(GameSetup(Canon, "BMV", 2))
    val p1 = game.session(PLAYER1)

    p1.action("CorporationCard, Manutech, 5 ProjectCard") {
      assertCounts(1 to "Production<Class<S>>", 1 to "Steel")
    }

    p1.action("PROD[8, 6T, 7P, 5E, 3H]") {
      assertThat(p1.production().values).containsExactly(8, 1, 6, 7, 5, 3).inOrder()
      assertCounts(43 to "M", 1 to "S", 6 to "T", 7 to "P", 5 to "E", 3 to "H")
    }

    p1.action("-7 Plant") { assertCounts(0 to "Plant") }

    p1.action("Moss") {
      assertThat(p1.production().values).containsExactly(8, 1, 6, 8, 5, 3).inOrder()
      assertCounts(43 to "M", 1 to "S", 6 to "T", 0 to "P", 5 to "E", 3 to "H")
    }
  }

  @Test
  fun sulphurEatingBacteria() {
    val game = Game.create(GameSetup(Canon, "BMV", 2))
    val p1 = game.session(PLAYER1)

    p1.action("5 ProjectCard, SulphurEatingBacteria") {
      assertCounts(0 to "Microbe", 0 to "Megacredit")
    }

    p1.action("UseAction1<SulphurEatingBacteria>") {
      assertCounts(1 to "Microbe", 0 to "Megacredit")
    }

    p1.action("UseAction2<SulphurEatingBacteria>") {
      doFirstTask("-Microbe<SulphurEatingBacteria> THEN 3")
      assertCounts(0 to "Microbe", 3 to "Megacredit")
    }

    p1.action("4 Microbe<SulphurEatingBacteria>") {
      assertCounts(4 to "Microbe", 3 to "Megacredit")
    }

    fun assertTaskFails(task: String, desc: String) =
        assertThrows<Exception>(desc) { p1.doFirstTask(task) }

    p1.action("UseAction2<C251>") {
      assertTaskFails("-Microbe<C251> THEN 4", "greed")
      assertTaskFails("-Microbe<C251> THEN 2", "shortchanged")
      assertTaskFails("-Microbe<C251>", "no get paid")
      assertTaskFails("-3 Microbe THEN 9", "which microbe")
      assertTaskFails("-5 Microbe<C251> THEN 15", "more than have")
      assertTaskFails("-0 Microbe<C251> THEN 0", "x can't be zero")
      assertTaskFails("-3 Resource<C251> THEN 9", "what kind")
      assertTaskFails("9 THEN -3 Microbe<C251>", "out of order")
      assertTaskFails("2 Microbe<C251> THEN -6", "inverse")

      assertCounts(4 to "Microbe", 3 to "Megacredit")

      p1.doFirstTask("-3 Microbe<C251> THEN 9")
      assertCounts(1 to "Microbe", 12 to "Megacredit")
    }
  }

  @Test
  fun unmi() {
    val game = Game.create(GameSetup(Canon, "BM", 2))
    val p1 = game.session(PLAYER1)

    p1.action("CorporationCard, UnitedNationsMarsInitiative") {
      assertCounts(40 to "Megacredit", 20 to "TR")
    }

    p1.action("UseAction1<UnitedNationsMarsInitiative>") {
      assertCounts(37 to "Megacredit", 20 to "TR")

      assertThat(taskReasons()).containsExactly("requirement not met: `HasRaisedTr<Player1>`")

      // Can't use UNMI action yet - fail, don't no-op, per https://boardgamegeek.com/thread/2525032
      assertThrows<RequirementException> { doFirstTask("TR") } // TODO or just prepare it?
      rollItBack()
    }

    // Do anything that raises TR
    p1.action("UseAction1<AsteroidSP>") { assertCounts(26 to "Megacredit", 21 to "TR") }

    p1.action("UseAction1<UnitedNationsMarsInitiative>") {
      assertCounts(23 to "Megacredit", 22 to "TR")
    }
  }

  @Test
  fun pristar() {
    val game = Game.create(GameSetup(Canon, "BMPT", 2))
    val eng = game.session(ENGINE)
    val p1 = game.session(PLAYER1)

    p1.assertCounts(0 to "Megacredit", 20 to "TR")

    p1.action("CorporationCard, Pristar") { assertCounts(53 to "Megacredit", 18 to "TR") }

    eng.action("PreludePhase")
    p1.action("PreludeCard, UnmiContractor") { assertCounts(53 to "Megacredit", 21 to "TR") }

    eng.action("ActionPhase")
    eng.action("ProductionPhase")
    p1.assertCounts(74 to "Megacredit", 21 to "TR", 0 to "Preservation")

    eng.action("ResearchPhase") {
      tryMatchingTask("2 BuyCard<Player1>")
      tryMatchingTask("2 BuyCard<Player2>")
    }
    p1.assertCounts(68 to "Megacredit", 21 to "TR", 0 to "Preservation")

    eng.action("ActionPhase")
    eng.action("ProductionPhase")
    p1.assertCounts(95 to "Megacredit", 21 to "TR", 1 to "Preservation")
  }

  @Test
  fun unmiOutOfOrder() {
    val game = Game.create(GameSetup(Canon, "BM", 2))
    val p1 = game.session(PLAYER1)

    p1.action("14")

    // Do anything that raises TR
    p1.action("UseAction1<AsteroidSP>") { assertCounts(0 to "Megacredit", 21 to "TR") }

    p1.action("CorporationCard, UnitedNationsMarsInitiative") {
      assertCounts(40 to "Megacredit", 21 to "TR")
    }

    p1.action("UseAction1<UnitedNationsMarsInitiative>") {
      assertCounts(37 to "Megacredit", 22 to "TR")
    }
  }

  @Test
  fun aiCentral() {
    val game = Game.create(GameSetup(Canon, "BRM", 2))
    val eng = game.session(ENGINE)
    val p1 = game.session(PLAYER1)

    eng.action("ActionPhase")
    p1.writer.unsafe().sneak("5 ProjectCard, 100, Steel")

    p1.playCard("SearchForLife", 3)
    p1.playCard("InventorsGuild", 9)

    p1.stdAction("PlayCardFromHand") {
      assertThrows<RequirementException>("1") { doFirstTask("PlayCard<Class<AiCentral>>") }
      rollItBack()
    }

    p1.playCard("DesignedMicroorganisms", 16)

    // Now I do have the 3 science tags, but not the energy production
    p1.playCard("AiCentral", 19, steel = 1) {
      assertThrows<LimitsException>("2") { doFirstTask("PROD[-Energy]") }
      rollItBack()
    }

    // Give energy prod and try again - success
    p1.writer.unsafe().sneak("PROD[Energy]")
    p1.playCard("AiCentral", 19, steel = 1) {
      assertCounts(0 to "PROD[Energy]")
    }

    // Use the action
    p1.assertCounts(1 to "ProjectCard")
    p1.cardAction("AiCentral") {
      assertCounts(3 to "ProjectCard")
      assertCounts(0 to "ActionUsedMarker<AiCentral>") // not yet!
    }
    p1.assertCounts(1 to "ActionUsedMarker<AiCentral>")

    // Can't use it again TODO reenable
    assertThrows<LimitsException>("3") { p1.cardAction("AiCentral") }
    p1.assertCounts(3 to "ProjectCard")
    p1.assertCounts(1 to "ActionUsedMarker<AiCentral>")
    assertThat(p1.tasks).isEmpty()

    // Next gen we can again
    eng.action("Generation")
    p1.useCardAction(1, "AiCentral")
    p1.assertCounts(5 to "ProjectCard")
  }

  @Test
  fun ceosFavoriteProject() {
    val game = Game.create(GameSetup(Canon, "CVERB", 2))
    val p1 = game.session(PLAYER1)

    p1.action("10 ProjectCard, ForcedPrecipitation")

    // We can't CEO's onto an empty card
    p1.action("CeosFavoriteProject") {
      assertThrows<NarrowingException> { doFirstTask("Floater<ForcedPrecipitation>") }
      rollItBack()
    }

    // But if we *manually* add a floater first...
    p1.action("Floater<ForcedPrecipitation>") { assertCounts(1 to "Floater") }

    p1.action("CeosFavoriteProject") {
      doFirstTask("Floater<ForcedPrecipitation>")
      assertCounts(2 to "Floater")
    }
  }

  // @Test TODO
  fun airScrappingExpedition() {
    val game = Game.create(GameSetup(Canon, "CVERB", 2))
    val p1 = game.session(PLAYER1)

    p1.action("10 ProjectCard, ForcedPrecipitation, AtmoCollectors") {
      doFirstTask("2 Floater<AtmoCollectors>")
      assertCounts(2 to "Floater")
    }

    p1.action("AirScrappingExpedition") {
      assertThrows<NarrowingException>("1") { doFirstTask("3 Floater<AtmoCollectors>") }
      assertCounts(2 to "Floater")
      rollItBack()
    }

    p1.action("AirScrappingExpedition") {
      tryMatchingTask("3 Floater<ForcedPrecipitation>")
      assertCounts(5 to "Floater")
    }
  }

  @Test
  fun communityServices() {
    val game = Game.create(GameSetup(Canon, "CVERB", 2))
    val p1 = game.session(PLAYER1)

    p1.action("10 ProjectCard, ForcedPrecipitation")
    p1.execute("AtmoCollectors", "2 Floater<AtmoCollectors>")
    p1.execute("Airliners", "2 Floater<AtmoCollectors>")
    assertThat(p1.production()[cn("Megacredit")]).isEqualTo(2)

    p1.action("CommunityServices") // should be 3 tagless cards (Atmo, Airl, Comm)
    assertThat(p1.production()[cn("Megacredit")]).isEqualTo(5)
  }

  @Test
  fun elCheapo() {
    val game = Game.create(GameSetup(Canon, "BRMVPCX", 2))
    val eng = game.session(ENGINE)
    val p1 = game.session(PLAYER1)

    eng.action("ActionPhase")

    p1.action("CorporationCard, 12 ProjectCard, Phobolog, Steel") // -1

    p1.action("AntiGravityTechnology") // -5
    p1.action("EarthCatapult")
    p1.execute("ResearchOutpost", "CityTile<M33>")

    p1.action("MassConverter") // -12
    p1.action("QuantumExtractor")
    p1.action("Shuttles")
    p1.action("SpaceStation")
    p1.action("WarpDrive")

    p1.action("AdvancedAlloys") // -4
    p1.action("MercurianAlloys")
    p1.action("RegoPlastics")

    /*
       3996: as Engine exec NewTurn<P1>
       3997: task B UseAction1<PlayCardFromHand>
       3998: task C PlayCard<Class<SpaceElevator>>
       3999: task J Ok
       4000: task K Pay<Class<T>> FROM T
       4001: task L Pay<Class<S>> FROM S
    */
    p1.playCard("SpaceElevator", 0, steel = 1, titanium = 1)
    assertThat(p1.has("SpaceElevator")).isTrue()
    assertThat(p1.count("M")).isEqualTo(23)
  }

  @Test
  fun doubleDown() {
    val game = Game.create(GameSetup(Canon, "BRHXP", 2))
    val eng = game.session(ENGINE)
    val p1 = game.session(PLAYER1)
    val p2 = game.session(PLAYER2)

    p1.startTurn("InterplanetaryCinematics", "7 BuyCard")
    p2.startTurn("PharmacyUnion", "5 BuyCard")

    eng.action("PreludePhase")

    p1.startTurn("UnmiContractor")
    p1.startTurn("CorporateArchives")

    with(p2) {
      startTurn("BiosphereSupport")
      assertThat(production().values).containsExactly(-1, 0, 0, 2, 0, 0).inOrder()

      startTurn("DoubleDown")
      assertThrows<Exception>("1") { doFirstTask("@copyPrelude(MartianIndustries)") }
      assertThrows<Exception>("2") { doFirstTask("@copyPrelude(UnmiContractor)") }
      assertThrows<Exception>("3") { doFirstTask("@copyPrelude(PharmacyUnion)") }
      assertThrows<Exception>("4") { doFirstTask("@copyPrelude(DoubleDown)") }

      doFirstTask("@copyPrelude(BiosphereSupport)")
      assertThat(production().values).containsExactly(-2, 0, 0, 4, 0, 0).inOrder()
    }
  }

  @Test
  fun optimalAerobraking() {
    val game = Game.create(GameSetup(Canon, "BRHXP", 2))
    val eng = game.session(ENGINE)
    val p1 = game.session(PLAYER1)

    p1.action("5 ProjectCard, OptimalAerobraking") { assertCounts(0 to "Megacredit", 0 to "Heat") }

    p1.action("AsteroidCard") {
      doFirstTask("Ok") // can it infer this? TODO
      assertCounts(3 to "Megacredit", 3 to "Heat")
    }
  }

  @Test
  fun excentricSponsor() {
    val game = Game.create(GameSetup(Canon, "BRHXP", 2))
    val eng = game.session(ENGINE)
    val p1 = game.session(PLAYER1)

    p1.action("NewTurn") {
      doFirstTask("InterplanetaryCinematics")
      doFirstTask("7 BuyCard")
    }

    eng.action("PreludePhase")

    p1.action("NewTurn") {
      doFirstTask("ExcentricSponsor") // currently we don't `PlayCard` these
      doFirstTask("PlayCard<Class<NitrogenRichAsteroid>>")
      doFirstTask("6 Pay<Class<M>> FROM M")
      doFirstTask("Ok") // the damn titanium
      assertCounts(0 to "Owed", 5 to "M", 1 to "ExcentricSponsor", 1 to "PlayedEvent")
    }
  }

  @Test
  fun terribleLabs() {
    val game = Game.create(GameSetup(Canon, "BMT", 2))
    val p1 = game.session(PLAYER1)

    p1.action("NewTurn") {
      doFirstTask("TerralabsResearch")
      doFirstTask("10 BuyCard")
      assertCounts(10 to "ProjectCard", 4 to "M")
    }

    p1.action("4 BuyCard")
    p1.assertCounts(14 to "ProjectCard", 0 to "M")
  }

  @Test
  fun polyphemos() {
    val game = Game.create(GameSetup(Canon, "BMRC", 2))
    val eng = game.session(ENGINE)
    val p1 = game.session(PLAYER1)

    p1.playCorp("Polyphemos", 10)
    p1.assertCounts(10 to "ProjectCard", 0 to "M")

    eng.action("ActionPhase")
    p1.writer.unsafe().sneak("14")

    p1.playCard("InventorsGuild", 9)
    p1.assertCounts(9 to "ProjectCard", 5 to "M")

    p1.cardAction("InventorsGuild") {
      doFirstTask("BuyCard")
      assertCounts(10 to "ProjectCard", 0 to "M")
    }
  }
}
