package dev.martianzoo.tfm.script

import dev.martianzoo.engine.AutoExecMode
import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Player
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class DoCommandTest {
  @Test
  internal fun routineReplayMatchesReferenceIncrementally() {
    val reference = routineGame(FIRST)
    val routine = routineGame(NONE)
    val referenceDad = reference.game.tfm(Player.PLAYER1).requireExplicitPaymentChoices()
    val referenceEllie = reference.game.tfm(Player.PLAYER2).requireExplicitPaymentChoices()

    referenceDad.playCorp(cn("PointLuna"), 7)
    routine.run(
        "become Dad",
        "do tasks(2 select)",
        "do tasks(10 ProjectCard<Selecting>, -3 ProjectCard<Selecting>)",
        "do playCard(PointLuna, ProjectCard, 38 MC, PROD[Titanium])",
        "do buyCards()",
    )
    referenceEllie.playCorp(cn("ValleyTrust"), 5)
    routine.run(
        "become Ellie",
        "do tasks(2 select)",
        "do tasks(10 ProjectCard<Selecting>, -5 ProjectCard<Selecting>)",
        "do playCard(ValleyTrust, 37 MC, ValleyTrust_Mandate)",
        "do buyCards()",
    )
    assertParity("Ellie corporation", reference, routine)

    referenceDad.turn {
      playPrelude(cn("Biofuels"))
      playPrelude(cn("Donation"))
    }
    routine.run(
        "become Dad",
        "do playCard(Biofuels, PROD[Plant, Energy], 2 Plant)",
        "do playCard(Donation, 21 MC)",
    )
    assertParity("Dad Preludes", reference, routine)

    referenceEllie.turn {
      playPrelude(cn("Supplier"))
      playPrelude(cn("MartianIndustries"))
    }
    routine.run(
        "become Ellie",
        "do playCard(Supplier, PROD[2 Energy], 4 Steel)",
        "do playCard(MartianIndustries, PROD[Energy, Steel], 6 MC)",
    )
    assertParity("Ellie Preludes", reference, routine)

    referenceDad.turn { playProject(cn("Pets"), 10) }
    routine.run(
        "become Dad",
        "do playCard(Pets, -10 MC, ProjectCard, Animal<Dad, Pets>)",
        "do endTurn()",
    )
    assertParity("Pets", reference, routine)

    referenceEllie.turn {
      stdAction("HandleMandates") {
        playPrelude(cn("DoubleDown")) { doTask("CopyPrelude<MartianIndustries>") }
      }
      playProject(cn("Psychrophiles"), 2)
    }
    routine.run(
        "become Ellie",
        "do useAction(1, HandleMandates)",
        "do playCard(DoubleDown, CopyPrelude<MartianIndustries>, PROD[Energy, Steel], 6 MC)",
        "do playCard(Psychrophiles, -2 MC)",
    )
    assertParity("Double Down and Psychrophiles", reference, routine)

    referenceDad.turn { playProject(cn("AerialMappers"), 11) }
    routine.run(
        "become Dad",
        "do playCard(AerialMappers, -11 MC)",
        "do endTurn()",
    )
    assertParity("Aerial Mappers", reference, routine)

    referenceEllie.turn { playProject(cn("ForcedPrecipitation"), 8) }
    routine.run(
        "become Ellie",
        "do playCard(ForcedPrecipitation, -8 MC)",
        "do endTurn()",
    )
    assertParity("Forced Precipitation", reference, routine)

    referenceDad.turn {
      cardAction1(cn("AerialMappers")) { doTask("Floater<AerialMappers>") }
    }
    routine.run(
        "become Dad",
        "do useAction(1, AerialMappers, Floater<Dad, AerialMappers>)",
        "do endTurn()",
    )
    assertParity("Aerial Mappers action", reference, routine)

    referenceEllie.turn { playProject(cn("ExtractorBalloons"), 21) }
    routine.run(
        "become Ellie",
        "do playCard(ExtractorBalloons, -21 MC, 3 Floater<Ellie, ExtractorBalloons>)",
        "do endTurn()",
    )
    assertParity("Extractor Balloons", reference, routine)

    referenceDad.pass()
    routine.run("become Dad", "do tasks(Pass)")
    assertParity("Dad passes", reference, routine)

    referenceEllie.turn {
      cardAction1(cn("Psychrophiles"))
      cardAction2(cn("ExtractorBalloons"))
      cardAction1(cn("ForcedPrecipitation"))
      pass()
    }
    routine.run(
        "become Ellie",
        "do useAction(1, Psychrophiles, Microbe<Ellie, Psychrophiles>)",
        "do useAction(2, ExtractorBalloons, -2 Floater<Ellie, ExtractorBalloons>, VenusStep, " +
            "TerraformRating)",
        "do useAction(1, ForcedPrecipitation, -2 MC, Floater<Ellie, ForcedPrecipitation>)",
        "do tasks(Pass)",
    )
    assertParity("Ellie finishes the action phase", reference, routine)

    referenceDad.doTask("OxygenStep! BY Engine")
    routine.run("become Dad", "do tasks(OxygenStep! BY Engine)")
    assertParity("Generation 1", reference, routine)

    referenceEllie.buyCards(4)
    routine.run("become Ellie", "do buyCards()")
    referenceDad.buyCards(3)
    routine.run(
        "become Dad",
        "do tasks(-ProjectCard<Selecting>)",
        "do buyCards()",
    )
    assertParity("Generation 2 research", reference, routine)

    referenceEllie.turn {
      playProject(cn("MiningRights"), 1, steel = 4) {
        doTask("MiningRights_SpecialTile<Utopia_3_6>")
      }
      playProject(cn("EnergyTapping"), 3) { doTask("PROD[-Energy<Dad>]") }
    }
    routine.run(
        "become Ellie",
        "do playCard(MiningRights, -4 Steel, -1 MC, MiningRights_SpecialTile<Utopia_3_6>, " +
            "2 ProjectCard, Titanium, PROD[Titanium])",
        "do playCard(EnergyTapping, -3 MC, PROD[-Energy<Dad>, Energy])",
    )
    assertParity("Mining Rights and Energy Tapping", reference, routine)

    referenceDad.turn {
      playProject(cn("CeosFavoriteProject"), 1) { doTask("Floater<AerialMappers>") }
    }
    routine.run(
        "become Dad",
        "do playCard(CeosFavoriteProject, -1 MC, Floater<Dad, AerialMappers>, " +
            "PlayedEvent<Class<CeosFavoriteProject>> FROM CeosFavoriteProject)",
        "do endTurn()",
    )
    assertParity("CEO's Favorite Project", reference, routine)

    referenceEllie.turn { cardAction1(cn("ExtractorBalloons")) }
    routine.run(
        "become Ellie",
        "do useAction(1, ExtractorBalloons, Floater<Ellie, ExtractorBalloons>)",
        "do endTurn()",
    )
    assertParity("Extractor Balloons action", reference, routine)

    referenceDad.turn { cardAction2(cn("AerialMappers")) }
    routine.run(
        "become Dad",
        "do useAction(2, AerialMappers, -Floater<Dad, AerialMappers>, ProjectCard)",
        "do endTurn()",
    )
    assertParity("Aerial Mappers draw", reference, routine)

    referenceEllie.turn { cardAction1(cn("Psychrophiles")) }
    routine.run(
        "become Ellie",
        "do useAction(1, Psychrophiles, Microbe<Ellie, Psychrophiles>)",
        "do endTurn()",
    )
    assertParity("Psychrophiles action", reference, routine)

    referenceDad.turn { sellPatents(1) }
    routine.run(
        "become Dad",
        "do useAction(1, SellPatents, MC FROM ProjectCard<Hand>)",
        "do endTurn()",
    )
    assertParity("Dad sells a patent", reference, routine)

    referenceEllie.turn { cardAction1(cn("ForcedPrecipitation")) }
    routine.run(
        "become Ellie",
        "do useAction(1, ForcedPrecipitation, -2 MC, Floater<Ellie, ForcedPrecipitation>)",
        "do endTurn()",
    )
    assertParity("Forced Precipitation action", reference, routine)

    referenceDad.turn { playProject(cn("SixteenPsyche"), 28, titanium = 1) }
    routine.run(
        "become Dad",
        "do playCard(SixteenPsyche, -28 MC, -Titanium, PROD[2 Titanium], 3 Titanium)",
        "do endTurn()",
    )
    assertParity("16 Psyche", reference, routine)

    referenceEllie.pass()
    routine.run("become Ellie", "do tasks(Pass)")
    referenceDad.pass()
    routine.run("become Dad", "do tasks(Pass)")

    referenceEllie.doTask("VenusStep! BY Engine")
    routine.run("become Ellie", "do tasks(VenusStep! BY Engine)")
    assertParity("Generation 2", reference, routine)

    referenceDad.buyCards(1)
    routine.run(
        "become Dad",
        "do tasks(-3 ProjectCard<Selecting>)",
        "do buyCards()",
    )
    referenceEllie.buyCards(0)
    routine.run(
        "become Ellie",
        "do tasks(-4 ProjectCard<Selecting>)",
        "do buyCards()",
    )
    assertParity("Generation 3 research", reference, routine)

    referenceDad.turn {
      playProject(cn("ImportedHydrogen"), 1, titanium = 5) {
        doTask("2 Animal<Pets>")
        doTask("OceanTile<Utopia_4_1>")
      }
    }
    routine.run(
        "become Dad",
        "do playCard(ImportedHydrogen, -5 Titanium, -1 MC, 2 Animal<Dad, Pets>, " +
            "OceanTile<Utopia_4_1>, ProjectCard, " +
            "TerraformRating, Plant, ProjectCard, " +
            "PlayedEvent<Class<ImportedHydrogen>> FROM ImportedHydrogen)",
        "do endTurn()",
    )
    assertParity("Imported Hydrogen", reference, routine)

    referenceEllie.turn {
      cardAction2(cn("ForcedPrecipitation"))
      cardAction2(cn("ExtractorBalloons"))
    }
    routine.run(
        "become Ellie",
        "do useAction(2, ForcedPrecipitation, -2 Floater<Ellie, ForcedPrecipitation>, VenusStep, " +
            "TerraformRating)",
        "do useAction(2, ExtractorBalloons, -2 Floater<Ellie, ExtractorBalloons>, VenusStep, " +
            "TerraformRating, ProjectCard)",
    )
    assertParity("Ellie raises Venus twice", reference, routine)

    referenceDad.turn { playProject(cn("Cartel"), 8) }
    routine.run(
        "become Dad",
        "do playCard(Cartel, -8 MC, ProjectCard, PROD[3 MC])",
        "do endTurn()",
    )
    assertParity("Cartel", reference, routine)

    referenceEllie.turn { playProject(cn("ColonizerTrainingCamp"), steel = 4) }
    routine.run(
        "become Ellie",
        "do playCard(ColonizerTrainingCamp, -4 Steel)",
        "do endTurn()",
    )
    assertParity("Colonizer Training Camp", reference, routine)

    referenceDad.turn { sellPatents(1) }
    routine.run(
        "become Dad",
        "do useAction(1, SellPatents, MC FROM ProjectCard<Hand>)",
        "do endTurn()",
    )

    referenceEllie.turn {
      sellPatents(1)
      playProject(cn("BeamFromAThoriumAsteroid"), 26, titanium = 2)
    }
    routine.run(
        "become Ellie",
        "do useAction(1, SellPatents, MC FROM ProjectCard<Hand>)",
        "do playCard(BeamFromAThoriumAsteroid, -2 Titanium, -26 MC, PROD[3 Heat, 3 Energy])",
    )
    assertParity("Beam from a Thorium Asteroid", reference, routine)

    referenceDad.turn { playProject(cn("ResearchCoordination"), 4) }
    routine.run(
        "become Dad",
        "do playCard(ResearchCoordination, -4 MC, WildTag<Dad, ResearchCoordination>)",
        "do endTurn()",
    )
    assertParity("Research Coordination", reference, routine)

    referenceEllie.turn { cardAction1(cn("Psychrophiles")) }
    routine.run(
        "become Ellie",
        "do useAction(1, Psychrophiles, Microbe<Ellie, Psychrophiles>)",
        "do endTurn()",
    )

    referenceDad.turn {
      doTask("VenusTag<WildTagUse<ResearchCoordination>>")
      playProject(cn("VenusGovernor"), 4)
    }
    routine.run(
        "become Dad",
        "do assignWildTag(VenusTag)",
        "do playCard(VenusGovernor, -4 MC, PROD[2 MC])",
        "do endTurn()",
    )
    assertParity("Venus Governor", reference, routine)

    referenceEllie.pass()
    routine.run("become Ellie", "do tasks(Pass)")
    referenceDad.turn {
      cardAction2(cn("AerialMappers"))
      pass()
    }
    routine.run(
        "become Dad",
        "do useAction(2, AerialMappers, -Floater<Dad, AerialMappers>, ProjectCard)",
        "do tasks(Pass)",
    )

    referenceDad.doTask("TemperatureStep! BY Engine")
    routine.run("do tasks(TemperatureStep! BY Engine)")
    assertParity("Generation 3", reference, routine)

    referenceEllie.buyCards(2)
    routine.run(
        "become Ellie",
        "do tasks(-2 ProjectCard<Selecting>)",
        "do buyCards()",
    )
    referenceDad.buyCards(2)
    routine.run(
        "become Dad",
        "do tasks(-2 ProjectCard<Selecting>)",
        "do buyCards()",
    )
    assertParity("Generation 4 research", reference, routine)

    referenceEllie.turn {
      stdAction("TradeSA", 2) { doTask("Trade<Pluto>") }
    }
    routine.run(
        "become Ellie",
        "do useAction(2, TradeSA, -3 Energy, Trade<Pluto>, " +
            "FlownTradeFleet<Ellie, Pluto> FROM ReserveTradeFleet<Ellie>, 3 ProjectCard, " +
            "ResetColonyProduction<Pluto>, -4 ColonyProduction<Pluto>)",
        "do endTurn()",
    )
    assertParity("Pluto trade", reference, routine)

    referenceDad.turn {
      playProject(cn("MarsUniversity"), 8) { doTask("-ProjectCard") }
    }
    routine.run(
        "become Dad",
        "do playCard(MarsUniversity, -8 MC, -ProjectCard<Hand>, ProjectCard)",
        "do endTurn()",
    )
    assertParity("Mars University", reference, routine)

    referenceEllie.turn {
      playProject(cn("Flooding"), 7) { doTask("OceanTile<Utopia_3_1>") }
      playProject(cn("Potatoes"), 0) {
        doTask("PayFromCard<Psychrophiles> FROM Microbe<Psychrophiles>")
      }
    }
    routine.run(
        "become Ellie",
        "do playCard(Flooding, -7 MC, OceanTile<Utopia_3_1>, 2 MC, TerraformRating, 3 Plant, " +
            "PlayedEvent<Class<Flooding>> FROM Flooding)",
        "do playCard(Potatoes, -Microbe<Psychrophiles>, -2 Plant, PROD[2 MC])",
    )
    assertParity("Flooding and Potatoes", reference, routine)

    referenceDad.intentionalUnderpay()
    referenceDad.turn {
      doTask("ScienceTag<WildTagUse<ResearchCoordination>>")
      playProject(cn("MercurianAlloys"), 3)
    }
    routine.run(
        "become Dad",
        "do assignWildTag(ScienceTag)",
        "do playCard(MercurianAlloys, -3 MC)",
        "do endTurn()",
    )
    assertParity("Mercurian Alloys", reference, routine)

    referenceEllie.turn { cardAction1(cn("Psychrophiles")) }
    routine.run(
        "become Ellie",
        "do useAction(1, Psychrophiles, Microbe<Ellie, Psychrophiles>)",
        "do endTurn()",
    )

    referenceDad.turn { playProject(cn("AsteroidRights"), 2, titanium = 2) }
    routine.run(
        "become Dad",
        "do playCard(AsteroidRights, -2 Titanium, -2 MC, ProjectCard, " +
            "2 Asteroid<Dad, AsteroidRights>)",
        "do endTurn()",
    )
    assertParity("Asteroid Rights", reference, routine)

    referenceEllie.turn { playProject(cn("Mine"), steel = 2) }
    routine.run(
        "become Ellie",
        "do playCard(Mine, -2 Steel, PROD[Steel])",
        "do endTurn()",
    )

    referenceDad.turn {
      cardAction2(cn("AsteroidRights")) { doTask("2 Titanium") }
    }
    routine.run(
        "become Dad",
        "do useAction(2, AsteroidRights, -Asteroid<Dad, AsteroidRights>, 2 Titanium)",
        "do endTurn()",
    )
    assertParity("Asteroid Rights action", reference, routine)

    referenceEllie.turn {
      cardAction1(cn("ForcedPrecipitation"))
      cardAction1(cn("ExtractorBalloons"))
    }
    routine.run(
        "become Ellie",
        "do useAction(1, ForcedPrecipitation, -2 MC, Floater<Ellie, ForcedPrecipitation>)",
        "do useAction(1, ExtractorBalloons, Floater<Ellie, ExtractorBalloons>)",
    )

    referenceDad.turn {
      doTask("ScienceTag<WildTagUse<ResearchCoordination>>")
      playProject(cn("FloatingHabs"), 5)
    }
    routine.run(
        "become Dad",
        "do assignWildTag(ScienceTag)",
        "do playCard(FloatingHabs, -5 MC)",
        "do endTurn()",
    )
    assertParity("Floating Habs", reference, routine)

    referenceEllie.turn {
      sellPatents(1)
      playProject(cn("NitriteReducingBacteria"), 11)
    }
    routine.run(
        "become Ellie",
        "do useAction(1, SellPatents, MC FROM ProjectCard<Hand>)",
        "do playCard(NitriteReducingBacteria, -11 MC, " +
            "3 Microbe<Ellie, NitriteReducingBacteria>)",
    )

    referenceDad.turn {
      cardAction1(cn("FloatingHabs")) {
        doTask("Floater<AerialMappers>")
      }
    }
    routine.run(
        "become Dad",
        "do useAction(1, FloatingHabs, -2 MC, Floater<Dad, AerialMappers>)",
        "do endTurn()",
    )

    referenceEllie.turn { cardAction2(cn("NitriteReducingBacteria")) }
    routine.run(
        "become Ellie",
        "do useAction(2, NitriteReducingBacteria, " +
            "-3 Microbe<Ellie, NitriteReducingBacteria>, TerraformRating)",
        "do endTurn()",
    )

    referenceDad.turn { cardAction2(cn("AerialMappers")) }
    routine.run(
        "become Dad",
        "do useAction(2, AerialMappers, -Floater<Dad, AerialMappers>, ProjectCard)",
        "do endTurn()",
    )
    assertParity("Generation 4 actions", reference, routine)

    referenceEllie.pass()
    routine.run("become Ellie", "do tasks(Pass)")
    referenceDad.pass()
    routine.run("become Dad", "do tasks(Pass)")
    referenceEllie.doTask("VenusStep! BY Engine")
    routine.run("become Ellie", "do tasks(VenusStep! BY Engine)")
    assertParity("Generation 4", reference, routine)

    referenceDad.buyCards(1)
    routine.run(
        "become Dad",
        "do tasks(-3 ProjectCard<Selecting>)",
        "do buyCards()",
    )
    referenceEllie.buyCards(1)
    routine.run(
        "become Ellie",
        "do tasks(-3 ProjectCard<Selecting>)",
        "do buyCards()",
    )
    assertParity("Generation 5 research", reference, routine)

    referenceDad.turn { playProject(cn("EnergyMarket"), 3) }
    routine.run(
        "become Dad",
        "do playCard(EnergyMarket, -3 MC)",
        "do endTurn()",
    )

    referenceEllie.turn {
      playProject(cn("HydrogenToVenus"), 5, titanium = 2) {
        doTask("2 Floater<ForcedPrecipitation>")
      }
      playProject(cn("HermeticOrderOfMars"), 10)
    }
    routine.run(
        "become Ellie",
        "do playCard(HydrogenToVenus, -2 Titanium, -5 MC, " +
            "2 Floater<Ellie, ForcedPrecipitation>, VenusStep, " +
            "TerraformRating, PlayedEvent<Class<HydrogenToVenus>> FROM HydrogenToVenus)",
        "do playCard(HermeticOrderOfMars, -10 MC, PROD[2 MC], 6 MC)",
    )
    assertParity("Hydrogen to Venus and Hermetic Order", reference, routine)

    referenceDad.turn {
      cardAction1(cn("EnergyMarket"), x = 3)
      stdAction("TradeSA", 2) { doTask("Trade<Io>") }
    }
    routine.run(
        "become Dad",
        "do useAction(1, EnergyMarket, -6 MC, 3 Energy)",
        "do useAction(2, TradeSA, -3 Energy, Trade<Io>, " +
            "FlownTradeFleet<Dad, Io> FROM ReserveTradeFleet<Dad>, 10 Heat, " +
            "ResetColonyProduction<Io>, -5 ColonyProduction<Io>)",
    )
    assertParity("Energy Market and Io trade", reference, routine)

    referenceEllie.turn {
      playProject(cn("StratosphericBirds"), 12) {
        doTask("-Floater<ForcedPrecipitation>")
      }
      stdAction("TradeSA", 2) {
        doTask("Trade<Miranda>")
        doTask("3 Animal<StratosphericBirds>")
      }
    }
    routine.run(
        "become Ellie",
        "do playCard(StratosphericBirds, -12 MC, -Floater<Ellie, ForcedPrecipitation>)",
        "do useAction(2, TradeSA, -3 Energy, Trade<Miranda>, " +
            "FlownTradeFleet<Ellie, Miranda> FROM ReserveTradeFleet<Ellie>, " +
            "3 Animal<Ellie, StratosphericBirds>, ResetColonyProduction<Miranda>, " +
            "-5 ColonyProduction<Miranda>)",
    )
    assertParity("Stratospheric Birds and Miranda trade", reference, routine)

    referenceDad.intentionalOverpay(1)
    referenceDad.turn {
      playProject(cn("BigAsteroid"), titanium = 7) { doTask("-Plant<Ellie>") }
    }
    routine.run(
        "become Dad",
        "do playCard(BigAsteroid, -7 Titanium, -Plant<Ellie>, TemperatureStep, " +
            "TerraformRating, " +
            "TemperatureStep, TerraformRating, PROD[Heat], 4 Titanium, " +
            "PlayedEvent<Class<BigAsteroid>> FROM BigAsteroid)",
        "do endTurn()",
    )
    assertParity("Big Asteroid", reference, routine)

    referenceEllie.turn {
      stdAction("ConvertHeatSA")
      stdAction("ConvertHeatSA")
    }
    routine.run(
        "become Ellie",
        "do useAction(1, ConvertHeatSA, -8 Heat, TemperatureStep, TerraformRating)",
        "do useAction(1, ConvertHeatSA, -8 Heat, TemperatureStep, TerraformRating, PROD[Heat])",
    )

    referenceDad.turn {
      doTask("EarthTag<WildTagUse<ResearchCoordination>>")
      playProject(cn("LunarMining"), 11)
    }
    routine.run(
        "become Dad",
        "do assignWildTag(EarthTag)",
        "do playCard(LunarMining, -11 MC, ProjectCard, PROD[3 Titanium])",
        "do endTurn()",
    )
    assertParity("Lunar Mining", reference, routine)

    referenceEllie.turn { cardAction1(cn("StratosphericBirds")) }
    routine.run(
        "become Ellie",
        "do useAction(1, StratosphericBirds, Animal<Ellie, StratosphericBirds>)",
        "do endTurn()",
    )

    referenceDad.turn {
      cardAction2(cn("AsteroidRights")) { doTask("2 Titanium") }
    }
    routine.run(
        "become Dad",
        "do useAction(2, AsteroidRights, -Asteroid<Dad, AsteroidRights>, 2 Titanium)",
        "do endTurn()",
    )

    referenceEllie.turn {
      cardAction1(cn("ForcedPrecipitation"))
      cardAction1(cn("ExtractorBalloons"))
    }
    routine.run(
        "become Ellie",
        "do useAction(1, ForcedPrecipitation, -2 MC, Floater<Ellie, ForcedPrecipitation>)",
        "do useAction(1, ExtractorBalloons, Floater<Ellie, ExtractorBalloons>)",
    )

    referenceDad.turn {
      doTask("EarthTag<WildTagUse<ResearchCoordination>>")
      playProject(cn("LunaMetropolis"), 1, titanium = 5)
    }
    routine.run(
        "become Dad",
        "do assignWildTag(EarthTag)",
        "do playCard(LunaMetropolis, -5 Titanium, -1 MC, ProjectCard, PROD[7 MC], " +
            "CityTile<LunaMetropolis_RemoteArea>, Animal<Dad, Pets>)",
        "do endTurn()",
    )
    assertParity("Luna Metropolis", reference, routine)

    referenceEllie.turn {
      cardAction1(cn("Psychrophiles"))
      cardAction1(cn("NitriteReducingBacteria"))
    }
    routine.run(
        "become Ellie",
        "do useAction(1, Psychrophiles, Microbe<Ellie, Psychrophiles>)",
        "do useAction(1, NitriteReducingBacteria, Microbe<Ellie, NitriteReducingBacteria>)",
    )

    referenceDad.turn {
      cardAction1(cn("FloatingHabs")) { doTask("Floater<AerialMappers>") }
      cardAction2(cn("AerialMappers"))
    }
    routine.run(
        "become Dad",
        "do useAction(1, FloatingHabs, -2 MC, Floater<Dad, AerialMappers>)",
        "do useAction(2, AerialMappers, -Floater<Dad, AerialMappers>, ProjectCard)",
    )
    assertParity("Generation 5 actions", reference, routine)

    referenceEllie.pass()
    routine.run("become Ellie", "do tasks(Pass)")
    referenceDad.turn {
      stdAction("ConvertHeatSA")
      pass()
    }
    routine.run(
        "become Dad",
        "do useAction(1, ConvertHeatSA, -8 Heat, TemperatureStep, TerraformRating)",
        "do tasks(Pass)",
    )
    referenceDad.doTask("OceanTile<Utopia_9_8>! BY Engine")
    routine.run("do tasks(OceanTile<Utopia_9_8>! BY Engine)")
    assertParity("Generation 5", reference, routine)

    referenceDad.buyCards(2)
    routine.run(
        "do tasks(-2 ProjectCard<Selecting>)",
        "do buyCards()",
    )
    referenceEllie.buyCards(1)
    routine.run(
        "become Ellie",
        "do tasks(-3 ProjectCard<Selecting>)",
        "do buyCards()",
    )
    assertParity("Generation 6 research", reference, routine)

    referenceEllie.turn {
      stdAction("TradeSA", 2) {
        doTask("Trade<Enceladus>")
        doTask("5 Microbe<NitriteReducingBacteria>")
      }
    }
    routine.run(
        "do useAction(2, TradeSA, -3 Energy, Trade<Enceladus>, " +
            "FlownTradeFleet<Ellie, Enceladus> FROM ReserveTradeFleet<Ellie>, " +
            "5 Microbe<Ellie, NitriteReducingBacteria>, " +
            "ResetColonyProduction<Enceladus>, -6 ColonyProduction<Enceladus>)",
        "do endTurn()",
    )

    referenceDad.turn {
      playProject(cn("IndustrialMicrobes"), 12)
      doTask("MicrobeTag<WildTagUse<ResearchCoordination>>")
      stdAction("ClaimMilestoneSA") { doTask("Ecologist") }
    }
    routine.run(
        "become Dad",
        "do playCard(IndustrialMicrobes, -12 MC, PROD[Steel, Energy])",
        "do assignWildTag(MicrobeTag)",
        "do useAction(1, ClaimMilestoneSA, -8 MC, Ecologist)",
    )
    reference.exMachina(Player.PLAYER1, "PROD[-Steel, -Energy]")
    routine.exMachina(Player.PLAYER1, "PROD[-Steel, -Energy]")
    assertParity("Ecologist claim", reference, routine)

    referenceEllie.turn {
      cardAction2(cn("ForcedPrecipitation"))
      cardAction2(cn("ExtractorBalloons"))
    }
    routine.run(
        "become Ellie",
        "do useAction(2, ForcedPrecipitation, -2 Floater<Ellie, ForcedPrecipitation>, VenusStep, " +
            "TerraformRating)",
        "do useAction(2, ExtractorBalloons, -2 Floater<Ellie, ExtractorBalloons>, VenusStep, " +
            "TerraformRating, TerraformRating)",
    )

    referenceDad.turn {
      playProject(cn("ImportOfAdvancedGhg"), 1, titanium = 2)
      stdAction("ClaimMilestoneSA") { doTask("Metallurgist") }
    }
    routine.run(
        "become Dad",
        "do playCard(ImportOfAdvancedGhg, -2 Titanium, -1 MC, ProjectCard, PROD[2 Heat], " +
            "PlayedEvent<Class<ImportOfAdvancedGhg>> FROM ImportOfAdvancedGhg)",
        "do useAction(1, ClaimMilestoneSA, -8 MC, Metallurgist)",
    )
    assertParity("Metallurgist claim", reference, routine)

    referenceEllie.turn {
      stdAction("ClaimMilestoneSA") { doTask("Tactician4") }
    }
    routine.run(
        "become Ellie",
        "do useAction(1, ClaimMilestoneSA, -8 MC, Tactician4)",
        "do endTurn()",
    )

    referenceDad.turn {
      cardAction1(cn("FloatingHabs")) { doTask("Floater<AerialMappers>") }
      cardAction2(cn("AerialMappers"))
    }
    routine.run(
        "become Dad",
        "do useAction(1, FloatingHabs, -2 MC, Floater<Dad, AerialMappers>)",
        "do useAction(2, AerialMappers, -Floater<Dad, AerialMappers>, ProjectCard)",
    )

    referenceEllie.turn { cardAction1(cn("Psychrophiles")) }
    routine.run(
        "become Ellie",
        "do useAction(1, Psychrophiles, Microbe<Ellie, Psychrophiles>)",
        "do endTurn()",
    )

    referenceDad.turn {
      sellPatents(1)
      playProject(cn("HiredRaiders"), 1) { doTask("3 MC<Dad> FROM MC<Ellie>") }
    }
    routine.run(
        "become Dad",
        "do useAction(1, SellPatents, MC FROM ProjectCard<Hand>)",
        "do playCard(HiredRaiders, -1 MC, 3 MC<Dad> FROM MC<Ellie>, " +
            "PlayedEvent<Class<HiredRaiders>> FROM HiredRaiders)",
    )

    referenceEllie.turn { cardAction2(cn("NitriteReducingBacteria")) }
    routine.run(
        "become Ellie",
        "do useAction(2, NitriteReducingBacteria, " +
            "-3 Microbe<Ellie, NitriteReducingBacteria>, TerraformRating)",
        "do endTurn()",
    )

    referenceDad.turn {
      cardAction1(cn("AsteroidRights")) { doTask("Asteroid<AsteroidRights>") }
    }
    routine.run(
        "become Dad",
        "do useAction(1, AsteroidRights, -1 MC, Asteroid<Dad, AsteroidRights>)",
        "do endTurn()",
    )

    referenceEllie.turn { stdAction("ConvertHeatSA") }
    routine.run(
        "become Ellie",
        "do useAction(1, ConvertHeatSA, -8 Heat, TemperatureStep, TerraformRating)",
        "do endTurn()",
    )

    referenceDad.turn {
      cardAction1(cn("EnergyMarket"), x = 1)
      stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Utopia_4_2>") }
    }
    routine.run(
        "become Dad",
        "do useAction(1, EnergyMarket, -2 MC, Energy)",
        "do useAction(1, ConvertPlantsSA, -8 Plant, GreeneryTile<Utopia_4_2>, " +
            "2 MC, 2 MC, OxygenStep, Plant, " +
            "TerraformRating)",
    )
    reference.exMachina(Player.PLAYER1, "TerraformRating")
    routine.exMachina(Player.PLAYER1, "TerraformRating")
    assertParity("Generation 6 actions", reference, routine)

    referenceEllie.turn {
      playProject(cn("NoctisCity"), 6, steel = 6) {
        doTask("CityTile<Utopia_3_2>")
      }
    }
    routine.run(
        "become Ellie",
        "do playCard(NoctisCity, -6 Steel, -6 MC, CityTile<Utopia_3_2>, " +
            "PROD[-Energy, 3 MC], 2 MC)",
        "become Dad",
        "do tasks(Animal<Dad, Pets>)",
        "become Ellie",
        "do endTurn()",
    )
    referenceDad.pass()
    routine.run("become Dad", "do tasks(Pass)")
    referenceEllie.turn { cardAction1(cn("StratosphericBirds")) }
    routine.run(
        "become Ellie",
        "do useAction(1, StratosphericBirds, Animal<Ellie, StratosphericBirds>)",
    )
    referenceEllie.pass()
    routine.run("do tasks(Pass)")
    referenceEllie.doTask("OceanTile<Utopia_6_4>! BY Engine")
    routine.run("do tasks(OceanTile<Utopia_6_4>! BY Engine)")
    assertParity("Generation 6", reference, routine)
  }

  @Test
  internal fun doRequiresPurpleMode() {
    val repl = ScriptSession()

    val output = repl.command("do tasks(Plant)")

    assertEquals(listOf("DO requires purple mode", "Usage: do <RoutineCall>"), output)
  }

  @Test
  internal fun routineCallKeepsNestedCommasTogether() {
    val call = RoutineCall.parse("tasks(PROD[Energy, Steel], Animal<Dad, Pets>)")

    assertEquals("tasks", call.name)
    assertEquals(listOf("PROD[Energy, Steel]", "Animal<Dad, Pets>"), call.arguments)
  }

  @Test
  internal fun routineCallNameMustUseLowerCamelCase() {
    assertFailsWith<ScriptSession.UsageException> { RoutineCall.parse("Tasks(Plant)") }
  }

  private fun routineGame(autoExecMode: AutoExecMode): ScriptSession =
      ScriptSession().also { repl ->
        repl.command("auto ${autoExecMode.name.lowercase()}")
        repl.command("newgame \"$config\" Dad Ellie purple")
      }

  private fun ScriptSession.run(vararg commands: String) {
    commands.forEach { command ->
      val output = command(command)
      val unexpected = output.filterNot { line ->
        line.isEmpty() ||
            line == "um, nothing happened" ||
            line == "New tasks pending:" ||
            line.startsWith("Hi, ") ||
            line.startsWith("[") ||
            line.firstOrNull()?.isDigit() == true
      }
      assertTrue(unexpected.isEmpty(), "$command:\n${output.joinToString("\n")}")
    }
  }

  private fun ScriptSession.exMachina(player: Player, instructions: String) {
    game.gameplay(player).godMode().sneak(instructions)
  }

  private fun assertParity(label: String, reference: ScriptSession, routine: ScriptSession) {
    val expected = componentGraph(reference)
    val actual = componentGraph(routine)
    val difference =
        (expected.keys + actual.keys).distinct().sorted().mapNotNull { component ->
          val expectedCount = expected[component] ?: 0
          val actualCount = actual[component] ?: 0
          if (expectedCount == actualCount) null
          else "$component: expected $expectedCount, actual $actualCount"
        }
    assertTrue(
        difference.isEmpty(),
        "$label component graph:\n${difference.joinToString("\n")}" +
            "\nDad tasks: ${routine.command("as Dad tasks")}" +
            "\nEllie tasks: ${routine.command("as Ellie tasks")}",
    )
  }

  private fun componentGraph(repl: ScriptSession): Map<String, Int> =
      repl.game.reader
          .getComponents("Component")
          .entries
          .sortedBy { (type, _) -> type.expressionFull.toString() }
          .associate { (type, count) -> type.expressionFull.toString() to count }

  private companion object {
    private val config =
        listOf(
                "UtopiaMap",
                "VenusNextExpansion",
                "PreludeExpansion",
                "ColoniesExpansion",
                "PromoCardPack",
                "Ecologist",
                "Merchant",
                "Metallurgist",
                "Tactician4",
                "Hoverlord",
                "Constructor",
                "Excentric",
                "Highlander",
                "Mogul",
                "Traveller",
                "Venuphile",
                "Enceladus",
                "Miranda",
                "Europa",
                "Io",
                "Pluto",
            )
            .joinToString()
  }
}
