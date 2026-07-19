package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.api.TfmRuleset
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class RulesetCompositionTest {
  @Test
  fun composedRulesetCreatesAWorkingGame() {
    val declarations =
        object : TfmRuleset.Empty() {
          override val explicitClassDeclarations = Canon.explicitClassDeclarations
          override val customClasses = Canon.customClasses
        }
    val definitions =
        object : TfmRuleset.Empty() {
          override val cardDefinitions = Canon.cardDefinitions
          override val marsMapDefinitions = Canon.marsMapDefinitions
          override val milestoneDefinitions = Canon.milestoneDefinitions
          override val colonyTileDefinitions = Canon.colonyTileDefinitions
          override val standardActionDefinitions = Canon.standardActionDefinitions
        }
    val ruleset = TfmRuleset.compose(declarations, definitions)

    val game = Engine.newGame(GameSetup(ruleset, "BM", 2))

    game.reader.ruleset shouldBe ruleset
    game.gameplay(PLAYER1).count("TerraformRating<Player1>") shouldBe 20
  }
}
