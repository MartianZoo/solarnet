package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ComponentGraphIndexTest {
  @Test
  internal fun componentInMultipleTopLevelBranchesIsCountedOnce() {
    val game = Engine.newGame(canonicalPremise(catalog = IndexProbeCatalog))
    val agent = game.agent(PLAYER1).godMode()
    val componentCount = agent.count("Component")
    val checkpoint = game.timeline.checkpoint()

    agent.manual("3 BothBranches!")

    agent.count("LeftBranch") shouldBe 3
    agent.count("RightBranch") shouldBe 3
    agent.count("BothBranches") shouldBe 3
    agent.count("Component") shouldBe componentCount + 3
    game.reader.getComponents(game.reader.resolve(parse<Expression>("Component"))).size shouldBe
        componentCount + 3

    game.timeline.rollBack(checkpoint)

    agent.count("LeftBranch") shouldBe 0
    agent.count("RightBranch") shouldBe 0
    agent.count("Component") shouldBe componentCount
  }
}

private object IndexProbeCatalog : TfmCatalog.Composite(Canon, IndexProbeDeclarations)

private object IndexProbeDeclarations : TfmCatalog() {
  override val explicitClassDeclarations =
      parseClasses(
              """
              ABSTRACT CLASS LeftBranch
              ABSTRACT CLASS RightBranch
              CLASS BothBranches : LeftBranch, RightBranch
              """
                  .trimIndent()
          )
          .toSet()
}
