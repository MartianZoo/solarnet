package dev.martianzoo.catalog

import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.types.ClassLoader

/** Supplies [catalog]'s static declarations and transforms to a loader. */
internal fun createClassLoader(catalog: Catalog): ClassLoader =
    ClassLoader(
        declarations = catalog.allClassDeclarations,
        transformHandlerFactories = catalog.transformHandlerFactories,
        validateDeclaration = ::validateCustomDeclaration,
    )

private fun validateCustomDeclaration(declaration: ClassDeclaration) {
  if (declaration.customMetric && declaration.customInstruction) {
    throw InvalidPetDefinitionException(
        "`${declaration.className}` cannot be both a virtual metric and a Signal"
    )
  }
}
