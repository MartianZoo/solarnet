package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.CustomCodeException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.CustomInstruction
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TaskProbeTest {
  @Test
  internal fun selectionPropagatesCustomCodeFailureAndRestoresTheAttempt() {
    checkBrokenImplementation { engine, task -> engine.canSelectTask(task) }
  }

  @Test
  internal fun executionPropagatesCustomCodeFailureAndRestoresTheAttempt() {
    checkBrokenImplementation { engine, task -> engine.canExecuteTask(task) }
  }

  private fun checkBrokenImplementation(probe: (ActorEngine, TaskId) -> Boolean) {
    val world =
        Engine.newGame(
            testGamePremise("CLASS Broken : CustomInstruction"),
            setOf(
                object : CustomInstruction("Broken") {
                  override fun translate(game: GameReader): InstructionTree =
                      error("broken translator")
                }
            ),
        )
    val engine = world.actorEngine(ADMIN)
    val task = engine.addTasks(InstructionGroup.of(parse<Instruction>("Broken!"))).single()
    val pending = world.tasks.extract { it }
    val before = world.events.nextOrdinal

    val failure = shouldThrow<CustomCodeException> { probe(engine, task) }

    failure.cause?.message shouldBe "broken translator"
    world.tasks.extract { it } shouldBe pending
    world.events.nextOrdinal shouldBe before
  }

  @Test
  internal fun selectionPropagatesFailureDuringDeferredPromotion() {
    checkFailedPromotion { engine, task -> engine.canSelectTask(task) }
  }

  @Test
  internal fun executionPropagatesFailureDuringDeferredPromotion() {
    checkFailedPromotion { engine, task -> engine.canExecuteTask(task) }
  }

  private fun checkFailedPromotion(probe: (ActorEngine, TaskId) -> Boolean) {
    val world = Engine.newGame(testGamePremise("CLASS Token", players = 2))
    val engine = world.actorEngine(ADMIN)
    val task =
        engine
            .addTasks(
                InstructionGroup.of(
                    parse<InstructionTree>("Token!, FINALLY Admin { TASK Player { Token! } }")
                )
            )
            .first()
    val pending = world.tasks.extract { it }
    val before = world.events.nextOrdinal

    shouldThrow<ExpressionException> { probe(engine, task) }

    world.tasks.extract { it } shouldBe pending
    world.events.nextOrdinal shouldBe before
    world.reader.count(parse<Metric>("Token")) shouldBe 0
  }

  @Test
  internal fun selectionDoesNotExecuteTheReceivingActorsInstruction() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Rejected {
                  This:: Notice!
                  This: Die
                }
                CLASS Notice
                """,
                players = 2,
            )
        )
    val engine = world.actorEngine(PLAYER1)
    val task =
        engine.addTasks(InstructionGroup.of(parse<Instruction>("Rejected! BY Player2"))).single()
    val pending = world.tasks.extract { it }
    val before = world.events.nextOrdinal
    val notices = mutableListOf<Int>()
    val subscription =
        world.components.listenToCount(
            world.reader.resolve(parse("Notice")),
            world.reader,
            notices::add,
        )

    engine.canSelectTask(task) shouldBe true
    engine.canExecuteTask(task) shouldBe false
    world.tasks.extract { it } shouldBe pending
    world.events.nextOrdinal shouldBe before
    notices shouldBe listOf(0)

    engine.selectTask(task)

    world.tasks.getTaskData(task).assignee shouldBe PLAYER2
    notices shouldBe listOf(0)
    subscription.cancel()
  }
}
