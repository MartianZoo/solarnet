package dev.martianzoo.state

import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.SystemClasses.CUSTOM_INSTRUCTION
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.types.ClassLoader

/**
 * Supplies [catalog]'s static declarations, transforms, and custom-class dependencies to a loader.
 */
public fun createClassLoader(catalog: Catalog): ClassLoader =
    ClassLoader(
        declarations = catalog.allClassDeclarations,
        transformHandlerFactories = catalog.transformHandlerFactories,
        validateDeclaration = ::validateCustomDeclaration,
        additionalRequiredClasses = { catalog.customClassDependencies[it.className].orEmpty() },
    )

private fun validateCustomDeclaration(declaration: ClassDeclaration) {
  if (
      declaration.customMetric && declaration.supertypes.any { it.className == CUSTOM_INSTRUCTION }
  ) {
    throw InvalidPetDefinitionException(
        "`${declaration.className}` cannot be both a virtual metric and a Signal"
    )
  }
}

/** Checks executable behavior when a static Catalog is bound to a Game World. */
internal fun validateCustomClasses(catalog: Catalog) {
  catalog.allClassDeclarations.values.forEach { declaration ->
    try {
      validateCustomImplementation(catalog, declaration)
    } catch (e: InvalidPetDefinitionException) {
      if (e.sourceLocation == null) e.sourceLocation = declaration.className.sourceLocation
      throw e
    }
  }
}

private fun validateCustomImplementation(catalog: Catalog, decl: ClassDeclaration) {
  val customInstruction = decl.supertypes.any { it.className == CUSTOM_INSTRUCTION }
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
