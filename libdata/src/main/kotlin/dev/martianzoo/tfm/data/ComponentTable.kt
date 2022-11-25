package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.petaform.api.Action
import dev.martianzoo.tfm.petaform.api.ClassName
import dev.martianzoo.tfm.petaform.api.Effect
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.Instruction
import dev.martianzoo.tfm.petaform.api.PetaformNode
import dev.martianzoo.tfm.petaform.parser.PetaformParser.parse

class ComponentTable {
  private val table = mutableMapOf<String, RichComponent>()

  init {
    add(Component("Component", true, true))
  }

  fun add(obj: TfmObject) {
    val backing = obj.asComponent

    val supertypes = backing.supertypesPetaform.map { parse<Expression>(it) }.toSet()
    verifyClassNames(supertypes)

    val dependencies: List<Expression> = backing.dependenciesPetaform.map(::parse)
    verifyClassNames(dependencies)

    val immediate: Instruction? = backing.immediatePetaform?.let(::parse)
    immediate?.let(::verifyClassNames)

    val actions: Set<Action> = backing.actionsPetaform.map { parse<Action>(it) }.toSet()
    verifyClassNames(actions)

    val effects: Set<Effect> = backing.effectsPetaform.map { parse<Effect>(it) }.toSet()
    verifyClassNames(effects)

    table[backing.name] = RichComponent(backing, supertypes, dependencies, immediate, actions, effects)
  }

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

  data class RichComponent(
      val backing: Component,
      val supertypes: Set<Expression>,
      val dependencies: List<Expression>,
      val immediate: Instruction?,
      val actions: Set<Action>,
      val effects: Set<Effect>)
}
