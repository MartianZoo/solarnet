package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.InputRequiredException
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AdminInputTest {
  @Test
  internal fun sessionCallCannotEscapeAnAlreadyOpenAttributedTransaction() {
    val world =
        Engine.newGame(testGamePremise("CLASS Start { This: Notice }\nCLASS Notice : System"))
    val agents = Agents(world) { NONE }
    val player = world.actorEngine(PLAYER1)
    val task = player.addTasks(InstructionGroup.of(parse<InstructionTree>("Start!"))).single()
    val before = world.events.nextOrdinal

    shouldThrow<IllegalStateException> {
      player.transaction(dev.martianzoo.state.Motivation.CLIENT_REQUEST) {
        player.doTask(task)
        agents.transact()
      }
    }

    world.events.nextOrdinal shouldBe before
    (task in world.tasks) shouldBe true
    agents[PLAYER1].count("Start") shouldBe 0
    agents[PLAYER1].count("Notice") shouldBe 0
  }

  @Test
  internal fun sessionCheckIsInheritedByNestedEngineCallsAndRemovedAfterFailure() {
    for (nested in listOf(false, true)) {
      val world = Engine.newGame(testGamePremise("CLASS Start { This: Draw }\nCLASS Draw : System"))
      val agents = Agents(world) { NONE }
      val player = world.actorEngine(PLAYER1)
      agents.installTransactionGuard(
          object : AgentTransactionGuard {
            override fun validate() {
              if (agents[PLAYER1].count("Draw") > 0) throw InputRequiredException("supply draw")
            }
          }
      )
      val task = player.addTasks(InstructionGroup.of(parse<InstructionTree>("Start!"))).single()
      val before = world.events.nextOrdinal

      shouldThrow<InputRequiredException> {
        agents.transact {
          if (nested) player.transact { player.doTask(task) } else player.doTask(task)
        }
      }
      world.events.nextOrdinal shouldBe before
      (task in world.tasks) shouldBe true

      // The failed session call must not leave its external-input check installed on the World.
      player.doTask(task)
      agents[PLAYER1].count("Draw") shouldBe 1
      world.tasks.isEmpty() shouldBe true
    }
  }

  @Test
  internal fun optionalPlayerExecutionCannotHideMissingInputFromMandatoryAdminWork() {
    for (competing in listOf(false, true)) {
      val world =
          Engine.newGame(
              testGamePremise("CLASS Start { This: Draw }\nCLASS Draw : System\nCLASS Coin")
          )
      val agents = Agents(world) { EAGER }
      agents.installTransactionGuard(
          object : AgentTransactionGuard {
            override fun validate() {
              if (agents[PLAYER1].count("Draw") > 0) throw InputRequiredException("supply draw")
            }
          }
      )
      val player = world.actorEngine(PLAYER1)
      player.addTasks(
          InstructionGroup.of(parse<InstructionTree>(if (competing) "Start!, Coin!" else "Start!"))
      )
      val before = world.events.nextOrdinal
      val pending = world.tasks.extract { it }

      shouldThrow<InputRequiredException> { agents[PLAYER1].autoExecNow() }

      world.events.nextOrdinal shouldBe before
      world.tasks.extract { it } shouldBe pending
      agents[PLAYER1].count("Start") shouldBe 0
      agents[PLAYER1].count("Coin") shouldBe 0
    }
  }

  @Test
  internal fun tryTaskPropagatesMandatoryAdminFailureAndRestoresTheAttempt() {
    val world =
        Engine.newGame(testGamePremise("CLASS Start { This: -Missing }\nCLASS Missing : System"))
    val player = Agents(world) { NONE }[PLAYER1]
    val task = player.addTasks("Start").single()
    val before = world.events.nextOrdinal

    shouldThrow<NotNowException> { player.tryTask(task) }

    world.events.nextOrdinal shouldBe before
    (task in world.tasks) shouldBe true
    player.count("Start") shouldBe 0
  }

  @Test
  internal fun stalledAdminReportsMissingInputBeforePlayerWorkAndCanRetry() {
    for (playerWork in listOf(false, true)) {
      val world = Engine.newGame(testGamePremise("CLASS Draw : System\nCLASS Coin"))
      val agents = Agents(world) { EAGER }
      val player = agents[PLAYER1]
      var supplied = false
      agents.installTransactionGuard(
          object : AgentTransactionGuard {
            override fun validate() {
              if (player.count("Draw") > 0 && !supplied) throw InputRequiredException("supply draw")
            }
          }
      )
      world.actorEngine(ADMIN).addTasks(InstructionGroup.of(parse<InstructionTree>("Draw!")))
      if (playerWork)
          world.actorEngine(PLAYER1).addTasks(InstructionGroup.of(parse<InstructionTree>("Coin!")))
      val before = world.events.nextOrdinal
      val tasks = world.tasks.extract { it }

      shouldThrow<InputRequiredException> { player.autoExecNow() }.message shouldBe "supply draw"
      world.events.nextOrdinal shouldBe before
      world.tasks.extract { it } shouldBe tasks
      player.count("Coin") shouldBe 0

      supplied = true
      player.autoExecNow()
      player.count("Draw") shouldBe 1
      player.count("Coin") shouldBe if (playerWork) 1 else 0
      world.tasks.isEmpty() shouldBe true
    }
  }

  @Test
  internal fun otherAdminProgressRetriesMissingInputBeforeReportingIt() {
    val world = Engine.newGame(testGamePremise("CLASS Draw : System\nCLASS Ready : System"))
    val agents = Agents(world) { NONE }
    val player = agents[PLAYER1]
    agents.installTransactionGuard(
        object : AgentTransactionGuard {
          override fun validate() {
            if (player.count("Draw") > 0 && player.count("Ready") == 0)
                throw InputRequiredException("prepare draw")
          }
        }
    )
    world.actorEngine(ADMIN).addTasks(InstructionGroup.of(parse<InstructionTree>("Draw!, Ready!")))

    player.autoExecNow()

    player.count("Draw") shouldBe 1
    player.count("Ready") shouldBe 1
    world.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun missingInputAfterAPlayerRequestRestoresTheRequestAndEarlierAdminWork() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
        CLASS Start : Signal { This: Earlier, Draw }
        CLASS Earlier : System
        CLASS Draw : System
    """
            )
        )
    val agents = Agents(world) { NONE }
    val player = agents[PLAYER1]
    agents.installTransactionGuard(
        object : AgentTransactionGuard {
          override fun validate() {
            if (player.count("Draw") > 0) throw InputRequiredException("supply draw")
          }
        }
    )
    player.addTasks("Start")
    val before = world.events.nextOrdinal
    val tasks = world.tasks.extract { it }

    shouldThrow<InputRequiredException> { player.doTask("Start") }

    world.events.nextOrdinal shouldBe before
    world.tasks.extract { it } shouldBe tasks
    player.count("Earlier") shouldBe 0
    player.count("Draw") shouldBe 0
  }
}
