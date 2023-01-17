package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.Game
import dev.martianzoo.tfm.pets.PetNodeVisitor
import dev.martianzoo.tfm.pets.SpecialClassNames.ANYONE
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.SpecialClassNames.OWNED
import dev.martianzoo.tfm.pets.SpecialClassNames.PLAYER
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.pets.deprodify
import dev.martianzoo.tfm.types.PetClass
import dev.martianzoo.tfm.types.PetClassLoader
import dev.martianzoo.tfm.types.PetType
import dev.martianzoo.tfm.types.PetType.PetGenericType
import dev.martianzoo.util.Debug.d
import dev.martianzoo.util.toStrings

typealias ReplCommand = (String?) -> List<String>

class ReplSession(val authority: Authority) {
  private var game: Game? = null
  private var defaultPlayer: TypeExpression? = null

  val commands = mapOf<String, ReplCommand>(
      "help" to { listOf(HELP.trimIndent()) },

      "newgame" to {
        it?.let { args ->
          val (bundleString, players) = args.trim().split(Regex("\\s+"), 2)
          game = Engine.newGame(GameSetup(authority, bundleString, players.toInt()))
          defaultPlayer = null
          listOf("New $players-player game created with bundles: $bundleString")
        } ?: listOf("Usage: newgame <bundles> <player count>")
      },

      "become" to { args ->
        val message = if (args == null) {
          if (defaultPlayer == null) {
            "Become whom?"
          } else {
            defaultPlayer = null
            "Okay you are back to being God again"
          }
        } else {
          val type: PetType = game!!.resolve(args)
          require(!type.abstract && type.isSubtypeOf(game!!.classTable[PLAYER].baseType))
          defaultPlayer = type.toTypeExpression()
          "Hi, $defaultPlayer"
        }
        listOf(message)
      },

      "count" to {
        it?.let { args ->
          val fixedType = cook(TypeExpression.from(args))
          val count = game!!.count(fixedType)
          listOf("$count $fixedType")
        } ?: listOf("Usage: count <TypeExpression>")
      },

      "list" to { args ->
        val g = game as Game

        // "list Heat" means my own unless I say "list Heat<Anyone>"
        val typeToList = g.resolve(cook(TypeExpression.from(args ?: "$COMPONENT")))
        val theStuff = g.getAll(typeToList)

        // figure out how to break it down
        val petClass = typeToList.petClass
        var subs = petClass.directSubclasses.sortedBy { it.name }
        if (subs.isEmpty()) subs = listOf(petClass)

        subs.mapNotNull { sub ->
          val thatType: PetGenericType = sub.baseType
          val count = theStuff.entrySet()
              .filter { it.element.hasType(thatType) }
              .sumOf { it.count }
          if (count > 0) {
            "$count".padEnd(4) + thatType
          } else {
            null
          }
        }
      },

      "has" to {
        it?.let { args ->
          val fixed = cook(Requirement.from(args))
          val result = game!!.isMet(fixed)
          listOf("$result: $fixed")
        } ?: listOf("Usage: has <Requirement>")
      },

      "map" to { args ->
        args?.let { listOf("Arguments unexpected: $it") } ?:
            MapToText(game!!).map()
      },

      "history" to { args ->
        args?.let { listOf("Arguments unexpected: $it") } ?:
            game!!.changeLog.toStrings()
      },

      "exec" to {
        it?.let { args ->
          val instr = cook(Instruction.from(args))
          game!!.execute(instr)
          listOf("Ok: $instr")
        } ?: listOf("Usage: exec <Instruction>")
      },

      "desc" to {
        it?.let { args ->
          val petClass: PetClass = game!!.classTable[cn(args.trim())]
          val subs = petClass.allSubclasses
          listOf(
              "Name: ${petClass.name}",
              "Abstract: ${petClass.abstract}",
              "Superclasses: ${petClass.allSuperclasses.joinToString()}",
              "Dependencies: ${petClass.dependencies.types}",
              "Subclasses: " +
                  if (subs.size <= 5) {
                    subs.joinToString()
                  } else {
                    "(${subs.size})"
                  },
              "Effects:",
          ) + petClass.effects.map { "    $it" }
        } ?: listOf("Usage: desc <ClassName>")
      },
  )

  fun command(wholeCommand: String): List<String> {
    val (_, command, args) = INPUT_REGEX.matchEntire(wholeCommand)?.groupValues ?: return listOf()
    return command(command, args.ifBlank { null })
  }

  fun command(command: String, args: String?): List<String> {
    if (command !in setOf("help", "newgame") && game == null) {
      return listOf("no game active")
    }
    return commands[command]?.invoke(args) ?: commands["exec"]!!(command + (args ?: ""))
  }

  private fun <P : PetNode> cook(node: P): P {
    if (defaultPlayer == null) {
      return node
    }
    val g = game!!
    val owned = g.resolve(OWNED.type)
    val anyone = g.resolve(ANYONE.type)

    class Fixer : PetNodeVisitor() {
      override fun <P : PetNode?> transform(node: P): P {
        if (node is GenericTypeExpression) {
          if (g.resolve(node).isSubtypeOf(owned)) {
            val hasPlayer = node.args.any { g.resolve(it).isSubtypeOf(anyone) }
            if (!hasPlayer) {
              return node.addArgs(listOf(defaultPlayer!!)) as P
            }
          }
          return node
        }
        return super.transform(node) as P
      }
    }
    val fixt = Fixer().transform(node)
    return deprodify(fixt, (game!!.classTable as PetClassLoader).resourceNames()).d("fixed: ")
  }
}

const val HELP = """
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
