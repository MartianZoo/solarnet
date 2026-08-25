package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.ClassSelection
import dev.martianzoo.pets.data.GamePremise
import dev.martianzoo.tfm.canon.TfmAuthority
import dev.martianzoo.tfm.engine.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

internal class GameWorldAtomicityTest {
  @Test
  internal fun failedOperationRestoresTheWholeWorldTogether() {
    val world = Engine.newGame(premise) as WholeWorld
    val engine = world.gameplay(ENGINE).godMode()
    val checkpoint = world.timeline.checkpoint()
    val revision = world.revision
    var successfulCompletions = 0
    world.onAtomicComplete = { successfulCompletions++ }

    shouldThrow<IllegalStateException> {
      engine.manual("Marker") {
        engine.addTasks("Decision")
        error("fail after changing both present and future")
      }
    }

    engine.count("Marker") shouldBe 0
    world.tasks.isEmpty() shouldBe true
    world.events.entriesSince(checkpoint).shouldBeEmpty()
    world.timeline.checkpoint() shouldBe checkpoint
    world.revision shouldNotBe revision
    successfulCompletions shouldBe 0
  }

  private companion object {
    val authority =
        object : TfmAuthority() {
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
            authority = authority,
            modules = emptySet(),
            classSelections =
                setOf(
                    ClassSelection(cn("Marker")),
                    ClassSelection(cn("Decision")),
                    ClassSelection(cn("Player")),
                ),
            initialComponentTypes = emptySet(),
        )
  }
}
