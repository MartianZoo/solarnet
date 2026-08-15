package dev.martianzoo.data

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.CustomMetric
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.types.ClassTable

/** One coherent catalog of everything the engine may know about a game. */
public interface Authority {
  /** The one fully compiled class universe from which playable projections are formed. */
  public val classTable: ClassTable

  /** Declarative validity checks evaluated against a resolved game premise. */
  public val bootstrapValidations: List<Set<Requirement>>
    get() = emptyList()

  /** The available Modules and the class selections each one contributes. */
  public val modules: Map<ClassName, Set<ClassSelection>>
    get() = emptyMap()

  /** ASCII display names keyed first by language tag and then by canonical class name. */
  public val displayNamesByLanguage: Map<String, Map<ClassName, String>>
    get() = emptyMap()

  /** Classes whose localized Pets names are derived from their natural display names. */
  public val derivedPetsNameClassNames: Set<ClassName>
    get() = emptySet()

  /** The unique declaration for every class in this Authority's namespace. */
  public val allClassDeclarations: Map<ClassName, ClassDeclaration>

  /** Every canonical class name in this Authority's namespace. */
  public val allClassNames: Set<ClassName>
    get() = allClassDeclarations.keys

  /** Direct source declarations, before structured definitions are converted to declarations. */
  public val explicitClassDeclarations: Set<ClassDeclaration>

  /** Every structured component definition known to this Authority. */
  public val allDefinitions: Set<Definition>

  /** Every exceptional Kotlin implementation for this Authority's `Custom` classes. */
  public val customClasses: Set<CustomClass>

  /** Returns the unique declaration having [name]. */
  public fun classDeclaration(name: ClassName): ClassDeclaration =
      allClassDeclarations[name]
          ?: throw IllegalArgumentException("no class declaration by name $name")

  /** Returns the custom instruction implementation having [className]. */
  public fun customClass(className: ClassName): CustomClass =
      customClasses.firstOrNull { it.className == className && it !is CustomMetric }
          ?: customClasses.firstOrNull { it.className == className }
          ?: throw IllegalArgumentException(
              "Custom class implementation for `$className` not found"
          )

  /** Returns the custom metric implementation having [className], if any. */
  public fun customMetric(className: ClassName): CustomMetric? =
      customClasses.filterIsInstance<CustomMetric>().firstOrNull { it.className == className }
}
