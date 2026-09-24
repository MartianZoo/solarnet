package dev.martianzoo.pets.data

import dev.martianzoo.pets.TransformHandler
import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.CustomMetric
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.types.ClassTable

/**
 * One coherent namespace containing everything the engine may know about a game.
 *
 * A Catalog owns one validated master [ClassTable]. That table is the reusable schema for all of
 * its games, not a playable world: each [GamePremise] selects an inhabited view of it. Catalog
 * implementations may use internal packaging such as bundles, but callers compose and play exactly
 * one Catalog, in which every class name has one meaning.
 */
public interface Catalog {
  /** The fully compiled Catalog structure shared by its playable games. */
  public val classTable: ClassTable

  /** Handlers for this game's explicitly marked Pets syntax, bound to one game class table. */
  public val transformHandlerFactories: Map<String, (ClassTable) -> TransformHandler>
    get() = emptyMap()

  /**
   * The available Modules and the Class selections each contributes by default.
   *
   * A selected Module and the Classes required by its own declaration form its intrinsic ambient
   * rules; they do not need entries here. Other selections can represent individually overridable
   * Content or implementation-level Classes such as map areas. A conditional selection contributes
   * its Class only when its requirement is met by the completed configuration. Selected Modules are
   * also the complete ambient-rule configuration of a live game.
   */
  public val modules: Map<ClassName, Set<ClassSelection>>
    get() = emptyMap()

  /** Modules whose selection makes each otherwise bundle-local ambient Class available. */
  public val classAvailabilityModules: Map<ClassName, Set<ClassName>>
    get() = emptyMap()

  /**
   * Natural-language display names keyed first by language tag and then by canonical class name.
   */
  public val displayNamesByLanguage: Map<String, Map<ClassName, String>>
    get() = emptyMap()

  /** The unique declaration for every class in this Catalog's namespace. */
  public val allClassDeclarations: Map<ClassName, ClassDeclaration>

  /** Every canonical class name in this Catalog's namespace. */
  public val allClassNames: Set<ClassName>
    get() = allClassDeclarations.keys

  /**
   * Direct source declarations, before transitional structured data is converted to declarations.
   */
  public val explicitClassDeclarations: Set<ClassDeclaration>

  /** Every exceptional Kotlin implementation for this Catalog's `Custom` classes. */
  public val customClasses: Set<CustomClass>

  /** Returns the unique declaration having [name]. */
  public fun classDeclaration(name: ClassName): ClassDeclaration =
      allClassDeclarations[name]
          ?: throw IllegalArgumentException("no class declaration named `$name`")

  /**
   * Returns the custom instruction implementation having [className].
   *
   * @throws InvalidPetDefinitionException if the Catalog declares no implementation for [className]
   */
  public fun customClass(className: ClassName): CustomClass =
      customClasses.firstOrNull { it.className == className && it !is CustomMetric }
          ?: customClasses.firstOrNull { it.className == className }
          ?: throw InvalidPetDefinitionException(
              "custom class implementation not found for `$className`"
          )

  /** Returns the custom metric implementation having [className], if any. */
  public fun customMetric(className: ClassName): CustomMetric? =
      customClasses.filterIsInstance<CustomMetric>().firstOrNull { it.className == className }
}
