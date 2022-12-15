package dev.martianzoo.tfm.data

import com.squareup.moshi.Json
import dev.martianzoo.tfm.pets.Component
import dev.martianzoo.tfm.pets.classNamePattern
import dev.martianzoo.util.toSetCareful

/**
 * The declaration of a component class, such as GreeneryTile. Models the declaration textually as
 * it was provided.
 */
data class ComponentDefinition(
    /** Unique name for this component class. */
    val name: String,

    /** If `true`, types are always abstract, even when all dependencies are concrete. Not inherited. */
    val abstract: Boolean = false,

    /**
     * Zero or more direct supertypes, including specializations, as PETS TypeExpressions. Don't
     * include `Component` or any types that are already indirect supertypes (unless specializing them).
     */
    @Json(name = "supertypes")
    val supertypesText: Set<String> = setOf(),

    /**
     * Dependencies declared for this component class; it will also inherit the dependencies of its
     * supertypes but those are not listed here.
     */
    @Json(name = "dependencies")
    val dependenciesText: List<String> = listOf(),

    @Json(name = "immediate")
    val immediateText: String? = null,

    @Json(name = "actions")
    val actionsText: Set<String> = setOf(),

    /**
     * Zero or more unordered effects that belong to each *instance* of this component class,
     * expressed in PETS. If the exact name of a dependency type is used in an effect, and a
     * subtype of this type specializes that dependency, then its inherited copy of this effect will
     * have that type specialized in the same way. These pets expressions can (and should) make
     * use of `This` and `Me` and can rely on type defaults.
     */
    @Json(name = "effects")
    val effectsText: Set<String> = setOf(),
) : Definition {

  init {
    require(name.matches(classNamePattern()))
  }

  override val asComponentDefinition = this

  companion object {
    fun from(component: Component): ComponentDefinition {
      return ComponentDefinition(
          component.expression.className,
          component.abstract,
          component.supertypes.map(Any::toString).toSetCareful(),
          component.expression.specializations.map(Any::toString),
          null,
          component.actions.map(Any::toString).toSetCareful(),
          component.effects.map(Any::toString).toSetCareful(),
      )
    }
  }
}
