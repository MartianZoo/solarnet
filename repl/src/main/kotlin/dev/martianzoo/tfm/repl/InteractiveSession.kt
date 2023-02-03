package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.engine.Component
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.Game
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.PetTransformer.Companion.transform
import dev.martianzoo.tfm.pets.SpecialClassNames.ANYONE
import dev.martianzoo.tfm.pets.SpecialClassNames.OWNED
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.types.AstTransforms
import dev.martianzoo.tfm.types.PType
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset

/** A programmatic entry point to a REPL session that is less textual than [ReplSession]. */
class InteractiveSession {
  internal var game: Game? = null // TODO private?
  internal var defaultPlayer: ClassName? = null

  fun newGame(setup: GameSetup) {
    game = Engine.newGame(setup)
    defaultPlayer = null
  }

  fun becomePlayer(player: Int) {
    defaultPlayer = cn("Player$player")
  }
  fun becomeNoOne() {
    defaultPlayer = null
  }

  fun count(typeExpr: TypeExpr) = game!!.countComponents(fixTypes(typeExpr))

  fun list(typeExpr: TypeExpr): Multiset<TypeExpr> {
    // "list Heat" means my own unless I say "list Heat<Anyone>"
    val typeToBeListed: PType = game!!.resolveType(fixTypes(typeExpr))
    val theStuff: Multiset<Component> = game!!.getComponents(typeToBeListed)

    // figure out how to break it down
    // val pclass = typeToBeListed.pclass
    // var subs = pclass.directSubclasses.sortedBy { it.name }
    // if (subs.isEmpty()) subs = listOf(pclass)
    //
    // subs.mapNotNull { sub ->
    //   val thatType: GenericPType = sub.baseType
    //   val count = theStuff.filter { it.hasType(thatType) }.size
    //   if (count > 0) {
    //     "$count".padEnd(4) + thatType
    //   } else {
    //     null
    //   }
    // }
    return HashMultiset() // TODO TODO
  }

  fun has(requirement: Requirement) = game!!.isMet(fixTypes(requirement))

  fun execute(instruction: Instruction): Instruction {
    val instr = fixTypes(instruction)
    game!!.execute(instr)
    return instr
  }

  fun <P : PetNode> fixTypes(node: P): P {
    if (defaultPlayer == null) {
      return node
    }
    val g = game!!
    val owned = g.resolveType(OWNED.type)
    val player = g.resolveType(ANYONE.type)

    // TODO do this elsewhere
    val fixer =
        object : PetTransformer() {
          override fun <P : PetNode> doTransform(node: P): P {
            if (node is TypeExpr) {
              if (g.resolveType(node).isSubtypeOf(owned)) {
                val hasPlayer = node.arguments.any { g.resolveType(it).isSubtypeOf(player) }
                if (!hasPlayer) {
                  return node.addArgs(defaultPlayer!!) as P
                }
              }
              return node
            }
            return defaultTransform(node)
          }
        }
    val fixt = node.transform(fixer)

    // TODO hmm
    return AstTransforms.deprodify(fixt, game!!.loader)
  }
}
