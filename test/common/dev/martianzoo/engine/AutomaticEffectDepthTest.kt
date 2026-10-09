package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.engine.Exceptions.RunawayEffectChainException
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.ClassSelection
import dev.martianzoo.state.GamePremise
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmEngine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AutomaticEffectDepthTest {
  @Test
  internal fun `automatic effect cycle fails atomically at the depth limit`() {
    val world = TfmEngine.newGame(premise) as WholeWorld
    val admin = world.testAgent(ADMIN)
    val checkpoint = world.timeline.checkpoint()

    val failure = shouldThrow<RunawayEffectChainException> { admin.runOperation("ChainA") }

    failure.maximumDepth shouldBe 8
    failure.effectChain.map { it.instructions.single() } shouldBe
        listOf(
                "ChainB!",
                "ChainA!",
                "ChainB!",
                "ChainA!",
                "ChainB!",
                "ChainA!",
                "ChainB!",
                "ChainA!",
                "ChainB!",
            )
            .map { parse<Instruction>(it) }
    admin.count("ChainA") shouldBe 0
    admin.count("ChainB") shouldBe 0
    world.events.entriesSince(checkpoint).shouldBeEmpty()
  }

  private companion object {
    val catalog =
        object : TfmCatalog() {
          override val explicitClassDeclarations =
              parseClasses(
                      """
                      CLASS ChainA { This:: ChainB }
                      CLASS ChainB { This:: ChainA }
                      ABSTRACT CLASS Player : Owner, Actor
                      """
                          .trimIndent()
                  )
                  .toSet()
        }

    val premise =
        GamePremise(
            catalog = catalog,
            classSelections =
                setOf(
                    ClassSelection(cn("ChainA")),
                    ClassSelection(cn("ChainB")),
                    ClassSelection(cn("Player")),
                ),
        )
  }
}
