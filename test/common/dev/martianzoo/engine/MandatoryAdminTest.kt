package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Motivation
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.count
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MandatoryAdminTest {
  @Test
  fun directPlayerExecutionFinishesAdminConsequencesInTheirOwnTransaction() {
    val world =
        Engine.newGame(testGamePremise("CLASS Start { This: Notice }\nCLASS Notice : System"))
    val player = world.actorEngine(PLAYER1)
    val task = player.addTasks(InstructionGroup.of(parse<InstructionTree>("Start!"))).single()
    val before = world.events.nextOrdinal

    player.doTask(task)

    world.reader.count("Start") shouldBe 1
    world.reader.count("Notice") shouldBe 1
    world.tasks.isEmpty() shouldBe true
    val transactions = world.events.entriesSince(before).mapNotNull { it.transaction }.distinct()
    transactions.map { it.actor } shouldBe listOf(PLAYER1, ADMIN)
    transactions.map { it.motivation } shouldBe
        listOf(Motivation.CLIENT_REQUEST, Motivation.ADMIN_EAGER_POLICY)
  }

  @Test
  fun failedAdminConsequenceRestoresThePlayerRequestAndEarlierAdminProgress() {
    val world =
        Engine.newGame(
            testGamePremise(
                "CLASS Start { This: Earlier, -Missing }\nCLASS Earlier : System\nCLASS Missing : System"
            )
        )
    val player = world.actorEngine(PLAYER1)
    val task = player.addTasks(InstructionGroup.of(parse<InstructionTree>("Start!"))).single()
    val before = world.events.nextOrdinal
    val pending = world.tasks.extract { it }

    shouldThrow<NotNowException> { player.doTask(task) }

    world.events.nextOrdinal shouldBe before
    world.tasks.extract { it } shouldBe pending
    world.reader.count("Start") shouldBe 0
    world.reader.count("Earlier") shouldBe 0
  }

  @Test
  fun aSelectedPlayerChoiceDelaysAdminUntilThePlayerFinishes() {
    val world =
        Engine.newGame(
            testGamePremise(
                "CLASS Choice\nCLASS Alternative\nCLASS Notice : System\nCLASS Queue { This:: SOON Admin { Notice } }"
            )
        )
    val player = world.actorEngine(PLAYER1)
    val choice =
        player
            .addTasks(InstructionGroup.of(parse<InstructionTree>("Choice! OR Alternative!")))
            .single()
    player.selectTask(choice)
    // A correction can admit automatic work while a Player's choice remains selected.
    world.actorEngine(ADMIN).exMachina(InstructionGroup.of(parse<InstructionTree>("Queue!")))
    world.actorEngine(ADMIN).transact {}
    world.reader.count("Notice") shouldBe 0
    world.tasks.selectedTask() shouldBe choice
    player.doTask(parse<InstructionTree>("Choice!"), taskId = choice)
    world.reader.count("Choice") shouldBe 1
    world.reader.count("Notice") shouldBe 1
  }

  @Test
  fun selectionProbesDoNotExecuteLaterAdminTasks() {
    val world =
        Engine.newGame(testGamePremise("CLASS Start { This: -Missing }\nCLASS Missing : System"))
    val player = world.actorEngine(PLAYER1)
    val task = player.addTasks(InstructionGroup.of(parse<InstructionTree>("Start!"))).single()
    val before = world.events.nextOrdinal

    player.canSelectTask(task) shouldBe true
    world.events.nextOrdinal shouldBe before
    world.reader.count("Start") shouldBe 0
    shouldThrow<NotNowException> { player.doTask(task) }
  }
}
