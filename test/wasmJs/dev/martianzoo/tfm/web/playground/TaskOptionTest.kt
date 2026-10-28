package dev.martianzoo.tfm.web.playground

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.Player
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.CardTracking
import dev.martianzoo.tfm.engine.TfmEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class TaskOptionTest {
  private val world = TfmEngine.newGame(Canon.gamePremise(GameConfig("TharsisMap", "Red")))
  private val agents = Agents(world)
  private val tracker = CardTracking.start(world, requireSelectingNames = true)
  private val player = world.actors.filterIsInstance<Player>().single()
  private val agent = agents[player].also { it.autoExecPolicy = AutoExecPolicy.NONE }

  @Test
  fun concretePerShowsAmountWithoutGainingResources() {
    agent.sneak("3 Heat")
    val id = agent.addTasks("Plant / Heat").single()
    world.onTransactionComplete = { error("A preview must not notify the application") }

    val option = preview(id)

    assertTrue(option.enabled)
    assertEquals("$id  3! Plant<Red>", option.label)
    assertEquals(id, option.taskId)
    assertEquals(0, agent.count("Plant"))
  }

  @Test
  fun evaluatedOptionalGainRemainsAChoiceWithoutHoldingTheSelection() {
    agent.sneak("3 Heat")
    val id = agent.addTasks("Plant? / Heat").single()

    val option = preview(id)

    assertTrue(option.enabled)
    assertEquals("$id  3? Plant<Red>", option.label)
    assertEquals(null, world.tasks.selectedTask())
  }

  @Test
  fun zeroPerPreservesTheTypeWithoutRemovingThePendingTask() {
    val id = agent.addTasks("Plant / Heat").single()

    val option = preview(id)

    assertTrue(option.enabled)
    assertEquals("$id  0! Plant<Red>", option.label)
  }

  @Test
  fun unavailableRemovalStaysListedAndDoesNotAffectTheNextPreview() {
    agent.sneak("3 Heat")
    val unavailable = agent.addTasks("-4 Heat").single()
    val available = agent.addTasks("Plant / Heat").single()

    val option = preview(unavailable)

    assertFalse(option.enabled)
    assertEquals("$unavailable  -4! Heat<Red>", option.label)
    assertTrue(preview(available).enabled)
    assertEquals(3, agent.count("Heat"))
  }

  @Test
  fun structuralSelectionOffersExpansionWithoutCombiningItsChildren() {
    agent.sneak("Heat")
    val id = agent.addTasks("Heat: (Plant?, Steel?)").single()

    val option = preview(id)

    assertTrue(option.enabled)
    assertEquals("$id  Expand into separate tasks", option.label)
    assertEquals(id, option.taskId)
  }

  @Test
  fun cardDrawPreviewDoesNotAskTheDealerForNames() {
    val id = agent.addTasks("ProjectCard").single()

    assertTrue(preview(id).enabled)
    assertEquals(emptySet(), tracker.hand(player))
    assertEquals(0, agent.count("ProjectCard"))
  }

  @Test
  fun unlistableNarrowingsStillShowTheOriginalOrArms() {
    val id = agent.addTasks("Heat OR Plant").single()

    val option =
        taskOption(world, world.tasks.getTaskData(id)) {
          throw UnsupportedOperationException("cannot enumerate this form")
        }

    assertTrue(option.enabled)
    assertEquals(2, option.arms.size)
    assertTrue(option.arms.any { it.contains("Heat") })
    assertTrue(option.arms.any { it.contains("Plant") })
    assertEquals(null, world.tasks.selectedTask())
  }

  private fun preview(id: TaskId): Prompt.Option {
    val tasks = world.tasks.extract { it }
    val events = world.events.entriesSince(Checkpoint(0))
    val components =
        world.reader.getComponents(cn("Component").expression).entries.associate {
          it.key to it.value
        }
    val positions = world.timeline.positions
    val checkpoint = world.timeline.checkpoint()

    val option = taskOption(world, world.tasks.getTaskData(id))

    assertEquals(tasks, world.tasks.extract { it })
    assertEquals(events, world.events.entriesSince(Checkpoint(0)))
    assertEquals(
        components,
        world.reader.getComponents(cn("Component").expression).entries.associate {
          it.key to it.value
        },
    )
    assertEquals(positions, world.timeline.positions)
    assertEquals(checkpoint, world.timeline.checkpoint())
    return option
  }
}
