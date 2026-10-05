package dev.martianzoo.state

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.ClassDeclaration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class CatalogTest {
  @Test
  internal fun missingClassDeclarationIsRejected() {
    val catalog = testCatalog("CLASS Known")

    shouldThrow<IllegalArgumentException> { catalog.classDeclaration(cn("Missing")) }
  }

  @Test
  internal fun everyCatalogIncludesThePetsRuntimeDeclarations() {
    Catalog().classDeclaration(COMPONENT)
  }

  @Test
  internal fun configuringPlayersRequiresAPlayerDeclaration() {
    val failure =
        shouldThrow<InvalidGameConfigException> {
          Catalog().gamePremise(GameConfig("", "Player1"))
        }

    failure.message.orEmpty() shouldContain "Catalog without Player"
  }

  @Test
  internal fun specializedThisInvariantCanLimitOneConcreteClassAcrossPlayers() {
    val table =
        catalog(
                *parseClasses(
                        """
                        ABSTRACT CLASS Player : Anyone {
                          HAS =1 This
                          CLASS Player1
                          CLASS Player2
                        }
                        ABSTRACT CLASS CardFront<@Player> : Owned<@Player> {
                          HAS MAX 1 This<Player>
                        }
                        CLASS ExampleCard : CardFront
                        """
                            .trimIndent()
                    )
                    .toTypedArray()
            )
            .classTable
    val expectedLimitType = table.resolve(parse("ExampleCard<Player>"))

    listOf("ExampleCard<Player1>", "ExampleCard<Player2>").forEach { expression ->
      table.componentLimits
          .limitsFor(table.resolve(parse(expression)))
          .single { it.range.last == 1 }
          .type shouldBe expectedLimitType
    }
  }

  @Test
  internal fun selectedClassViabilityUsesItsLoadedDeclaration() {
    val source =
        catalog(
            *parseClasses(
                    """
                    CLASS Missing
                    CLASS Selected { This: -Missing! }
                    """
                        .trimIndent()
                )
                .toTypedArray()
        )

    val unavailable =
        shouldThrow<InvalidGameConfigException> {
          source.gamePremise(GameConfig("Selected")).classTable
        }

    unavailable.message.orEmpty() shouldContain
        "unviable game premise: `Selected` has reachable mandatory removal Missing"
  }

  @Test
  internal fun compositionCoalescesIdenticalClassDeclarations() {
    val declaration = parseOneLinerClass("CLASS Shared")
    val composed = Catalog(catalog(declaration), catalog(declaration))

    composed.classDeclaration(cn("Shared")) shouldBe declaration
  }

  @Test
  internal fun compositionRejectsDifferentDeclarationsWithTheSameName() {
    val concrete = parseOneLinerClass("CLASS Shared")
    val abstract = parseOneLinerClass("ABSTRACT CLASS Shared")

    shouldThrow<InvalidPetDefinitionException> {
      Catalog(catalog(concrete), catalog(abstract)).allClassDeclarations
    }
  }

  @Test
  internal fun compositionRejectsConflictingDisplayNames() {
    fun named(displayName: String) =
        object : Catalog() {
          override val displayNamesByLanguage = mapOf("en" to mapOf(cn("Shared") to displayName))
        }

    shouldThrow<IllegalArgumentException> {
      Catalog(named("First"), named("Second")).displayNamesByLanguage
    }
  }

  private fun catalog(vararg declarations: ClassDeclaration): Catalog =
      object : Catalog() {
        override val explicitClassDeclarations = declarations.toSet()
      }
}
