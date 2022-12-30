package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.spellOutQes
import dev.martianzoo.tfm.types.PetClassTable
import dev.martianzoo.tfm.types.PetType

class Game(val components: ComponentGraph, private val table: PetClassTable) {
  // val tasks = mutableListOf<Task>()

  fun resolve(type: TypeExpression) = table.resolve(type)
  fun resolve(typeText: String) = resolve(parse(typeText))

  fun count(type: PetType) = components.count(type)
  fun count(type: TypeExpression) = count(resolve(type))
  fun count(typeText: String) = count(resolve(typeText))

  fun getAll(type: PetType) = components.getAll(type)
  fun getAll(type: TypeExpression) = getAll(resolve(type))
  fun getAll(typeText: String) = getAll(resolve(typeText))

  fun isMet(requirement: Requirement) = requirement.evaluate(::count)
  fun isMet(requirementText: String) = isMet(spellOutQes(parse(requirementText)))
}
