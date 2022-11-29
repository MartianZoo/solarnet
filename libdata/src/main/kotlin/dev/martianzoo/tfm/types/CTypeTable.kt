package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.CTypeData
import dev.martianzoo.tfm.data.TfmData
import dev.martianzoo.tfm.types.CType.RegularCType
import dev.martianzoo.tfm.types.CType.This
import dev.martianzoo.tfm.petaform.api.Action
import dev.martianzoo.tfm.petaform.api.ClassName
import dev.martianzoo.tfm.petaform.api.Effect
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.Instruction
import dev.martianzoo.tfm.petaform.api.PetaformNode
import dev.martianzoo.tfm.petaform.parser.PetaformParser
import dev.martianzoo.tfm.petaform.parser.PetaformParser.parse

class CTypeTable {
  private val table = mutableMapOf<String, CTypeDefinition>()

  init {
    add(CTypeData("Component", abstract = true)) // TODO constant somewhere
  }

  fun addAll(objects: Iterable<TfmData>) = objects.forEach(::add)

  fun add(obj: TfmData) {
    val data = obj.asRawComponentType

    val supertypes = data.supertypesPetaform.map { parse<Expression>(it) }.toSet()
    verifyClassNames(supertypes)

    val dependencies = data.dependenciesPetaform.withIndex().map {
      (i, dep) ->
        if (dep.startsWith("TYPE ")) {
          BaseDependency(data.name, resolve(dep.substring(5)), isTypeOnly = true, index = i)
        } else {
          BaseDependency(data.name, resolve(dep), isTypeOnly = false, index = i)
        }
    }

    val immediate: Instruction? = data.immediatePetaform?.let(::parse)
    immediate?.let(::verifyClassNames)

    val actions: Set<Action> = data.actionsPetaform.map { parse<Action>(it) }.toSet()
    verifyClassNames(actions)

    val effects: Set<Effect> = data.effectsPetaform.map { parse<Effect>(it) }.toSet()
    verifyClassNames(effects)

    table[data.name] = CTypeDefinition(data.name, supertypes, dependencies, immediate, actions, effects, data, this)
  }

  fun all() = table.values

  fun resolve(expr: Expression): CType {
    val rootType = expr.rootType
    return when (rootType) {
      is dev.martianzoo.tfm.petaform.api.This -> This
      // is Me -> Me
      is ClassName -> RegularCType(
          table[rootType.ctypeName]!!,
          mapOf(),
          listOf()
      )
    }
  }

  fun resolve(exprPetaform: String): CType = resolve(parse(exprPetaform))

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
