package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
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

    // Not bothering to give CorporationCard & 2 PreludeCard then take them away again
    replit("as P1 exec LakefrontResorts, 3 BuyCard")
    replit("as P2 exec InterplanetaryCinematics, 8 BuyCard")

    replit("as P1 exec MartianIndustries, GalileanMining")
    replit("as P2 exec MiningOperations, UnmiContractor")

    replit("exec ActionPhase")

    replit("as P1 exec -30, AsteroidMining")

    replit("become P2")
    replit("exec -4 Steel, -1, NaturalPreserve", 1)
    replit("task B drop") // TODO reifying refinements
    replit("exec Tile044<E37>")
    replit("exec -13 Steel, -1, SpaceElevator")
    replit("exec UseAction1<SpaceElevator>")
    replit("exec -2, InventionContest", 1)
    replit("exec -SCT<P2, InventionContest<P2>>", 1) // TODO how to remove tags?
    replit("task A")
    replit("exec -6, GreatEscarpmentConsortium", 1)
    replit("task A PROD[-Steel<P1>]", 0)

    replit("become")
    replit("exec ProductionPhase FROM ActionPhase")
    replit("exec GenerationPhase FROM ProductionPhase")
    replit("exec ResearchPhase FROM GenerationPhase", 2)

    replit("as P1 task A 4 BuyCard", 1)
    replit("as P2 task B 1 BuyCard")
    replit("exec ActionPhase FROM ResearchPhase")

    replit("become P2")
    replit("exec UseAction1<SpaceElevator>")
    replit("exec -23, EarthCatapult")

    replit("become P1")
    replit("exec -7, TitaniumMine")
    replit("exec -9, RoboticWorkforce", 1)
    replit("task A drop") // TODO reify?
    replit("exec @copyProductionBox(MartianIndustries)", 0)

    replit("become P2")
    replit("exec -5 Steel, IndustrialMicrobes")
    replit("exec -Titanium, TechnologyDemonstration", 1)
    replit("exec -SCT<TechnologyDemonstration>", 1) // TODO remove tags
    replit("exec -SPT<TechnologyDemonstration>", 1)
    replit("task A")

    replit("as P1 exec -6, Sponsors")

    replit("exec -1, EnergyTapping", 1)
    replit("task A PROD[-Energy<P1>]")
    replit("exec -2 Steel, BuildingIndustries")

    replit("become")
    replit("exec ProductionPhase FROM ActionPhase")
    replit("exec GenerationPhase FROM ProductionPhase")
    replit("exec ResearchPhase FROM GenerationPhase", 2)

    replit("as P1 task A 3 BuyCard", 1)
    replit("as P2 task B 2 BuyCard")
    replit("exec ActionPhase FROM ResearchPhase")

    replit("as P1 exec -2, -1 Steel, Mine")

    replit("become P2")
    replit("exec UseAction1<SpaceElevator>")
    replit("exec -5, -5 Steel, ElectroCatapult")
    replit("exec UseAction1<ElectroCatapult>", 1)
    replit("task A -Steel THEN 7") // TODO just one
    replit("exec -Titanium, -7, SpaceHotels")
    replit("exec -6, MarsUniversity")
    replit("exec -10, ArtificialPhotosynthesis", 1)
    replit("task B PROD[2 Energy]")
    replit("exec -5, BribedCommittee", 1)
    replit("exec -EarthTag<BribedCommittee>", 1)
    replit("task A", 0)

    replit("become")
    replit("exec ProductionPhase FROM ActionPhase")
    replit("exec GenerationPhase FROM ProductionPhase")
    replit("exec ResearchPhase FROM GenerationPhase", 2)

    replit("as P1 task A 3 BuyCard", 1)
    replit("as P2 task B 2 BuyCard")
    replit("exec ActionPhase FROM ResearchPhase")

    replit("become P2")
    replit("exec UseAction1<ElectroCatapult>", 1)
    replit("task A drop") // TODO just one
    replit("exec -Steel THEN 7")
    replit("exec UseAction1<SpaceElevator>")

    replit("become P1")
    replit("exec -2 Steel, -14, ResearchOutpost", 1)
    replit("task A drop")
    replit("exec CityTile<E56>") // TODO reif refi
    replit("exec -13 Titanium, -1, IoMiningIndustries")

    replit("become P2")
    replit("exec -Titanium, -1, TransNeptuneProbe")
    replit("exec -1, Hackers", 1)
    replit("task B PROD[-2 Megacredit<P1>]", 0)

    replit("become P1")
    replit("exec UseAction1<SellPatents>", 0)

    replit("become P2")
    replit("exec -4 Steel, -1, SolarPower")
    replit("exec UseAction1<UseStandardProject>", 1)
    replit("task A UseAction1<CitySP>", 1)
    replit("task B drop") // split
    replit("exec -25, CityTile<E65>, PROD[1]")
    replit("exec PROD[-Plant, Energy]") // CORRECTION TODO WHY

    replit("become")
    replit("exec ProductionPhase FROM ActionPhase")
    replit("exec GenerationPhase FROM ProductionPhase")
    replit("exec ResearchPhase FROM GenerationPhase", 2)

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

  @Test fun ellieGame() {
    val repl = ReplSession(Canon, GameSetup(Canon, "BRHXP", 2))
    fun replit(s: String, tasksExpected: Int = 0) {
      repl.command(s)
      assertThat(repl.session.game.taskQueue.size).isEqualTo(tasksExpected)
    }

    replit("as P1 exec InterplanetaryCinematics, 7 BuyCard")
    replit("as P2 exec PharmacyUnion, -8, 5 BuyCard") // TODO: should have done the -8

    replit("as P1 exec UnmiContractor, CorporateArchives")
    replit("as P2 exec BiosphereSupport, SocietySupport")

    replit("exec ActionPhase")

    replit("become P1")
    replit("exec -6, MediaGroup")
    replit("exec -1, Sabotage", 1)
    replit("task B -7 Megacredit<Player2>")

    replit("become P2")
    replit("exec -11, Research", 2) // TODO: Pharma should have said Disease<Pharma>
    replit("task A TR<Player2> FROM Disease<P2, PharmacyUnion<P2>>", 1)
    // TODO: could have added 2 science tags at once
    replit("task B TR<Player2> FROM Disease<P2, PharmacyUnion<P2>>")
    replit("exec -9, MartianSurvey", 2)
    replit("exec -ScienceTag<MartianSurvey>", 2)
    replit("task B", 1)
    replit("task A drop") // TODO: should have been able to choose Ok, it stacktraced

    replit("exec -3, SearchForLife", 1) // TODO: why does she have 3 of these??
    replit("task A drop")
    replit("exec -2 MicrobeTag<PharmacyUnion>") // TODO: make automatic?
    replit("exec PlayedEvent<Class<Harvest>> FROM PharmacyUnion, 3 TR")
    // TODO: didn't let Pharma in eventpile

    replit("exec UseAction1<UseActionFromCard>", 1)
    replit("task A UseAction1<SearchForLife> THEN ActionUsedMarker<SearchForLife>", 1)
    replit("task B drop")
    replit("exec -1")

    replit("become")
    replit("exec ProductionPhase, GenerationPhase FROM ProductionPhase, " +
        "ResearchPhase FROM GenerationPhase", 2)
    replit("as P1 task A BuyCard", 1)
    replit("as P2 task B 3 BuyCard")
    replit("exec ActionPhase FROM ResearchPhase")

    replit("become P2")
    replit("exec UseAction1<SellPatents>")
    replit("exec -15, VestaShipyard") // TODO: handle negative cpt count without stacktracing

    replit("become P1") // TODO: Hi, null
    replit("exec -23, EarthCatapult")
    replit("exec -4 Steel, OlympusConference", 1) // TODO: recognize when one option is impossible
    replit("task A Science<OlympusConference>")
    replit("exec -4 Steel, -1, DevelopmentCenter", 1)
    replit("task A ProjectCard FROM Science<OlympusConference>")
    replit("exec -4 Steel, -1, GeothermalPower")
    replit("exec -10, MirandaResort")
    replit("exec -1, Hackers", 1)
    replit("task B PROD[-2 M<P2>]")
    replit("exec -1, MicroMills")

    // TODO: didn't get VPs from played events
    replit("become")
    replit("exec ProductionPhase FROM ActionPhase, GenerationPhase FROM ProductionPhase, " +
        "ResearchPhase FROM GenerationPhase", 2)
    replit("as P1 task A 3 BuyCard", 1)
    replit("as P2 task B BuyCard") // TODO: why does she appear to have 3 SearchForLifes?")
    replit("exec ActionPhase FROM ResearchPhase")

    replit("become P1")
    replit("exec UseAction1<UseActionFromCard>", 1)
    replit("task A UseAction1<DevelopmentCenter> THEN ActionUsedMarker<DevelopmentCenter>")
    replit("exec -5 Steel, -1, ImmigrantCity", 1) // TODO bug with choosing city location
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
        .containsExactly(22, 3, 0, 0, 0, 1).inOrder()

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
        .containsExactly(18, 0, 1, 6, 1, 3).inOrder()

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
