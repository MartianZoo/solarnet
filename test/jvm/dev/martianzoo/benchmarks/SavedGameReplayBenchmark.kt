package dev.martianzoo.benchmarks

import dev.martianzoo.tfm.web.gameviewer.games.OtbGame20260825
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State

/** Measures the replay work performed before the game viewer can render a selected saved game. */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(jvmArgsAppend = ["-Xmixed"])
public open class SavedGameReplayBenchmark {
  @Benchmark public fun replayAugust25Game(): Int = OtbGame20260825().record().positions.size
}
