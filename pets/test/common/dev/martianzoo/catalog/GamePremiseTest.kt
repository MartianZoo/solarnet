package dev.martianzoo.catalog

import dev.martianzoo.catalogtestsupport.testCatalog
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.ClassDeclaration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
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

    shouldThrow<InvalidGameConfigException> {
      premise(catalog, playerNames = listOf(cn("Missing")))
    }
    shouldReject(catalog, playerNames = listOf(cn("Blue"), cn("Blue")))
    shouldReject(catalog, selections = setOf(ClassSelection(cn("Missing"))))
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
    premise(catalog, selections = setOf(ClassSelection(cn("OptionalModule")))).modules shouldBe
        setOf(cn("OptionalModule"))
  }

  @Test
  internal fun rejectsInvalidBootstrapAndPremiseClasses() {
    val catalog =
        testCatalog(
            """
            CLASS Ordinary
            ABSTRACT CLASS Abstract
            CLASS Dependent<Ordinary>
            """
                .trimIndent()
        )

    listOf(cn("Missing"), cn("Abstract"), cn("Dependent")).forEach { invalid ->
      shouldReject(catalog, premiseClassName = invalid)
      shouldReject(catalog, bootstrapClassName = invalid)
    }
  }

  @Test
  internal fun rejectsConflictingPremiseDeclarationsAsInvalidConfiguration() {
    val catalog = testCatalog("CLASS Existing")
    val duplicateDeclarations =
        (parseClasses("CLASS Local") + parseClasses("ABSTRACT CLASS Local")).toSet()

    shouldThrow<InvalidGameConfigException> {
      premise(catalog, premiseDeclarations = duplicateDeclarations)
    }
    shouldThrow<InvalidGameConfigException> {
      premise(catalog, premiseDeclarations = parseClasses("CLASS Existing").toSet())
    }
  }

  private fun shouldReject(
      catalog: Catalog,
      selections: Set<ClassSelection> = emptySet(),
      playerNames: List<ClassName> = emptyList(),
      bootstrapClassName: ClassName? = null,
      premiseClassName: ClassName? = null,
  ) {
    shouldThrow<InvalidGameConfigException> {
      premise(
          catalog,
          selections,
          playerNames,
          bootstrapClassName,
          premiseClassName,
      )
    }
  }

  private fun premise(
      catalog: Catalog,
      selections: Set<ClassSelection> = emptySet(),
      playerNames: List<ClassName> = emptyList(),
      bootstrapClassName: ClassName? = null,
      premiseClassName: ClassName? = null,
      premiseDeclarations: Set<ClassDeclaration> = emptySet(),
  ): GamePremise =
      GamePremise(
          catalog = catalog,
          classSelections = selections,
          playerNames = playerNames,
          bootstrapClassName = bootstrapClassName,
          premiseClassName = premiseClassName,
          premiseClassDeclarations = premiseDeclarations,
      )
}
