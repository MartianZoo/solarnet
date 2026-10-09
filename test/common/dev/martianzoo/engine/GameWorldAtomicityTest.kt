package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.catalog.ClassSelection
import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmEngine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GameWorldAtomicityTest {
  @Test
  internal fun failedOperationRestoresTheWholeWorldTogether() {
    val world = TfmEngine.newGame(premise) as WholeWorld
    val admin = world.testAgent(ADMIN)
    val checkpoint = world.timeline.checkpoint()
    var successfulCompletions = 0
    world.onTransactionComplete = { successfulCompletions++ }

    shouldThrow<IllegalStateException> {
      admin.runOperation("Marker") {
        admin.addTasks("Decision")
        error("fail after changing both present and future")
      }
    }

    admin.count("Marker") shouldBe 0
    world.tasks.isEmpty() shouldBe true
    world.events.entriesSince(checkpoint).shouldBeEmpty()
    world.timeline.checkpoint() shouldBe checkpoint
    successfulCompletions shouldBe 0
  }

  private companion object {
    val catalog =
        object : TfmCatalog() {
          override val explicitClassDeclarations =
              parseClasses(
                      """
                      CLASS Marker
                      CLASS Decision
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
                    ClassSelection(cn("Marker")),
                    ClassSelection(cn("Decision")),
                    ClassSelection(cn("Player")),
                ),
        )
  }
}
