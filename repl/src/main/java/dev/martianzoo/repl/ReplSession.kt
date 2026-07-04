package dev.martianzoo.repl

import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.SIGNAL
import dev.martianzoo.api.SystemClasses.SYSTEM
import dev.martianzoo.data.GameEvent.ChangeEvent
import dev.martianzoo.data.Player
import dev.martianzoo.data.Player.Companion.ENGINE
import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Game
import dev.martianzoo.engine.Gameplay.TurnLayer
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.repl.Access.BlueMode
import dev.martianzoo.repl.Access.GreenMode
import dev.martianzoo.repl.Access.PurpleMode
import dev.martianzoo.repl.Access.RedMode
import dev.martianzoo.repl.Access.YellowMode
import dev.martianzoo.repl.ReplSession.ReplMode.BLUE
import dev.martianzoo.repl.ReplSession.ReplMode.GREEN
import dev.martianzoo.repl.ReplSession.ReplMode.PURPLE
import dev.martianzoo.repl.ReplSession.ReplMode.RED
import dev.martianzoo.repl.ReplSession.ReplMode.YELLOW
import dev.martianzoo.repl.commands.AsCommand
import dev.martianzoo.repl.commands.StatusCommand
import dev.martianzoo.repl.commands.AutoCommand
import dev.martianzoo.repl.commands.BecomeCommand
import dev.martianzoo.repl.commands.CountCommand
import dev.martianzoo.repl.commands.DescCommand
import dev.martianzoo.repl.commands.ExecCommand
import dev.martianzoo.repl.commands.HasCommand
import dev.martianzoo.repl.commands.HelpCommand
import dev.martianzoo.repl.commands.HistoryCommand
import dev.martianzoo.repl.commands.ListCommand
import dev.martianzoo.repl.commands.LogCommand
import dev.martianzoo.repl.commands.ModeCommand
import dev.martianzoo.repl.commands.NewGameCommand
import dev.martianzoo.repl.commands.PhaseCommand
import dev.martianzoo.repl.commands.RollbackCommand
import dev.martianzoo.repl.commands.ScriptCommand
import dev.martianzoo.repl.commands.TaskCommand
import dev.martianzoo.repl.commands.TasksCommand
import dev.martianzoo.repl.commands.TurnCommand
import dev.martianzoo.tfm.canon.Canon.SIMPLE_GAME
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.repl.TfmColor
import dev.martianzoo.tfm.repl.TfmColor.ENERGY
import dev.martianzoo.tfm.repl.TfmColor.HEAT
import dev.martianzoo.tfm.repl.TfmColor.MEGACREDIT
import dev.martianzoo.tfm.repl.TfmColor.OCEAN_TILE
import dev.martianzoo.tfm.repl.TfmColor.PLANT
import dev.martianzoo.tfm.repl.commands.TfmBoardCommand
import dev.martianzoo.tfm.repl.commands.TfmMapCommand
import dev.martianzoo.tfm.repl.commands.TfmPayCommand
import dev.martianzoo.tfm.repl.commands.TfmPlayCommand
import dev.martianzoo.tfm.repl.commands.TfmSampleCommand
import dev.martianzoo.types.MType
import dev.martianzoo.util.toStrings

internal fun main(args: Array<String>) { // JVM entry point for the shadow JAR
  if ("--serve" in args) {
    ReplServer().run()
    return
  }
  val repl = ReplSession()
  repl.jline = JlineRepl(ReplCompleter(repl))
  repl.loop()
  println("Bye")
}

internal class ReplSession(internal var jline: JlineRepl? = null) {
  internal lateinit var setup: GameSetup
  internal lateinit var game: Game // TODO maybe remove and just have reader/events/...?
  internal lateinit var gameplay: TurnLayer

  internal var mode: ReplMode = GREEN

  internal fun newGame(setup: GameSetup, purple: Boolean = false) {
    this.setup = setup
    game = Engine.newGame(setup)
    gameplay = game.gameplay(ENGINE) as TurnLayer // default autoexec mode
    if (purple) {
      mode = PURPLE
      TfmWorkflow.Auto(game, setup).launch()
    }
  }

  init {
    newGame(SIMPLE_GAME)
  }

  internal fun loop() = jline!!.loop(::prompt, ::executeAll, welcome)

  private fun prompt() = mode.color.foreground(promptPlain())

  internal fun promptPlain(): String =
      with(gameplay) {
        val bundles = setup.bundles.joinToString("")
        val phase = list("Phase").single()
        val checkpoint = game.timeline.checkpoint()
        "$bundles $phase $player/${setup.players} @$checkpoint> "
      }

  private val inputRegex = Regex("""^\s*(\S+)(.*)$""")

