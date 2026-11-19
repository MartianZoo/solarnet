package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilEventsTest : TfmGameplayTest() {
  @Test
  internal fun `premise deck draws both setup events and Admin places their neutral delegates`() {
    newTestGame(
        "TurmoilExpansion",
        2,
        firstGlobalEvents = listOf("$ExperimentalLifeforms", "$SelfSufficiencyProgram"),
    )
    game.reader.drawOrder.take(2).map { it.toString() } shouldBe
        listOf("$ExperimentalLifeforms", "$SelfSufficiencyProgram")
    admin.count("Coming<$ExperimentalLifeforms>") shouldBe 1
    admin.count("Distant<$SelfSufficiencyProgram>") shouldBe 1
    admin.count("PartyLeader<Greens, Neutral>") shouldBe 1
    admin.count("PartyFollower<Greens, Neutral>") shouldBe 1
  }

  @Test
  internal fun `Democratic Reform cannot place its ocean on land`() {
    newTestGame(
        "TurmoilExpansion, -QuickStartVariant",
        2,
        firstGlobalEvents =
            listOf("$FreeAcademiaTreaty", "$ExploreFirstDirective", "$MoralMovement"),
    )
    admin.exMachina(
        "$DemocraticReform, Current<$DemocraticReform>, " +
            "PendingGlobalEffect<Current<$DemocraticReform>, Class<AquiferReleasedByPublicCouncil>>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("VenusStep! BY Admin")
    shouldThrow<TaskException> { kim.doTask("OceanTile<Tharsis_3_3> BY Admin") }
    admin.count("OceanTile") shouldBe 0
    kim.doTask("OceanTile<Tharsis_1_2> BY Admin").expect("OceanTile, 0 TerraformRating")
    players.forEach { it.doTasks("0 Plant", "0 Steel") }
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `An event stays dormant until Current and is discarded after resolving`() {
    // This test is about the event's entire journey, so retain the actual generations.
    newTestGame(
        "TurmoilExpansion, -QuickStartVariant",
        2,
        firstGlobalEvents =
            listOf(
                "$DemocraticReform",
                "$MinimalImpactPolicy",
                "$ExperimentalLifeforms",
                "$ExploreFirstDirective",
                "$MoralMovement",
                "$ScientificProgress",
            ),
    )
    players.forEach { it.exMachina("PROD[2 Plant]") }

    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTasks("20 MC", "2 Plant") }
    kim.doTask("VenusStep! BY Admin")
    admin.count("Distant<$ExperimentalLifeforms>") shouldBe 1
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }

    stan.doTask("Pass")
    kim.doTask("Pass")
    players.forEach { it.doTasks("19 MC", "2 Plant") }
    stan.doTasks(
        "VenusStep! BY Admin",
        "OceanTile<Tharsis_1_2> BY Admin",
    )
    players.forEach { it.doTasks("0 Plant", "0 Steel") }
    admin.count("Coming<$ExperimentalLifeforms>") shouldBe 1
    players.forEach { it.count("Plant") shouldBe 4 }
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }

    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTasks("18 MC", "2 Plant") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "-OceanTile<Tharsis_1_2>",
    )
    players.forEach { it.doTask("EACH") } // No influence to exchange for resources.
    admin.count("Current<$ExperimentalLifeforms>") shouldBe 1
    admin.count("Coming<$ExploreFirstDirective>") shouldBe 1
    admin.count("Distant<$MoralMovement>") shouldBe 1
    players.forEach { it.count("Plant") shouldBe 6 }
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }

    stan.doTask("Pass")
    kim.doTask("Pass")
    players.forEach { it.doTasks("18 MC", "2 Plant") }
    players.forEach { it.count("Plant") shouldBe 8 }
    stan.doTask("VenusStep! BY Admin")
    players.forEach { it.doTask("-5 Plant") }
    players.forEach { it.count("Plant") shouldBe 3 }
    admin.count("$ExperimentalLifeforms") shouldBe 0
    admin.count("Current<$ExploreFirstDirective>") shouldBe 1
    admin.count("Coming<$MoralMovement>") shouldBe 1
    admin.count("Distant<$ScientificProgress>") shouldBe 1
    admin.count("PartyLeader<Scientists, Neutral>") shouldBe 1
    admin.count("PartyFollower<Scientists, Neutral>") shouldBe 1
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Asteroid Mining measures this generation's influence before paying titanium`() {
    prepareEvent("$MiningRestrictions", "AsteroidMiningGlobalEffect")
    kim.exMachina("$VestaShipyard")
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("VenusStep! BY Admin")
    kim.count("Influence") shouldBe 2
    kim.doTask("3 Titanium")
    // Both rewards are zero; name the influence term to distinguish them.
    stan.selectTask("Titanium / Influence")
    stan.doTasks(
        "0 Titanium",
        "0 Titanium",
    )
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Homeworld Support caps Earth tags at five before adding influence`() {
    prepareEvent("$RedResistance", "HomeworldSupport")
    kim.exMachina("$EarthOffice, $Sponsors, $AcquiredCompany, $LunaGovernor, $Cartel")
    kim.count("EarthTag") shouldBe 6
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "14 MC",
    )
    // Both rewards are zero; name the influence term to distinguish them.
    stan.selectTask("2 MC / Influence")
    stan.doTasks(
        "0 MC",
        "0 MC",
    )
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Productivity pays for steel production plus influence`() {
    prepareEvent("$ScientificProgress", "Productivity")
    kim.exMachina("PROD[3 Steel]")
    stan.exMachina("PROD[Steel]")
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("3 Steel")
    stan.doTask("Steel")
    kim.doTasks(
        "VenusStep! BY Admin",
        "5 Steel",
    )
    stan.doTasks("Steel", "0 Steel")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Successful Organisms pays for plant production plus influence`() {
    prepareEvent("$LocalTerraformingSupport", "SuccessfulOrganisms")
    kim.exMachina("PROD[2 Plant]")
    stan.exMachina("PROD[Plant]")
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("2 Plant")
    stan.doTask("Plant")
    kim.doTasks(
        "VenusStep! BY Admin",
        "4 Plant",
    )
    stan.doTasks("Plant", "0 Plant")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Scientific Community pays for the current hand and influence`() {
    prepareEvent("$ExploreFirstDirective", "ScientificCommunity")
    kim.exMachina("-7 ProjectCard<Hand>")
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "5 MC",
    )
    stan.doTasks("10 MC", "0 MC")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Celebrity Leaders pays the owner of played events`() {
    prepareEvent("$SolarnetGlobalEvent", "CelebrityLeaders")
    kim.exMachina("PlayedEvent<Class<$InvestmentLoan>>, PlayedEvent<Class<$BribedCommittee>>")
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "4 MC",
        "0 MC",
    )
    // Both rewards are zero; name the influence term to distinguish them.
    stan.selectTask("2 MC / Influence")
    stan.doTasks(
        "0 MC",
        "0 MC",
    )
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Strong Society pays only for owned cities`() {
    prepareEvent("$MoralMovement", "StrongSociety")
    kim.exMachina("NormalCityTile<Tharsis_3_3>, NormalCityTile<Tharsis_8_8>")
    admin.exMachina("NormalCityTile<Tharsis_6_4, Neutral>")
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "4 MC",
        "0 MC",
    )
    // Both rewards are zero; name the influence term to distinguish them.
    stan.selectTask("2 MC / Influence")
    stan.doTasks(
        "0 MC",
        "0 MC",
    )
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Global Dust Storm consumes heat and influence reduces the building penalty`() {
    prepareEvent("$HeatFirstPolicy", "GlobalDustStorm")
    kim.exMachina("$Mine, $PowerPlant, $BuildingIndustries, 7 Heat")
    stan.exMachina("4 Heat")
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "-7 Heat",
        "-2 MC",
    )
    stan.doTasks("-4 Heat", "-0 MC")
    kim.count("Heat") shouldBe 0
    stan.count("Heat") shouldBe 0
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Pandemic caps six building tags before subtracting influence`() {
    prepareEvent("$ViralModificationsApproved", "Pandemic")
    kim.exMachina(
        "$Mine, $PowerPlant, $IndustrialMicrobes, $BuildingIndustries, $TitaniumMine, $FuelFactory"
    )
    kim.count("BuildingTag") shouldBe 6
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "-9 MC",
    )
    stan.doTask("-0 MC")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Solarnet Shutdown counts active cards but excludes automated cards and played events`() {
    prepareEvent("$AiResearch", "SolarnetShutdown")
    kim.exMachina(
        "$Pets, $SpaceMirrors, $Tardigrades, $StandardTechnology, $Mine, PlayedEvent<Class<$BribedCommittee>>"
    )
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "-6 MC",
    )
    stan.doTask("-0 MC")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `War on Earth reduces rating loss for influence while still charging other players`() {
    prepareEvent("$SeparatistMovement", "WarOnEarth")
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "-2 TerraformRating",
    )
    stan.doTask("-4 TerraformRating")
    kim.count("TerraformRating") shouldBe 18
    stan.count("TerraformRating") shouldBe 15
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Paradigm Breakdown discards the last card without demanding a second`() {
    prepareEvent("$ScientificConsensus", "ParadigmBreakdown")
    kim.exMachina("-9 ProjectCard<Hand>")
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "-1 ProjectCard",
        "4 MC",
    )
    stan.doTasks("-2 ProjectCard", "0 MC")
    kim.count("ProjectCard<Hand>") shouldBe 0
    stan.count("ProjectCard<Hand>") shouldBe 8
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Sabotage reduces available production without taking energy production below zero`() {
    prepareEvent("$CorporateAlliance", "SabotageGlobalEffect")
    kim.exMachina("PROD[3 Steel, 0 Energy]")
    stan.exMachina("PROD[Steel, Energy]")
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("3 Steel")
    stan.doTasks("Steel", "Energy")
    kim.doTasks(
        "VenusStep! BY Admin",
        "PROD[-Steel]",
        "PROD[-0 Energy]",
        "2 Steel",
    )
    stan.doTasks("PROD[-Steel]", "PROD[-Energy]", "0 Steel")
    kim.count("PROD[Steel]") shouldBe 2
    kim.count("PROD[Energy]") shouldBe 0
    stan.count("PROD[Steel]") shouldBe 0
    stan.count("PROD[Energy]") shouldBe 0
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Miners on Strike takes only the titanium available`() {
    prepareEvent("$RisingAlloyDemand", "MinersOnStrike")
    kim.exMachina("$VestaShipyard, $MirandaResort, $JovianEmbassy, 2 Titanium")
    stan.exMachina("4 Titanium")
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "-2 Titanium",
    )
    stan.doTask("-0 Titanium")
    kim.count("Titanium") shouldBe 0
    stan.count("Titanium") shouldBe 4
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Mud Slides counts each coastal tile once even beside two oceans`() {
    prepareEvent("$ThawMining", "MudSlides")
    kim.exMachina("NormalCityTile<Tharsis_4_4>, GreeneryTile<Tharsis_4_5>")
    admin.exMachina("OceanTile<Tharsis_5_4>, OceanTile<Tharsis_5_5>")
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "-8 MC",
    )
    stan.doTask("-0 MC")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Diversity does not count duplicate science tags as distinct tags`() {
    prepareEvent("$FreeAcademiaTreaty", "Diversity")
    kim.exMachina("$Research, $PowerPlant, $VestaShipyard, $EarthOffice")
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "0 MC",
    )
    stan.doTask("0 MC")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Diversity combines distinct tags with influence to reach nine`() {
    prepareEvent("$FreeAcademiaTreaty", "Diversity")
    kim.exMachina("$Research, $PowerPlant, $VestaShipyard, $EarthOffice, $AdaptedLichen")
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "10 MC",
    )
    stan.doTask("0 MC")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Improved Energy Templates combines power tags and influence before rounding`() {
    prepareEvent("$SecondEnergyCrisis", "ImprovedEnergyTemplates")
    kim.exMachina("$PowerPlant, $SpaceMirrors, $SolarWindPower")
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "PROD[2 Energy]",
    )
    stan.doTask("PROD[0 Energy]")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Sponsored Projects grows occupied cards for both players but skips empty cards`() {
    prepareEvent("$ScienceSummit", "SponsoredProjects")
    kim.exMachina("$AerialMappers, Floater<$AerialMappers>, $FloatingHabs, $Tardigrades")
    stan.exMachina("$TitanShuttles, 2 Floater<$TitanShuttles>")
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("VenusStep! BY Admin")
    kim.doTask("EACH")
        .expect(
            "Floater<$AerialMappers>, Floater<$TitanShuttles<Stan>>, 0 Floater<$FloatingHabs>, 0 Microbe<$Tardigrades>"
        )
    kim.doTask("2 ProjectCard")
    stan.doTask("0 ProjectCard")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Election awards tied first places without a second prize`() {
    prepareEvent("$SelfSufficiencyProgram", "Election")
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "2 TerraformRating",
    )
    stan.doTask("2 TerraformRating")
    kim.count("TerraformRating") shouldBe 21
    stan.count("TerraformRating") shouldBe 21
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Election awards distinct first and second places`() {
    prepareEvent("$SelfSufficiencyProgram", "Election")
    kim.exMachina("$Mine")
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "2 TerraformRating",
    )
    stan.doTask("TerraformRating")
    kim.count("TerraformRating") shouldBe 21
    stan.count("TerraformRating") shouldBe 20
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Revolution leaves a zero score alone`() {
    prepareEvent("$WorldGovernmentDirectives", "Revolution")
    kim.exMachina("$EarthOffice")
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "-2 TerraformRating",
    )
    kim.count("TerraformRating") shouldBe 17
    stan.count("TerraformRating") shouldBe 19
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Revolution applies the first place penalty to both tied players`() {
    prepareEvent("$WorldGovernmentDirectives", "Revolution")
    kim.exMachina("$EarthOffice")
    stan.exMachina("$Sponsors")
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "-2 TerraformRating",
    )
    stan.doTask("-2 TerraformRating")
    kim.count("TerraformRating") shouldBe 17
    stan.count("TerraformRating") shouldBe 17
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Cloud Societies gives everyone a floater then groups each player's influence on a card`() {
    prepareEvent("$TitanVenusAlliance", "CloudSocieties")
    kim.exMachina("$FloatingHabs, $AerialMappers")
    stan.exMachina("$TitanShuttles")
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("VenusStep! BY Admin")
    kim.doTask("EACH")
        .expect("Floater<$FloatingHabs>, Floater<$AerialMappers>, Floater<$TitanShuttles<Stan>>")
    shouldThrow<TaskException> { kim.doTask("2 Floater<$TitanShuttles<Stan>>") }
    kim.doTask("2 Floater<$FloatingHabs>")
    stan.doTask("0 Floater<$TitanShuttles>")
    kim.count("Floater<$FloatingHabs>") shouldBe 3
    kim.count("Floater<$AerialMappers>") shouldBe 1
    stan.count("Floater<$TitanShuttles>") shouldBe 1
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Corrosive Rain consumes two floaters from the same card before drawing for influence`() {
    prepareEvent("$ImmigrationToVenus", "CorrosiveRain")
    kim.exMachina("$TitanShuttles, 2 Floater<$TitanShuttles>")
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("VenusStep! BY Admin")
    kim.count("ProjectCard<Hand>") shouldBe 10 // Draw waits for the floater payment.
    kim.doTask("-2 Floater<$TitanShuttles>").expect("0 MC")
    kim.doTask("2 ProjectCard")
    stan.doTasks("-10 MC", "0 ProjectCard")
    kim.count("Floater<$TitanShuttles>") shouldBe 0
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Corrosive Rain charges money when floaters are split across cards`() {
    prepareEvent("$ImmigrationToVenus", "CorrosiveRain")
    kim.exMachina("$FloatingHabs, Floater<$FloatingHabs>, $AerialMappers, Floater<$AerialMappers>")
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "-10 MC",
        "2 ProjectCard",
    )
    stan.doTasks("-10 MC", "0 ProjectCard")
    kim.count("Floater<$FloatingHabs>") shouldBe 1
    kim.count("Floater<$AerialMappers>") shouldBe 1
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Jovian Tax Rights pays production for owned colonies and titanium for influence`() {
    prepareEvent("$JovianColonyBoom", "JovianTaxRights", extraOptions = "Luna, Ceres, Triton")
    kim.exMachina("Colony<Luna>, Colony<Ceres>, Colony<Triton>")
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "PROD[3 MC]",
        "2 Titanium",
    )
    stan.doTasks("PROD[0 MC]", "0 Titanium")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Microgravity Health Problems subtracts influence from owned colonies`() {
    prepareEvent("$Diaspora", "MicrogravityHealthProblems", extraOptions = "Luna, Ceres, Triton")
    kim.exMachina("Colony<Luna>, Colony<Ceres>, Colony<Triton>")
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "-3 MC",
    )
    stan.doTask("-0 MC")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Venus Infrastructure pays for Venus tags and influence`() {
    prepareEvent("$MartianProtectionism", "VenusInfrastructure")
    kim.exMachina("$AerialMappers, $Dirigibles")
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "8 MC",
    )
    // Both rewards are zero; name the influence term to distinguish them.
    stan.selectTask("2 MC / Influence")
    stan.doTasks(
        "0 MC",
        "0 MC",
    )
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Snow Cover cannot lower a completed temperature track`() {
    prepareEvent("$MoholeLakeGlobalEvent", "SnowCover")
    admin.exMachina("19 TemperatureStep")
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "2 ProjectCard",
    )
    stan.doTask("0 ProjectCard")
    admin.count("TemperatureStep") shouldBe 19
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Volcanic Eruptions pays heat production for measured influence`() {
    prepareEvent("$PateraBoring", "VolcanicEruptions")
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "PROD[2 Heat]",
    )
    stan.doTask("PROD[0 Heat]")
    admin.count("TemperatureStep") shouldBe 2
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Volcanic Eruptions lets the first player place the threshold ocean for Admin`() {
    prepareEvent("$PateraBoring", "VolcanicEruptions")
    admin.exMachina("13 TemperatureStep")
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("VenusStep! BY Admin")
    kim.count("TerraformRating") shouldBe 19 // Revision; Admin's temperature gain gives no TR.
    shouldThrow<TaskException> { kim.doTask("OceanTile<Tharsis_3_3> BY Admin") }
    kim.doTask("OceanTile<Tharsis_1_4> BY Admin").expect("OceanTile, 0 TerraformRating")
    players.forEach { it.doTask("PROD[0 Heat]") }
    admin.count("TemperatureStep") shouldBe 15
    kim.count("TerraformRating") shouldBe 19
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Generous Funding combines the post-revision rating bracket with influence`() {
    prepareEvent("$TerraformingLobbying", "GenerousFunding")
    kim.count("TerraformRating") shouldBe 20
    // Leading Mars First and having another delegate supply two influence.
    kim.exMachina(
        "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 4 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTasks(
        "VenusStep! BY Admin",
        "4 MC",
        "0 MC",
    )
    // Both rewards are zero; name the influence term to distinguish them.
    stan.selectTask("2 MC / Influence")
    stan.doTasks(
        "0 MC",
        "0 MC",
    )
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Dry Deserts grants independent resource choices for each influence without taking TR`() {
    newTestGame(
        "TurmoilExpansion, -QuickStartVariant",
        2,
        firstGlobalEvents =
            listOf("$DemocraticReform", "$FreeAcademiaTreaty", "$ExploreFirstDirective"),
    )
    admin.exMachina(
        "OceanTile<Tharsis_1_2>, $MinimalImpactPolicy, Current<$MinimalImpactPolicy>, " +
            "PendingGlobalEffect<Current<$MinimalImpactPolicy>, Class<DryDeserts>>"
    )
    kim.exMachina(
        "Chairman<Kim> FROM Chairman<Neutral>, " +
            "PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 3 PartyFollower<MarsFirst>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("VenusStep! BY Admin")
    kim.count("Influence") shouldBe 3
    kim.doTask("-OceanTile<Tharsis_1_2>").expect("0 TerraformRating")
    kim.doTasks("EACH", "Heat", "Plant", "Steel")
    stan.doTask("EACH") // No influence to exchange for resources.
    admin.count("OceanTile") shouldBe 0
    kim.count("TerraformRating") shouldBe 20 // Revision and chairman TR cancel.
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Revealing events still works when all neutral delegates are on the board`() {
    newTestGame(
        "TurmoilExpansion, -QuickStartVariant",
        2,
        firstGlobalEvents = listOf("$DemocraticReform", "$MinimalImpactPolicy", "$ScienceSummit"),
    )
    // Put the complete neutral reserve in parties that will survive this election.
    kim.exMachina(
        "Chairman<Kim> FROM Chairman<Neutral>, PartyLeader<Scientists>, 5 PartyFollower<Scientists>"
    )
    stan.exMachina("6 PartyFollower<Scientists>")
    admin.exMachina(
        "4 PartyFollower<MarsFirst, Neutral>, 4 PartyFollower<Reds, Neutral>, " +
            "PartyLeader<Unity, Neutral>, 3 PartyFollower<Unity, Neutral>"
    )
    admin.count("Delegate<Neutral>") shouldBe 14
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("VenusStep! BY Admin")
    admin.count("Delegate<Neutral>") shouldBe 14
    admin.count("Current<$DemocraticReform>") shouldBe 1
    admin.count("Distant<$ScienceSummit>") shouldBe 1
    admin.count("GlobalEventDrawn") shouldBe 3
    admin.count("PartyDelegate<Greens, Neutral>") shouldBe 0
    admin.count("PartyDelegate<Scientists, Neutral>") shouldBe 0
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Solo Election requires five points rather than awarding an automatic first place`() {
    prepareEvent(
        "$SelfSufficiencyProgram",
        "Election",
        playerCount = 1,
        extraOptions = "-ColoniesExpansion",
    )
    kim.doTask("Pass")
    kim.doTasks(
        "14 MC",
        "VenusStep! BY Admin",
        "0 TerraformRating",
    )
    kim.count("TerraformRating") shouldBe 13
    kim.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Solo Election combines buildings and city tiles to reach five`() {
    prepareEvent(
        "$SelfSufficiencyProgram",
        "Election",
        playerCount = 1,
        extraOptions = "-ColoniesExpansion",
    )
    kim.exMachina(
        "$Mine, $PowerPlant, $IndustrialMicrobes, $BuildingIndustries, NormalCityTile<Tharsis_8_8>"
    )
    kim.doTask("Pass")
    kim.doTasks(
        "14 MC",
        "VenusStep! BY Admin",
        "TerraformRating",
    )
    kim.count("TerraformRating") shouldBe 14
    kim.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Solo Revolution does not penalize fewer than four Earth tags and influence`() {
    prepareEvent(
        "$WorldGovernmentDirectives",
        "Revolution",
        playerCount = 1,
        extraOptions = "-ColoniesExpansion",
    )
    kim.exMachina("$EarthOffice, $Sponsors, $AcquiredCompany")
    kim.doTask("Pass")
    kim.doTasks(
        "14 MC",
        "VenusStep! BY Admin",
    )
    kim.count("TerraformRating") shouldBe 13
    kim.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Solo Revolution penalizes reaching four Earth tags`() {
    prepareEvent(
        "$WorldGovernmentDirectives",
        "Revolution",
        playerCount = 1,
        extraOptions = "-ColoniesExpansion",
    )
    kim.exMachina("$EarthOffice, $Sponsors, $AcquiredCompany, $Cartel")
    kim.doTask("Pass")
    kim.doTasks(
        "14 MC",
        "VenusStep! BY Admin",
        "-2 TerraformRating",
    )
    kim.count("TerraformRating") shouldBe 11
    kim.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    admin.count("ActionPhase") shouldBe 1
  }

  private fun prepareEvent(
      event: String,
      effect: String,
      playerCount: Int = 2,
      extraOptions: String = "",
  ) {
    newTestGame(
        "TurmoilExpansion, -QuickStartVariant, $extraOptions",
        playerCount,
        firstGlobalEvents =
            listOf("$DemocraticReform", "$MinimalImpactPolicy", "$ExperimentalLifeforms"),
    )
    // Earlier reveals and their unrelated effects are incidental to these focused event tests.
    admin.exMachina("$event, Current<$event>, PendingGlobalEffect<Current<$event>, Class<$effect>>")
  }
}
