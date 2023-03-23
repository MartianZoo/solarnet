package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.api.Exceptions.UserException
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Actor
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.Result
import dev.martianzoo.tfm.pets.Parsing.parseAsIs
import dev.martianzoo.tfm.pets.Parsing.parseInput
import dev.martianzoo.tfm.pets.PetFeature.DEFAULTS
import dev.martianzoo.tfm.pets.PetFeature.SHORT_NAMES
import dev.martianzoo.tfm.pets.Raw
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.repl.ReplSession.ReplMode.BLUE
import dev.martianzoo.tfm.repl.ReplSession.ReplMode.GREEN
import dev.martianzoo.tfm.repl.ReplSession.ReplMode.PURPLE
import dev.martianzoo.tfm.repl.ReplSession.ReplMode.RED
import dev.martianzoo.tfm.repl.ReplSession.ReplMode.YELLOW
import dev.martianzoo.util.Multiset
import dev.martianzoo.util.toStrings
import java.io.File
import org.jline.reader.History

internal fun main() {
  val jline = JlineRepl()
  val repl = ReplSession(Canon, GameSetup(Canon, "BRM", 2), jline)
  val session = repl.session

  fun prompt(): String {
    val count = session.game.setup.players
    val bundles = session.game.setup.bundles.joinToString("")
    val phase = session.list(parseInput("Phase")).singleOrNull()
    val logPosition = repl.session.game.eventLog.size
    val actor = repl.session.agent.actor
    return repl.mode.color.foreground("$bundles $phase $actor/$count @$logPosition> ")
  }

  // We don't actually have to start another game.....
  val welcome =
      """
      Welcome to REgo PLastics. Type `help` for help.
      Warning: this is a bare-bones tool that is not trying to be easy to use... at all

  """
          .trimIndent()

  jline.loop(::prompt, repl::command, welcome)
  println("Bye")
}

