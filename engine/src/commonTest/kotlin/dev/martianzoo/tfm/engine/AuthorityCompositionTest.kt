package dev.martianzoo.tfm.engine

import dev.martianzoo.api.Exceptions.PetException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.TfmAuthority
import dev.martianzoo.tfm.canon.Canon
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldInclude
import kotlin.test.Test

internal class AuthorityCompositionTest {
  @Test
  internal fun composedAuthorityCreatesAWorkingGame() {
    val extension =
        object : TfmAuthority() {
          override val explicitClassDeclarations =
              setOf(parseOneLinerClass("CLASS CompositionProbe"))
        }
    val authority = TfmAuthority.compose(Canon, extension)

    val game = setUpGame(canonicalPremise(authority = authority))

    game.classTable.allClassNames.shouldContain(cn("CompositionProbe"))
    game.gameplay(PLAYER1).count("TerraformRating<Player1>") shouldBe 20
  }

  @Test
  internal fun singletonCreationWaitsForDependencies() {
    val extension =
        object : TfmAuthority() {
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
    val authority = TfmAuthority.compose(Canon, extension)

    val game = Engine.newGame(canonicalPremise(authority = authority))

    game.gameplay(PLAYER1).count("BootstrapDependency") shouldBe 1
    game.gameplay(PLAYER1).count("DependentBootstrap<BootstrapDependency>") shouldBe 1
  }

  @Test
  internal fun singletonDependencyStallHasUsefulDiagnostic() {
    val extension =
        object : TfmAuthority() {
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
    val authority = TfmAuthority.compose(Canon, extension)

    val failure =
        shouldThrow<PetException> {
          Engine.newGame(canonicalPremise(authority = authority))
        }

    failure.message.orEmpty().shouldInclude("BlockedBootstrap<MissingBootstrapDependency>")
    failure.message.orEmpty().shouldInclude("requires MissingBootstrapDependency")
  }
}
