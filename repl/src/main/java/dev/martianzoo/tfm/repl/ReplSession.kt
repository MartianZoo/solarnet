package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Expression.Companion.expression
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.tfm.pets.ast.Metric.Companion.metric
import dev.martianzoo.tfm.pets.ast.Requirement.Companion.requirement
import dev.martianzoo.tfm.repl.ReplSession.ReplMode.YELLOW
import dev.martianzoo.util.Multiset
import dev.martianzoo.util.toStrings
import org.jline.reader.History
import org.jline.utils.AttributedStyle

internal fun main() {
  val jline = JlineRepl()
  val repl = ReplSession(Canon, jline)
  val session = repl.session

  fun prompt(): Pair<String, Int> {
    val player: ClassName? = session.defaultPlayer
    val gameNo = session.gameNumber
    return when {
      session.game == null -> "Table" to AttributedStyle.RED
      player == null -> "Game$gameNo" to AttributedStyle.GREEN
      else -> "Game$gameNo as $player" to AttributedStyle.CYAN
    }
  }
  jline.loop(::prompt, repl::command)
  println("Bye!")
}

/** A programmatic entry point to a REPL session that is more textual than [ReplSession]. */
public class ReplSession(private val authority: Authority, val jline: JlineRepl? = null) {
  internal val session = InteractiveSession()
  internal var mode: ReplMode = YELLOW
    set(f) {
      session.effectsOn = (f == YELLOW)
      field = f
    }
  init {
    mode = YELLOW // setting it redundantly to make the `effectsOn` thing happen TODO
  }

  public enum class ReplMode(val message: String) {
    RED("Allows arbitrary state changes, which don't trigger effects"),
    YELLOW("Allows arbitrary state changes, auto-executing effects when possible"),
    GREEN("Allows arbitrary state changes, enqueueing resulting tasks TODO"),
    BLUE("Allows only initiating `UseAction<StandardAction>` and handling enqueued tasks TODO"),
    PURPLE("The game engine will fully orchestrate the game workflow TODO"),
  }

  private val inputRegex = Regex("""^\s*(\S+)(.*)$""")

  public fun command(wholeCommand: String): List<String> {
    val (_, command, args) = inputRegex.matchEntire(wholeCommand)?.groupValues ?: return listOf()
    return command(command, args.trim().ifEmpty { null })
  }

  internal fun command(commandName: String, args: String?): List<String> {
    if (commandName !in setOf("help", "newgame") && session.game == null) {
      return listOf("No game active")
    }
    val command =
        commands.firstOrNull { it.name == commandName }
            ?: return listOf("¯\\_(ツ)_/¯ Type `help` for help")

    return try {
      if (args == null) command.noArgs() else command.withArgs(args.trim())
    } catch (e: UsageException) {
      listOfNotNull(e.message, "Usage: ${command.usage}")
    }
  }

  private class UsageException(message: String? = null) : RuntimeException(message)

  private abstract class ReplCommand(val name: String) {
    abstract val usage: String

    open fun noArgs(): List<String> = throw UsageException()
    open fun withArgs(args: String): List<String> = throw UsageException()
  }

