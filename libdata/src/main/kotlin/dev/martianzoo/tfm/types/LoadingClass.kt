package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.ComponentClassDefinition
import dev.martianzoo.tfm.petaform.api.Action
import dev.martianzoo.tfm.petaform.api.Effect
import dev.martianzoo.tfm.petaform.api.Instruction
import dev.martianzoo.tfm.petaform.parser.PetaformParser.parse
import dev.martianzoo.tfm.types.DependencyMap.DependencyKey
import java.util.Objects.hash

/**
 * Complete knowledge about a component class, irrespective of how it happened to be defined. This
 * data is relatively "cooked", but I'm still deciding how much inherited information it should
 * include.
 */
class ComponentClass(
    val loader: ComponentClassLoader,
    val name: String,
    val abstract: Boolean,
    val defn : ComponentClassDefinition
) {
  init {
    require(loader.table.put(name, this) == null)
  }

  private val supertypes by lazy {
    when {
      defn.name == "Component" -> setOf()
      defn.supertypesPetaform.isEmpty() -> setOf(loader.resolve("Component"))
      else -> {
        // TODO: feeble attempt to prune
        val set = mutableSetOf<ComponentType>()
        defn.supertypesPetaform.map(loader::resolve).forEach { next ->
          if (!set.any { it.isSubtypeOf(next) }) {
            set.add(next)
          }
        }
        set
      }
    }
  }

  val superclasses: Set<ComponentClass> by lazy {
    supertypes.map { it.componentClass }.toSet()
  }

  val dependencies: DependencyMap by lazy {
    val brandNewDeps = defn.dependenciesPetaform.withIndex().map { (i, typeExpr) ->
      DependencyKey(defn.name, index = i + 1) to loader.resolve(typeExpr)
    }.toMap()
    DependencyMap.merge(supertypes.map { it.dependencies } + DependencyMap(brandNewDeps))
  }

  val immediate: Instruction? = defn.immediatePetaform?.let(::parse)
  val actions: Set<Action> = defn.actionsPetaform.map<String, Action> { parse(it) }.toSet()
  val effects: Set<Effect> = defn.effectsPetaform.map<String, Effect> { parse(it) }.toSet()

  fun isSubclassOf(other: ComponentClass): Boolean {
    return other == this || superclasses.any { it.isSubclassOf(other) }
  }

  fun glb(other: ComponentClass) = when {
    this.isSubclassOf(other) -> this
    other.isSubclassOf(this) -> other
    else -> error("ad-hoc intersection types not supported")
  }

  override fun equals(other: Any?) = other is ComponentClass &&
      loader == other.loader && name == other.name
  override fun hashCode() = hash(loader, name)

  override fun toString() = name

}
