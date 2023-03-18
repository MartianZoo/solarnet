package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.api.Exceptions.UserException
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Actor
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.PlayerAgent
import dev.martianzoo.tfm.engine.Result
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Expression.Companion.expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.tfm.pets.ast.Metric.Companion.metric
import dev.martianzoo.tfm.pets.ast.Requirement.Companion.requirement
import dev.martianzoo.tfm.repl.ReplSession.ReplMode.BLUE
import dev.martianzoo.tfm.repl.ReplSession.ReplMode.GREEN
import dev.martianzoo.tfm.repl.ReplSession.ReplMode.RED
import dev.martianzoo.tfm.repl.ReplSession.ReplMode.YELLOW
import dev.martianzoo.util.Multiset
import dev.martianzoo.util.toStrings
import java.io.File
import org.jline.reader.History

internal fun main() {
  val jline = JlineRepl()
  val repl = ReplSession(Canon, GameSetup(Canon, "BM", 2), jline)
  val session = repl.session

  fun prompt(): String {
    val actor: Actor = repl.session.agent.actor
    val gameNo = session.gameNumber

    val text = "Game$gameNo as $actor"
    return repl.mode.color.foreground("$text> ")
  }

  // We don't actually have to start another game.....
  val welcome =
      """
      Welcome to REgo PLastics. Type `help` for help.
      Warning: this is a bare-bones tool that is not trying to be easy to use... at all
      
      ${repl.command("newgame BM 2").joinToString("""
      """)}

  """
          .trimIndent()

  jline.loop(::prompt, repl::command, welcome)
  println("Bye")
}

