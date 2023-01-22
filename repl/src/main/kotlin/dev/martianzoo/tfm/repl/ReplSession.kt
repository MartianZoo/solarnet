package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.tfm.pets.ast.Requirement.Companion.requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.typeExpression
import dev.martianzoo.tfm.types.PetClass
import dev.martianzoo.util.toStrings
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp.Capability

class ReplSession(val authority: Authority) {
  private val session = InteractiveSession()

  val commands =
      mapOf<String, (String?) -> List<String>>(
          "help" to { listOf(HELP.trimIndent()) },
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
                  val type = session.fixTypes(typeExpression(args))
                  val count = session.count(type)
                  listOf("$count $type")
                }
                    ?: listOf("Usage: count <TypeExpression>")
              },
          "list" to
              { args ->
                session.list(typeExpression(args!!))
                listOf()
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
                if (it != null) {
                  MapToText(session.game!!).map()
                } else {
                  listOf("Arguments unexpected: $it")
                }
              },
          "history" to
              { args ->
                args?.let { listOf("Arguments unexpected: $it") }
                    ?: session.game!!.changeLog.toStrings()
              },
          "exec" to
              {
                it?.let { args ->
                  val instr = session.execute(instruction(args))
                  listOf("Ok: $instr")
                }
                    ?: listOf("Usage: exec <Instruction>")
              },
          "desc" to
              {
                it?.let { args ->
                  val petClass: PetClass = session.game!!.classTable[cn(args.trim())]
                  val subs = petClass.allSubclasses
                  listOf(
                      "Name: ${petClass.name}",
                      "Abstract: ${petClass.abstract}",
                      "Superclasses: ${petClass.allSuperclasses.joinToString()}",
                      "Dependencies: ${petClass.baseType.dependencies.types}",
                      "Subclasses: " +
                          if (subs.size <= 5) {
                            subs.joinToString()
                          } else {
                            "(${subs.size})"
                          },
                  )
                }
                    ?: listOf("Usage: desc <ClassName>")
              },
      )

  fun command(wholeCommand: String): List<String> {
    val (_, command, args) = INPUT_REGEX.matchEntire(wholeCommand)?.groupValues ?: return listOf()
    return command(command, args.ifBlank { null })
  }

  fun command(command: String, args: String?): List<String> {
    if (command !in setOf("help", "newgame") && session.game == null) {
      return listOf("no game active")
    }
    return commands[command]?.invoke(args) ?: commands["exec"]!!(command + (args ?: ""))
  }
}

val INPUT_REGEX = Regex("""^\s*(\S+)(.*)$""")

fun main() {
  val terminal = TerminalBuilder.builder().color(true).build()

  terminal.enterRawMode()
  terminal.puts(Capability.enter_ca_mode)
  terminal.puts(Capability.keypad_xmit)

  val reader = LineReaderBuilder.builder().terminal(terminal).history(DefaultHistory()).build()
  reader.readLine("Welcome to REgo PLastics! Press enter.")

  val repl = ReplSession(Canon)
  repl.command("newgame BM 2").forEach(::println)

  while (true) {
    val inputLine =
        try {
          reader.readLine("> ")
        } catch (e: EndOfFileException) {
          return
        }
    val results =
        try {
          repl.command(inputLine)
        } catch (e: Exception) {
          listOf("${e::class}: ${e.message}")
        }
    results.forEach(::println)
    println()
  }
}

const val HELP =
    """
  newgame BVE 3         ->  begins a new 3p game with Base, Venus, Elysium
  become Player1        ->  make Player1 the default player for future commands
  become                ->  have no default Player anymore
  count Plant           ->  counts how many Plants the default player has
  count Plant<Anyone>   ->  counts how many Plants anyone has
  list Tile             ->  lists all Tiles you have
  has MAX 3 OceanTile   ->  evaluates a requirement against the current game state
  exec PROD[3 Heat]     ->  gives the default player 3 heat production
  PROD[3 Heat]          ->  that too
  desc Microbe          ->  describes the Microbe class
  map                   ->  displays an extremely bad looking map
  help                  ->  see this message
"""
