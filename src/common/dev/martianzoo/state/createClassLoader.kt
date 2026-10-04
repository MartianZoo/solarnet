package dev.martianzoo.state

import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.SystemClasses.CUSTOM_INSTRUCTION
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.types.ClassLoader

/**
 * Supplies [catalog]'s declarations, transforms, and Kotlin implementation checks to a loader.
 * Ordinary function dispatch preserves overridden declarations and transforms on delegating
 * Catalogs.
 */
public fun createClassLoader(catalog: Catalog): ClassLoader =
    ClassLoader(
        declarations = catalog.allClassDeclarations,
        transformHandlerFactories = catalog.transformHandlerFactories,
        validateDeclaration = { validateCustomImplementation(catalog, it) },
        additionalRequiredClasses = { requiredClassNames(catalog, it) },
    )

/** Dependencies declared by the Kotlin implementation rather than by the Pets source. */
internal fun requiredClassNames(catalog: Catalog, declaration: ClassDeclaration): Set<ClassName> =
    if (
        declaration.customMetric ||
            declaration.supertypes.any { it.className == CUSTOM_INSTRUCTION }
    ) {
      catalog.customClass(declaration.className).requiredClassNames
    } else emptySet()

private fun validateCustomImplementation(catalog: Catalog, decl: ClassDeclaration) {
  val customInstruction = decl.supertypes.any { it.className == CUSTOM_INSTRUCTION }
  if (decl.customMetric && customInstruction) {
    throw InvalidPetDefinitionException(
        "`${decl.className}` cannot be both a virtual metric and a Signal"
    )
  }
  if (decl.customMetric || customInstruction) {
    val implementation = catalog.customClass(decl.className)
    if (
        decl.customMetric && implementation !is CustomMetric ||
            customInstruction && implementation !is CustomInstruction
    ) {
      throw InvalidPetDefinitionException(
          "`${decl.className}` must use a matching Kotlin implementation"
      )
    }
  } else if (catalog.customClasses.any { it.className == decl.className }) {
    throw InvalidPetDefinitionException(
        "non-custom class `${decl.className}` has a custom implementation"
    )
  }
}
