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
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.repl.TfmColor
import dev.martianzoo.tfm.repl.TfmColor.ENERGY
import dev.martianzoo.tfm.repl.TfmColor.HEAT
import dev.martianzoo.tfm.repl.TfmColor.MEGACREDIT
import dev.martianzoo.tfm.repl.TfmColor.OCEAN_TILE
import dev.martianzoo.tfm.repl.TfmColor.PLANT
import dev.martianzoo.tfm.repl.commands.BoardCommand
import dev.martianzoo.tfm.repl.commands.MapCommand
import dev.martianzoo.tfm.repl.commands.TfmPayCommand
import dev.martianzoo.tfm.repl.commands.TfmPlayCommand
import dev.martianzoo.tfm.repl.commands.TfmSampleCommand
import dev.martianzoo.types.MType
import dev.martianzoo.util.toStrings

internal fun main() {
  val jline = JlineRepl()
  val repl = ReplSession(jline)
  repl.loop()
  println("Bye")
}

internal class ReplSession(val jline: JlineRepl? = null) {
  lateinit var setup: GameSetup
  lateinit var game: Game
  lateinit var tfm: TfmGameplay

  var mode: ReplMode = GREEN

  fun newGame(setup: GameSetup) {
    this.setup = setup
    game = Engine.newGame(setup)
    tfm = game.tfm(ENGINE) // default autoexec mode
  }

  init {
    newGame(SIMPLE_GAME)
  }

  fun loop() = jline!!.loop(::prompt, ::command, welcome)

  private fun prompt(): String {
    return with(tfm) {
      val bundles = setup.bundles.joinToString("")
      val phase = list("Phase").single()
      val checkpoint = game.timeline.checkpoint()
      mode.color.foreground("$bundles $phase $player/${setup.players} @$checkpoint> ")
    }
  }

  private val inputRegex = Regex("""^\s*(\S+)(.*)$""")

  internal class UsageException(message: String? = null) : Exception(message ?: "")

  internal val commands =
      listOf(
              AsCommand(this),
              AutoCommand(this),
              BecomeCommand(this),
              BoardCommand(this),
              CountCommand(this),
              DescCommand(this),
              ExecCommand(this),
              HasCommand(this),
              HelpCommand(this),
              HistoryCommand(this),
              ListCommand(this),
              LogCommand(this),
              MapCommand(this),
              ModeCommand(this),
              NewGameCommand(this),
              PhaseCommand(this),
              RollbackCommand(this),
              ScriptCommand(this),
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
        RED -> RedMode(tfm.godMode())
        YELLOW -> YellowMode(tfm.godMode())
        GREEN -> GreenMode(tfm.godMode())
        BLUE -> BlueMode(tfm.godMode())
        PURPLE -> PurpleMode(tfm.godMode())
      }

  fun describeExecutionResults(result: TaskResult): List<String> {
    val changes = result.changes.filterNot { isSystem(it, tfm.reader) }.toStrings()

    val newTasks: Set<TaskId> = result.tasksSpawned
    val taskLines =
        if (newTasks.any()) {
          listOf("New tasks pending:") +
              tfm.game.tasks
                  .extract { if (it.id in newTasks) it.toStringWithoutCause() else null }
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

  fun isSystem(event: ChangeEvent, game: GameReader): Boolean {
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

  public fun command(wholeCommand: String): List<String> {
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

  public enum class ReplMode(val message: String, val color: TfmColor) {
    RED("Change integrity: make changes without triggered effects", HEAT),
    YELLOW("Task integrity: changes have consequences", MEGACREDIT),
    GREEN("Operation integrity: clear task queue before starting new operation", PLANT),
    BLUE("Turn integrity: must perform a valid game turn for this phase", OCEAN_TILE),
    PURPLE("Game integrity: the engine fully controls the workflow", ENERGY),
  }

  fun player(name: String): Player {
    // In case a shortname was used
    val type: MType = tfm.reader.resolve(cn(name).expression) as MType
    return Player(type.className)
  }
}

private val welcome =
    """
      Welcome to REgo PLastics. Type `help` or `help <command>` for help.
      Warning: this is a bare-bones tool that is not trying to be easy to use... at all

    """
        .trimIndent()
