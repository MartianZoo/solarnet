package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.ResourceUtils.lookUpProductionLevels
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Metric.Companion.metric
import org.junit.jupiter.api.Test

class EntireGameTest {
  @Test
  fun fourWholeGenerations() {
    val repl = ReplSession(Canon, GameSetup(Canon, "BREPT", 2))
    fun replit(s: String, tasksExpected: Int = 0) {
      repl.command(s)
      assertThat(repl.session.game.taskQueue.size).isEqualTo(tasksExpected)
    }

    replit("as P1 exec CorporationCard, LakefrontResorts, 3 BuyCard")
    replit("as P2 exec CorporationCard, InterplanetaryCinematics, 8 BuyCard")

    replit("as P1 exec 2 PreludeCard, MartianIndustries, GalileanMining")
    replit("as P2 exec 2 PreludeCard, MiningOperations, UnmiContractor")

    replit("exec ActionPhase")

    replit("as P1 exec -30 THEN AsteroidMining")

    replit("become P2")
    replit("exec -4 Steel THEN -1 THEN NaturalPreserve", 1)
    replit("task B drop") // TODO reifying refinements
    replit("exec Tile044<E37>")
    replit("exec -13 Steel THEN -1 THEN SpaceElevator")
    replit("exec UseAction1<SpaceElevator>")
    replit("exec -2 THEN InventionContest")
    replit("exec -6 THEN GreatEscarpmentConsortium", 1)
    replit("task A PROD[-Steel<P1>]", 0)

    replit("become")
    replit("exec ProductionPhase", 2)
    replit("as P1 task A 4 BuyCard", 1)
    replit("as P2 task B 1 BuyCard")
    replit("exec ActionPhase")

    replit("become P2")
    replit("exec UseAction1<SpaceElevator>")
    replit("exec -23 THEN EarthCatapult")

    replit("become P1")
    replit("exec -7 THEN TitaniumMine")
    replit("exec -9 THEN RoboticWorkforce", 1)
    replit("task A drop") // TODO reify?
    replit("exec @copyProductionBox(MartianIndustries)", 0)

    replit("become P2")
    replit("exec -5 Steel THEN IndustrialMicrobes")
    replit("exec -Titanium THEN TechnologyDemonstration")

    replit("as P1 exec -6 THEN Sponsors")

    replit("exec -1 THEN EnergyTapping", 1)
    replit("task A PROD[-Energy<P1>]")
    replit("exec -2 Steel THEN BuildingIndustries")

    replit("become")
    replit("exec ProductionPhase", 2)
    replit("as P1 task A 3 BuyCard", 1)
    replit("as P2 task B 2 BuyCard")
    replit("exec ActionPhase")

    replit("as P1 exec -2 THEN -1 Steel THEN Mine")

    replit("become P2")
    replit("exec UseAction1<SpaceElevator>")
    replit("exec -5 THEN -5 Steel THEN ElectroCatapult")
    replit("exec UseAction1<ElectroCatapult>", 1)
    replit("task A -Steel THEN 7") // TODO just one
    replit("exec -Titanium THEN -7 THEN SpaceHotels")
    replit("exec -6 THEN MarsUniversity")
    replit("exec -10 THEN ArtificialPhotosynthesis", 1)
    replit("task B PROD[2 Energy]")
    replit("exec -5 THEN BribedCommittee")

    replit("become")
    replit("exec ProductionPhase", 2)
    replit("as P1 task A 3 BuyCard", 1)
    replit("as P2 task B 2 BuyCard")
    replit("exec ActionPhase")

    replit("become P2")
    replit("exec UseAction1<ElectroCatapult>", 1)
    replit("task A drop") // TODO just one
    replit("exec -Steel THEN 7")
    replit("exec UseAction1<SpaceElevator>")

    replit("become P1")
    replit("exec -2 Steel THEN -14 THEN ResearchOutpost", 1)
    replit("task A drop")
    replit("exec CityTile<E56>") // TODO reif refi
    replit("exec -13 Titanium THEN -1 THEN IoMiningIndustries")

    replit("become P2")
    replit("exec -Titanium THEN -1 THEN TransNeptuneProbe")
    replit("exec -1 THEN Hackers", 1)
    replit("task B PROD[-2 Megacredit<P1>]")

    replit("become P1")
    replit("exec UseAction1<SellPatents>", 1)
    replit("task A Megacredit FROM ProjectCard")

    replit("become P2")
    replit("exec -4 Steel THEN -1 THEN SolarPower")
    replit("exec UseAction1<UseStandardProject>", 1)
    replit("task A UseAction1<CitySP>", 1)
    replit("task B drop") // split
    replit("exec -25 THEN CityTile<E65> THEN PROD[1]")
    replit("exec PROD[-Plant, Energy]") // CORRECTION TODO WHY

    replit("become")
    replit("exec ProductionPhase", 2)

    // Stuff
    assertThat(repl.counts("Generation")).containsExactly(5)
    assertThat(repl.counts("OceanTile, OxygenStep, TemperatureStep")).containsExactly(0, 0, 0)

    // P1

    repl.command("become P1")

    repl.assertCount("TerraformRating", 20)

    val prods = lookUpProductionLevels(repl.session.game.reader, cn("P1").expr)
    assertThat(prods.values).containsExactly(2, 2, 7, 0, 1, 0).inOrder()

    assertThat(repl.counts("M, Steel, Titanium, Plant, Energy, Heat"))
        .containsExactly(34, 2, 8, 3, 1, 3).inOrder()

    assertThat(repl.counts("ProjectCard, CardFront, ActiveCard, AutomatedCard, PlayedEvent"))
        .containsExactly(5, 10, 1, 6, 0)

    // tag abbreviations
    assertThat(repl.counts("BUT, SPT, SCT, POT, EAT, JOT, PLT, MIT, ANT, CIT"))
        .containsExactly(5, 2, 2, 0, 1, 3, 0, 0, 0, 1)
        .inOrder()

    assertThat(repl.counts("CityTile, GreeneryTile, SpecialTile"))
        .containsExactly(1, 0, 0)
        .inOrder()

    // P2

    repl.command("become P2")

    repl.assertCount("TerraformRating", 25)

    val prods2 = lookUpProductionLevels(repl.session.game.reader, cn("P2").expr)
    assertThat(prods2.values).containsExactly(8, 6, 1, 0, 2, 0).inOrder()

    assertThat(repl.counts("M, Steel, Titanium, Plant, Energy, Heat"))
        .containsExactly(47, 6, 1, 1, 2, 3).inOrder()

    assertThat(repl.counts("ProjectCard, CardFront, ActiveCard, AutomatedCard, PlayedEvent"))
        .containsExactly(3, 17, 4, 10, 3)

    // tag abbreviations
    assertThat(repl.counts("BUT, SPT, SCT, POT, EAT, JOT, PLT, MIT, ANT, CIT"))
        .containsExactly(9, 3, 4, 2, 3, 0, 0, 1, 0, 0)
        .inOrder()

    assertThat(repl.counts("CityTile, GreeneryTile, SpecialTile"))
        .containsExactly(1, 0, 1)
        .inOrder()
  }

