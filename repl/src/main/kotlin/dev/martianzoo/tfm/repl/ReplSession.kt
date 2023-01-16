package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.FakeAuthority
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.GameState
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.StateChange.Cause
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
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.pets.deprodify
import dev.martianzoo.tfm.types.PetClass
import dev.martianzoo.tfm.types.PetClassLoader
import dev.martianzoo.tfm.types.PetType
import dev.martianzoo.tfm.types.PetType.PetGenericType

class ReplSession {
  private var game: Game? = null
  private var defaultPlayer: TypeExpression? = null

  fun newgame(args: String): List<String> {
    val (players, bundles) = args.split(Regex("\\s+"), 2)
    game = Engine.newGame(Canon, players.toInt(), bundles.asSequence().map { "$it" }.toList())
    defaultPlayer = null
    return listOf("New $players-player game created with bundles: $bundles")
  }

  fun become(args: String): List<String> {
    val type: PetType = game!!.resolve(args)
    require(!type.abstract && type.isSubtypeOf(game!!.classTable[PLAYER].baseType))
    defaultPlayer = type.toTypeExpressionFull()
    return listOf("Hi, $defaultPlayer")
  }

  fun count(args: String): List<String> {
    val fixedType = cook(TypeExpression.from(args))
    val count = game!!.count(fixedType)
    return listOf("$count $fixedType")
  }

  fun list(args: String?): List<String> {
    val g = game as Game

    // "list Heat" means my own unless I say "list Heat<Anyone>"
    val typeToList = g.resolve(cook(TypeExpression.from(args ?: COMPONENT.string)))
    val theStuff = g.getAll(typeToList)

    // figure out how to break it down
    val petClass = typeToList.petClass
    var subs = petClass.directSubclasses.sortedBy { it.name }
    if (subs.isEmpty()) subs = listOf(petClass)
    return subs.mapNotNull { sub ->
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
  }

  fun has(args: String): List<String> {
    val fixed = cook(Requirement.from(args))
    val result = game!!.isMet(args)
    return listOf("$result: $fixed")
  }

  fun map() = MapToText(game!!).map()

  fun history() = game!!.changeLog.map { it.toString() }

  fun exec(args: String): List<String> {
    val instr = cook(Instruction.from(args))
    game!!.execute(instr)
    return listOf("Ok: $instr")
  }

  fun desc(args: String): List<String> {
    val petClass: PetClass = game!!.classTable[cn(args)]
    val subs = petClass.allSubclasses
    return listOf(
        "Name: ${petClass.name}",
        "Abstract: ${petClass.abstract}",
        "Superclasses: ${petClass.allSuperclasses.joinToString()}",
        "Dependencies: ${petClass.dependencies.keyToDependency.values}",
        "Subclasses: " +
            if (subs.size <= 5) {
              subs.joinToString()
            } else {
              "(${subs.size})"
            },
        "Effects:",
    ) + petClass.effects.map { "    $it" }
  }

  fun replCommand(command: String, args: String?): List<String> {
    return when (command) {
      "newgame" ->  newgame(args!!)
      "become" ->   become(args!!)
      "count" ->    count(args!!)
      "list" ->     list(args)
      "has" ->      has(args!!)
      "map" ->      map()
      "history" ->  history()
      "exec" ->     exec(args!!)
      "desc" ->     desc(args!!)
      "help" ->     listOf("""
          newgame 3 BVE        -- begins a new 3p game with Base, Venus, Elysium
          become Player1       -- make Player1 the default player for future commands
          become               -- have no default Player anymore
          count Plant          -- counts how many Plants the default player has
          count Plant<Anyone>  -- counts how many Plants anyone has
          list Tile            -- lists all Tiles you have
          has MAX 3 OceanTile  -- evaluates a requirement against the current game state
          exec 20 PROD[3 Heat] -- gives the default player 20 TR
          map                  -- displays an extremely bad looking map
          help                 -- see this message

      """.trimIndent())
      else -> exec("$command " + (args ?: ""))
    }
  }

  private fun <P : PetNode> cook(node: P): P {
    if (defaultPlayer == null) {
      return node
    }
    val g = game!!
    val owned = g.resolve(OWNED.type)
    val anyone = g.resolve(ANYONE.type)

    class Fixer() : PetNodeVisitor() {
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
    return deprodify(fixt, (game!!.classTable as PetClassLoader).resourceNames())
  }

  object NullGame : GameState {
    override fun applyChange(
        count: Int,
        gaining: GenericTypeExpression?,
        removing: GenericTypeExpression?,
        cause: Cause?,
    ) = TODO("Not yet implemented")

    override val setup = GameSetup(FakeAuthority(), 2, listOf("M", "B"))

    override fun resolve(type: TypeExpression) = throe()
    override fun resolve(typeText: String) = throe()

    override fun count(type: TypeExpression) = throe()
    override fun count(typeText: String) = throe()

    override fun getAll(type: ClassLiteral) = throe()
    override fun getAll(type: GenericTypeExpression) = throe()
    override fun getAll(type: TypeExpression) = throe()
    override fun getAll(typeText: String) = throe()

    override fun isMet(requirement: Requirement) = throe()

    private fun throe(): Nothing = throw RuntimeException("no game has been started")
  }
}
