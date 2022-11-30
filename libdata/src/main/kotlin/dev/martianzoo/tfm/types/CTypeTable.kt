package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.CTypeDefinition
import dev.martianzoo.tfm.data.TfmDefinitionObject
import dev.martianzoo.tfm.petaform.api.Action
import dev.martianzoo.tfm.petaform.api.Effect
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.Instruction
import dev.martianzoo.tfm.petaform.api.PetaformNode
import dev.martianzoo.tfm.petaform.api.RootType
import dev.martianzoo.tfm.petaform.parser.PetaformParser.parse
import dev.martianzoo.tfm.types.CTypeClass.DependencyKey

class CTypeTable {
  internal val table = mutableMapOf<String, CTypeClass>()

  fun loadAll(objects: Iterable<TfmDefinitionObject>) = objects.forEach(::load)

  fun load(obj: TfmDefinitionObject): CTypeClass {
    val data = obj.asRawComponentType

    val supertypeExpressions = deriveSupertypes(data)
    val dependencies = deriveDependencies(data, supertypeExpressions)
    val superclasses = supertypeExpressions.map(CType::cTypeClass).toSet()
    return CTypeClass(
        data.name, superclasses, dependencies, deriveImmediate(data),
        deriveActions(data), deriveEffects(data), data, this
    )
  }

  fun all() = table.values

  fun resolve(expr: Expression): CType {
    val rootType = table[expr.rootType.ctypeName]!!
    val specializations = expr.specializations.map(::resolve)
    return CType(rootType, rootType.dependencies.specialize(specializations))
  }

  fun resolve(exprPetaform: String): CType = resolve(parse(exprPetaform))

  operator fun contains(name: String) = name in table
  operator fun get(name: String) = table[name]

  private fun deriveSupertypes(defn: CTypeDefinition) = when {
    defn.name == "Component" -> setOf()
    defn.supertypesPetaform.isEmpty() -> setOf(resolve("Component"))
    else -> defn.supertypesPetaform.map(::resolve).toSet()
  }

  private fun deriveDependencies(defn: CTypeDefinition, supertypesAsGiven: Set<CType>): DependencyMap {
    val newDeps = defn.dependenciesPetaform.withIndex().map { (i, depText) ->
      if (depText.startsWith("TYPE ")) {
        DependencyKey(defn.name, isTypeOnly = true, index = i) to resolve(depText.substring(5))
      } else {
        DependencyKey(defn.name, isTypeOnly = false, index = i) to resolve(depText)
      }
    }.toMap()
    return DependencyMap.merge(supertypesAsGiven.map { it.dependencies } + DependencyMap(newDeps))
  }

  private fun deriveImmediate(defn: CTypeDefinition): Instruction? {
    val immediate: Instruction? = defn.immediatePetaform?.let(::parse)
    immediate?.let(::verifyClassNames)
    return immediate
  }

  private fun deriveActions(defn: CTypeDefinition) =
      defn.actionsPetaform.map { parse<Action>(it) }.toSet().also(::verifyClassNames)

  private fun deriveEffects(defn: CTypeDefinition) =
      defn.effectsPetaform.map { parse<Effect>(it) }.toSet().also(::verifyClassNames)

  private fun verifyClassNames(nodes: Iterable<PetaformNode>) {
    nodes.forEach(::verifyClassNames)
  }

  private fun verifyClassNames(node: PetaformNode) {
    if (node is RootType) {
      require(node.ctypeName in table) { node.ctypeName }
    } else {
      verifyClassNames(node.children)
    }
  }
}
