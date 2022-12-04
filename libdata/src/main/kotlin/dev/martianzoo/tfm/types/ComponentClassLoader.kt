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
import dev.martianzoo.tfm.types.DependencyMap.DependencyKey

class ComponentClassLoader {
  internal val table = mutableMapOf<String, ComponentClass>()

  fun snapshot() = ComponentClassTable(table)

  fun loadAll(objects: Iterable<TfmDefinitionObject>) = objects.forEach(::load)

  fun load(obj: TfmDefinitionObject): ComponentClass {
    val defn = obj.asComponentClassDefinition
    val supertypes = deriveSupertypes(defn)
    val superclasses = supertypes.map(ComponentType::componentClass).toSet()
    val dependencies = deriveDependencies(defn, supertypes)

    println("${pad(defn.name, 19)} ${pad(supertypes, 59)} $dependencies")
    return ComponentClass(
        this, defn.name, defn.abstract, superclasses, dependencies,
        deriveImmediate(defn), deriveActions(defn), deriveEffects(defn))
  }
  fun pad(s: Any, width: Int) = ("$s" + " ".repeat(width)).substring(0, width)

  fun resolve(expr: Expression): ComponentType {
    val rootType = table[expr.rootType.name] ?: error(expr.rootType.name)
    val specializations = expr.specializations.map(::resolve)
    return ComponentType(rootType, rootType.dependencies.specialize(specializations))
  }

  fun resolve(exprPetaform: String): ComponentType = resolve(parse(exprPetaform))

  private fun deriveSupertypes(defn: ComponentClassDefinition) = when {
    defn.name == "Component" -> setOf()
    defn.supertypesPetaform.isEmpty() -> setOf(resolve("Component"))
    else -> {
      // TODO: feeble attempt to prune
      val set = mutableSetOf<ComponentType>()
      defn.supertypesPetaform.map(::resolve).forEach { next ->
        if (!set.any { it.isSubtypeOf(next) }) {
          set.add(next)
        }
      }
      set
    }
  }

  private fun deriveDependencies(defn: ComponentClassDefinition, supertypes: Set<ComponentType>): DependencyMap {
    val brandNewDeps = defn.dependenciesPetaform.withIndex().map { (i, typeExpr) ->
      DependencyKey(defn.name, index = i + 1) to resolve(typeExpr)
    }.toMap()
    return DependencyMap.merge(supertypes.map { it.dependencies } + DependencyMap(brandNewDeps))
  }

  private fun deriveImmediate(defn: ComponentClassDefinition): Instruction? {
    val immediate: Instruction? = defn.immediatePetaform?.let(::parse)
    immediate?.let(::verifyClassNames)
    return immediate
  }

  private fun deriveActions(defn: ComponentClassDefinition) =
      defn.actionsPetaform.map { parse<Action>(it) }.toSet().also {
        verifyClassNames(it.mapNotNull(Action::cost))
      }

  private fun deriveEffects(defn: ComponentClassDefinition): Set<Effect> {
    val fx = defn.effectsPetaform.map { parse<Effect>(it) }.toSet()
    fx.forEach { verifyClassNames(it.trigger) }
    return fx
  }

  private fun verifyClassNames(nodes: Iterable<PetaformNode>) {
    nodes.forEach(::verifyClassNames)
  }

  private fun verifyClassNames(node: PetaformNode) {
    if (node is RootType) {
      require(node.name in table) { node.name }
    } else {
      verifyClassNames(node.children)
    }
  }
}
