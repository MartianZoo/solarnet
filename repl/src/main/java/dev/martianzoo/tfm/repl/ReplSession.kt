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

  public fun command(wholeCommand: String): List<String> {
    val (_, command, args) = inputRegex.matchEntire(wholeCommand)?.groupValues ?: return listOf()
    return command(command, args.ifBlank { null })
  }

  internal val inputRegex = Regex("""^\s*(\S+)(.*)$""")

  internal fun command(commandName: String, args: String?): List<String> {
    if (commandName !in setOf("help", "newgame") && session.game == null) {
      return listOf("no game active")
    }
    val command = commands[commandName] ?: error("No command named $commandName")
    return command(args)
  }

  private val commands =
      mapOf<String, (String?) -> List<String>>(
          "help" to { listOf(helpText) },
          "newgame" to
              {
                it?.let { args ->
                  val (bundleString, players) = args.trim().split(Regex("\\s+"), 2)
                  session.newGame(GameSetup(authority, bundleString, players.toInt()))
                  listOf("New $players-player game created with bundles: $bundleString")
                } ?: listOf("Usage: newgame <bundles> <player count>")
              },
          "become" to
              { args ->
                val message =
                    if (args == null) {
                      session.becomeNoOne()
                      "Okay you are no one"
                    } else {
                      session.becomePlayer(cn(args.trim()))
                      "Hi, ${session.defaultPlayer}"
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
          "map" to
              {
                if (it == null) {
                  MapToText(session.game!!.reader).map()
                } else {
                  listOf("Arguments unexpected: $it")
                }
              },
          "board" to
              {
                val player = if (it == null) session.defaultPlayer!! else cn(it.trim())
                BoardToText(session.game!!.reader).board(player.expr)
              },
          "changes" to
              { args ->
                args?.let { listOf("Arguments unexpected: $it") }
                    ?: session.game!!.changeLog().toStrings()
              },
          "allchanges" to
              { args ->
                args?.let { listOf("Arguments unexpected: $it") }
                    ?: session.game!!.changeLogFull().toStrings()
              },
          "exec1" to
              {
                it?.let { args ->
                  session.execute(instruction(args), withEffects = false).toStrings()
                } ?: listOf("Usage: exec1 <Instruction>")
              },
          "exec2" to
              {
                it?.let { args ->
                  val changes = session.execute(instruction(args), withEffects = true)
                  val oops = session.game!!.pendingAbstractTasks.mapIndexed { i, it ->
                    "$i: ${it.instruction} ${it.cause}"
                  }
                  changes.toStrings() + if (oops.any()) {
                    listOf("", "There were abstract tasks left over:") + oops
                  } else {
                    listOf()
                  }

                } ?: listOf("Usage: exec2 <Instruction>")
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
      newgame BMV 3        ->  ERASES CURRENT GAME, starts a new 3p game with Base/Tharsis/Venus
      become Player1       ->  makes Player1 the default player for future commands
      count Plant          ->  counts how many Plants the default player has
      count Plant<Anyone>  ->  counts how many Plants anyone has
      list Tile            ->  list all Tiles you have
      has MAX 3 OceanTile  ->  evaluates a requirement (true/false) in the current game state
      exec PROD[3 Heat]    ->  gives the default player 3 heat production, NOT triggering effects
      rollback 987         ->  undoes recent changes up to and including change 987
      changes              ->  shows the changelog (the useful bits) for the current game
      allchanges           ->  shows the entire disgusting changelog
      history              ->  shows your *command* history
      board                ->  displays an extremely bad looking player board
      map                  ->  displays an extremely bad looking map
      desc Microbe         ->  describes the Microbe class in detail (given this game setup)
      help                 ->  shows this message
      exit                 ->  go waste time differently
  """.trimIndent()
}
