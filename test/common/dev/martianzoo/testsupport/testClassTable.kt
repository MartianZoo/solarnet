package dev.martianzoo.engine

import dev.martianzoo.catalog.Catalog
import dev.martianzoo.catalog.ClassSelection
import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.types.ClassTable

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
      classSelections = selections,
      playerNames = (1..players).map { cn("Player$it") },
  )
}

/** Adds ordinary automatic premise setup for engine tests without production-only setup state. */
internal fun GamePremise.withTestSetup(instruction: String): GamePremise {
  if (instruction.isBlank()) return this
  val effect = parse<Effect>("This:: $instruction")
  val existingName = premiseClassName
  if (existingName != null) {
    val existing = premiseClassDeclarations.single { it.className == existingName }
    return copy(
        premiseClassDeclarations =
            premiseClassDeclarations - existing +
                existing.copy(authoredEffects = existing.authoredEffects + effect)
    )
  }

  val declaration = parseClasses("CLASS TestPremise : System").single()
  val playerEffect =
      if (playerNames.isEmpty()) null
      else parse<Effect>("This:: ${playerNames.joinToString(" THEN ")}")
  return copy(
      premiseClassName = declaration.className,
      premiseClassDeclarations =
          premiseClassDeclarations +
              declaration.copy(authoredEffects = listOfNotNull(playerEffect) + effect),
  )
}

private fun testCatalog(source: String): Catalog {
  val explicitDeclarations = parseClasses(source.trimIndent()).toSet()
  return object : Catalog() {
    override val explicitClassDeclarations: Set<ClassDeclaration> = explicitDeclarations
  }
}
