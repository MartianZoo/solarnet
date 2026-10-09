package dev.martianzoo.state

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.types.ClassTable

/** Builds a catalog from Pets source, plus the system classes. */
internal fun testCatalog(
    petsText: String,
    customClassDependencies: Map<ClassName, Set<ClassName>> = emptyMap(),
    moduleSelections: Map<ClassName, Set<ClassSelection>> = emptyMap(),
    classAvailabilityModules: Map<ClassName, Set<ClassName>> = emptyMap(),
): Catalog {
  val explicitDeclarations = parseClasses(petsText).toSet()
  return object : Catalog() {
    override val explicitClassDeclarations: Set<ClassDeclaration> = explicitDeclarations
    override val customClassDependencies: Map<ClassName, Set<ClassName>> = customClassDependencies
    override val modules: Map<ClassName, Set<ClassSelection>> = moduleSelections
    override val classAvailabilityModules: Map<ClassName, Set<ClassName>> = classAvailabilityModules
  }
}

/** Builds the game view of [catalog] whose premise selects exactly [selectedClassNames]. */
internal fun gameView(catalog: Catalog, vararg selectedClassNames: String): ClassTable =
    GamePremise(
            catalog,
            emptySet(),
            selectedClassNames.mapTo(linkedSetOf()) { ClassSelection(cn(it)) },
            emptySet(),
        )
        .classTable
