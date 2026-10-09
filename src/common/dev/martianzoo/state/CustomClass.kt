package dev.martianzoo.state

import dev.martianzoo.catalog.Catalog
import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

/**
 * Shared identity of a Kotlin-backed Pets metric or instruction. By default its Pets class name is
 * the implementation's Kotlin simple name.
 */
public abstract class CustomClass(name: String? = null) : HasClassName {
  public constructor(className: ClassName) : this(className.toString())

  final override val className: ClassName = cn(name ?: requireNotNull(this::class.simpleName))
}

/** Checks the executable implementations supplied when starting a live game. */
public fun validateCustomClasses(catalog: Catalog, customClasses: Set<CustomClass>) {
  val implementationsByName = customClasses.groupBy(CustomClass::className)
  implementationsByName.entries
      .firstOrNull { it.value.size > 1 }
      ?.let { (name, _) ->
        throw InvalidPetDefinitionException(
            "multiple custom implementations for `$name`",
            sourceLocation = catalog.allClassDeclarations[name]?.className?.sourceLocation,
        )
      }

  catalog.allClassDeclarations.values.forEach { declaration ->
    try {
      val implementation = implementationsByName[declaration.className]?.singleOrNull()
      if (declaration.customMetric || declaration.customInstruction) {
        if (implementation == null) {
          throw InvalidPetDefinitionException(
              "custom class implementation not found for `${declaration.className}`"
          )
        }
        if (
            declaration.customMetric && implementation !is CustomMetric ||
                declaration.customInstruction && implementation !is CustomInstruction
        ) {
          throw InvalidPetDefinitionException(
              "`${declaration.className}` must use a matching Kotlin implementation"
          )
        }
      } else if (implementation != null) {
        throw InvalidPetDefinitionException(
            "non-custom class `${declaration.className}` has a custom implementation"
        )
      }
    } catch (e: InvalidPetDefinitionException) {
      if (e.sourceLocation == null) e.sourceLocation = declaration.className.sourceLocation
      throw e
    }
  }
}
