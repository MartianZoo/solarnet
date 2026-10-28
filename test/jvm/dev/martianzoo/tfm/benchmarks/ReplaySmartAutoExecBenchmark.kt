package dev.martianzoo.tfm.benchmarks

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy
import dev.martianzoo.agent.TaskLog
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.GameFileJson
import dev.martianzoo.state.Player
import dev.martianzoo.state.RecordedInput
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.DecisionSearch
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.fake.FakeCanon
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State

/**
 * Replays four of `OtbGame20260828Test`'s nine generations with manual Players, then times
 * generations five and six with SAFE Player autoexecution. The search variant probes recorded
 * Player choices through the shared [DecisionSearch] library. The export has no named-card or deck
 * inputs, so searches needing those facts return UNKNOWN. The default probes every eighth Player
 * choice; set [searchStride] to 1 to search every choice. The compressed fixture comes from
 * `:tfm-tests:replayTest -PreplayExports --tests
 * dev.martianzoo.tfm.tests.replays.OtbGame20260828Test`.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public open class ReplaySmartAutoExecBenchmark {
  private companion object {
    const val START_INPUT = 1800
    const val END_INPUT = 3207
  }

  @JvmField @Param("8") public var searchStride: Int = 8
  private lateinit var premise: dev.martianzoo.catalog.GamePremise
  private lateinit var inputs: List<RecordedInput>
  private lateinit var agents: Agents
  private lateinit var reference: Agents

  @Setup(Level.Trial)
  public fun loadReplay() = runBlocking {
    require(searchStride > 0)
    val resource = checkNotNull(javaClass.getResourceAsStream("/OtbGame20260828Test.short.json.gz"))
    val document =
        GameFileJson.parse(
            java.util.zip.GZIPInputStream(resource).bufferedReader().use { it.readText() }
        )
    val catalog = TfmCatalog(Canon, FakeCanon)
    premise = document.premise(catalog)
    inputs = document.inputs(premise)
    check((inputs[START_INPUT] as RecordedInput.Command).instruction == "Generation!")
    check((inputs[END_INPUT] as RecordedInput.Command).instruction == "Generation!")
    reference = TfmEngine.agents(TfmEngine.newGame(premise))
    TaskLog.replay(
        inputs.take(END_INPUT),
        reference,
        startGame = { reference.tfm(ADMIN).startGame() },
    )
  }

  @Setup(Level.Invocation)
  public fun setUp() {
    agents = TfmEngine.agents(TfmEngine.newGame(premise))
    TaskLog.replay(inputs.take(START_INPUT), agents, startGame = { agents.tfm(ADMIN).startGame() })
  }

  @Benchmark public fun safeGenerationsFiveAndSix(): Int = playGenerations(search = false)

  @Benchmark public fun sampledSmartGenerationsFiveAndSix(): Int = playGenerations(search = true)

  private fun playGenerations(search: Boolean): Int {
    val players = agents.world.actors.filterIsInstance<Player>()
    players.forEach { agents[it].autoExecPolicy = AutoExecPolicy.SAFE }
    players.forEach { agents[it].autoExecNow() }
    var skipped = 0
    var searches = 0
    var knownVerdicts = 0
    var playerChoices = 0
    inputs.subList(START_INPUT, END_INPUT).forEach { command ->
      if (
          command is RecordedInput.Command &&
              command.actor is Player &&
              command.kind == RecordedInput.Kind.CHOOSE
      ) {
        if (search && playerChoices % searchStride == 0) {
          agents.world.timeline.recordPosition()
          val verdict =
              DecisionSearch(agents, null).verdict { speculative ->
                TaskLog.replay(
                    listOf(RecordedInput.StartGame, command),
                    speculative,
                    startGame = {
                      players.forEach { speculative[it].autoExecPolicy = AutoExecPolicy.SAFE }
                    },
                )
              }
          searches++
          if (verdict != DecisionSearch.Verdict.UNKNOWN) knownVerdicts++
        }
        playerChoices++
      }
      try {
        TaskLog.replay(
            listOf(RecordedInput.StartGame, command),
            agents,
            startGame = {
              players.forEach { agents[it].autoExecPolicy = AutoExecPolicy.SAFE }
            },
        )
      } catch (failure: IllegalArgumentException) {
        val absent =
            failure.cause?.message?.let {
              it.startsWith("recorded task is absent:") || it.startsWith("no matching task;")
            } == true
        if (!absent) throw failure
        skipped++
      }
    }
    val expected = reference.world.reader.getComponents(COMPONENT.expression).entries
    val actual = agents.world.reader.getComponents(COMPONENT.expression).entries
    check(actual == expected) {
      "skipped $skipped; expected only ${expected - actual}; actual only ${actual - expected}"
    }
    if (search) {
      check(searches == (playerChoices + searchStride - 1) / searchStride && knownVerdicts > 0)
    }
    return agents.world.timeline.checkpoint().ordinal + knownVerdicts
  }
}
