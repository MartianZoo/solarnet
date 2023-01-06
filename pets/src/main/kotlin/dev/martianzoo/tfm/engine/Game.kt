package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.Authority
import dev.martianzoo.tfm.engine.ComponentGraph.Component
import dev.martianzoo.tfm.pets.GameApi
import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.StateChange.Cause
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.types.PetClassTable
import dev.martianzoo.tfm.types.PetType

class Game(
    override val authority: Authority,
    val components: ComponentGraph,
    private val table: PetClassTable
) : GameApi {
  // val tasks = mutableListOf<Task>()

  fun resolve(type: TypeExpression) = table.resolve(type)
  fun resolve(typeText: String) = resolve(parse(typeText))

  fun count(type: PetType) = components.count(type)
  override fun count(type: TypeExpression) = count(resolve(type))
  fun count(typeText: String) = count(resolve(typeText))

  fun getAll(type: PetType) = components.getAll(type)
  fun getAll(type: TypeExpression) = getAll(resolve(type))
  fun getAll(typeText: String) = getAll(resolve(typeText))

  override fun isMet(requirement: Requirement) = requirement.evaluate(this)
  fun isMet(requirementText: String) = isMet(parse(requirementText))

  override fun applyChange(
      count: Int, gaining: TypeExpression?, removing: TypeExpression?, cause: Cause?) {
    val g = gaining?.let { Component(resolve(it)) }
    val r = removing?.let { Component(resolve(it)) }
    components.applyChange(count, g, r, cause)
  }
}
