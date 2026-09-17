package dev.martianzoo.pets.data

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.types.testCatalog
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class GamePremiseTest {
  @Test
  internal fun rejectsInvalidPlayersAndSelections() {
    val catalog =
        testCatalog(
            """
            ABSTRACT CLASS Player : Owner, Actor { CLASS Blue }
            CLASS Ordinary
            CLASS OptionalModule
            """
                .trimIndent(),
            moduleSelections = mapOf(cn("OptionalModule") to emptySet()),
        )

    shouldReject(catalog, playerNames = listOf(cn("Missing")))
    shouldReject(catalog, playerNames = listOf(cn("Blue"), cn("Blue")))
    shouldReject(catalog, modules = setOf(cn("Missing")))
    shouldReject(
        catalog,
        selections =
            setOf(
                ClassSelection(cn("Ordinary")),
                ClassSelection(cn("Ordinary"), included = false),
            ),
    )
    shouldReject(
        catalog,
        selections = setOf(ClassSelection(cn("Ordinary"), requirement = parse("Ordinary"))),
    )
    shouldReject(catalog, selections = setOf(ClassSelection(cn("Missing"))))
    shouldReject(catalog, selections = setOf(ClassSelection(cn("OptionalModule"))))
  }

  @Test
  internal fun rejectsInvalidInitialBootstrapAndPremiseClasses() {
    val catalog =
        testCatalog(
            """
            CLASS Ordinary
            ABSTRACT CLASS Abstract
            CLASS Dependent<Ordinary>
            """
                .trimIndent()
        )

    shouldReject(catalog, initialTypes = setOf(parse("Missing")))
    listOf(cn("Missing"), cn("Abstract"), cn("Dependent")).forEach { invalid ->
      shouldReject(catalog, premiseClassName = invalid)
      shouldReject(catalog, bootstrapClassName = invalid)
    }
  }

  private fun shouldReject(
      catalog: Catalog,
      modules: Set<ClassName> = emptySet(),
      selections: Set<ClassSelection> = emptySet(),
      initialTypes: Set<Expression> = emptySet(),
      playerNames: List<ClassName> = emptyList(),
      bootstrapClassName: ClassName? = null,
      premiseClassName: ClassName? = null,
  ) {
    shouldThrow<IllegalArgumentException> {
      GamePremise(
          catalog,
          modules,
          selections,
          initialTypes,
          playerNames,
          bootstrapClassName,
          premiseClassName,
      )
    }
  }
}
