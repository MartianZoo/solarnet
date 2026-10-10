package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilEventsTest : TfmGameplayTest() {
  @Test
  internal fun `An event stays dormant until Current and is discarded after resolving`() {
    prepareEvent("ExperimentalLifeforms") {
      kim.turn { playProject(AdaptedLichen, 9) }
      stan.turn { playProject(Archaebacteria, 6) }
    }
    admin.count("Current<ExperimentalLifeforms>") shouldBe 1
    admin.count("Coming<ExploreFirstDirective>") shouldBe 1
    admin.count("Distant<MoralMovement>") shouldBe 1
    // Both players already exceed the three plants protected by the event.
    kim.count("Plant") shouldBe 6
    stan.count("Plant") shouldBe 6
    kim.pass()
    kim.count("Plant") shouldBe 8
    stan.wgt("VenusStep")
    kim.count("Plant") shouldBe 3
    stan.count("Plant") shouldBe 3
    admin.count("ExperimentalLifeforms") shouldBe 0
    admin.count("Current<ExploreFirstDirective>") shouldBe 1
    admin.count("Coming<MoralMovement>") shouldBe 1
    admin.count("Distant") shouldBe 0
    admin.doTask("ScientificProgress")
    admin.count("Distant<ScientificProgress>") shouldBe 1
  }

  @Test
  internal fun `Asteroid Mining measures this generation's influence before paying titanium`() {
    prepareEvent("MiningRestrictions")
    kim.playProject(VestaShipyard, 15)
    lobbyForInfluence()
    kim.pass()
    val titanium = kim.count("Titanium")
    stan.wgt("VenusStep")
    kim.count("Influence") shouldBe 2
    kim.count("Titanium") shouldBe titanium + 3 // A Jovian tag and two influence.
  }

  @Test
  internal fun `Homeworld Support caps six Earth tags before adding influence`() {
    prepareEvent("RedResistance")
    kim.playProject(EarthOffice, 1)
    kim.playProject(Sponsors, 3)
    kim.playProject(AcquiredCompany, 7)
    kim.playProject(LunaGovernor, 0)
    kim.playProject(Cartel, 5)
    lobbyForInfluence()
    kim.pass()
    val money = kim.count("MC")
    val otherMoney = stan.count("MC")
    stan.wgt("VenusStep")
    kim.count("MC") shouldBe money + 14 // Five counted tags, plus two influence, at 2 MC each.
    stan.count("MC") shouldBe otherMoney
  }

  @Test
  internal fun `Productivity pays for steel production plus influence`() {
    prepareEvent("ScientificProgress")
    kim.playProject(Mine, 4)
    kim.playProject(IndustrialMicrobes, 12)
    lobbyForInfluence()
    kim.pass()
    val steel = kim.count("Steel")
    val otherSteel = stan.count("Steel")
    stan.wgt("VenusStep")
    kim.count("Steel") shouldBe steel + 5 // Three production and two influence.
    stan.count("Steel") shouldBe otherSteel + 1
  }

  @Test
  internal fun `Successful Organisms pays for plant production plus influence`() {
    prepareEvent("LocalTerraformingSupport")
    kim.playProject(AdaptedLichen, 9)
    lobbyForInfluence()
    kim.pass()
    val plants = kim.count("Plant")
    val otherPlants = stan.count("Plant")
    stan.wgt("VenusStep")
    kim.count("Plant") shouldBe plants + 4
    stan.count("Plant") shouldBe otherPlants + 1
  }

  @Test
  internal fun `Scientific Community pays for the current hand and influence`() {
    prepareEvent("ExploreFirstDirective")
    kim.sellPatents(7)
    lobbyForInfluence()
    kim.pass()
    val money = kim.count("MC")
    val otherMoney = stan.count("MC")
    stan.wgt("VenusStep")
    kim.count("MC") shouldBe money + 5
    stan.count("MC") shouldBe otherMoney + 10
  }

  @Test
  internal fun `Celebrity Leaders pays the owner of played events`() {
    prepareEvent("SolarnetGlobalEvent")
    kim.playProject(InvestmentLoan, 3)
    kim.playProject(BribedCommittee, 7)
    kim.pass()
    val money = kim.count("MC")
    val otherMoney = stan.count("MC")
    stan.wgt("VenusStep")
    kim.count("MC") shouldBe money + 4
    stan.count("MC") shouldBe otherMoney
  }

  @Test
  internal fun `Strong Society pays only for owned cities`() {
    prepareEvent("MoralMovement")
    kim.stdProject("CityProject") { placeTile(3, 3) }
    kim.stdProject("CityProject") { placeTile(8, 8) }
    kim.pass()
    val money = kim.count("MC")
    val otherMoney = stan.count("MC")
    stan.wgt("VenusStep")
    kim.count("MC") shouldBe money + 4
    stan.count("MC") shouldBe otherMoney
  }

  @Test
  internal fun `Global Dust Storm consumes heat and influence reduces the building penalty`() {
    prepareEvent("HeatFirstPolicy")
    kim.playProject(Mine, 4)
    kim.playProject(PowerPlant, 4)
    kim.playProject(BuildingIndustries, 6)
    lobbyForInfluence()
    kim.pass()
    val money = kim.count("MC")
    stan.wgt("VenusStep")
    kim.count("Heat") shouldBe 0
    stan.count("Heat") shouldBe 0
    // Three building tags minus two influence costs 2 MC; Mars First then pays 3 MC.
    kim.count("MC") shouldBe money + 1
  }

  @Test
  internal fun `Pandemic caps six building tags before subtracting influence`() {
    prepareEvent("ViralModificationsApproved")
    kim.playProject(Mine, 4)
    kim.playProject(PowerPlant, 4)
    kim.playProject(IndustrialMicrobes, 12)
    kim.playProject(BuildingIndustries, 6)
    kim.playProject(TitaniumMine, 7)
    kim.playProject(FuelFactory, 6)
    lobbyForInfluence()
    kim.pass()
    val money = kim.count("MC")
    stan.wgt("VenusStep")
    // (Five counted buildings - two influence) costs 9; Mars First pays for all six buildings.
    kim.count("MC") shouldBe money - 3
  }

  @Test
  internal fun `Solarnet Shutdown counts active cards but excludes automated cards and played events`() {
    prepareEvent("AiResearch")
    kim.playProject(Pets, 10)
    kim.playProject(SpaceMirrors, 3)
    kim.playProject(Tardigrades, 4)
    kim.playProject(StandardTechnology, 6)
    kim.playProject(Mine, 4)
    kim.playProject(BribedCommittee, 7)
    lobbyForInfluence()
    kim.pass()
    val money = kim.count("MC")
    stan.wgt("VenusStep")
    // Four active cards minus two influence costs 6; Mars First pays 1 for Mine.
    kim.count("MC") shouldBe money - 5
  }

  @Test
  internal fun `War on Earth reduces rating loss for influence while still charging other players`() {
    prepareEvent("SeparatistMovement")
    lobbyForInfluence()
    val rating = kim.count("TerraformRating")
    val otherRating = stan.count("TerraformRating")
    kim.pass()
    stan.wgt("VenusStep")
    // Kim loses two plus revision, then becomes chairman; Stan loses four plus revision.
    kim.count("TerraformRating") shouldBe rating - 2
    stan.count("TerraformRating") shouldBe otherRating - 5
  }

  @Test
  internal fun `Paradigm Breakdown discards the last card without demanding a second`() {
    prepareEvent("ScientificConsensus")
    kim.sellPatents(9)
    lobbyForInfluence()
    kim.pass()
    val money = kim.count("MC")
    stan.wgt("VenusStep")
    kim.count("ProjectCard") shouldBe 0
    stan.count("ProjectCard") shouldBe 8
    kim.count("MC") shouldBe money + 4
  }

  @Test
  internal fun `Sabotage reduces available production without taking energy production below zero`() {
    prepareEvent("CorporateAlliance")
    kim.playProject(BuildingIndustries, 6)
    lobbyForInfluence()
    kim.pass()
    val steel = kim.count("Steel")
    stan.wgt("VenusStep")
    kim.count("PROD[Steel]") shouldBe 2
    kim.count("PROD[Energy]") shouldBe 0
    stan.count("PROD[Steel]") shouldBe 0
    stan.count("PROD[Energy]") shouldBe 0
    kim.count("Steel") shouldBe steel + 2
  }

  @Test
  internal fun `Miners on Strike takes only the titanium available`() {
    prepareEvent("RisingAlloyDemand")
    kim.playProject(VestaShipyard, mc = 6, titanium = 3)
    kim.playProject(MirandaResort, 12)
    kim.playProject(JovianEmbassy, 14)
    kim.pass()
    kim.count("Titanium") shouldBe 2
    stan.wgt("VenusStep")
    kim.count("Titanium") shouldBe 0
    stan.count("Titanium") shouldBe 4
  }

  @Test
  internal fun `Mud Slides counts each coastal tile once even beside two oceans`() {
    prepareEvent("ThawMining")
    kim.stdProject("CityProject") { placeTile(4, 4) }
    kim.stdProject("GreeneryProject") { placeTile(4, 5) }
    kim.stdProject("AquiferProject") { placeTile(5, 4) }
    kim.stdProject("AquiferProject") { placeTile(5, 5) }
    kim.pass()
    val money = kim.count("MC")
    stan.wgt("VenusStep")
    kim.count("MC") shouldBe money - 8
  }

  @Test
  internal fun `Diversity does not count duplicate science tags as distinct tags`() {
    prepareDiversity()
    kim.pass()
    val money = kim.count("MC")
    stan.wgt("VenusStep")
    // Six distinct tag kinds plus two influence is short; Mars First pays for Power Plant.
    kim.count("MC") shouldBe money + 1
  }

  @Test
  internal fun `Diversity combines distinct tags with influence to reach nine`() {
    prepareDiversity()
    kim.playProject(AdaptedLichen, 9)
    kim.pass()
    val money = kim.count("MC")
    stan.wgt("VenusStep")
    kim.count("MC") shouldBe money + 11
  }

  @Test
  internal fun `Improved Energy Templates combines power tags and influence before rounding`() {
    prepareEvent("SecondEnergyCrisis")
    kim.playProject(PowerPlant, 4)
    kim.playProject(SpaceMirrors, 3)
    kim.playProject(SolarWindPower, 11)
    lobbyForInfluence()
    kim.pass()
    val production = kim.count("PROD[Energy]")
    stan.wgt("VenusStep")
    kim.count("PROD[Energy]") shouldBe production + 2 // Floor of (three tags + two influence)/2.
  }

  @Test
  internal fun `Sponsored Projects grows occupied cards for both players but skips empty cards`() {
    prepareEvent("ScienceSummit") {
      kim.turn { playProject(Research, 11) }
      stan.turn {
        playProject(TitanShuttles, 23)
        cardAction1(TitanShuttles) { addCardResources(TitanShuttles, 2) }
      }
    }
    kim.playProject(AerialMappers, 11)
    kim.playProject(FloatingHabs, 5)
    kim.playProject(Tardigrades, 4)
    kim.cardAction1(AerialMappers) { addCardResources(AerialMappers) }
    lobbyForInfluence()
    kim.pass()
    val hand = kim.count("ProjectCard")
    stan.wgt("VenusStep")
    kim.count("Floater<$AerialMappers>") shouldBe 2
    kim.count("Floater<$FloatingHabs>") shouldBe 0
    kim.count("Microbe<$Tardigrades>") shouldBe 0
    stan.count("Floater<$TitanShuttles>") shouldBe 3
    kim.count("ProjectCard") shouldBe hand + 2
  }

  @Test
  internal fun `Election awards tied first places without a second prize`() {
    prepareEvent("SelfSufficiencyProgram")
    val kimRating = kim.count("TerraformRating")
    val stanRating = stan.count("TerraformRating")
    kim.pass()
    stan.wgt("VenusStep")
    kim.count("TerraformRating") shouldBe kimRating + 1 // Revision -1, tied first +2.
    stan.count("TerraformRating") shouldBe stanRating + 1
  }

  @Test
  internal fun `Election awards distinct first and second places`() {
    prepareEvent("SelfSufficiencyProgram")
    kim.playProject(Mine, 4)
    val kimRating = kim.count("TerraformRating")
    val stanRating = stan.count("TerraformRating")
    kim.pass()
    stan.wgt("VenusStep")
    kim.count("TerraformRating") shouldBe kimRating + 1
    stan.count("TerraformRating") shouldBe stanRating
  }

  @Test
  internal fun `Revolution leaves a zero score alone`() {
    prepareEvent("WorldGovernmentDirectives")
    kim.playProject(EarthOffice, 1)
    val kimRating = kim.count("TerraformRating")
    val stanRating = stan.count("TerraformRating")
    kim.pass()
    stan.wgt("VenusStep")
    kim.count("TerraformRating") shouldBe kimRating - 2 // Revision -1, Revolution -2, Reds +1.
    stan.count("TerraformRating") shouldBe stanRating - 1
  }

  @Test
  internal fun `Revolution applies the first place penalty to both tied players`() {
    prepareEvent("WorldGovernmentDirectives") {
      kim.turn { playProject(EarthOffice, 1) }
      stan.turn { playProject(Sponsors, 6) }
    }
    val kimRating = kim.count("TerraformRating")
    val stanRating = stan.count("TerraformRating")
    kim.pass()
    stan.wgt("VenusStep")
    kim.count("TerraformRating") shouldBe kimRating - 2 // Revision -1, Revolution -2, Reds +1.
    stan.count("TerraformRating") shouldBe stanRating - 2
  }

  @Test
  internal fun `Cloud Societies gives everyone a floater then groups each player's influence on a card`() {
    prepareEvent("TitanVenusAlliance") {
      kim.turn { playProject(Research, 11) }
      stan.turn { playProject(TitanShuttles, 23) }
    }
    kim.playProject(FloatingHabs, 5)
    kim.playProject(AerialMappers, 11)
    lobbyForInfluence()
    kim.pass()
    stan.wgt("VenusStep")
    shouldThrow<NarrowingException> { kim.doTask("2 Floater<$TitanShuttles<Stan>>") }
    kim.doTask("2 Floater<$FloatingHabs>")
    kim.count("Floater<$FloatingHabs>") shouldBe 3
    kim.count("Floater<$AerialMappers>") shouldBe 1
    stan.count("Floater<$TitanShuttles>") shouldBe 1
  }

  @Test
  internal fun `Corrosive Rain consumes two floaters from the same card before drawing for influence`() {
    prepareEvent("ImmigrationToVenus")
    kim.playProject(TitanShuttles, 23)
    kim.cardAction1(TitanShuttles) { addCardResources(TitanShuttles, 2) }
    lobbyForInfluence()
    kim.pass()
    val hand = kim.count("ProjectCard")
    val money = kim.count("MC")
    stan.wgt("VenusStep")
    kim.count("ProjectCard") shouldBe hand
    kim.doTask("-2 Floater<$TitanShuttles>")
    kim.count("Floater<$TitanShuttles>") shouldBe 0
    kim.count("MC") shouldBe money
    kim.count("ProjectCard") shouldBe hand + 2
  }

  @Test
  internal fun `Corrosive Rain charges money when floaters are split across cards`() {
    prepareEvent("ImmigrationToVenus")
    kim.playProject(Research, 11)
    kim.playProject(FloatingHabs, 5)
    kim.playProject(AerialMappers, 11)
    kim.cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }
    kim.cardAction1(AerialMappers) { addCardResources(AerialMappers) }
    lobbyForInfluence()
    kim.pass()
    val money = kim.count("MC")
    stan.wgt("VenusStep")
    kim.count("MC") shouldBe money - 10
    kim.count("Floater<$FloatingHabs>") shouldBe 1
    kim.count("Floater<$AerialMappers>") shouldBe 1
  }

  @Test
  internal fun `Jovian Tax Rights pays production for owned colonies and titanium for influence`() {
    prepareEvent("JovianColonyBoom")
    listOf("Luna", "Ceres", "Triton").forEach { track ->
      kim.stdProject("BuildColonyProject") { doTask("Colony<$track>") }
    }
    lobbyForInfluence()
    kim.pass()
    val production = kim.count("PROD[MC]")
    val otherProduction = stan.count("PROD[MC]")
    val titanium = kim.count("Titanium")
    stan.wgt("VenusStep")
    kim.count("PROD[MC]") shouldBe production + 3
    kim.count("Titanium") shouldBe titanium + 2
    stan.count("PROD[MC]") shouldBe otherProduction
  }

  @Test
  internal fun `Microgravity Health Problems subtracts influence from owned colonies`() {
    prepareEvent("Diaspora")
    listOf("Luna", "Ceres", "Triton").forEach { track ->
      kim.stdProject("BuildColonyProject") { doTask("Colony<$track>") }
    }
    lobbyForInfluence()
    kim.pass()
    val money = kim.count("MC")
    val otherMoney = stan.count("MC")
    stan.wgt("VenusStep")
    kim.count("MC") shouldBe money - 3
    stan.count("MC") shouldBe otherMoney
  }

  @Test
  internal fun `Venus Infrastructure pays for Venus tags and influence`() {
    prepareEvent("MartianProtectionism")
    kim.playProject(AerialMappers, 11)
    kim.playProject(Dirigibles, 11)
    lobbyForInfluence()
    kim.pass()
    val money = kim.count("MC")
    stan.wgt("VenusStep")
    kim.count("MC") shouldBe money + 8
  }

  @Test
  internal fun `Snow Cover cannot lower a completed temperature track`() {
    prepareTemperatureEvent("MoholeLakeGlobalEvent", 19)
    val first = players.single { it.count("StartToken") == 1 }
    first.pass()
    // Generation 20 starts with Stan; Kim can still lobby before passing.
    lobbyForInfluence()
    kim.pass()
    val hand = kim.count("ProjectCard")
    first.wgt("VenusStep")
    admin.count("TemperatureStep") shouldBe 19
    kim.count("ProjectCard") shouldBe hand + 2
  }

  @Test
  internal fun `Volcanic Eruptions lets the first player place the threshold ocean for Admin`() {
    prepareTemperatureEvent("PateraBoring", 13)
    val first = players.single { it.count("StartToken") == 1 }
    first.pass()
    lobbyForInfluence()
    kim.pass()
    val rating = first.count("TerraformRating")
    val heatProduction = kim.count("PROD[Heat]")
    first.wgt("VenusStep")
    first.doTask("OceanTile<Tharsis_1_4> BY Admin").expect("OceanTile")
    admin.count("TemperatureStep") shouldBe 15
    admin.count("Ruling<MarsFirst>") shouldBe 1
    first.count("TerraformRating") shouldBe rating - 1 // Only revision; the ocean grants no TR.
    kim.count("PROD[Heat]") shouldBe heatProduction + 2
  }

  @Test
  internal fun `Generous Funding combines the post-revision rating bracket with influence`() {
    prepareEvent("TerraformingLobbying")
    repeat(2) { kim.stdProject("AsteroidProject") }
    lobbyForInfluence()
    kim.count("TerraformRating") shouldBe 20
    kim.pass()
    val money = kim.count("MC")
    stan.wgt("VenusStep")
    kim.count("MC") shouldBe money + 4 // Revision drops TR below the next payout bracket.
  }

  @Test
  internal fun `Dry Deserts grants independent resource choices for each influence without taking TR`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    kim.turn {
      stdAction("LobbyAction") { doTask("PartyDelegate<Scientists>") }
      stdAction("LobbyAction<Action2>") { doTask("PartyDelegate<Scientists>") }
    }
    stan.pass()
    kim.pass()
    kim.wgt("VenusStep")
    admin.doTask("RedResistance")
    players.forEach { it.buyCards(0) }

    // Kim is chairman and takes Unity's leadership and an ordinary delegate's influence.
    stan.pass()
    kim.stdAction("LobbyAction") { doTask("PartyDelegate<Unity>") }
    repeat(2) { kim.stdAction("LobbyAction<Action2>") { doTask("PartyDelegate<Unity>") } }
    kim.pass()
    val plants = kim.count("Plant")
    val steel = kim.count("Steel")
    stan.wgt("VenusStep")
    stan.doTask("OceanTile<Tharsis_1_2> BY Admin")
    kim.count("Plant") shouldBe plants + 3
    kim.count("Steel") shouldBe steel + 3
    admin.doTask("CharismaticWgPresident")
    players.forEach { it.buyCards(0) }

    kim.turn {
      stdAction("LobbyAction") { doTask("PartyDelegate<MarsFirst>") }
      stdAction("LobbyAction<Action2>") { doTask("PartyDelegate<MarsFirst>") }
    }
    stan.pass()
    kim.stdAction("LobbyAction<Action2>") { doTask("PartyDelegate<MarsFirst>") }
    kim.pass()
    val rating = kim.count("TerraformRating")
    val heat = kim.count("Heat")
    val beforePlants = kim.count("Plant")
    val beforeSteel = kim.count("Steel")
    kim.wgt("VenusStep")
    // These indistinguishable choices may be answered in any order, using their public task IDs.
    val choices = kim.tasks.extract { it.id }
    kim.doTask("Heat", choices[0])
    kim.doTask("Plant", choices[1])
    kim.doTask("Steel", choices[2])
    kim.count("Heat") shouldBe heat + 1
    kim.count("Plant") shouldBe beforePlants + 1
    kim.count("Steel") shouldBe beforeSteel + 1
    admin.count("OceanTile") shouldBe 0
    kim.count("TerraformRating") shouldBe rating // Revision and chairman TR cancel.
  }

  @Test
  internal fun `Revealing events still works when all neutral delegates are on the board`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    val reveals =
        listOf(
            "CharismaticWgPresident",
            "RedResistance",
            "TerraformingLobbying",
            "SolarnetGlobalEvent",
        )
    repeat(7) { generation ->
      val first = players.single { it.count("StartToken") == 1 }
      val other = players.single { it != first }
      repeat(2) { round ->
        listOf(first, other).forEach { player ->
          player.turn {
            val lobbyAction = if (round == 0) "LobbyAction<Action1>" else "LobbyAction<Action2>"
            stdAction(lobbyAction) {
              doTask("PartyDelegate<Scientists>")
            }
            stdAction("LobbyAction<Action2>") { doTask("PartyDelegate<Scientists>") }
          }
        }
      }
      listOf(first, other).forEach { player ->
        player.turn { stdAction("LobbyAction<Action2>") { doTask("PartyDelegate<Scientists>") } }
      }
      first.pass()
      other.pass()
      if (generation == 6) admin.count("Delegate<Neutral>") shouldBe 14
      first.wgt("VenusStep")
      if (generation == 1) first.doTask("OceanTile<Tharsis_1_2> BY Admin")
      if (generation == 2)
          players.forEach { player ->
            player.tasks.extract { it.id }.forEach { player.doTask("Heat", it) }
          }
      admin.doTask(reveals[generation % reveals.size])
      players.forEach { it.buyCards(0) }
    }
    admin.count("Delegate<Neutral>") shouldBe 14
    admin.count("Distant<TerraformingLobbying>") shouldBe 1
  }

  @Test
  internal fun `Solo Election requires five points rather than awarding an automatic first place`() {
    prepareSoloRankingEvent("SelfSufficiencyProgram")
    val rating = kim.count("TerraformRating")
    kim.pass()
    kim.wgt("VenusStep")
    kim.count("TerraformRating") shouldBe rating - 1
  }

  @Test
  internal fun `Solo Election combines buildings and city tiles to reach five`() {
    prepareSoloRankingEvent("SelfSufficiencyProgram")
    kim.playProject(Mine, 4)
    kim.playProject(PowerPlant, 4)
    kim.playProject(IndustrialMicrobes, 12)
    kim.playProject(BuildingIndustries, 6)
    kim.stdProject("CityProject") { placeTile(8, 8) }
    val rating = kim.count("TerraformRating")
    kim.pass()
    kim.wgt("VenusStep")
    kim.count("TerraformRating") shouldBe rating // Revision -1 and Election +1.
  }

  @Test
  internal fun `Solo Revolution does not penalize fewer than four Earth tags and influence`() {
    prepareSoloRankingEvent("WorldGovernmentDirectives")
    kim.playProject(EarthOffice, 1)
    kim.playProject(Sponsors, 3)
    kim.playProject(AcquiredCompany, 7)
    val rating = kim.count("TerraformRating")
    kim.pass()
    kim.wgt("VenusStep")
    kim.count("TerraformRating") shouldBe rating // Revision and Reds bonus cancel.
  }

  @Test
  internal fun `Solo Revolution penalizes reaching four Earth tags`() {
    prepareSoloRankingEvent("WorldGovernmentDirectives")
    kim.playProject(EarthOffice, 1)
    kim.playProject(Sponsors, 3)
    kim.playProject(AcquiredCompany, 7)
    kim.playProject(Cartel, 5)
    val rating = kim.count("TerraformRating")
    kim.pass()
    kim.wgt("VenusStep")
    kim.count("TerraformRating") shouldBe
        rating - 2 // Revision and Reds cancel; Revolution costs two.
  }

  private fun prepareSoloRankingEvent(event: String) {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 1)
    listOf(event, "ExploreFirstDirective", "MoralMovement").forEachIndexed { index, reveal ->
      kim.pass()
      kim.wgt("VenusStep")
      if (index == 1) kim.doTask("OceanTile<Tharsis_1_2> BY Admin")
      admin.doTask(reveal)
      kim.buyCards(0)
    }
  }

  private fun prepareDiversity() {
    prepareEvent("FreeAcademiaTreaty")
    kim.playProject(Research, 11)
    kim.playProject(PowerPlant, 4)
    kim.playProject(VestaShipyard, 15)
    kim.playProject(EarthOffice, 1)
    lobbyForInfluence()
  }

  private fun lobbyForInfluence() {
    kim.stdAction("LobbyAction") { doTask("PartyDelegate<MarsFirst>") }
    repeat(4) { kim.stdAction("LobbyAction<Action2>") { doTask("PartyDelegate<MarsFirst>") } }
  }

  private fun prepareEvent(event: String, setup: () -> Unit = {}) {
    newTestGame(
        addOptions = "TurmoilExpansion, Luna, Ceres, Triton, Ganymede, Callisto",
        playerCount = 2,
    )
    setup()
    // Reveal the subject in generation 1; it moves through Coming and Current before generation 4.
    listOf(
            event,
            if (event == "ExploreFirstDirective") "ScientificProgress" else "ExploreFirstDirective",
            if (event == "MoralMovement") "BioengineeringBoom" else "MoralMovement",
        )
        .forEachIndexed { index, reveal ->
          val first = players.single { it.count("StartToken") == 1 }
          first.pass()
          players.single { it != first }.pass()
          first.wgt("VenusStep")
          if (index == 1) first.doTask("OceanTile<Tharsis_1_2> BY Admin") // Democratic Reform.
          // Minimal Impact Policy removes that sole ocean automatically in generation 3.
          admin.doTask(reveal)
          players.forEach { it.buyCards(0) }
        }
    stan.pass() // Kim may now prepare the subject's relevant position in generation 4.
  }

  private fun prepareTemperatureEvent(event: String, temperature: Int) {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    val laterEvents =
        listOf(
            "RedResistance",
            "CharismaticWgPresident",
            "BioengineeringBoom",
            "MiningRestrictions",
        )
    repeat(temperature) { index ->
      val first = players.single { it.count("StartToken") == 1 }
      first.pass()
      players.single { it != first }.pass()
      first.wgt("TemperatureStep")
      if (index == 1) first.doTask("OceanTile<Tharsis_1_2> BY Admin") // Democratic Reform.
      if (index == 14) first.doTask("OceanTile<Tharsis_1_2> BY Admin") // Temperature threshold.
      admin.doTask(if (index == temperature - 3) event else laterEvents[index % laterEvents.size])
      players.forEach { it.buyCards(0) }
    }
    admin.count("Current<$event>") shouldBe 1
    admin.count("TemperatureStep") shouldBe temperature
  }
}
