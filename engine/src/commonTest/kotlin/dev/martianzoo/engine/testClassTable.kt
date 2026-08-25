package dev.martianzoo.engine

import dev.martianzoo.api.CustomClass
import dev.martianzoo.data.Authority
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.ClassSelection
import dev.martianzoo.data.Definition
import dev.martianzoo.data.GamePremise
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.systemClassDeclarations
import dev.martianzoo.types.ClassLoader
import dev.martianzoo.types.ClassTable

internal fun testClassTable(source: String): ClassTable = testAuthority(source).classTable

internal fun testGamePremise(source: String = "CLASS Token", players: Int = 1): GamePremise {
  require(players in 0..5)
  val playerDeclarations =
      if (players == 0) ""
      else
          """
          ABSTRACT CLASS Player : Owner, Actor {
            HAS =1 This
            CLASS ${(1..players).joinToString { "Player$it" }}
          }
          """
  val authority = testAuthority("$playerDeclarations\n$source")
  val selections = parseClasses(source.trimIndent()).map { ClassSelection(it.className) }.toSet()
  return GamePremise(
      authority = authority,
      modules = emptySet(),
      classSelections = selections,
      initialComponentTypes = emptySet(),
      playerNames = (1..players).map { cn("Player$it") },
  )
}

private fun testAuthority(source: String): Authority {
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
  return authority
}
