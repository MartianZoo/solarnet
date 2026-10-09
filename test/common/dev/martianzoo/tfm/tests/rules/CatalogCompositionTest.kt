package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.engine.*
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.state.GameConfig
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.tests.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldInclude
import kotlin.test.Test

internal class CatalogCompositionTest {
  @Test
  internal fun composedCatalogCreatesAWorkingGame() {
    val extension =
        object : TfmCatalog() {
          override val explicitClassDeclarations =
              setOf(parseOneLinerClass("CLASS CompositionProbe"))
        }
    val catalog = TfmCatalog(Canon, extension)

    val game = setUpGame(canonicalPremise(catalog = catalog))

    game.classTable.allClassNames.shouldContain(cn("CompositionProbe"))
    game.testAgent(PLAYER1).count("TerraformRating<Player1>") shouldBe 20
  }

  @Test
  internal fun premiseSetupWaitsForDependencies() {
    val extension =
        object : TfmCatalog() {
          override val explicitClassDeclarations =
              parseClasses(
                      """
                      CLASS DependentBootstrap<BootstrapDependency> { HAS =1 This }
                      CLASS BootstrapDependency { HAS =1 This }
                      """
                          .trimIndent()
                  )
                  .toSet()
        }
    val catalog = TfmCatalog(Canon, extension)

    val premise =
        canonicalPremise(
            catalog = catalog,
            setupComponents =
                setOf(
                    cn("BootstrapDependency").expression,
                    parse<Expression>("DependentBootstrap"),
                ),
        )
    val game = TfmEngine.newGame(premise)

    game.testAgent(PLAYER1).count("BootstrapDependency") shouldBe 1
    game.testAgent(PLAYER1).count("DependentBootstrap") shouldBe 1
  }

  @Test
  internal fun premiseSetupReportsMissingDependency() {
    val extension =
        object : TfmCatalog() {
          override val explicitClassDeclarations =
              parseClasses(
                      """
                      CLASS MissingBootstrapDependency { HAS MAX 1 This }
                      CLASS BlockedBootstrap<MissingBootstrapDependency> {
                        HAS =1 This
                      }
                      """
                          .trimIndent()
                  )
                  .toSet()
        }
    val catalog = TfmCatalog(Canon, extension)

    val premise =
        canonicalPremise(
            catalog = catalog,
            setupComponents = setOf(parse<Expression>("BlockedBootstrap")),
        )
    val failure = shouldThrow<InvalidGameConfigException> { TfmEngine.newGame(premise) }

    failure.message.orEmpty().shouldInclude("missing dependencies: `MissingBootstrapDependency`")
  }

  @Test
  internal fun `inactive gated Module effect does not create its target`() {
    val extension =
        object : TfmCatalog() {
          override val explicitClassDeclarations =
              parseClasses(
                      """
                      CLASS BootstrapSource : Module {
                        This IF ColoniesExpansion: BootstrapTarget
                      }
                      CLASS BootstrapTarget { HAS =1 This }
                      """
                          .trimIndent()
                  )
                  .toSet()
        }
    val catalog = TfmCatalog(Canon, extension)
    val premise = catalog.gamePremise(GameConfig("BootstrapSource", "Player1", "Player2"))

    val game = TfmEngine.newGame(premise)

    game.testAgent(PLAYER1).count("BootstrapTarget") shouldBe 0
  }
}