  internal class UsageException(message: String? = null) : Exception(message ?: "")

  // Splits on semicolons and executes each chunk; used by both interactive and server modes.
  internal fun executeAll(input: String): List<String> {
    val allOutput = mutableListOf<String>()
    for (chunk in input.split(";").map { it.trim() }.filter { it.isNotEmpty() }) {
      val lines =
          try {
            command(chunk)
          } catch (e: Exception) {
            listOf("Error: ${e.message ?: e.toString()}")
          }
      allOutput += lines
      allOutput += ""
    }
    return allOutput
  }

  internal val commands =
      listOf(
              AsCommand(this),
              AutoCommand(this),
              BecomeCommand(this),
              TfmBoardCommand(this),
              CountCommand(this),
              DescCommand(this),
              ExecCommand(this),
              HasCommand(this),
              HelpCommand(this),
              HistoryCommand(this),
              ListCommand(this),
              LogCommand(this),
              TfmMapCommand(this),
              ModeCommand(this),
              NewGameCommand(this),
              PhaseCommand(this),
              RollbackCommand(this),
              ScriptCommand(this),
              StatusCommand(this),
              TaskCommand(this),
              TasksCommand(this),
              TurnCommand(this),
              TfmPayCommand(this),
              TfmPlayCommand(this),
              TfmSampleCommand(this),
          )
          .associateBy { it.name }

  internal fun access(): Access = // TODO maybe don't do this "just-in-time"...
  when (mode) {
        RED -> RedMode(gameplay.godMode())
        YELLOW -> YellowMode(gameplay.godMode())
        GREEN -> GreenMode(gameplay.godMode())
        BLUE -> BlueMode(gameplay.godMode())
        PURPLE -> PurpleMode(gameplay.godMode())
      }

  internal fun describeExecutionResults(result: TaskResult): List<String> {
    val changes = result.changes.filterNot { isSystem(it, game.reader) }.toStrings()

    val newTasks: Set<TaskId> = result.tasksSpawned
    val taskLines =
        if (newTasks.any()) {
          listOf("New tasks pending:") +
              game.tasks
                  .extract {
                    if (it.id in newTasks) it.toStringWithoutCause(queueOwner = it.owner) else null
                  }
                  .filterNotNull()
        } else {
          listOf()
        }
    return if (changes.none() && taskLines.none()) {
      listOf("um, nothing happened")
    } else if (changes.any() && taskLines.any()) {
      changes + listOf("") + taskLines
    } else {
      changes + taskLines
    }
  }

  internal fun isSystem(event: ChangeEvent, game: GameReader): Boolean {
    val g = event.change.gaining
    val r = event.change.removing

    val system = game.resolve(SYSTEM.expression)
    if (listOfNotNull(g, r).all { game.resolve(it).narrows(system) }) return true

    if (r != null) {
      val signal = game.resolve(SIGNAL.expression)
      if (game.resolve(r).narrows(signal)) return true
    }
    return false
  }

  internal fun command(wholeCommand: String): List<String> {
    val stripped = wholeCommand.replace(Regex("//.*"), "")
    val groups = inputRegex.matchEntire(stripped)?.groupValues
    return if (groups == null) {
      listOf()
    } else {
      val (_, commandName, arguments) = groups
      val args = arguments.trim().ifEmpty { null }
      val command = commands[commandName.lowercase()]
      if (command == null) {
        listOf("¯\\_(ツ)_/¯ Type `help` for help")
      } else {
        command(command, args)
      }
    }
  }

  internal fun command(command: ReplCommand, args: String? = null): List<String> {
    return try {
      if (args == null) command.noArgs() else command.withArgs(args.trim())
    } catch (e: RuntimeException) {
      throw e
    } catch (e: Exception) {
      val usage = if (e is UsageException) "Usage: ${command.usage}" else ""
      listOf(e.message ?: "", usage).filter { it.any() }
    }
  }

  internal enum class ReplMode(public val message: String, public val color: TfmColor) {
    RED("Change integrity: make changes without triggered effects", HEAT),
    YELLOW("Task integrity: changes have consequences", MEGACREDIT),
    GREEN("Operation integrity: clear task queue before starting new operation", PLANT),
    BLUE("Turn integrity: must perform a valid game turn for this phase", OCEAN_TILE),
    PURPLE("Game integrity: the engine fully controls the workflow", ENERGY),
  }

  internal fun player(name: String): Player {
    // In case a shortname was used
    val type: MType = game.reader.resolve(cn(name).expression) as MType
    return Player(type.className)
  }
}

private val welcome =
    """
      Welcome to REgo PLastics. Type `help` or `help <command>` for help.
      Warning: this is a bare-bones tool that is not trying to be easy to use... at all

    """
        .trimIndent()
