package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.engine.TfmEngine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AdminTimingExperimentTest {
  @Test
  fun adminUsesFiniteTransactionUniquenessEvenWithManualPlayerPolicy() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
        ABSTRACT CLASS Choice
        CLASS Allowed : Choice
        CLASS Forbidden : Choice { HAS MAX 0 This }
        """
            )
        )
    val agents = Agents(world) { NONE }
    world.actorEngine(ADMIN).addTasks(InstructionGroup.of(parse<InstructionTree>("Choice!")))
    agents[PLAYER1].autoExecNow()
    agents[PLAYER1].count("Allowed") shouldBe 1
    world.tasks.isEmpty() shouldBe true
  }

  @Test
  fun nextPlayerLinksExistBeforeTheNextSeatIsCreated() {
    val world = TfmEngine.newGame(canonicalPremise(players = 3))
    fun gainedAt(expression: String): Int {
      val type = world.reader.resolve(parse<Expression>(expression))
      return world.events.changesSince(0).single { it.change.gaining?.type == type }.ordinal
    }
    (gainedAt("Player2") < gainedAt("NextPlayer<Player1, Player2>")) shouldBe true
    (gainedAt("NextPlayer<Player1, Player2>") < gainedAt("Player3")) shouldBe true
    (gainedAt("Player3") < gainedAt("NextPlayer<Player2, Player3>")) shouldBe true
    (gainedAt("NextPlayer<Player2, Player3>") < gainedAt("NextPlayer<Player3, Player1>")) shouldBe
        true
  }

  @Test
  fun currentAdmissionRejectsDirectGatedAndLaterDieButDropsOptionalDie() {
    val world = Engine.newGame(testGamePremise("CLASS Foo"))
    val engine = world.actorEngine(PLAYER1)
    for (instruction in listOf("Die!", "Foo: Die!", "Foo! THEN Die!")) {
      val before = world.events.nextOrdinal
      shouldThrow<DeadEndException> {
        engine.addTasks(InstructionGroup.of(parse<InstructionTree>(instruction)))
      }
      world.events.nextOrdinal shouldBe before
      world.tasks.isEmpty() shouldBe true
    }
    engine.addTasks(InstructionGroup.of(parse<InstructionTree>("Die?")))
    world.tasks.isEmpty() shouldBe true
  }
}
