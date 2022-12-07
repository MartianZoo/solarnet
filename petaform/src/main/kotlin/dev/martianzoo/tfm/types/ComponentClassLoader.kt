package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.ComponentDefinition
import dev.martianzoo.tfm.data.Definition
import dev.martianzoo.tfm.petaform.Action
import dev.martianzoo.tfm.petaform.Effect
import dev.martianzoo.tfm.petaform.Expression
import dev.martianzoo.tfm.petaform.Instruction
import dev.martianzoo.tfm.petaform.PetaformParser.parse
import dev.martianzoo.tfm.types.DependencyMap.DependencyKey

class ComponentClassLoader {
  internal val table = mutableMapOf<String, ComponentClass>()

  fun snapshot() = ComponentClassTable(table)

  fun loadAll(objects: Iterable<Definition>) = objects.forEach(::load)

  fun load(obj: Definition): ComponentClass {
    val defn = obj.asComponentDefinition
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
    val theClass = table[expr.className] ?: error(expr.className)
    val specializations = expr.specializations.map(::resolve)
    return ComponentType(theClass, theClass.dependencies.specialize(specializations))
  }

  fun resolve(exprPetaform: String): ComponentType = resolve(parse(exprPetaform))

  private fun deriveSupertypes(defn: ComponentDefinition) = when {
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

  private fun deriveDependencies(defn: ComponentDefinition, supertypes: Set<ComponentType>): DependencyMap {
    val brandNewDeps = defn.dependenciesPetaform.withIndex().map { (i, typeExpr) ->
      DependencyKey(defn.name, index = i + 1) to resolve(typeExpr)
    }.toMap()
    return DependencyMap.merge(supertypes.map { it.dependencies } + DependencyMap(brandNewDeps))
  }

  private fun deriveImmediate(defn: ComponentDefinition): Instruction? {
    return defn.immediatePetaform?.let(::parse)
  }

  private fun deriveActions(defn: ComponentDefinition) =
      defn.actionsPetaform.map { parse<Action>(it) }.toSet()

  private fun deriveEffects(defn: ComponentDefinition) =
      defn.effectsPetaform.map<String, Effect> { parse(it) }.toSet()
}
