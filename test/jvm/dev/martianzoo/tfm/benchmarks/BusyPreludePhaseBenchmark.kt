package dev.martianzoo.tfm.benchmarks

import dev.martianzoo.agent.Agents
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.TfmEngine
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
  private lateinit var beforeCorporationPlay: Checkpoint

  @Setup(Level.Trial)
  public fun setUp() {
    game =
        TfmEngine.newGame(
            TfmCatalog(Canon, FakeCanon)
                .gamePremise(
                    GameConfig(
                        "TerraformingMars, TharsisMap, PreludeExpansion, " +
                            "ColoniesExpansion, promos:cards, fakes:cards, Callisto, Ceres, Ganymede, " +
                            "Luna",
                        "Player1",
                    )
                )
        )
    val agents = Agents(game)
    me = agents.tfm(PLAYER1)
    val admin = agents.tfm(ADMIN)

    admin.startGame()
    me.keepStartingProjects(10)
    me.doTask("-SelectedColonyTile<Class<Ceres>>")
    admin.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    admin.doTask("CityTile<Tharsis_5_8, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_5_7, SoloOpponent>")
    check(admin.count("CorporationPhase") == 1) {
      "benchmark setup did not reach the corporation phase:\n${game.tasks}"
    }

    beforeCorporationPlay = game.timeline.checkpoint()
  }

  @Benchmark
  public fun corporationThroughFirstActionPhase(): Int {
    me.playCorp(cn("Teractor"))

    me.playPrelude(fakeHeadStart) {
      doTask("PlayProject<Class<EarthOffice>>")
      me.pay(0)
      doTask("PlayProject<Class<HeavyTaxation>>")
      me.pay(0)
    }
    me.playPrelude(cn("NewPartner")) {
      me.playPrelude(cn("Merger")) {
        doTask("PlayCard<Class<CorporationCard>, Class<ValleyTrust>, Selecting>")
      }
    }

    // Jacob Fryxelius's ruling makes Valley Trust's required action the first action-phase action.
    // https://boardgamegeek.com/thread/3055761/article/41996773#41996773
    me.stdAction("RequiredActionsSignal") {
      me.playPrelude(cn("DoubleDown")) {
        doTask("CopyPrelude<$fakeHeadStart>")
        doTask("PlayProject<Class<LunaGovernor>>")
        me.pay(0)
        doTask("PlayProject<Class<ProductiveOutpost>>")
        me.pay(0)
      }
    }
    return me.count("CardFront")
  }

  /** A realistic live World with many distinct Terraforming Mars component and effect types. */
  @Benchmark public fun forkBusySetupWorld(): World = Engine.fork(game)

  @TearDown(Level.Invocation)
  public fun rollBack() {
    if (game.timeline.checkpoint() == beforeCorporationPlay) return
    // Teractor + Valley Trust, four Preludes, and four projects.
    check(me.count("CardFront") == 10)
    val mc = me.count("MC")
    check(mc == 65) { "expected 65 MC, found $mc" }
    game.timeline.seek(beforeCorporationPlay)
    game.timeline.truncate()
  }
}
