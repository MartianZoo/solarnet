package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.engine.Component
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.Game
import dev.martianzoo.tfm.pets.AstTransforms.replaceTypes
import dev.martianzoo.tfm.pets.SpecialClassNames.OWNER
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.types.PType
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset

/** A programmatic entry point to a REPL session that is less textual than [ReplSession]. */
class InteractiveSession {
  internal var game: Game? = null // TODO private?
  internal var gameNumber: Int = 0
  internal var defaultPlayer: ClassName? = null

  fun newGame(setup: GameSetup) {
    game = Engine.newGame(setup)
    gameNumber++
    defaultPlayer = null
  }

  fun becomePlayer(player: Int) {
    val p = cn("Player$player")
    game!!.resolveType(p.type)
    defaultPlayer = p
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

  fun rollBackToBefore(ordinal: Int) = game!!.rollBack(ordinal)

  // TODO somehow do this with Type not TypeExpr
  fun <P : PetNode> fixTypes(node: P): P {
    val xer = game!!.loader.transformer
    var result = node
    result = xer.deprodify(result)
    result = xer.insertDefaultPlayer(result)
    if (defaultPlayer != null) {
      result = replaceTypes(result, OWNER.type, defaultPlayer!!.type) // TODO
    }
    return result
  }
}
