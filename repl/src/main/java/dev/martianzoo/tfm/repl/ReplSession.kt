package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.Component
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.tfm.pets.ast.Requirement.Companion.requirement
import dev.martianzoo.tfm.pets.ast.TypeExpr.Companion.typeExpr
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
    val (_, command, args) = INPUT_REGEX.matchEntire(wholeCommand)?.groupValues ?: return listOf()
    return command(command, args.ifBlank { null })
  }

  internal fun command(command: String, args: String?): List<String> {
    if (command !in setOf("help", "newgame") && session.game == null) {
      return listOf("no game active")
    }
    return commands[command]?.invoke(args) ?: commands["exec"]!!(command + (args ?: ""))
  }

  private val commands =
      mapOf<String, (String?) -> List<String>>(
          "help" to { listOf(HELP) },
          "newgame" to
              {
                it?.let { args ->
                  val (bundleString, players) = args.trim().split(Regex("\\s+"), 2)
                  session.newGame(GameSetup(authority, bundleString, players.toInt()))
                  listOf("New $players-player game created with bundles: $bundleString")
                }
                    ?: listOf("Usage: newgame <bundles> <player count>")
              },
          "become" to
              { args ->
                val message =
                    if (args == null) {
                      session.becomeNoOne()
                      "Okay you are no one"
                    } else {
                      val trimmed = args.trim()
                      require(trimmed.length == 7 && trimmed.startsWith("Player"))
                      val p = trimmed.substring(6).toInt()
                      session.becomePlayer(p)
                      "Hi, $trimmed"
                    }
                listOf(message)
              },
          "count" to
              {
                it?.let { args ->
                  val typeExpr = session.fixTypes(typeExpr(args))
                  val count = session.count(typeExpr)
                  listOf("$count $typeExpr")
                }
                    ?: listOf("Usage: count <TypeExpr>")
              },
          "has" to
              {
                it?.let { args ->
                  val fixed = session.fixTypes(requirement(args))
                  val result = session.has(fixed)
                  listOf("$result: $fixed")
                }
                    ?: listOf("Usage: has <Requirement>")
              },
          "map" to
              {
                if (it == null) {
                  MapToText(session.game!!).map()
                } else {
                  listOf("Arguments unexpected: $it")
                }
              },
          "board" to
              {
                val player = if (it == null) session.defaultPlayer!! else cn(it.trim())
                BoardToText(session.game!!).board(player.type)
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
          "exec" to
              {
                it?.let { args ->
                  val instr = session.execute(instruction(args))
                  listOf("Ok: $instr")
                }
                    ?: listOf("Usage: exec <Instruction>")
              },
          "rollback" to
              {
                it?.let { args ->
                  val ord = args.trim().toInt()
                  session.rollBackToBefore(ord)
                  listOf("Done")
                }
                    ?: listOf("Usage: rollback <ordinal>")
              },
          "desc" to
              {
                it?.let { args ->
                  val typeExpr = typeExpr(args.trim())
                  val ptype = session.game!!.resolveType(typeExpr)
                  if (typeExpr.isTypeOnly) {
                    listOf(PClassToText.describe(ptype.pclass))
                  } else {
                    listOf(Component(ptype).describe())
                  }
                }
                    ?: listOf("Usage: desc <ClassName>")
              },
      )
}

internal val INPUT_REGEX = Regex("""^\s*(\S+)(.*)$""")

private val HELP =
    """
      newgame BMP 3        ->  ERASE CURRENT GAME and start a new 3p game with Base/Tharsis/Prelude
      become Player1       ->  make Player1 the default player for future commands
      count Plant          ->  counts how many Plants the default player has
      count Plant<Anyone>  ->  counts how many Plants anyone has
      list Tile            ->  lists all Tiles you have
      has MAX 3 OceanTile  ->  evaluates a requirement in the current game state
      exec PROD[3 Heat]    ->  gives the default player 3 heat production
      PROD[3 Heat]         ->  that too
      rollback 987         ->  undo recent changes up to and including change 987
      changes              ->  see the changelog for the current game
      allchanges           ->  see the entire disgusting changelog
      history              ->  see your *command* history
      board                ->  displays an extremely bad looking player board
      map                  ->  displays an extremely bad looking map
      desc Microbe         ->  describes the Microbe class in detail (given this game setup)
      help                 ->  see this message
    """
        .trimIndent()