/** A programmatic entry point to a REPL session that is more textual than [ReplSession]. */
public class ReplSession(
    private val authority: Authority,
    initialSetup: GameSetup,
    private val jline: JlineRepl? = null
) {
  internal var session = InteractiveSession(Engine.newGame(initialSetup))
  internal var mode: ReplMode = GREEN

  public enum class ReplMode(val message: String, val color: TfmColor) {
    RED("Arbitrary state changes with few restrictions", TfmColor.HEAT),
    YELLOW("Arbitrary state changes, within limits", TfmColor.MEGACREDIT),
    GREEN("Arbitrary state changes, triggering effects", TfmColor.PLANT),
    BLUE("Can only perform valid game actions", TfmColor.OCEAN_TILE),
    PURPLE("The engine fully orchestrates everything", TfmColor.ENERGY),
  }

  private val inputRegex = Regex("""^\s*(\S+)(.*)$""")

  private class UsageException(message: String? = null) : UserException(message ?: "")

  internal abstract inner class ReplCommand(val name: String) {
    open val isReadOnly: Boolean = false
    abstract val usage: String
    open fun noArgs(): List<String> = throw UsageException()

    open fun withArgs(args: String): List<String> = throw UsageException()
  }

  internal val commands =
      listOf(
              AsCommand(),
              AutoCommand(),
              BecomeCommand(),
              BoardCommand(),
              CountCommand(),
              DescCommand(),
              ExecCommand(),
              HasCommand(),
              HelpCommand(),
              HistoryCommand(),
              ListCommand(),
              LogCommand(),
              MapCommand(),
              ModeCommand(),
              NewGameCommand(),
              RollbackCommand(),
              ScriptCommand(),
              TaskCommand(),
              TasksCommand(),
          )
          .associateBy { it.name }

  internal inner class HelpCommand : ReplCommand("help") {
    override val usage = "help [command]"
    override val isReadOnly = true
    override fun noArgs() = listOf(helpText)
    override fun withArgs(args: String) = TODO()
  }

  internal inner class AsCommand : ReplCommand("as") {
    override val usage = "as <PlayerN> <full command>"
    override fun noArgs() = throw UsageException()
    override fun withArgs(args: String): List<String> {
      val (player, rest) = args.trim().split(Regex("\\s+"), 2)

      // TODO this is not awesome
      val saved = session
      return try {
        session = session.asActor(Actor(cn(player)))
        command(rest)
      } finally {
        session = saved
      }
    }
  }

  internal inner class NewGameCommand : ReplCommand("newgame") {
    override val usage = "newgame <bundles> <player count>"

    override fun withArgs(args: String): List<String> {
      try {
        val (bundleString, players) = args.trim().split(Regex("\\s+"), 2)

        val newGame = Engine.newGame(GameSetup(authority, bundleString, players.toInt()))
        session = InteractiveSession(newGame)
        return listOf("New $players-player game created with bundles: $bundleString") +
            if (players.toInt() == 1) {
              listOf("NOTE: No solo mode rules are implemented.")
            } else {
              listOf()
            }
      } catch (e: RuntimeException) {
        throw UsageException(e.message)
      }
    }
  }

  internal inner class BecomeCommand : ReplCommand("become") {
    override val usage = "become [PlayerN]"

    override fun noArgs(): List<String> {
      session = session.asActor(Actor.ENGINE)
      return listOf("Okay, you are the game engine now")
    }

    override fun withArgs(args: String): List<String> {
      session = session.asActor(Actor(cn(args)))
      return listOf("Hi, ${session.agent.actor}")
    }
  }

  internal inner class HasCommand : ReplCommand("has") {
    override val usage = "has <Requirement>"
    override val isReadOnly = true

    override fun withArgs(args: String): List<String> {
      val reqt: Raw<Requirement> = parseInput(args, session.features)
      val result = session.has(reqt)
      return listOf("$result: ${session.prep(reqt)}")
    }
  }

  internal inner class CountCommand : ReplCommand("count") {
    override val usage = "count <Metric>"
    override val isReadOnly = true

    override fun withArgs(args: String): List<String> {
      val metric: Raw<Metric> = parseInput(args, session.features)
      val count = session.count(metric)
      return listOf("$count ${session.prep(metric)}")
    }
  }

  internal inner class ListCommand : ReplCommand("list") {
    override val usage = "list <Expression>"
    override val isReadOnly = true
    override fun noArgs() = withArgs(COMPONENT.toString())

    override fun withArgs(args: String): List<String> {
      val expr: Raw<Expression> = parseInput(args)
      val counts: Multiset<Expression> = session.list(expr)
      return listOf("${counts.size} ${session.prep(expr)}") +
          counts.entries.sortedByDescending { (_, ct) -> ct }.map { (e, ct) -> "  $ct $e" }
    }
  }

  internal inner class BoardCommand : ReplCommand("board") {
    override val usage = "board [PlayerN]"
    override val isReadOnly = true

    override fun noArgs(): List<String> = PlayerBoardToText(session.agent, jline != null).board()

    override fun withArgs(args: String) =
        PlayerBoardToText(session.agent(cn(args)), jline != null).board()
  }

  internal inner class MapCommand : ReplCommand("map") {
    override val usage = "map"
    override val isReadOnly = true
    override fun noArgs() = MapToText(session.game.reader, jline != null).map()
  }

  internal inner class ModeCommand : ReplCommand("mode") {
    override val usage = "mode <mode name>"
    override fun noArgs() = listOf("Mode $mode: ${mode.message}")

    override fun withArgs(args: String): List<String> {
      try {
        val thing = ReplMode.valueOf(args.uppercase())
        mode = thing
      } catch (e: Exception) {
        throw UsageException(
            "Valid modes are: ${ReplMode.values().joinToString { it.toString().lowercase() } }")
      }
      return noArgs()
    }
  }

  var auto: Boolean = true

  internal inner class AutoCommand : ReplCommand("auto") {
    override val usage = "auto [ on | off ]"
    override fun noArgs() = listOf("Autoexecute is " + if (auto) "on" else "off")

    override fun withArgs(args: String): List<String> {
      auto =
          when (args) {
            "on" -> true
            "off" -> false
            else -> throw UsageException()
          }
      return noArgs()
    }
  }

  internal inner class ExecCommand : ReplCommand("exec") {
    override val usage = "exec <Instruction>"

    override fun withArgs(args: String): List<String> {
      val instruction = args
      val changes: Result =
          when (mode) {
            RED -> session.sneakyChange(instruction)
            YELLOW -> session.sneakyChange(instruction)
            GREEN -> initiate(instruction)
            BLUE -> {
              val instr: Instruction = parseAsIs(instruction)
              if (instr is Gain && instr.gaining.className == cn("Turn")) {
                initiate(instruction)
              } else if (session.agent.actor != Actor.ENGINE) {
                throw UsageException("In blue mode you must be Engine to do this")
              } else if (instr is Gain &&
                  instr.gaining.className.toString().endsWith("Phase")) { // TODO hack
                initiate(instruction)
              } else {
                throw UsageException("Eep, can't do that in ${mode.name.lowercase()} mode")
              }
            }
            PURPLE -> TODO()
          }

      return describeExecutionResults(changes)
    }

    private fun initiate(instruction: Raw<Instruction>): Result {
      if (mode == BLUE && !session.game.taskQueue.isEmpty()) {
        throw UserException("Must clear your task queue first (blue mode)")
      }
      return session.execute(instruction, auto)
    }
    private fun initiate(instruction: String): Result {
      if (mode == BLUE && !session.game.taskQueue.isEmpty()) {
        throw UserException("Must clear your task queue first (blue mode)")
      }
      return session.execute(instruction, auto)
    }
  }

  internal inner class TasksCommand : ReplCommand("tasks") {
    override val usage = "tasks"
    override val isReadOnly = true
    override fun noArgs(): List<String> {
      return session.game.taskQueue.toStrings()
    }
  }

  internal inner class TaskCommand : ReplCommand("task") {
    override val usage = "task <id> [<Instruction> | drop]"
    override fun withArgs(args: String): List<String> {
      val q = session.game.taskQueue

      val split = Regex("\\s+").split(args, 2)
      val idString = split.firstOrNull() ?: throw UsageException()
      val id = TaskId(idString)
      if (id !in q) throw UsageException("valid ids are ${q.ids}")
      val rest: String? =
          if (split.size > 1 && split[1].isNotEmpty()) {
            split[1]
          } else {
            null
          }
      if (rest == "drop") {
        session.game.removeTask(id)
        return listOf("Task $id deleted")
      }
      val instruction: Raw<Instruction>? = rest?.let(::parseInput)
      val result: Result =
          when (mode) {
            RED,
            YELLOW -> throw UsageException("Can't execute tasks in this mode")
            GREEN,
            BLUE ->
                if (auto) {
                  session.doTaskAndAutoExec(id, instruction)
                } else {
                  session.agent.doTask(id, instruction?.let(session::prep))
                }
            else -> TODO()
          }
      return describeExecutionResults(result)
    }
  }

  private fun describeExecutionResults(changes: Result): List<String> {
    val oops: List<Task> = changes.newTaskIdsAdded.map { session.game.taskQueue[it] }

    val interesting: List<ChangeEvent> = changes.changes // .filterNot { isSystemOnly(it.change) }
    val changeLines = interesting.toStrings().ifEmpty { listOf("No state changes") }
    val taskLines =
        if (oops.any()) {
          listOf("", "There are new pending tasks:") + oops.toStrings()
        } else {
          listOf()
        }
    return changeLines + taskLines
  }

  private fun isSystemOnly(change: StateChange): Boolean {
    val system = session.game.resolve(cn("System").expr)
    return listOfNotNull(change.gaining, change.removing).all {
      session.game.resolve(it).isSubtypeOf(system)
    }
  }

  internal inner class LogCommand : ReplCommand("log") {
    override val usage = "log [full]"
    override val isReadOnly = true

    // TODO filter it
    override fun noArgs() = session.game.eventLog.changesSince(session.game.start).toStrings()

    override fun withArgs(args: String): List<String> {
      if (args == "full") {
        return session.game.eventLog.events.toStrings()
      } else {
        throw UsageException()
      }
    }
  }

  internal inner class RollbackCommand : ReplCommand("rollback") {
    override val usage = "rollback <logid>"

    override fun withArgs(args: String): List<String> {
      session.rollBack(args.toInt())
      return listOf("Rollback done")
    }
  }

  internal inner class HistoryCommand : ReplCommand("history") {
    override val usage = "history <count>"
    override val isReadOnly = true
    val history = jline?.history

    override fun noArgs() = fmt(history!!)

    override fun withArgs(args: String): List<String> {
      val max = args.toIntOrNull() ?: throw UsageException()
      val drop = (history!!.size() - max).coerceIn(0, null)
      return fmt(history.drop(drop))
    }

    private fun fmt(entries: Iterable<History.Entry>) =
        entries.map { "${it.index() + 1}: ${it.line()}" }
  }

  internal inner class DescCommand : ReplCommand("desc") {
    override val usage = "desc <Expression>"
    override val isReadOnly = true

    override fun withArgs(args: String): List<String> {
      val expression =
          if (args == "random") {
            val randomBaseType = session.game.loader.allClasses.random().baseType
            val randomType = randomBaseType.concreteSubtypesSameClass().toList().random()
            Raw(randomType.expression, setOf())
          } else {
            parseInput(args, setOf(DEFAULTS, SHORT_NAMES))
          }
      return listOf(MTypeToText.describe(expression, session))
    }
  }

  internal inner class ScriptCommand : ReplCommand("script") {
    override val usage = "script <filename>"
    override fun withArgs(args: String) =
        File(args)
            .readLines()
            .takeWhile { it.trim() != "stop" }
            .filter { it.isNotEmpty() }
            .flatMap { listOf(">>> $it") + command(it) + "" }
  }

  public fun command(wholeCommand: String): List<String> {
    val stripped = wholeCommand.replace(Regex("//.*"), "")
    val groups = inputRegex.matchEntire(stripped)?.groupValues
    return if (groups == null) {
      listOf()
    } else {
      val (_, command, args) = groups
      command(command, args.trim().ifEmpty { null })
    }
  }

  internal fun command(command: ReplCommand, args: String? = null): List<String> {
    return try {
      if (args == null) command.noArgs() else command.withArgs(args.trim())
    } catch (e: UserException) {
      val usage = if (e is UsageException) "Usage: ${command.usage}" else ""
      listOf(e.message, usage).filter { it.isNotEmpty() }
    }
  }

  internal fun command(commandName: String, args: String?): List<String> {
    val command = commands[commandName] ?: return listOf("¯\\_(ツ)_/¯ Type `help` for help")
    return command(command, args)
  }

  private fun cn(s: String) = session.game.resolve(parseAsIs(s)).className // TODO fishy
}

