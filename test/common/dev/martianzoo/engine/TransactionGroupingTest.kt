package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.GameEvent.TaskEditedEvent
import dev.martianzoo.state.GameEvent.TaskRemovedEvent
import dev.martianzoo.state.Motivation.ADMIN_EAGER_POLICY
import dev.martianzoo.state.Motivation.CLIENT_REQUEST
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Transaction grouping recorded on the events themselves.
 *
 * `Token`'s effect gains a System `Reward`, which is assigned to Admin, so a Player gaining a Token
 * is the same shape as placing a greenery whose hex bonus grants steel: the Player's own work and
 * Admin's handling of the consequence are separate transactions inside one synchronous call.
 */
internal class TransactionGroupingTest {
  private val premise =
      testGamePremise(
          """
          CLASS Token { This: Reward! }
          CLASS Reward : System
          CLASS Coin
          """
      )

  @Test
  internal fun initializationPrecedesEveryTransaction() {
    val game = Engine.newGame(premise)

    game.events.entriesSince(0).all { it.transaction == null } shouldBe true
    game.events.lastTransaction shouldBe null
    game.events.transactionRange(0) shouldBe null

    val setupEnd = game.events.nextOrdinal
    game.testAgent(PLAYER1).sneak("Coin")

    game.events.lastTransaction?.serial shouldBe 0
    game.events.transactionStarts.first() shouldBe setupEnd
  }

  @Test
  internal fun aPlayerCallGroupsItsOwnWorkApartFromAdminHandling() {
    val game = Engine.newGame(premise)
    val agent = game.testAgent(PLAYER1)
    // Queue the work first, as a client holding a pending task would: this call is the submission.
    val task = agent.addTasks("Token").single()
    val before = game.events.nextOrdinal

    val result = agent.doTask(task)

    // The whole call reports both the Token and the Reward it caused.
    result.changes.mapNotNull { it.change.gaining?.type?.rootClass?.className?.toString() } shouldBe
        listOf("Token", "Reward")

    val transactions =
        game.events.entriesSince(before).map { checkNotNull(it.transaction) }.distinct()
    transactions.size shouldBe 2
    val (player, admin) = transactions.sortedBy { it.serial }
    player.actor shouldBe PLAYER1
    player.motivation shouldBe CLIENT_REQUEST
    admin.actor shouldBe ADMIN
    admin.motivation shouldBe ADMIN_EAGER_POLICY

    // The Reward is Admin's, and the Token is not.
    game.events
        .changesSince(before)
        .single { it.change.gaining?.expression.toString() == "Reward" }
        .transaction shouldBe admin
  }

  @Test
  internal fun everyEventIsFoundThroughItsOwnTransaction() {
    val game = Engine.newGame(premise)
    val agent = game.testAgent(PLAYER1)
    agent.runOperation("Token")
    agent.sneak("Coin")

    val gameplay = game.events.entriesSinceSetup()
    gameplay.isEmpty() shouldBe false
    gameplay.forEach { event ->
      val serial = checkNotNull(event.transaction).serial
      checkNotNull(game.events.transactionRange(serial)).contains(event.ordinal) shouldBe true
      game.events.transactionAt(event.ordinal) shouldBe event.transaction
    }

    // Ranges are contiguous, nonempty, and cover gameplay exactly once.
    val serials = gameplay.mapNotNull { it.transaction?.serial }.distinct()
    serials shouldBe (0..serials.last()).toList()
    serials.flatMap { checkNotNull(game.events.transactionRange(it)) } shouldBe
        gameplay.map { it.ordinal }
  }

  @Test
  internal fun navigationStopsAreTransactionStartsAndTheCurrentEnd() {
    val game = Engine.newGame(premise)
    val agent = game.testAgent(PLAYER1)
    val setupEnd = game.events.nextOrdinal
    game.events.transactionStarts shouldBe listOf(setupEnd)

    agent.sneak("Coin")
    val afterFirst = game.events.nextOrdinal
    game.events.transactionStarts shouldBe listOf(setupEnd, afterFirst)

    agent.sneak("Coin")
    game.events.transactionStarts shouldBe listOf(setupEnd, afterFirst, game.events.nextOrdinal)
  }

  @Test
  internal fun promotionBelongsToTheTransactionThatMadeItEligible() {
    val game = Engine.newGame(premise)
    val agent = game.testAgent(PLAYER1)
    val token = agent.addTasks("Token, LATER { Coin }").first()
    val before = game.events.nextOrdinal

    agent.doTask(token)

    // Coin stays deferred while Admin still owes the Reward, so the edit that makes it ordinary
    // belongs to Admin's transaction, not to the Player's and not to a queue edit of its own.
    val promotion =
        game.events.entriesSince(before).filterIsInstance<TaskEditedEvent>().single {
          it.oldTask.deferred && !it.task.deferred
        }
    val admin = checkNotNull(promotion.transaction)
    admin.actor shouldBe ADMIN
    admin.motivation shouldBe ADMIN_EAGER_POLICY

    // That same transaction is the one that finished the Reward.
    game.events
        .entriesSince(before)
        .filterIsInstance<TaskRemovedEvent>()
        .single { it.task.assignee == ADMIN }
        .transaction shouldBe admin

    agent.count("Coin") shouldBe 1
  }

  @Test
  internal fun aTaskEditWithoutAComponentChangeStillEarnsATransaction() {
    val game = Engine.newGame(premise)
    val agent = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }

    agent.addTasks("Coin OR Token")

    val added = game.events.entriesSinceSetup().single()
    checkNotNull(added.transaction).serial shouldBe 0
    game.events.changesSinceSetup() shouldBe emptyList()
  }

  @Test
  internal fun aRestoredProbeAdvancesNoSerial() {
    val game = Engine.newGame(premise)
    val agent = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val task = agent.addTasks("Coin OR Token").single()
    val before = game.events.lastTransaction
    val beforeOrdinal = game.events.nextOrdinal

    agent.canExecuteTask(task) shouldBe false
    agent.tryTask("Coin OR Token")

    game.events.lastTransaction shouldBe before
    game.events.nextOrdinal shouldBe beforeOrdinal
  }

  @Test
  internal fun restorationFreesTheSerialItWouldHaveUsed() {
    val game = Engine.newGame(premise)
    val agent = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    agent.addTasks("Coin")
    game.events.lastTransaction?.serial shouldBe 0

    shouldThrow<IllegalStateException> {
      game.timeline.atomic {
        agent.sneak("Coin")
        game.events.lastTransaction?.serial shouldBe 1
        error("reject this scope")
      }
    }

    game.events.lastTransaction?.serial shouldBe 0
    agent.sneak("Coin")
    game.events.lastTransaction?.serial shouldBe 1
  }
}
