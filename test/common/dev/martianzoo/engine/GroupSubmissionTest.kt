package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GroupSubmissionTest {
  private val world =
      Engine.newGame(
          testGamePremise(
              """
      CLASS First { This: Reaction }
      CLASS Second
      CLASS Reaction
      CLASS Following
      CLASS Other
      CLASS Forbidden { HAS MAX 0 This }
      """
          )
      )
  private val engine = world.actorEngine(PLAYER1)

  @Test
  fun explicitGroupLeavesReactionsPending() {
    engine.addTasks(InstructionGroup.of(parse<InstructionTree>("(First!, Second!) OR Other!")))

    engine.doTask(parse<InstructionTree>("First!, Second!"))

    world.reader.count(parse<Metric>("First")) shouldBe 1
    world.reader.count(parse<Metric>("Second")) shouldBe 1
    world.reader.count(parse<Metric>("Reaction")) shouldBe 0
    world.reader.count(parse<Metric>("Following")) shouldBe 0
    engine.tasks.ids().size shouldBe 1
  }

  @Test
  fun submittedGroupLeavesAdminMembersForLaterTransactions() {
    engine.addTasks(
        InstructionGroup.of(parse<InstructionTree>("(First! BY Admin, Second!) OR Other!"))
    )

    engine.doTask(parse<InstructionTree>("First! BY Admin, Second!"))

    world.reader.count(parse<Metric>("First")) shouldBe 0
    world.reader.count(parse<Metric>("Second")) shouldBe 1
    world.reader.count(parse<Metric>("Reaction")) shouldBe 0
    val admin = world.actorEngine(ADMIN)
    admin.doTask(admin.tasks.ids().single())
    world.reader.count(parse<Metric>("First")) shouldBe 1
    world.reader.count(parse<Metric>("Reaction")) shouldBe 0
  }

  @Test
  fun explicitGroupUsesItsOwnTasksInsteadOfAnUnrelatedPermission() {
    val unrelated =
        engine
            .addTasks(InstructionGroup.of(parse<InstructionTree>("First! THEN Forbidden!")))
            .single()
    val original = world.tasks.getTaskData(unrelated)
    engine.addTasks(InstructionGroup.of(parse<InstructionTree>("(First!, Second!) OR Other!")))

    engine.doTask(parse<InstructionTree>("First!, Second!"))

    world.reader.count(parse<Metric>("First")) shouldBe 1
    world.reader.count(parse<Metric>("Second")) shouldBe 1
    world.tasks.getTaskData(unrelated) shouldBe original
    world.reader.count(parse<Metric>("Reaction")) shouldBe 0
  }

  @Test
  fun agentGroupLeavesReactionsPendingUnderManualPlayerPolicy() {
    val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
    agent.addTasks("(First!, Second!) OR Other!")

    agent.doTask("First!, Second!")

    agent.count("First") shouldBe 1
    agent.count("Second") shouldBe 1
    agent.count("Following") shouldBe 0
    agent.count("Reaction") shouldBe 0
  }

  @Test
  fun currentInstructionLeavesReactionAndContinuationPending() {
    engine.addTasks(InstructionGroup.of(parse<InstructionTree>("First! THEN Following!")))

    engine.doTask(parse<InstructionTree>("First!"))

    world.reader.count(parse<Metric>("First")) shouldBe 1
    world.reader.count(parse<Metric>("Reaction")) shouldBe 0
    world.reader.count(parse<Metric>("Following")) shouldBe 0
    engine.tasks.ids().size shouldBe 2
  }

  @Test
  fun failedGroupRestoresEarlierMembersAndTheOriginalPermission() {
    engine.addTasks(InstructionGroup.of(parse<InstructionTree>("(First!, Forbidden!) OR Other!")))
    val before = world.events.nextOrdinal
    val tasks = world.tasks.extract { it }

    shouldThrow<LimitsException> { engine.doTask(parse<InstructionTree>("First!, Forbidden!")) }

    world.events.nextOrdinal shouldBe before
    world.tasks.extract { it } shouldBe tasks
    world.reader.count(parse<Metric>("First")) shouldBe 0
    world.reader.count(parse<Metric>("Reaction")) shouldBe 0
  }

  @Test
  fun normalizedThenMemberExecutesItsCurrentInstruction() {
    engine.addTasks(
        InstructionGroup.of(parse<InstructionTree>("((First! THEN Following!), Second!) OR Other!"))
    )
    engine.doTask(parse<InstructionTree>("(First! THEN Following!), Second!"))
    world.reader.count(parse<Metric>("First")) shouldBe 1
    world.reader.count(parse<Metric>("Second")) shouldBe 1
    world.reader.count(parse<Metric>("Following")) shouldBe 0
    world.reader.count(parse<Metric>("Reaction")) shouldBe 0
  }

  @Test
  fun membersWithDifferentContinuationsDoNotCompete() {
    engine.addTasks(
        InstructionGroup.of(parse<InstructionTree>("((First! THEN Following!), First!) OR Other!"))
    )
    engine.doTask(parse<InstructionTree>("(First! THEN Following!), First!"))
    world.reader.count(parse<Metric>("First")) shouldBe 2
    world.reader.count(parse<Metric>("Following")) shouldBe 0
  }

  @Test
  fun aDispatchMemberOnlyQueuesItsBody() {
    engine.addTasks(
        InstructionGroup.of(parse<InstructionTree>("(SOON Player1 { First! }, Second!) OR Other!"))
    )
    engine.doTask(parse<InstructionTree>("SOON Player1 { First! }, Second!"))
    world.reader.count(parse<Metric>("First")) shouldBe 0
    world.reader.count(parse<Metric>("Second")) shouldBe 1
  }

  @Test
  fun aDispatchThenMemberLeavesItsBodyAndContinuationPending() {
    engine.addTasks(
        InstructionGroup.of(
            parse<InstructionTree>("((SOON Player1 { First! } THEN Following!), Second!) OR Other!")
        )
    )

    engine.doTask(parse<InstructionTree>("(SOON Player1 { First! } THEN Following!), Second!"))

    world.reader.count(parse<Metric>("First")) shouldBe 0
    world.reader.count(parse<Metric>("Following")) shouldBe 0
    world.reader.count(parse<Metric>("Second")) shouldBe 1
    world.reader.count(parse<Metric>("Reaction")) shouldBe 0
    engine.tasks.ids().size shouldBe 2
  }
}