/** A programmatic entry point to a REPL session that is more textual than [ReplSession]. */
public class ReplSession(
    private val authority: Authority,
    initialSetup: GameSetup,
    val jline: JlineRepl? = null
) {
  internal val session = InteractiveSession(initialSetup)
  internal var mode: ReplMode = GREEN

  public enum class ReplMode(val message: String, val color: TfmColor) {
    RED("Arbitrary state changes that don't trigger effects", TfmColor.HEAT),
    YELLOW("Arbitrary state changes, enqueueing all effects", TfmColor.MEGACREDIT),
    GREEN("Arbitrary state changes, auto-executing effects", TfmColor.PLANT),
    BLUE("Only allows performing tasks on your task list", TfmColor.OCEAN_TILE),
    PURPLE("The engine fully orchestrates everything", TfmColor.ENERGY),
  }

  private val inputRegex = Regex("""^\s*(\S+)(.*)$""")

  private class UsageException(message: String? = null) : UserException(message ?: "")

  internal abstract inner class ReplCommand(val name: String) {
    abstract val usage: String
    open fun noArgs(): List<String> = throw UsageException()

    open fun withArgs(args: String): List<String> = throw UsageException()
  }

  private val commands =
      listOf(
              AsCommand(),
              BecomeCommand(),
              BoardCommand(),
              CountCommand(),
              DescCommand(),
              ExecCommand(),
              GiveTurnCommand(),
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
    override fun noArgs() = listOf(helpText)
    override fun withArgs(args: String) = listOf("Sorry, haven't implemented this yet")
  }

  internal inner class AsCommand : ReplCommand("as") {
    override val usage = "as <PlayerN> <full command>"
    override fun noArgs() = throw UsageException()
    override fun withArgs(args: String): List<String> {
      val current: PlayerAgent = session.agent
      val (player, rest) = args.trim().split(Regex("\\s+"), 2)

      try { // TODO
        session.becomePlayer(cn(player))
        return command(rest)
      } finally {
        session.agent = current
      }
    }
  }

  internal inner class NewGameCommand : ReplCommand("newgame") {
    override val usage = "newgame <bundles> <player count>"

    override fun withArgs(args: String): List<String> {
      val (bundleString, players) = args.trim().split(Regex("\\s+"), 2)
      try {
        session.newGame(GameSetup(authority, bundleString, players.toInt()))
      } catch (e: RuntimeException) {
        throw UsageException(e.message)
      }
      return listOf("New $players-player game created with bundles: $bundleString") +
          if (players.toInt() == 1) listOf("NOTE: No solo mode rules are implemented.")
          else listOf()
    }
  }

  internal inner class BecomeCommand : ReplCommand("become") {
    override val usage = "become [PlayerN]"

    override fun noArgs(): List<String> {
      session.becomeNoOne()
      return listOf("Okay, you are no one")
    }

    override fun withArgs(args: String): List<String> {
      session.becomePlayer(cn(args))
      return listOf("Hi, ${session.defaultPlayer}")
    }
  }

  internal inner class HasCommand : ReplCommand("has") {
    override val usage = "has <Requirement>"

    override fun withArgs(args: String): List<String> {
      val reqt = requirement(args)
      val result = session.has(reqt)
      return listOf("$result: ${session.prep(reqt)}")
    }
  }

  internal inner class CountCommand : ReplCommand("count") {
    override val usage = "count <Metric>"

    override fun withArgs(args: String): List<String> {
      val metric = metric(args)
      val count = session.count(metric)
      return listOf("$count ${session.prep(metric)}")
    }
  }

  internal inner class ListCommand : ReplCommand("list") {
    override val usage = "list <Expression>"
    override fun noArgs() = withArgs(COMPONENT.toString())

    override fun withArgs(args: String): List<String> {
      val expr = expression(args)
      val counts: Multiset<Expression> = session.list(expr)
      return listOf("${counts.size} ${session.prep(expr)}") +
          counts.entries.sortedByDescending { (_, ct) -> ct }.map { (e, ct) -> "  $ct $e" }
    }
  }

  internal inner class BoardCommand : ReplCommand("board") {
    override val usage = "board [PlayerN]"

    override fun noArgs(): List<String> {
      val player: ClassName =
          session.defaultPlayer
              ?: throw UsageException("Must specify a player (or `become` that player first)")
      return withArgs(player.toString())
    }

    override fun withArgs(args: String) =
        BoardToText(session.game.reader, jline != null).board(cn(args).expr)
  }

  internal inner class MapCommand : ReplCommand("map") {
    override val usage = "map"
    override fun noArgs() = MapToText(session.game.reader, jline != null).map()
  }

  internal inner class ModeCommand : ReplCommand("mode") {
    override val usage = "mode"
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

  internal inner class ExecCommand : ReplCommand("exec") {
    override val usage = "exec <Instruction>"

    override fun withArgs(args: String): List<String> {
      val instruction = instruction(args)
      val changes: Result =
          try {
            when (mode) {
              RED -> session.sneakyChange(instruction)
              YELLOW -> session.initiateOnly(instruction)
              GREEN -> session.initiateAndAutoExec(instruction)
              else -> return listOf("Eep, can't do that in ${mode.name.lowercase()} mode")
            }
          } catch (e: UserException) {
            return listOf(e.toString())
          }

      return describeExecutionResults(changes)
    }
  }

  internal inner class TasksCommand : ReplCommand("tasks") {
    override val usage = "tasks"
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
      if (id !in q) throw UsageException("not a valid id: $idString")
      val rest: String? =
          if (split.size > 1 && split[1].isNotEmpty()) {
            split[1]
          } else {
            null
          }
      if (rest == "drop") {
        session.game.taskQueue.removeTask(id)
        return listOf("Task $id deleted")
      }
      val instruction: Instruction? = rest?.let { instruction(it) }
      val result: Result =
          try {
            when (mode) {
              RED -> return listOf("Can't execute tasks in red mode")
              YELLOW,
              BLUE -> session.agent.doTask(id, session.prep(instruction))
              GREEN -> session.doTaskAndAutoExec(id, instruction)
              else -> TODO()
            }
          } catch (e: UserException) {
            return listOf(e.toString())
          }
      return describeExecutionResults(result)
    }
  }

  internal inner class GiveTurnCommand : ReplCommand("giveturn") {
    override val usage = "giveturn [PlayerN]"

    override fun noArgs(): List<String> {
      if (session.defaultPlayer == null) {
        throw UsageException("Must specify a player (or `become` that player first)")
      }
      val result = session.enqueueTasks(instruction("UseAction<StandardAction>"))
      return describeExecutionResults(result)
    }

    override fun withArgs(args: String): List<String> {
      val agent = session.agents[Actor(cn(args))]!!
      val result = agent.enqueueTasks(session.prep(instruction("UseAction<StandardAction>")))
      return describeExecutionResults(result)
    }
  }

  private fun describeExecutionResults(changes: Result): List<String> {
    val oops: List<Task> = changes.newTaskIdsAdded.map { session.game.taskQueue[it] }

    val changeLines = changes.changes.toStrings().ifEmpty { listOf("No state changes") }
    val taskLines =
        if (oops.any()) {
          listOf("", "There are new pending tasks:") + oops.toStrings()
        } else {
          listOf()
        }
    return changeLines + taskLines
  }

  internal inner class LogCommand : ReplCommand("log") {
    override val usage = "log [full]"

    // TODO filter it
    override fun noArgs() = session.game.eventLog.changesSince(session.start).toStrings()

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
    val history = jline?.history

    override fun noArgs() = fmt(history!!)

    override fun withArgs(args: String): List<String> {
      val max =
          try {
            args.toInt()
          } catch (e: RuntimeException) {
            throw UsageException()
          }
      val drop = (history!!.size() - max).coerceIn(0, null)
      return fmt(history.drop(drop))
    }

    private fun fmt(entries: Iterable<History.Entry>) =
        entries.map { "${it.index() + 1}: ${it.line()}" }
  }

  internal inner class DescCommand : ReplCommand("desc") {
    override val usage = "desc <Expression>"

    override fun withArgs(args: String): List<String> {
      val expression =
          if (args == "random") {
            val randomClass = session.game.loader.allClasses.random()
            randomClass.baseType.concreteSubtypesSameClass().toList().random().expression
          } else {
            expression(args)
          }
      return listOf(MTypeToText.describe(expression, session.game.loader))
    }
  }

  internal inner class ScriptCommand : ReplCommand("script") {
    override val usage = "script <filename>"
    override fun withArgs(args: String) =
        File(args).readLines()
            .takeWhile { it.trim() != "stop" }
            .filter { it.isNotEmpty() }
            .flatMap { listOf(">>> $it") + command(it) + "" }
  }

  public fun command(wholeCommand: String): List<String> {
    val stripped = wholeCommand.replace(Regex("//.*"), "")
    val (_, command, args) = inputRegex.matchEntire(stripped)?.groupValues ?: return listOf()
    return command(command, args.trim().ifEmpty { null })
  }

  internal fun command(command: ReplCommand, args: String? = null): List<String> {
    return try {
      if (args == null) command.noArgs() else command.withArgs(args.trim())
    } catch (e: UsageException) {
      val usage = "Usage: ${command.usage}"
      listOf(e.message, usage).filter { it.isNotEmpty() }
    }
  }

  internal fun command(commandName: String, args: String?): List<String> {
    val command = commands[commandName] ?: return listOf("¯\\_(ツ)_/¯ Type `help` for help")
    return command(command, args)
  }
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
