package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.state.Catalog
import dev.martianzoo.state.ClassSelection
import dev.martianzoo.state.GamePremise

internal fun testClassTable(source: String): ClassTable = testCatalog(source).classTable

internal fun testGamePremise(source: String = "CLASS Token", players: Int = 1): GamePremise {
  require(players >= 0)
  val playerDeclarations =
      if (players == 0) ""
      else
          """
          ABSTRACT CLASS Player : Owner, Actor {
            HAS =1 This
            ${(1..players).joinToString("\n            ") { "CLASS Player$it" }}
          }
          """
  val catalog = testCatalog("$playerDeclarations\n$source")
  val selections = parseClasses(source.trimIndent()).map { ClassSelection(it.className) }.toSet()
  return GamePremise(
      catalog = catalog,
      modules = emptySet(),
      classSelections = selections,
      initialComponentTypes = emptySet(),
      playerNames = (1..players).map { cn("Player$it") },
  )
}

private fun testCatalog(source: String): Catalog {
  val explicitDeclarations = parseClasses(source.trimIndent()).toSet()
  return object : Catalog() {
    override val explicitClassDeclarations: Set<ClassDeclaration> = explicitDeclarations
  }
}