  @Test
  fun ellieGame() {
    val repl = ReplSession(Canon, GameSetup(Canon, "BRHXP", 2))
    fun replit(s: String, tasksExpected: Int = 0) {
      repl.command(s)
      assertWithMessage("${repl.session.game.taskQueue}")
          .that(repl.session.game.taskQueue.size)
          .isEqualTo(tasksExpected)
    }

    replit("mode blue")

    replit("become P1")
    replit("exec Turn", 2)
    replit("task B InterplanetaryCinematics", 1)
    replit("task C 7 BuyCard")

    replit("become P2")
    replit("exec Turn", 2)
    replit("task B PharmacyUnion", 1)
    replit("task C 5 BuyCard")

    replit("become")
    replit("exec PreludePhase")

    replit("become P1")
    replit("exec Turn", 1)
    replit("task B UnmiContractor")
    replit("exec Turn", 1)
    replit("task B CorporateArchives")

    replit("become P2")
    replit("exec Turn", 1)
    replit("task B BiosphereSupport")
    replit("exec Turn", 1)
    replit("task B SocietySupport")

    replit("become")
    replit("exec ActionPhase")

    replit("mode green")

    replit("become P1")
    replit("exec -6 THEN MediaGroup")
    replit("exec -1 THEN Sabotage", 1)
    replit("task B -7 Megacredit<P2>")

    replit("become P2")
    replit("exec -11 THEN Research")
    replit("exec -9 THEN MartianSurvey", 1)
    replit("task A Ok")

    replit("exec -3 THEN SearchForLife", 1) // TODO: why does she have 3 of these??
    replit("task A PlayedEvent<Class<PharmacyUnion>> FROM PharmacyUnion THEN 3 TerraformRating")

    replit("exec UseAction1<UseActionFromCard>", 1)
    replit("task A UseAction1<SearchForLife> THEN ActionUsedMarker<SearchForLife>", 1)
    replit("task B -1 THEN Ok")

    replit("become")
    replit("exec ProductionPhase", 2)
    replit("as P1 task A BuyCard", 1)
    replit("as P2 task B 3 BuyCard")
    replit("exec ActionPhase")

    replit("become P2")
    replit("exec UseAction1<SellPatents>", 1)
    replit("task A Megacredit FROM ProjectCard") // TODO wrong
    replit("exec -15 THEN VestaShipyard") // TODO: handle negative cpt count without stacktracing

    replit("become P1") // TODO: Hi, null
    replit("exec -23 THEN EarthCatapult")
    replit("exec -4 Steel THEN OlympusConference", 1) // TODO: recognize when one option is impossible
    replit("task A Science<OlympusConference>")
    replit("exec -4 Steel THEN -1 THEN DevelopmentCenter", 1)
    replit("task A ProjectCard FROM Science<OlympusConference>")
    replit("exec -4 Steel THEN -1 THEN GeothermalPower")
    replit("exec -10 THEN MirandaResort")
    replit("exec -1 THEN Hackers", 1)
    replit("task B PROD[-2 M<P2>]")
    replit("exec -1 THEN MicroMills")

    replit("become")
    replit("exec ProductionPhase", 2)
    replit("as P1 task A 3 BuyCard", 1)
    replit("as P2 task B BuyCard") // TODO: why does she appear to have 3 SearchForLifes?")
    replit("exec ActionPhase")

    replit("become P1")
    replit("exec UseAction1<UseActionFromCard>", 1)
    replit("task A UseAction1<DevelopmentCenter> THEN ActionUsedMarker<DevelopmentCenter>")
    replit("exec -5 Steel THEN -1 THEN ImmigrantCity", 1) // TODO bug with choosing city location
    replit("task C drop")
    // exec CityTile<Hellas_9_7> // TODO ouchbug

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
  }
  private fun ReplSession.assertCount(text: String, i: Int) {
    assertThat(session.count(metric(text))).isEqualTo(i)
  }

  private fun ReplSession.counts(text: String): List<Int> =
      text.split(",").map { session.count(metric(it)) }
}
