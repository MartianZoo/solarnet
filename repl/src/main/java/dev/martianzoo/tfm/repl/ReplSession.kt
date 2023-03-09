package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Expression.Companion.expression
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.tfm.pets.ast.Metric.Companion.metric
import dev.martianzoo.tfm.pets.ast.Requirement.Companion.requirement
import dev.martianzoo.tfm.repl.ReplSession.ReplMode.YELLOW
import dev.martianzoo.util.toStrings
import org.jline.utils.AttributedStyle

internal fun main() {
  val repl = ReplSession(Canon)
  val session = repl.session
  val jline = JlineRepl()

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
public class ReplSession(private val authority: Authority) {
  internal val session = InteractiveSession()
  internal var mode: ReplMode = YELLOW
    set(f) {
      session.effectsOn = (f == YELLOW)
      field =f
    }
  init {
    mode = YELLOW // setting it redundantly to make the `effectsOn` thing happen TODO
  }

  enum class ReplMode(val message: String) {
    GRAY("No game active."),
    RED("Allows arbitrary state changes, which don't trigger effects."),
    YELLOW("Allows arbitrary state changes, auto-executing effects when possible."),
    GREEN("Allows arbitrary state changes, enqueueing resulting tasks. TODO"),
    BLUE("Allows only initiating `UseAction1<StandardAction>`, and handling enqueued tasks. TODO")
  }

  public fun command(wholeCommand: String): List<String> {
    val (_, command, args) = inputRegex.matchEntire(wholeCommand)?.groupValues ?: return listOf()
    return command(command, args.ifBlank { null })
  }

  internal val inputRegex = Regex("""^\s*(\S+)(.*)$""")

  internal fun command(commandName: String, args: String?): List<String> {
    if (commandName !in setOf("help", "new") && session.game == null) {
      return listOf("no game active")
    }
    val command = commands[commandName] ?: error("No command named $commandName")
    return command(args)
  }

  private val commands =
      mapOf<String, (String?) -> List<String>>(
          "help" to { listOf(helpText) },
          "new" to
              {
                it?.let { args ->
                  val (bundleString, players) = args.trim().split(Regex("\\s+"), 2)
                  session.newGame(GameSetup(authority, bundleString, players.toInt()))
                  listOf("New $players-player game created with bundles: $bundleString")
                } ?: listOf("Usage: new <bundles> <player count>")
              },
          "become" to
              { args ->
                val message =
                    if (args == null) {
                      session.becomeNoOne()
                      "Now issuing commands as `Game`"
                    } else {
                      session.becomePlayer(cn(args.trim()))
                      "Hi, `${session.defaultPlayer}`"
                    }
                listOf(message)
              },
          "has" to
              {
                it?.let { args ->
                  val fixed = session.fixTypes(requirement(args))
                  val result = session.has(fixed)
                  listOf("$result: $fixed")
                } ?: listOf("Usage: has <Requirement>")
              },
          "count" to
              {
                it?.let { args ->
                  val metric = session.fixTypes(metric(args))
                  val count = session.count(metric)
                  listOf("$count $metric")
                } ?: listOf("Usage: count <Expression>")
              },
          "list" to
              {
                it?.let { args ->
                  val counts = session.list(expression(args))
                  counts.elements
                      .sortedByDescending { counts.count(it) }
                      .map { "${counts.count(it)} $it" }
                } ?: listOf("Usage: list <Expression>")
              },
          "board" to
              {
                val player = if (it == null) session.defaultPlayer!! else cn(it.trim())
                BoardToText(session.game!!.reader).board(player.expr)
              },
          "map" to
              {
                if (it == null) {
                  MapToText(session.game!!.reader).map()
                } else {
                  listOf("Usage: map")
                }
              },
          "mode" to
              { arg ->
                if (arg != null) {
                  mode = ReplMode.valueOf(arg.trim().uppercase())
                }
                listOf("Mode $mode: ${mode.message}")
              },
          "exec" to
              {
                it?.let { args ->
                  val changes = session.execute(instruction(args))
                  val oops = session.game!!.pendingTasks.flatMapIndexed { i, it ->
                    listOf(
                        "$i: ${it.instruction} ${it.cause}",
                        "        ${it.reasonPending}"
                    )
                  }
                  changes.toStrings() + if (oops.any()) {
                    listOf("", "Failed tasks:") + oops
                  } else {
                    listOf()
                  }

                } ?: listOf("Usage: exec <Instruction>")
              },
          "changes" to
              { args ->
                args?.let { listOf("Usage: changes") }
                    ?: session.game!!.changeLog().toStrings()
              },
          "changesfull" to
              { args ->
                args?.let { listOf("Usage: changesfull") }
                    ?: session.game!!.changeLogFull().toStrings()
              },
          "rollback" to
              {
                it?.let { args ->
                  val ord = args.trim().toInt()
                  session.rollBackToBefore(ord)
                  listOf("Done")
                } ?: listOf("Usage: rollback <ordinal>")
              },
          "desc" to
              {
                it?.let { args ->
                  val expression = expression(args.trim())
                  listOf(MTypeToText.describe(expression, session.game!!.loader))
                } ?: listOf("Usage: desc <ClassName>")
              },
      )

  private val helpText: String = """
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
      changesfull         -> shows the entire disgusting changelog
      rollback 123        -> undoes recent changes up to and *including* change 123
      history             -> shows your *command* history
    METADATA
      desc Microbe        -> describes the Microbe class in detail
      desc Microbe<Ants>  -> describes the Microbe<Ants> type in detail
  """.trimIndent()
}
