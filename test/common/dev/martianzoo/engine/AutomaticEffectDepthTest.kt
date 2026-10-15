package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.catalog.ClassSelection
import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.ResourceLimitException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Actor.Companion.ADMIN
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

    val failure = shouldThrow<ResourceLimitException> { admin.runOperation("ChainA") }

    failure.detail shouldBe
        "automatic effect chain exceeded depth 8: " +
            "ChainB! -> ChainA! -> ChainB! -> ChainA! -> ChainB! -> ChainA! -> ChainB! -> ChainA! -> ChainB!"
    admin.count("ChainA") shouldBe 0
    admin.count("ChainB") shouldBe 0
    world.events.entriesSince(checkpoint).shouldBeEmpty()
  }

  @Test
  internal fun canSelectTaskPropagatesResourceLimitsAndRollsBack() {
    val world = TfmEngine.newGame(premise) as WholeWorld
    val admin = world.testAgent(ADMIN).also { it.autoExecPolicy = NONE }
    val task = admin.addTasks("ChainA").single()
    val checkpoint = world.timeline.checkpoint()

    shouldThrow<ResourceLimitException> { admin.canSelectTask(task) }

    world.timeline.checkpoint() shouldBe checkpoint
    admin.tasks.ids() shouldBe setOf(task)
    admin.count("ChainA") shouldBe 0
    admin.count("ChainB") shouldBe 0
    world.events.entriesSince(checkpoint).shouldBeEmpty()
  }

  @Test
  internal fun canExecuteTaskPropagatesResourceLimitsAndRollsBack() {
    val world = TfmEngine.newGame(premise) as WholeWorld
    val admin = world.testAgent(ADMIN).also { it.autoExecPolicy = NONE }
    val task = admin.addTasks("ChainA").single()
    val checkpoint = world.timeline.checkpoint()

    shouldThrow<ResourceLimitException> { admin.canExecuteTask(task) }

    world.timeline.checkpoint() shouldBe checkpoint
    admin.tasks.ids() shouldBe setOf(task)
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
