package dev.martianzoo.benchmarks

import dev.martianzoo.agent.createAgents
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.fake.FakeCanon
import dev.martianzoo.tfm.web.gameviewer.cardnames.FakeEstablishedMethods
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
  private lateinit var game: World
  private lateinit var me: TfmGameplay
  private lateinit var workflow: TfmWorkflow.Stepwise
  private lateinit var beforeCorporationPhase: Checkpoint

  @Setup(Level.Trial)
  public fun setUp() {
    game =
        Engine.newGame(
            TfmCatalog.compose(Canon, FakeCanon)
                .gamePremise(
                    GameConfig(
                        "TerraformingMars, TharsisMap, PreludeExpansion, " +
                            "ColoniesExpansion, PromoCardPack, FakeStuffBundle, Callisto, Ceres, Ganymede, " +
                            "Luna",
                        "Player1",
                    )
                )
        )
    val agents = createAgents(game)
    me = game.tfm(agents, PLAYER1)
    val admin = game.tfm(agents, ADMIN)
    workflow = TfmWorkflow.Stepwise(agents)

    workflow.setupPhase()
    me.doTask("-ColonyTileSelection<Class<Ceres>>")
    admin.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    admin.doTask("CityTile<Tharsis_5_8, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_5_7, SoloOpponent>")
    check(game.tasks.isEmpty()) { "benchmark setup left pending tasks:\n${game.tasks}" }

    beforeCorporationPhase = game.timeline.checkpoint()
  }

  @Benchmark
  public fun corporationThroughFirstActionPhase(): Int {
    workflow.corporationPhase()
    me.playCorp(cn("Teractor"), 10)

    workflow.preludePhase()
    me.playPrelude(FakeEstablishedMethods) {
      doTask("UseAction<PlayCardFromHandAction, Action1>")
      doTask("PlayCard<Class<ProjectCard>, Class<EarthOffice>, Hand>")
      me.pay(0)
      doTask("UseAction<PlayCardFromHandAction, Action1>")
      doTask("PlayCard<Class<ProjectCard>, Class<HeavyTaxation>, Hand>")
      me.pay(0)
    }
    me.playPrelude(cn("NewPartner")) {
      me.playPrelude(cn("Merger")) {
        doTask("PlayCard<Class<CorporationCard>, Class<ValleyTrust>, Selecting>")
      }
    }

    workflow.actionPhase()
    // Jacob Fryxelius's ruling makes Valley Trust's required action the first action-phase action.
    // https://boardgamegeek.com/thread/3055761/article/41996773#41996773
    me.stdAction("DoRequiredActionsAction") {
      me.playPrelude(cn("DoubleDown")) {
        doTask("CopyPrelude<$FakeEstablishedMethods>")
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
    check(mc == 89) { "expected 89 MC, found $mc" }
    game.timeline.rollBack(beforeCorporationPhase)
  }
}
