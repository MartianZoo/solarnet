package dev.martianzoo.benchmarks

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GameConfig
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
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
  private lateinit var workflow: TfmWorkflow.Manual
  private lateinit var beforeCorporationPhase: Checkpoint

  @Setup(Level.Trial)
  public fun setUp() {
    game =
        Engine.newGame(
            Canon.gamePremise(
                GameConfig(
                    "TerraformingMars, TharsisMapOption, PreludeExpansion, " +
                        "ColoniesExpansion, PromoCardPack, Callisto, Ganymede, " +
                        "Luna",
                    "Me",
                )
            )
        )
    me = game.tfm(PLAYER1)
    val engine = game.tfm(ENGINE)
    workflow = TfmWorkflow.Manual(game)

    workflow.setupPhase()
    engine.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    engine.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    engine.doTask("CityTile<Tharsis_5_8, SoloOpponent>")
    engine.doTask("GreeneryTile<Tharsis_5_7, SoloOpponent>")

    beforeCorporationPhase = game.timeline.checkpoint()
  }

  @Benchmark
  public fun corporationThroughFirstActionPhase(): Int {
    workflow.corporationPhase()
    me.playCorp(cn("Teractor"), 10)

    workflow.preludePhase()
    me.playPrelude(cn("HeadStart")) {
      doTask("UseAction<PlayCardSA, First>")
      doTask("PlayCard<Class<ProjectCard>, Class<EarthOffice>>")
      me.pay(0)
      doTask("UseAction<PlayCardSA, First>")
      doTask("PlayCard<Class<ProjectCard>, Class<HeavyTaxation>>")
      me.pay(0)
    }
    me.playPrelude(cn("NewPartner")) {
      me.playPrelude(cn("Merger")) {
        doTask("PlayCard<Class<CorporationCard>, Class<ValleyTrust>>")
      }
    }

    workflow.actionPhase()
    // Jacob Fryxelius's ruling makes Valley Trust's mandate the first action-phase action.
    // https://boardgamegeek.com/thread/3055761/article/41996773#41996773
    me.stdAction("HandleMandates") {
      me.playPrelude(cn("DoubleDown")) {
        doTask("CopyPrelude<HeadStart>")
        doTask("UseAction<PlayCardSA, First>")
        doTask("PlayCard<Class<ProjectCard>, Class<LunaGovernor>>")
        me.pay(0)
        doTask("UseAction<PlayCardSA, First>")
        doTask("PlayCard<Class<ProjectCard>, Class<ProductiveOutpost>>")
        me.pay(0)
      }
    }
    return me.count("CardFront")
  }

  @TearDown(Level.Invocation)
  public fun rollBack() {
    // Teractor + Valley Trust, four Preludes, and four projects.
    check(me.count("CardFront") == 10)
    check(me.count("Megacredit") == 65)
    game.timeline.rollBack(beforeCorporationPhase)
  }
}
