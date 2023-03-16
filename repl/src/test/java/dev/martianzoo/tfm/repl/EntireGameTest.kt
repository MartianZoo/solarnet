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

    repl.command("as P1 exec LakefrontResorts, -9, 3 ProjectCard")
    repl.command("as P2 exec InterplanetaryCinematics, 8 ProjectCard, -24")

    repl.command("as P1 exec MartianIndustries, GalileanMining")
    repl.command("as P2 exec MiningOperations, UnmiContractor")

    repl.command("as P1 exec -30, AsteroidMining")

    repl.command("become P2")
    repl.command("exec -4 S, -1, NaturalPreserve")
    repl.command("task B drop") // TODO reifying refinements
    repl.command("exec Tile044<E37>")
    repl.command("exec -13 S, -1, SpaceElevator")
    repl.command("exec UA1<SpaceElevator>")
    repl.command("exec -2, InventionContest")
    repl.command("task B") // TODO it should do these even if A messed up
    repl.command("task C")
    repl.command("exec -SCT<P2, InventionContest<P2>>") // TODO how to remove tags?
    repl.command("task A")
    repl.command("exec -6, GreatEscarpmentConsortium")
    repl.command("task A PROD[-S<P1>]")
    repl.command("task B")

    repl.command("become")
    repl.command("exec ProductionPhase") // TODO Generation

    repl.command("as P1 exec -12, 4 ProjectCard")
    repl.command("as P2 exec -3, ProjectCard")

    repl.command("become P2")
    repl.command("exec UA1<SpaceElevator>")
    repl.command("exec -23, EarthCatapult")

    repl.command("become P1")
    repl.command("exec -7, TitaniumMine")
    repl.command("exec -9, RoboticWorkforce")
    repl.command("task A drop") // TODO reify?
    repl.command("exec @copyProductionBox(MartianIndustries)")
    repl.command("task A PROD[E]") // TODO why didn't autoexec?
    repl.command("task B PROD[S]")

    repl.command("become P2")
    repl.command("exec -5 S, IndustrialMicrobes")
    repl.command("exec -T, TechnologyDemonstration")
    repl.command("task B") // TODO should have happened anyway
    repl.command("task C")
    repl.command("exec -SCT<TechnologyDemonstration>") // TODO remove tags
    repl.command("exec -SPT<TechnologyDemonstration>")
    repl.command("task A")

    repl.command("as P1 exec -6, Sponsors")

    repl.command("exec -1, EnergyTapping")
    repl.command("task B")
    repl.command("task A PROD[-E<P1>]")
    repl.command("exec -2 S, BuildingIndustries")

    repl.command("become")
    repl.command("exec ProductionPhase")

    repl.command("as P1 exec -9, 3 ProjectCard")
    repl.command("as P2 exec -6, 2 ProjectCard")

    repl.command("as P1 exec -2, -1 S, Mine")

    repl.command("become P2")
    repl.command("exec UA1<SpaceElevator>")
    repl.command("exec -5, -5 S, ElectroCatapult")
    repl.command("exec UA1<ElectroCatapult>")
    repl.command("task A -Steel THEN 7") // TODO recognize one is impossible
    repl.command("exec -T, -7, SpaceHotels")
    repl.command("exec -6, MarsUniversity")
    repl.command("exec -10, ArtificialPhotosynthesis")
    repl.command("task B") // TODO huh?
    repl.command("task A drop") // TODO wha hapnd?
    repl.command("exec PROD[2 E]")
    repl.command("exec -5, BribedCommittee")
    repl.command("task B")
    repl.command("task C")
    repl.command("exec -EarthTag<BribedCommittee>")
    repl.command("task A")

    repl.command("become")
    repl.command("exec ProductionPhase")

    repl.command("as P1 exec -9, 3 ProjectCard")

    repl.command("become P2")
    repl.command("exec -6, 2 ProjectCard")
    repl.command("exec UA1<ElectroCatapult>")
    repl.command("task A drop") // TODO just one
    repl.command("exec -S THEN 7")
    repl.command("exec UA1<SpaceElevator>")

    repl.command("become P1")
    repl.command("exec -2 S, -14, ResearchOutpost")
    repl.command("task A drop")
    repl.command("exec CityTile<E56>") // reif refi
    repl.command("exec -13 T, -1, IoMiningIndustries")

    repl.command("become P2")
    repl.command("exec -T, -1, TransNeptuneProbe")
    repl.command("exec -1, Hackers")
    repl.command("task C")
    repl.command("task B PROD[-2 Megacredit<Player1>]")

    repl.command("become P1")
    repl.command("exec UA1<SellPatents>")
    repl.command("task A Megacredit FROM ProjectCard")

    repl.command("become P2")
    repl.command("exec -4 S, -1, SolarPower")
    repl.command("exec UA1<UseStandardProject>")
    repl.command("task A UA1<CitySP>")
    repl.command("task B drop") // split
    repl.command("exec -25, CityTile<E65>, PROD[1]")
    repl.command("exec PROD[-P, E]") // CORRECTION TODO WHY

    repl.command("become")
    repl.command("exec ProductionPhase")

    // Player1

    repl.command("become P1")

    repl.assertCount("TR", 20)

    val prods = lookUpProductionLevels(repl.session.game.reader, cn("Player1").expr)
    assertThat(prods.values).containsExactly(2, 2, 7, 0, 1, 0).inOrder()

    assertThat(repl.counts("M, S, T, P, E, H")).containsExactly(34, 2, 8, 3, 1, 3).inOrder()

    assertThat(repl.counts("ProjectCard, CardFront, ActiveCard, AutomatedCard, PlayedEvent"))
        .containsExactly(5, 10, 1, 6, 0)

    assertThat(repl.counts("BUT, SPT, SCT, POT, EAT, JOT, PLT, MIT, ANT, CIT"))
        .containsExactly(5, 2, 2, 0, 1, 3, 0, 0, 0, 1)
        .inOrder()

    assertThat(repl.counts("CT, GT, ST"))
        .containsExactly(1, 0, 0)
        .inOrder()

    // Player2

    repl.command("become P2")

    repl.assertCount("TR", 25)

    val prods2 = lookUpProductionLevels(repl.session.game.reader, cn("Player2").expr)
    assertThat(prods2.values).containsExactly(8, 6, 1, 0, 2, 0).inOrder()

    assertThat(repl.counts("M, S, T, P, E, H")).containsExactly(47, 6, 1, 1, 2, 3).inOrder()

    assertThat(repl.counts("ProjectCard, CardFront, ActiveCard, AutomatedCard, PlayedEvent"))
        .containsExactly(3, 17, 4, 10, 3)

    assertThat(repl.counts("BUT, SPT, SCT, POT, EAT, JOT, PLT, MIT, ANT, CIT"))
        .containsExactly(9, 3, 4, 2, 3, 0, 0, 1, 0, 0)
        .inOrder()

    assertThat(repl.counts("CT, GT, ST"))
        .containsExactly(1, 0, 1)
        .inOrder()
  }

  private fun ReplSession.assertCount(text: String, i: Int) {
    assertThat(session.count(metric(text))).isEqualTo(i)
  }

  private fun ReplSession.counts(text: String): List<Int> =
      text.split(",").map { session.count(metric(it)) }
}
