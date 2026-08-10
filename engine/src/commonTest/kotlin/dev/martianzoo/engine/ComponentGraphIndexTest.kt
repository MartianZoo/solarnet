package dev.martianzoo.engine

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.tfm.api.TfmRuleset
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.canonicalPremise
import dev.martianzoo.types.te
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ComponentGraphIndexTest {
  @Test
  fun componentInMultipleTopLevelBranchesIsCountedOnce() {
    val game = Engine.newGame(canonicalPremise(ruleset = IndexProbeRuleset))
    val gameplay = game.gameplay(PLAYER1).godMode()
    val componentCount = gameplay.count("Component")
    val checkpoint = game.timeline.checkpoint()

    gameplay.manual("3 BothBranches!")

    gameplay.count("LeftBranch") shouldBe 3
    gameplay.count("RightBranch") shouldBe 3
    gameplay.count("BothBranches") shouldBe 3
    gameplay.count("Component") shouldBe componentCount + 3
    game.reader.getComponents(game.reader.resolve(te("Component"))).size shouldBe componentCount + 3

    game.timeline.rollBack(checkpoint)

    gameplay.count("LeftBranch") shouldBe 0
    gameplay.count("RightBranch") shouldBe 0
    gameplay.count("Component") shouldBe componentCount
  }
}

private object IndexProbeRuleset : TfmRuleset.Composite(Canon, IndexProbeDeclarations)

private object IndexProbeDeclarations : TfmRuleset.Empty() {
  override val explicitClassDeclarations =
      parseClasses(
              """
              ABSTRACT CLASS LeftBranch
              ABSTRACT CLASS RightBranch
              CLASS BothBranches : LeftBranch, RightBranch, AutoLoad
              """
                  .trimIndent()
          )
          .toSet()
}
