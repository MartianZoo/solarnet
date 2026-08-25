package dev.martianzoo.engine

import dev.martianzoo.api.CustomClass
import dev.martianzoo.data.Authority
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.Definition
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.systemClassDeclarations
import dev.martianzoo.types.ClassLoader
import dev.martianzoo.types.ClassTable

internal fun testClassTable(source: String): ClassTable {
  val explicitDeclarations = parseClasses(source.trimIndent()).toSet()
  val declarations = systemClassDeclarations + explicitDeclarations
  val authority =
      object : Authority {
        override val explicitClassDeclarations: Set<ClassDeclaration> = explicitDeclarations
        override val allClassDeclarations: Map<ClassName, ClassDeclaration> =
            declarations.associateBy(ClassDeclaration::className).also {
              require(it.size == declarations.size) { "duplicate test Class declaration" }
            }
        override val allDefinitions: Set<Definition> = emptySet()
        override val customClasses: Set<CustomClass> = emptySet()
        override val classTable: ClassTable by lazy { ClassLoader(this).loadEverything() }
      }
  return authority.classTable
}