  private val commands: List<ReplCommand> =
      listOf(
          object : ReplCommand("help") {
            override val usage = "help [command]"
            override fun noArgs() = listOf(helpText)
            override fun withArgs(args: String) = listOf("Sorry, haven't implemented this yet")
          },
          object : ReplCommand("newgame") {
            override val usage = "newgame <bundles> <player count>"

            override fun withArgs(args: String): List<String> {
              val (bundleString, players) = args.trim().split(Regex("\\s+"), 2)
              try {
                session.newGame(GameSetup(authority, bundleString, players.toInt()))
              } catch (e: RuntimeException) {
                throw UsageException(e.message)
              }
              return listOf("New $players-player game created with bundles: $bundleString")
            }
          },
          object : ReplCommand("become") {
            override val usage = "become [PlayerN]"

            override fun noArgs(): List<String> {
              session.becomeNoOne()
              return listOf("Okay, you are no one")
            }
            override fun withArgs(args: String): List<String> {
              session.becomePlayer(cn(args))
              return listOf("Hi, ${session.defaultPlayer}")
            }
          },
          object : ReplCommand("has") {
            override val usage = "has <Requirement>"

            override fun withArgs(args: String): List<String> {
              val reqt = requirement(args)
              val result = session.has(reqt)
              return listOf("$result: ${session.fixTypes(reqt)}")
            }
          },
          object : ReplCommand("count") {
            override val usage = "count <Metric>"

            override fun withArgs(args: String): List<String> {
              val metric = metric(args)
              val count = session.count(metric)
              return listOf("$count ${session.fixTypes(metric)}")
            }
          },
          object : ReplCommand("list") {
            override val usage = "list <Expression>"
            override fun noArgs() = withArgs(COMPONENT.toString())

            override fun withArgs(args: String): List<String> {
              val expr = expression(args)
              val counts: Multiset<Expression> = session.list(expr)
              return listOf("Listing ${session.fixTypes(expr)}...") +
                  counts.elements
                      .sortedByDescending { counts.count(it) }
                      .map { "${counts.count(it)} $it" }
            }
          },
          object : ReplCommand("board") {
            override val usage = "board [PlayerN]"

            override fun noArgs(): List<String> {
              val player: ClassName =
                  session.defaultPlayer
                      ?: throw UsageException(
                          "Must specify a player (or `become` that player first)")
              return withArgs(player.toString())
            }
            override fun withArgs(args: String) =
                BoardToText(session.game!!.reader).board(cn(args).expr)
          },
          object : ReplCommand("map") {
            override val usage = "map"
            override fun noArgs() = MapToText(session.game!!.reader).map()
          },
          object : ReplCommand("mode") {
            override val usage = "mode"
            override fun noArgs() = listOf("Mode $mode: ${mode.message}")

            override fun withArgs(args: String): List<String> {
              try {
                mode = ReplMode.valueOf(args.uppercase())
              } catch (e: Exception) {
                throw UsageException("Valid modes are: ${ReplMode.values().joinToString()}")
              }
              return noArgs()
            }
          },
          object : ReplCommand("exec") {
            override val usage = "exec <Instruction>"

            override fun withArgs(args: String): List<String> {
              val pend = session.game!!.pendingTasks
              val already = pend.size

              val changes = session.execute(instruction(args))
              val oops =
                  pend.subList(already, pend.size).flatMapIndexed { i, it ->
                    listOf("${it.id}: ${it.instruction} ${it.cause} (${it.why})")
                  }
              return changes.toStrings() +
                  if (oops.any()) {
                    listOf("", "There are new pending tasks:") + oops
                  } else {
                    listOf()
                  }
            }
          },
          object : ReplCommand("log") {
            override val usage = "log [full]"
            override fun noArgs() = session.game!!.changeLog().toStrings()

            override fun withArgs(args: String): List<String> {
              if (args == "full") {
                return session.game!!.changeLogFull().toStrings()
              } else {
                throw UsageException()
              }
            }
          },
          object : ReplCommand("rollback") {
            override val usage = "rollback <logid>"

            override fun withArgs(args: String): List<String> {
              session.rollBackToBefore(args.toInt())
              return listOf("Rollback done")
            }
          },
          object : ReplCommand("history") {
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
          },
          object : ReplCommand("desc") {
            override val usage = "desc <Expression>"

            override fun withArgs(args: String): List<String> {
              val expression = expression(args)
              return listOf(MTypeToText.describe(expression, session.game!!.loader))
            }
          })
}

private val helpText: String =
    """
    CONTROL
      help                -> shows this message
      new BMV 3           -> erases current game and starts 3p game with Base, default Map, Venus
      become Player1      -> makes Player1 the default player for queries & executions
      exit                -> go waste time differently
    QUERYING
      has MAX 3 OceanTile -> evaluates a requirement (true/false) in the current game state
      count Plant         -> counts how many Plants the default player has
      count Plant<Anyone> -> counts how many Plants anyone has
      list Tile           -> list all Tiles you have (categorized)
      board               -> displays an extremely bad looking player board
      map                 -> displays an extremely bad looking map
    EXECUTION
      exec PROD[3 Heat]   -> gives the default player 3 heat production, NOT triggering effects
      mode green          -> changes to Green Mode (also try red, yellow, blue, purple)
    HISTORY
      changes             -> shows the changelog (the useful bits) for the current game
      changes full        -> shows the entire disgusting changelog
      rollback 123        -> undoes recent changes up to and *including* change 123
      history             -> shows your *command* history
    METADATA
      desc Microbe        -> describes the Microbe class in detail
      desc Microbe<Ants>  -> describes the Microbe<Ants> type in detail
  """
        .trimIndent()
