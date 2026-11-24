package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agenttestsupport.runSystem
import dev.martianzoo.catalog.ClassSelection
import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.count
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
    val world = TfmEngine.newGame(premise)
    val admin = Agents(world)
    val checkpoint = world.events.nextOrdinal

    shouldThrow<IllegalStateException> {
      admin.runSystem("Marker") {
        admin.world
            .actorEngine(ADMIN)
            .addTasks(InstructionGroup.of(Parsing.parse<InstructionTree>("Decision")))
        error("fail after changing both present and future")
      }
    }

    admin.world.reader.count("Marker") shouldBe 0
    world.tasks.isEmpty() shouldBe true
    world.events.entriesSince(checkpoint).shouldBeEmpty()
    world.events.nextOrdinal shouldBe checkpoint
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
