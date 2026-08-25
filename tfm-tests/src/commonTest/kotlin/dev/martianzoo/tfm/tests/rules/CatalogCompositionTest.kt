package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.*
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
    val catalog = TfmCatalog.compose(Canon, extension)

    val game = setUpGame(canonicalPremise(catalog = catalog))

    game.classTable.allClassNames.shouldContain(cn("CompositionProbe"))
    game.gameplay(PLAYER1).count("TerraformRating<Player1>") shouldBe 20
  }

  @Test
  internal fun singletonCreationWaitsForDependencies() {
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
    val catalog = TfmCatalog.compose(Canon, extension)

    val game = Engine.newGame(canonicalPremise(catalog = catalog))

    game.gameplay(PLAYER1).count("BootstrapDependency") shouldBe 1
    game.gameplay(PLAYER1).count("DependentBootstrap<BootstrapDependency>") shouldBe 1
  }

  @Test
  internal fun singletonDependencyStallHasUsefulDiagnostic() {
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
    val catalog = TfmCatalog.compose(Canon, extension)

    val failure = shouldThrow<PetException> { Engine.newGame(canonicalPremise(catalog = catalog)) }

    failure.message.orEmpty().shouldInclude("BlockedBootstrap<MissingBootstrapDependency>")
    failure.message.orEmpty().shouldInclude("requires MissingBootstrapDependency")
  }

  @Test
  internal fun `inactive gated provenance incorrectly creates a bootstrap cycle`() {
    val extension =
        object : TfmCatalog() {
          override val explicitClassDeclarations =
              parseClasses(
                      """
                      CLASS BootstrapSource<BootstrapTarget> : Module {
                        This IF ColoniesExpansion: BootstrapTarget
                      }
                      CLASS BootstrapTarget { HAS =1 This }
                      """
                          .trimIndent()
                  )
                  .toSet()
        }
    val catalog = TfmCatalog.compose(Canon, extension)
    val premise = catalog.gamePremise(GameConfig("BootstrapSource", "Player1", "Player2"))

    val failure = shouldThrow<PetException> { Engine.newGame(premise) }

    failure.message.orEmpty().shouldInclude("is waiting for a constructive source")
  }
}
