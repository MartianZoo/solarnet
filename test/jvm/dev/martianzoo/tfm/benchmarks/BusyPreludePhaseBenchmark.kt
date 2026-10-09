package dev.martianzoo.tfm.benchmarks

import dev.martianzoo.agent.Agents
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.TfmEngine as Engine
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.fake.FakeCanon
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public open class BusyPreludePhaseBenchmark {
  private val fakeHeadStart = cn("FakeHeadStart")
  private lateinit var game: World
  private lateinit var me: TfmGameplay
  private lateinit var admin: TfmGameplay
  private lateinit var beforeCorporationPhase: Checkpoint

  @Setup(Level.Trial)
  public fun setUp() {
    game =
        Engine.newGame(
            TfmCatalog(Canon, FakeCanon)
                .gamePremise(
                    GameConfig(
                        "TerraformingMars, TharsisMap, PreludeExpansion, " +
                            "ColoniesExpansion, PromoCardPack, FakeStuffBundle, Callisto, Ceres, Ganymede, " +
                            "Luna",
                        "Player1",
                    )
                )
        )
    val agents = Agents(game)
    me = agents.tfm(PLAYER1)
    admin = agents.tfm(ADMIN)

    admin.beginOperation("SetupPhase FROM Phase")
    me.keepStartingProjects(10)
    me.doTask("-SelectedColonyTile<Class<Ceres>>")
    admin.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    admin.doTask("CityTile<Tharsis_5_8, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_5_7, SoloOpponent>")
    check(game.tasks.isEmpty()) { "benchmark setup left pending tasks:\n${game.tasks}" }

    beforeCorporationPhase = game.timeline.checkpoint()
  }

  @Benchmark
  public fun corporationThroughFirstActionPhase(): Int {
    admin.runOperation("CorporationPhase FROM Phase")
    me.playCorp(cn("Teractor"))

    admin.runOperation("PreludePhase FROM Phase")
    me.playPrelude(fakeHeadStart) {
      doTask("UseAction<PlayCardFromHandAction, Action1>")
      doTask("PlayCard<Class<ProjectCard>, Class<EarthOffice>, Hand>")
      me.pay(0)
      doTask("UseAction<PlayCardFromHandAction, Action1>")
      doTask("PlayCard<Class<ProjectCard>, Class<HeavyTaxation>, Hand>")
      me.pay(0)
    }
    me.playPrelude(cn("NewPartner")) {
      me.playPrelude(cn("Merger")) {
        doTask("PlayCard<Class<StandardCorporationCard>, Class<ValleyTrust>, Selecting>")
      }
    }

    admin.runOperation("ActionPhase FROM Phase")
    // Jacob Fryxelius's ruling makes Valley Trust's required action the first action-phase action.
    // https://boardgamegeek.com/thread/3055761/article/41996773#41996773
    me.stdAction("DoRequiredActionsAction") {
      me.playPrelude(cn("DoubleDown")) {
        doTask("CopyPrelude<$fakeHeadStart>")
        doTask("UseAction<PlayCardFromHandAction, Action1>")
        doTask("PlayCard<Class<ProjectCard>, Class<LunaGovernor>, Hand>")
        me.pay(0)
        doTask("UseAction<PlayCardFromHandAction, Action1>")
        doTask("PlayCard<Class<ProjectCard>, Class<ProductiveOutpost>, Hand>")
        me.pay(0)
      }
    }
    return me.count("CardFront")
  }

  @TearDown(Level.Invocation)
  public fun rollBack() {
    // Teractor + Valley Trust, four Preludes, and four projects.
    check(me.count("CardFront") == 10)
    val mc = me.count("MC")
    check(mc == 65) { "expected 65 MC, found $mc" }
    game.timeline.rollBack(beforeCorporationPhase)
  }
}
