package dev.martianzoo.tfm.benchmarks

import dev.martianzoo.agent.Agents
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.World
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.testsupport.PLAYER1
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public open class WorldForkBenchmark {
  private lateinit var game: World

  @Setup(Level.Trial)
  public fun setUp() {
    game = Engine.newGame(testGamePremise("CLASS Token"))
    val player = Agents(game)[PLAYER1]
    repeat(1_000) { player.runOperation("Token") }
    check(game.events.entriesSince(Checkpoint(0)).size >= 3_000)
  }

  @Benchmark public fun forkCompletedWorld(): World = Engine.fork(game)

  @Benchmark
  public fun forkAndReadHistory(): Int = Engine.fork(game).events.entriesSince(Checkpoint(0)).size
}
