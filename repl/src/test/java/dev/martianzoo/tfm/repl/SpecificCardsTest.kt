package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.Exceptions.AbstractException
import dev.martianzoo.tfm.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.api.Exceptions.DependencyException
import dev.martianzoo.tfm.api.Exceptions.LimitsException
import dev.martianzoo.tfm.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.api.Exceptions.PetSyntaxException
import dev.martianzoo.tfm.api.Exceptions.RequirementException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Game
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.engine.TerraformingMars.cardAction1
import dev.martianzoo.tfm.engine.TerraformingMars.cardAction2
import dev.martianzoo.tfm.engine.TerraformingMars.pass
import dev.martianzoo.tfm.engine.TerraformingMars.playCard
import dev.martianzoo.tfm.engine.TerraformingMars.playCorp
import dev.martianzoo.tfm.engine.TerraformingMars.production
import dev.martianzoo.tfm.engine.TerraformingMars.sellPatents
import dev.martianzoo.tfm.engine.TerraformingMars.stdAction
import dev.martianzoo.tfm.engine.TerraformingMars.stdProject
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.repl.TestHelpers.assertCounts
import dev.martianzoo.tfm.repl.TestHelpers.assertProductions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SpecificCardsTest {
  @Test
  fun localHeatTrapping_notEnoughHeat() {
    val game = Game.create(Canon.SIMPLE_GAME)

    with(game.session(PLAYER1)) {
      operation("4 Heat, 2 ProjectCard, Pets")
      assertCounts(0 to "Plant", 4 to "Heat", 1 to "Animal")
      assertCounts(3 to "Card", 2 to "CardBack", 1 to "CardFront", 0 to "PlayedEvent")

      operation("LocalHeatTrapping") {
        // The card is played but nothing else
        assertCounts(2 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
        assertCounts(0 to "Plant", 4 to "Heat", 1 to "Animal")

        // And for the expected reasons
        assertThat(tasks.map { it.whyPending })
            .containsExactly(
                // TODO "When gaining null and removing Heat<Player1>: can do only 4 of 5 required",
                null,
                "choice required in: `4 Plant<Player1>! OR 2 Animal<Player1>.`",
            )

        rollItBack()
      }
    }
  }

  @Test
  fun localHeatTrapping_plants() {
    val game = Game.create(Canon.SIMPLE_GAME)

    with(game.session(PLAYER1)) {
      operation("6 Heat, 2 ProjectCard, Pets")

      operation("LocalHeatTrapping") {
        // The card is played and the heat is gone
        assertCounts(1 to "CardFront", 1 to "PlayedEvent")
        assertCounts(0 to "Plant", 1 to "Heat", 1 to "Animal")

        assertThat(tasks.single().whyPending)
            .isEqualTo("choice required in: `4 Plant<Player1>! OR 2 Animal<Player1>.`")

        task("4 Plant")
      }

      assertCounts(2 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
      assertCounts(4 to "Plant", 1 to "Heat", 1 to "Animal")
    }
  }

  @Test
  fun localHeatTrapping_pets() {
    val game = Game.create(Canon.SIMPLE_GAME)
    with(game.session(PLAYER1)) {
      operation("6 Heat, 2 ProjectCard, Pets")

      operation("LocalHeatTrapping") {
        // The card is played and the heat is gone
        assertCounts(2 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
        assertCounts(0 to "Plant", 1 to "Heat", 1 to "Animal")

        assertThrows<AbstractException>("1") { task("2 Animal") }
        assertCounts(2 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
        assertCounts(0 to "Plant", 1 to "Heat", 1 to "Animal")

        // card I don't have
        assertThrows<DependencyException>("2") { task("2 Animal<Fish>") }

        // but this should work
        task("2 Animal<Pets>")
      }
      assertCounts(2 to "CardBack", 1 to "CardFront", 1 to "PlayedEvent")
      assertCounts(0 to "Plant", 1 to "Heat", 3 to "Animal")
    }
  }

  @Test
  fun manutech() {
    val game = Game.create(GameSetup(Canon, "BMV", 2))
    with(game.session(PLAYER1)) {
      playCorp("Manutech", 5)
      assertCounts(1 to "PROD[Steel]", 1 to "Steel")

      operation("PROD[8, 6T, 7P, 5E, 3H]")
      assertThat(production().values).containsExactly(8, 1, 6, 7, 5, 3).inOrder()
      assertCounts(28 to "M", 1 to "S", 6 to "T", 7 to "P", 5 to "E", 3 to "H")

      operation("-7 Plant")
      assertCounts(0 to "Plant")

      operation("Moss")
      assertThat(production().values).containsExactly(8, 1, 6, 8, 5, 3).inOrder()
      assertCounts(28 to "M", 1 to "S", 6 to "T", 0 to "P", 5 to "E", 3 to "H")
    }
  }

  @Test
  fun sulphurEatingBacteria() {
    val game = Game.create(GameSetup(Canon, "BMV", 2))
    with(game.session(PLAYER1)) {
      phase("Action")

      operation("5 ProjectCard, SulphurEatingBacteria")
      assertCounts(0 to "Microbe", 0 to "Megacredit")

      operation("UseAction1<SulphurEatingBacteria>")
      assertCounts(1 to "Microbe", 0 to "Megacredit")

      operation("UseAction2<SulphurEatingBacteria>", "-Microbe<SulphurEatingBacteria> THEN 3")
      assertCounts(0 to "Microbe", 3 to "Megacredit")

      operation("4 Microbe<SulphurEatingBacteria>")
      assertCounts(4 to "Microbe", 3 to "Megacredit")

      fun assertTaskFails(task: String, desc: String) = assertThrows<Exception>(desc) { task(task) }

      cardAction2("C251") {
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

        task("-3 Microbe<C251> THEN 9")
        assertCounts(1 to "Microbe", 12 to "Megacredit")
      }
    }
  }

  @Test
  fun unmi() {
    val game = Game.create(GameSetup(Canon, "BM", 2))
    with(game.session(PLAYER1)) {
      operation("CorporationCard, UnitedNationsMarsInitiative")
      assertCounts(40 to "Megacredit", 20 to "TR")

      phase("Action")

      assertThrows<RequirementException> { cardAction1("UnitedNationsMarsInitiative") }

      // Do anything that raises TR
      stdProject("AsteroidSP")
      assertCounts(26 to "Megacredit", 21 to "TR")

      cardAction1("UnitedNationsMarsInitiative")
      assertCounts(23 to "Megacredit", 22 to "TR")
    }
  }

  @Test
  fun pristar() {
    val game = Game.create(GameSetup(Canon, "BMPT", 2))
    val eng = game.session(ENGINE)
    val p1 = game.session(PLAYER1)
    val p2 = game.session(PLAYER2)

    p1.assertCounts(0 to "Megacredit", 20 to "TR")

    p1.playCorp("Pristar")
    p1.assertCounts(53 to "Megacredit", 18 to "TR")

    eng.phase("Prelude")
    p1.turn("UnmiContractor")
    p1.assertCounts(53 to "Megacredit", 21 to "TR")

    eng.phase("Action")
    eng.phase("Production")
    p1.assertCounts(74 to "Megacredit", 21 to "TR", 0 to "Preservation")

    eng.operation("ResearchPhase FROM Phase") {
      p1.task("2 BuyCard")
      p2.task("2 BuyCard")
    }
    p1.assertCounts(68 to "Megacredit", 21 to "TR", 0 to "Preservation")

    eng.phase("Action")
    eng.phase("Production")
    p1.assertCounts(95 to "Megacredit", 21 to "TR", 1 to "Preservation")
  }

  @Test
  fun unmiOutOfOrder() {
    val game = Game.create(GameSetup(Canon, "BM", 2))
    with(game.session(PLAYER1)) {
      writer.unsafe().sneak("14")
      assertCounts(14 to "Megacredit", 20 to "TR")

      // Do anything that raises TR
      operation("UseAction1<AsteroidSP>")
      assertCounts(0 to "Megacredit", 21 to "TR")

      playCorp("UnitedNationsMarsInitiative")
      assertCounts(40 to "Megacredit", 21 to "TR")

      phase("Action")
      cardAction1("UnitedNationsMarsInitiative")
      assertCounts(37 to "Megacredit", 22 to "TR")
    }
  }

  @Test
  fun aiCentral() {
    val game = Game.create(GameSetup(Canon, "BRM", 2))

    with(game.session(PLAYER1)) {
      phase("Action")
      writer.unsafe().sneak("5 ProjectCard, 100, Steel")

      playCard("SearchForLife", 3)
      playCard("InventorsGuild", 9)

      assertThrows<RequirementException>("1") { playCard("AiCentral") }
      playCard("DesignedMicroorganisms", 16)

      // Now I do have the 3 science tags, but not the energy production
      assertThrows<DeadEndException>("2") { playCard("AiCentral", 19, steel = 1) }

      // Give energy prod and try again - success
      writer.unsafe().sneak("PROD[Energy]")
      playCard("AiCentral", 19, steel = 1)
      assertCounts(0 to "PROD[Energy]")

      // Use the action
      assertCounts(1 to "ProjectCard")
      cardAction1("AiCentral")
      assertCounts(3 to "ProjectCard")
      assertCounts(1 to "ActionUsedMarker<AiCentral>")

      assertThrows<LimitsException>("3") { cardAction1("AiCentral") }
      assertCounts(3 to "ProjectCard")
      assertCounts(1 to "ActionUsedMarker<AiCentral>")

      // Next gen we can again
      operation("Generation")
      cardAction1("AiCentral")
      assertCounts(5 to "ProjectCard")
    }
  }

  @Test
  fun ceosFavoriteProject() {
    val game = Game.create(GameSetup(Canon, "CVERB", 2))

    with(game.session(PLAYER1)) {
      operation("10 ProjectCard, ForcedPrecipitation")

      // We can't CEO's onto an empty card
      assertThrows<DeadEndException> { // why that kind?
        operation("CeosFavoriteProject", "Floater<ForcedPrecipitation>")
      }

      writer.unsafe().sneak("Floater<ForcedPrecipitation>")
      assertCounts(1 to "Floater")

      operation("CeosFavoriteProject", "Floater<ForcedPrecipitation>")
      assertCounts(2 to "Floater")
    }
  }

  // @Test TODO
  fun airScrappingExpedition() {
    val game = Game.create(GameSetup(Canon, "CVERB", 2))
    val p1 = game.session(PLAYER1)
    with(p1) {
      operation("3 ProjectCard, ForcedPrecipitation")
      operation("AtmoCollectors", "2 Floater<AtmoCollectors>")
      assertCounts(2 to "Floater")

      assertThrows<NarrowingException>("1") {
        operation("AirScrappingExpedition", "3 Floater<AtmoCollectors>")
      }

      operation("AirScrappingExpedition", "3 Floater<ForcedPrecipitation>")
      assertCounts(5 to "Floater")
    }
  }

  @Test
  fun communityServices() {
    val game = Game.create(GameSetup(Canon, "CVERB", 2))
    with(game.session(PLAYER1)) {
      operation("10 ProjectCard, ForcedPrecipitation")
      operation("AtmoCollectors", "2 Floater<AtmoCollectors>")
      operation("Airliners", "2 Floater<AtmoCollectors>")

      assertThat(production(cn("M"))).isEqualTo(2)

      operation("CommunityServices") // 3 tagless cards: Atmo, Airl, Comm
      assertThat(production(cn("M"))).isEqualTo(5)
    }
  }

  @Test
  fun elCheapo() {
    val game = Game.create(GameSetup(Canon, "BRMVPCX", 2))

    with(game.session(PLAYER1)) {
      phase("Action")
      operation("CorporationCard, 12 ProjectCard, Phobolog, Steel") // -1

      operation("AntiGravityTechnology, EarthCatapult")
      operation("ResearchOutpost", "CityTile<M33>")

      operation("MassConverter, QuantumExtractor, Shuttles, SpaceStation, WarpDrive")
      operation("AdvancedAlloys, MercurianAlloys, RegoPlastics")

      assertCounts(0 to "SpaceElevator", 23 to "M", 1 to "S", 10 to "T")

      playCard("SpaceElevator", 0, steel = 1, titanium = 1)
      assertCounts(1 to "SpaceElevator", 23 to "M", 0 to "S", 9 to "T")
    }
  }

  @Test
  fun doubleDown() {
    val game = Game.create(GameSetup(Canon, "BRHXP", 2))
    val eng = game.session(ENGINE)
    val p1 = game.session(PLAYER1)
    val p2 = game.session(PLAYER2)

    p1.playCorp("InterplanetaryCinematics", 7)
    p2.playCorp("PharmacyUnion", 5)

    eng.phase("Prelude")

    p1.turn("UnmiContractor")
    p1.turn("CorporateArchives")

    with(p2) {
      turn("BiosphereSupport")
      assertThat(production().values).containsExactly(-1, 0, 0, 2, 0, 0).inOrder()

      turn("DoubleDown") {
        assertThrows<DependencyException>("exist") { task("CopyPrelude<MartianIndustries>") }
        assertThrows<DependencyException>("mine") { task("CopyPrelude<UnmiContractor>") }
        assertThrows<NarrowingException>("prelude") { task("CopyPrelude<PharmacyUnion>") }
        assertThrows<NarrowingException>("other") { task("CopyPrelude<DoubleDown>") }

        task("CopyPrelude<BiosphereSupport>")
        assertThat(production().values).containsExactly(-2, 0, 0, 4, 0, 0).inOrder()
      }
    }
  }

  @Test
  fun optimalAerobraking() {
    val game = Game.create(GameSetup(Canon, "BRHXP", 2))

    with(game.session(PLAYER1)) {
      operation("5 ProjectCard, OptimalAerobraking")
      assertCounts(0 to "Megacredit", 0 to "Heat")
      operation("AsteroidCard", "Ok") // TODO infer this??
      assertCounts(3 to "Megacredit", 3 to "Heat")
    }
  }

  @Test
  fun excentricSponsor() {
    val game = Game.create(GameSetup(Canon, "BRHXP", 2))

    with(game.session(PLAYER1)) {
      playCorp("InterplanetaryCinematics", 7)
      phase("Prelude")

      turn(
          "ExcentricSponsor",
          "PlayCard<Class<ProjectCard>, Class<NitrogenRichAsteroid>>",
          "6 Pay<Class<M>> FROM M",
          "Ok", // the damn titanium
      )
      assertCounts(0 to "Owed", 5 to "M", 1 to "ExcentricSponsor", 1 to "PlayedEvent")
    }
  }

  @Test
  fun terribleLabs() {
    val game = Game.create(GameSetup(Canon, "BMT", 2))
    val p1 = game.session(PLAYER1)

    p1.playCorp("TerralabsResearch", 10)
    p1.assertCounts(10 to "ProjectCard", 4 to "M")

    p1.operation("4 BuyCard")
    p1.assertCounts(14 to "ProjectCard", 0 to "M")
  }

  @Test
  fun polyphemos() {
    val game = Game.create(GameSetup(Canon, "BRMC", 2))
    with(game.session(PLAYER1)) {
      playCorp("Polyphemos", 10)
      assertCounts(10 to "ProjectCard", 0 to "M")

      phase("Action")
      writer.unsafe().sneak("14")

      playCard("InventorsGuild", 9)
      assertCounts(9 to "ProjectCard", 5 to "M")

      cardAction1("InventorsGuild", "BuyCard")
      assertCounts(10 to "ProjectCard", 0 to "M")
    }
  }

  @Test
  fun indenturedWorkers() {
    val game = Game.create(GameSetup(Canon, "BRM", 2))
    with(game.session(PLAYER1)) {
      playCorp("Teractor", 6)

      phase("Action")
      playCard("IndenturedWorkers")

      // doing these things in between doesn't matter
      stdProject("AsteroidSP")
      writer.unsafe().sneak("9 Heat")
      stdAction("ConvertHeat")
      sellPatents(2)

      // we still have the discount
      playCard("EarthCatapult", 15)
      assertCounts(1 to "EarthCatapult")

      // but no more
      assertThrows<RequirementException> { playCard("AdvancedAlloys", 1) }
      playCard("AdvancedAlloys", 9)
      assertCounts(6 to "M")
    }
  }

  @Test
  fun indenturedWorkersGenerational() {
    val game = Game.create(GameSetup(Canon, "BRM", 2))
    with(game.session(PLAYER1)) {
      playCorp("Teractor", 10)

      phase("Action")
      playCard("IndenturedWorkers")
      operation("Generation")

      assertThrows<RequirementException> { playCard("AdvancedAlloys", 1) } // no diskey no morey
      playCard("AdvancedAlloys", 9)
    }
  }

  @Test
  fun celestic() {
    val game = Game.create(GameSetup(Canon, "BRMV", 2))
    with(game.session(PLAYER1)) {
      playCorp("Celestic", 5)
      assertCounts(5 to "ProjectCard", 27 to "M")

      phase("Action")
      assertThrows<NarrowingException> { playCard("Mine") }
      assertThrows<NarrowingException> { stdProject("Aquifer") }
      assertThrows<NarrowingException> { stdAction("ConvertPlants") }

      pass()

      phase("Production")
      operation("ResearchPhase FROM Phase") {
        task("2 BuyCard")
        asPlayer(PLAYER2).task("2 BuyCard")
      }
      phase("Action")
      assertThrows<NarrowingException> { playCard("Mine") }

      assertCounts(1 to "Mandate")
      assertCounts(7 to "ProjectCard")
      turn("UseAllMandates")
      assertCounts(9 to "ProjectCard")
      playCard("Mine", 4)
    }
  }

  @Test
  fun valleyTrust() {
    val game = Game.create(GameSetup(Canon, "BRMP", 2))
    with(game.session(PLAYER1)) {
      playCorp("ValleyTrust", 5)
      assertCounts(5 to "ProjectCard", 22 to "M")

      phase("Action")
      assertCounts(1 to "Mandate")
      assertCounts(0 to "PreludeCard")
      turn("UseAllMandates") {
        assertCounts(1 to "PreludeCard")
        task("PlayCard<Class<PreludeCard>, Class<MartianIndustries>>")
        task("Ok") // TODO damm stupid steel
        assertCounts(1 to "PROD[S]", 1 to "PROD[E]")
        assertCounts(0 to "PreludeCard")
      }
    }
  }

  @Test
  fun insulation_normal() {
    val game = Game.create(GameSetup(Canon, "BRM", 2))
    with(game.session(PLAYER1)) {
      playCorp("Teractor", 5)
      phase("Action")
      writer.unsafe().sneak("PROD[-1, 3 Heat]")
      assertProductions(-1 to "M", 3 to "H")

      assertThrows<PetSyntaxException> {
        playCard("Insulation", 2, "PROD[-0 Heat THEN 0]")
      }
      assertThrows<PetSyntaxException> { playCard("Insulation", 2, "PROD[Ok]") }
      assertThrows<NarrowingException> { playCard("Insulation", 2, "Ok") }
      assertThrows<NarrowingException> { playCard("Insulation", 2, "PROD[-4 Heat THEN 2]") }
      assertThrows<NarrowingException> {
        playCard("Insulation", 2, "PROD[-2 Heat<P2> THEN 2 Megacredit<P2>]")
      }

      playCard("Insulation", 2) {
        task("PROD[-2 Heat THEN 2]")
        assertProductions(1 to "M", 1 to "H")
        rollItBack()
      }

      playCard("Insulation", 2) {
        task("PROD[-Heat THEN 1]")
        assertProductions(0 to "M", 2 to "H")
        rollItBack()
      }
    }
  }

}
