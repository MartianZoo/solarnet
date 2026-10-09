package dev.martianzoo.state

import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
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
  if (declaration.customMetric && declaration.customInstruction) {
    throw InvalidPetDefinitionException(
        "`${declaration.className}` cannot be both a virtual metric and a Signal"
    )
  }
}
