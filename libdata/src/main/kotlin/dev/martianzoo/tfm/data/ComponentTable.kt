package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.petaform.api.Action
import dev.martianzoo.tfm.petaform.api.ClassName
import dev.martianzoo.tfm.petaform.api.Effect
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.Instruction
import dev.martianzoo.tfm.petaform.api.PetaformNode
import dev.martianzoo.tfm.petaform.parser.PetaformParser.parse

class ComponentTable {
  private val table = mutableMapOf<String, ComponentType>()

  init {
    add(ComponentTypeData("Component", abstract = true)) // TODO constant somewhere
  }

  fun addAll(objects: Iterable<TfmData>) = objects.forEach(::add)

  fun add(obj: TfmData) {
    val data = obj.asRawComponentType

    val supertypes = data.supertypesPetaform.map { parse<Expression>(it) }.toSet()
    verifyClassNames(supertypes)

    val dependencies: List<Expression> = data.dependenciesPetaform.map(::parse)
    verifyClassNames(dependencies)

    val immediate: Instruction? = data.immediatePetaform?.let(::parse)
    immediate?.let(::verifyClassNames)

    val actions: Set<Action> = data.actionsPetaform.map { parse<Action>(it) }.toSet()
    verifyClassNames(actions)

    val effects: Set<Effect> = data.effectsPetaform.map { parse<Effect>(it) }.toSet()
    verifyClassNames(effects)

    table[data.name] = ComponentType(data, supertypes, dependencies, immediate, actions, effects)
  }

  fun all() = table.values

  operator fun get(name: String) = table[name]

  private fun verifyClassNames(nodes: Iterable<PetaformNode>) {
    nodes.forEach(::verifyClassNames)
  }

  private fun verifyClassNames(node: PetaformNode) {
    if (node is ClassName) {
      require(node.ctypeName in table) { node.ctypeName }
    } else {
      verifyClassNames(node.children)
    }
  }

}
