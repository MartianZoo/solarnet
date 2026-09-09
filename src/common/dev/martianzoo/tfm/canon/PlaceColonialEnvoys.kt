package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.types.Type
import dev.martianzoo.tfm.canon.ApiUtils.getOwner

internal object PlaceColonialEnvoys : CustomClass() {
  override fun translate(game: GameReader, type0: Type): InstructionTree {
    val owner = type0
    val colonyCount = game.getComponents("Colony").count { getOwner(game, it) == owner }
    val placeDelegate = parse<InstructionTree>("PartyDelegate<Party> FROM ReserveDelegate")
    return InstructionGroup.createTree(List(colonyCount) { placeDelegate })
  }
}
