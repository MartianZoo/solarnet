package dev.martianzoo.tfm.benchmarks

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Exceptions.IncompleteAnalysisException
import dev.martianzoo.engine.World
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.TfmEngine
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown

/** Compares a raw transaction and a complete manual-Player Agent call on identical fixed states. */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public open class TransactionInferenceBenchmark {
  @Param("uniqueGain", "optionalCardRemoval", "optionalCardRemovalWide")
  public lateinit var scenario: String
  private lateinit var game: World
  private lateinit var player: Agent
  private var task: TaskId? = null
  private lateinit var start: Checkpoint

  @Setup(Level.Trial)
  public fun setUp() {
    game =
        when (scenario) {
          "uniqueGain" -> {
            // Every rejected candidate changes components before its immediate effect fails.
            val rejected =
                (1..63).joinToString("\n") {
                  "CLASS Rejected$it : Choice { This:: Notice!, -Missing! }"
                }
            Engine.newGame(
                testGamePremise(
                    "ABSTRACT CLASS Choice\n$rejected\n" +
                        "CLASS Allowed : Choice { This:: Notice! }\nCLASS Notice\nCLASS Missing"
                )
            )
          }
          "optionalCardRemoval",
          "optionalCardRemovalWide" ->
              TfmEngine.newGame(
                  TfmCatalog(Canon)
                      .gamePremise(GameConfig("TerraformingMars, TharsisMap", "Player1", "Player2"))
              )
          else -> error("unknown scenario: $scenario")
        }
    player = Agents(game) { NONE }[PLAYER1]
    if (scenario == "optionalCardRemoval") player.sneak("AcquiredCompany")
    // Two present types keep removal abstract, exercising the broader concrete-card domain.
    if (scenario == "optionalCardRemovalWide") player.sneak("Zeppelins, Worms")
    task =
        player
            .addTasks(if (scenario == "uniqueGain") "Choice!" else "-1? CardFront<Player1>")
            .single()
    start = game.timeline.checkpoint()
    game.actorEngine(PLAYER1).selectTask(checkNotNull(task))
    val selected = game.tasks.getTaskData(checkNotNull(task)).instruction as Change
    val target = checkNotNull(selected.gaining ?: selected.removing)
    val domain = game.classTable.allConcreteSubtypes(player.reader.resolve(target)).toList()
    val optional = scenario != "uniqueGain"
    // For these fixed cases, all 64 gains are examined; optional removal stops after zero and
    // the first present card succeed. Report the selected domain, after existing type deduction.
    val attempts =
        if (!optional) domain.size
        else
            2 +
                domain
                    .indexOfFirst { player.count(it.expression.toString()) > 0 }
                    .also {
                      check(it >= 0)
                    }
    println(
        "inference\tscenario=$scenario\tcandidates=${domain.size + if (optional) 1 else 0}\tattempts=$attempts"
    )
    game.timeline.seek(start)
    game.timeline.truncate()
  }

  @Benchmark
  public fun rawTransaction(): Int = attempt {
    game.actorEngine(PLAYER1).doTask(checkNotNull(task))
  }

  @Benchmark public fun completeAgentCall(): Int = attempt { player.doTask(checkNotNull(task)) }

  private fun attempt(block: () -> Unit): Int {
    try {
      block()
      check(scenario == "uniqueGain") { "optional removal must retain its zero/positive choice" }
    } catch (e: NotFullySpecifiedException) {
      check(scenario != "uniqueGain" && e !is IncompleteAnalysisException) { e }
      check(e.detail == "multiple successful concrete narrowings") { e }
    }
    return game.events.nextOrdinal
  }

  @TearDown(Level.Invocation)
  public fun restore() {
    if (scenario == "uniqueGain") {
      check(player.count("Choice") == 1 && player.count("Notice") == 1)
      check(game.tasks.isEmpty())
      game.timeline.seek(start)
      game.timeline.truncate()
    } else {
      val expectedCard = if (scenario == "optionalCardRemoval") "AcquiredCompany" else "Zeppelins"
      check(player.count(expectedCard) == 1 && checkNotNull(task) in player.tasks)
      if (scenario == "optionalCardRemovalWide") check(player.count("Worms") == 1)
      check(game.timeline.checkpoint() == start)
    }
  }
}
