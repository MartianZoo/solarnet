package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.TfmRuleset
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldInclude
import kotlin.test.Test

internal class RulesetCompositionTest {
  @Test
  fun composedRulesetCreatesAWorkingGame() {
    val extension =
        object : TfmRuleset.Empty() {
          override val explicitClassDeclarations =
              setOf(parseOneLinerClass("CLASS CompositionProbe : AutoLoad"))
        }
    val ruleset = TfmRuleset.compose(Canon, extension)

    val options = Canon.options("BM", 2)
    val game = setUpGame(GameSetup(ruleset.resolve(Canon.bundleNames(options)), options))

    game.reader.ruleset.allClassNames shouldBe game.setup.ruleset.allClassNames
    game.classes.allClassNamesAndIds.shouldContain(cn("CompositionProbe"))
    game.gameplay(PLAYER1).count("TerraformRating<Player1>") shouldBe 20
  }

  @Test
  fun singletonCreationWaitsForDependencies() {
    val extension =
        object : TfmRuleset.Empty() {
          override val explicitClassDeclarations =
              parseClasses(
                      """
                      CLASS DependentBootstrap<BootstrapDependency> : AutoLoad { HAS =1 This }
                      CLASS BootstrapDependency : AutoLoad { HAS =1 This }
                      """
                          .trimIndent()
                  )
                  .toSet()
        }
    val options = Canon.options("BM", 2)
    val ruleset = TfmRuleset.compose(Canon, extension)

    val game = Engine.newGame(GameSetup(ruleset.resolve(Canon.bundleNames(options)), options))

    game.gameplay(PLAYER1).count("BootstrapDependency") shouldBe 1
    game.gameplay(PLAYER1).count("DependentBootstrap<BootstrapDependency>") shouldBe 1
  }

  @Test
  fun singletonDependencyStallHasUsefulDiagnostic() {
    val extension =
        object : TfmRuleset.Empty() {
          override val explicitClassDeclarations =
              parseClasses(
                      """
                      CLASS MissingBootstrapDependency : AutoLoad { HAS MAX 1 This }
                      CLASS BlockedBootstrap<MissingBootstrapDependency> : AutoLoad {
                        HAS =1 This
                      }
                      """
                          .trimIndent()
                  )
                  .toSet()
        }
    val options = Canon.options("BM", 2)
    val ruleset = TfmRuleset.compose(Canon, extension)

    val failure =
        shouldThrow<IllegalStateException> {
          Engine.newGame(GameSetup(ruleset.resolve(Canon.bundleNames(options)), options))
        }

    failure.message.orEmpty().shouldInclude("BlockedBootstrap<MissingBootstrapDependency>")
    failure.message.orEmpty().shouldInclude("requires MissingBootstrapDependency")
  }
}
