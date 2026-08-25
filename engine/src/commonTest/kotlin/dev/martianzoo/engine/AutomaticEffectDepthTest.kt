package dev.martianzoo.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.ClassSelection
import dev.martianzoo.data.GamePremise
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.tfm.canon.TfmAuthority
import dev.martianzoo.tfm.engine.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AutomaticEffectDepthTest {
  @Test
  internal fun `automatic effect cycle fails atomically at the depth limit`() {
    val world = Engine.newGame(premise) as WholeWorld
    val engine = world.gameplay(ENGINE).godMode()
    val checkpoint = world.timeline.checkpoint()

    val failure = shouldThrow<RunawayEffectChainException> { engine.manual("ChainA") }

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
    engine.count("ChainA") shouldBe 0
    engine.count("ChainB") shouldBe 0
    world.events.entriesSince(checkpoint).shouldBeEmpty()
  }

  private companion object {
    val authority =
        object : TfmAuthority() {
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
            authority = authority,
            modules = emptySet(),
            classSelections =
                setOf(
                    ClassSelection(cn("ChainA")),
                    ClassSelection(cn("ChainB")),
                    ClassSelection(cn("Player")),
                ),
            initialComponentTypes = emptySet(),
        )
  }
}
