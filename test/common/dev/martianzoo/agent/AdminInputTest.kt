package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AdminInputTest {
  @Test
  internal fun stalledAdminReportsMissingInputBeforePlayerWorkAndCanRetry() {
    for (playerWork in listOf(false, true)) {
      val world = Engine.newGame(testGamePremise("CLASS Draw : System\nCLASS Coin"))
      val agents = Agents(world)
      val player = agents[PLAYER1]
      var supplied = false
      agents.installTransactionGuard(
          object : AgentTransactionGuard {
            override fun validate() {
              if (player.count("Draw") > 0 && !supplied)
                  throw AgentInputRequiredException("supply draw")
            }
          }
      )
      world.actorEngine(ADMIN).addTasks(InstructionGroup.of(parse<InstructionTree>("Draw!")))
      if (playerWork)
          world.actorEngine(PLAYER1).addTasks(InstructionGroup.of(parse<InstructionTree>("Coin!")))
      val before = world.events.nextOrdinal
      val tasks = world.tasks.extract { it }

      shouldThrow<AgentInputRequiredException> { player.autoExecNow() }.message shouldBe
          "supply draw"
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
                throw AgentInputRequiredException("prepare draw")
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
            if (player.count("Draw") > 0) throw AgentInputRequiredException("supply draw")
          }
        }
    )
    player.addTasks("Start")
    val before = world.events.nextOrdinal
    val tasks = world.tasks.extract { it }

    shouldThrow<AgentInputRequiredException> { player.doTask("Start") }

    world.events.nextOrdinal shouldBe before
    world.tasks.extract { it } shouldBe tasks
    player.count("Earlier") shouldBe 0
    player.count("Draw") shouldBe 0
  }
}