private val helpText: String =
    """
    CONTROL
      help                -> shows this message
      newgame BMV 3       -> erases current game and starts 3p game with Base, default Map, Venus
      become Player1      -> makes Player1 the default player for queries & executions
      exit                -> go waste time differently
      rebuild             -> restart after code changes (game is forgotten)
    QUERYING
      has MAX 3 OceanTile -> evaluates a requirement (true/false) in the current game state
      count Plant         -> counts how many Plants the default player has
      count Plant<Anyone> -> counts how many Plants anyone has
      list Tile           -> list all Tiles (categorized)
      board               -> displays an extremely bad looking player board
      map                 -> displays an extremely bad looking Mars board
    EXECUTION
      exec PROD[3 Heat]   -> gives the default player 3 heat production, NOT triggering effects
      tasks               -> shows your current to-do list
      task F              -> do task F on your to-do list
      task F Plant        -> do task F, substituting `Plant` for an abstract instruction
      task F drop         -> bye task F
      mode yellow         -> switches to Yellow Mode (also try red, green, blue, purple)
    HISTORY
      log                 -> shows the useful bits in the current game's event log
      log full            -> shows the entire disgusting event log you were warned
      rollback 123        -> undoes recent changes up to and *including* change 123
      history             -> shows your *command* history
    METADATA
      desc Microbe        -> describes the Microbe class in detail
      desc Microbe<Ants>  -> describes the Microbe<Ants> type in detail
  """
        .trimIndent()
