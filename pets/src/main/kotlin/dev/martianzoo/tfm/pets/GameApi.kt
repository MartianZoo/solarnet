package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression

interface GameApi {
  fun count(type: TypeExpression): Int
  fun isMet(requirement: Requirement): Boolean
}
