package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.ComponentClassDefinition
import dev.martianzoo.tfm.data.TfmDefinitionObject
import dev.martianzoo.tfm.petaform.api.Action
import dev.martianzoo.tfm.petaform.api.Effect
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.Instruction
import dev.martianzoo.tfm.petaform.api.PetaformNode
import dev.martianzoo.tfm.petaform.api.RootType
import dev.martianzoo.tfm.petaform.parser.PetaformParser.parse
import dev.martianzoo.tfm.types.ComponentClass.DependencyKey

class ComponentClassLoader {
  internal val table = mutableMapOf<String, ComponentClass>()

  fun snapshot() = ComponentClassTable(table)

  fun loadAll(objects: Iterable<TfmDefinitionObject>) = objects.forEach(::load)

  fun load(obj: TfmDefinitionObject): ComponentClass {
    val defn = obj.asComponentClassDefinition
    //println(defn.name)

    val supertypes = deriveSupertypes(defn)
    val superclasses = supertypes.map(ComponentType::componentClass).toSet()
    val dependencies = deriveDependencies(defn, supertypes)
    return ComponentClass(
        this, defn.name, defn.abstract, superclasses, dependencies,
        deriveImmediate(defn), deriveActions(defn), deriveEffects(defn)
    )
  }

  fun resolve(expr: Expression): ComponentType {
    val rootType = table[expr.rootType.ctypeName] ?: error(expr.rootType.ctypeName)
    val specializations = expr.specializations.map(::resolve)
    return ComponentType(rootType, rootType.dependencies.specialize(specializations))
  }

  fun resolve(exprPetaform: String): ComponentType = resolve(parse(exprPetaform))

  private fun deriveSupertypes(defn: ComponentClassDefinition) = when {
    defn.name == "Component" -> setOf()
    defn.supertypesPetaform.isEmpty() -> setOf(resolve("Component"))
    else -> defn.supertypesPetaform.map(::resolve).toSet()
  }

  private fun deriveDependencies(defn: ComponentClassDefinition, supertypesAsGiven: Set<ComponentType>): DependencyMap {
    val newDeps = defn.dependenciesPetaform.withIndex().map { (i, typeExpr) ->
      DependencyKey(defn.name, index = i) to resolve(typeExpr)
    }.toMap()
    return DependencyMap.merge(supertypesAsGiven.map { it.dependencies } + DependencyMap(newDeps))
  }

  private fun deriveImmediate(defn: ComponentClassDefinition): Instruction? {
    val immediate: Instruction? = defn.immediatePetaform?.let(::parse)
    immediate?.let(::verifyClassNames)
    return immediate
  }

  private fun deriveActions(defn: ComponentClassDefinition) =
      defn.actionsPetaform.map { parse<Action>(it) }.toSet().also(::verifyClassNames)

  private fun deriveEffects(defn: ComponentClassDefinition) =
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
