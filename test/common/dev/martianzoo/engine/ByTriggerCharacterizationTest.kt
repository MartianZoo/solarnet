package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.*
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ByTriggerCharacterizationTest {
  @Test
  internal fun byAnyoneAcceptsPlayer() {
    assertByAnyone(PLAYER1)
  }

  @Test
  internal fun byAnyoneAcceptsAdmin() {
    assertByAnyone(ADMIN)
  }

  private fun assertByAnyone(actor: Actor) {
    val game = newGame()
    val agent = game.testAgent(actor).also { it.autoExecPolicy = NONE }
    agent.sneak("ActorTriggerProbe!")

    agent.beginOperation("ActorTriggerSignal!") {
      game.tasks
          .extract { it.assignee to it.instruction.toString() }
          .shouldContainExactly(actor to "Plant<Player1>!")
    }
  }

  @Test
  internal fun byPlayerAcceptsPlayer() {
    val game = newGame()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    p1.sneak("ActorTriggerProbe!, ActorTriggerSignal!")

    p1.beginOperation("-ActorTriggerSignal!") {
      game.tasks
          .extract { it.assignee to it.instruction.toString() }
          .shouldContainExactly(PLAYER1 to "Steel<Player1>!")
    }
  }

  @Test
  internal fun byPlayerBindsTheConcreteActorInTheTriggerAndInstruction() {
    val game = newGame()
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }
    p2.sneak("ActorBindingProbe!, OwnedActorTrigger<Player1>!")

    p2.beginOperation("-OwnedActorTrigger<Player1>!") {
      game.tasks
          .extract { it.instruction.toString() }
          .shouldContainExactlyInAnyOrder(
              "Steel<Player2>!",
              "Heat<Player1>!",
          )
    }
  }

  @Test
  internal fun byPlayerRejectsAdmin() {
    val game = newGame()
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }
    admin.sneak("ActorTriggerProbe!, ActorTriggerSignal!")

    admin.beginOperation("-ActorTriggerSignal!")

    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun byOwnerTestsThePerformerNotTheActorReceivingTheEffect() {
    val game = newGame()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }
    p1.sneak("OwnedByProbe<Player2>!")

    p1.runOperation("ActorTriggerSignal!")
    game.tasks.isEmpty() shouldBe true

    p2.beginOperation("-ActorTriggerSignal!") {
      game.tasks
          .extract { it.assignee to it.instruction.toString() }
          .shouldContainExactly(PLAYER2 to "Heat<Player2>!")
    }
  }

  @Test
  internal fun anUnownedTriggerDefaultsToTheEffectOwner() {
    val game = newGame()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }
    p1.sneak("RepeatedOwnerProbe<Player2>!")
    val checkpoint = game.timeline.checkpoint()

    p1.runOperation("ActorTriggerSignal!")
    game.tasks.isEmpty() shouldBe true

    p2.beginOperation("ActorTriggerSignal!") {
      game.tasks
          .extract { it.assignee to it.instruction.toString() }
          .shouldContainExactlyInAnyOrder(
              PLAYER2 to "Plant<Player2>!",
              PLAYER2 to "Steel<Player2>!",
          )
    }

    p2.autoExecPolicy = EAGER

    game.tasks.isEmpty() shouldBe true
    p1.count("Plant<Player2>") shouldBe 1
    p1.count("Steel<Player2>") shouldBe 1
    game.events.changesSince(checkpoint).takeLast(2).all { it.actor == PLAYER2 } shouldBe true
  }

  @Test
  internal fun anOwnedTriggerUsesItsAuthoredOwnershipInsteadOfAnImplicitActorFilter() {
    val game = newGame()
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }
    p1.sneak("OwnedTriggerProbe<Player1>!")

    p2.beginOperation("OwnedActorTrigger<Player2>!") {
      game.tasks.extract { it.instruction.toString() }.shouldContainExactly("Plant<Player1>!")
    }
  }

  @Test
  internal fun anOwnedTriggerRetainsItsSelectorWhenItsEffectOwnerIsBound() {
    val table = ProbeCatalog.classTable
    val component = Component(table.resolve(parse("OwnedTriggerProbe<Player1>")))
    val elaborator = PetElaborator(table)
    val sourceEffect = elaborator.classEffects(component.type.rootClass).single()

    sourceEffect.typeVariables.variables.associate { variable ->
      variable.declaration.expression.toString() to
          sourceEffect.typeVariables.expressionsOf(variable).map(Any::toString).toSet()
    } shouldBe emptyMap()

    LiveEffect.compile(component, elaborator)
        .map { it.effect.toString() }
        .shouldContainExactly("OwnedActorTrigger<Anyone>: Plant<Player1>!")
  }

  @Test
  internal fun byNotOwnerAcceptsOtherPlayersButRejectsTheOwnerAndAdmin() {
    val game = newGame()
    val owner = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val other = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }
    owner.sneak("OpponentByProbe<Player1>!")

    owner.runOperation("ActorTriggerSignal!")
    admin.runOperation("ActorTriggerSignal!")
    game.tasks.isEmpty() shouldBe true

    other.beginOperation("ActorTriggerSignal!") {
      game.tasks.extract { it.instruction.toString() }.shouldContainExactly("Heat<Player1>!")
    }
  }

  @Test
  internal fun orTriggerMatchesItsRemovalAlternative() {
    val game = newGame()
    val owner = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val other = game.testAgent(PLAYER2).also { it.autoExecPolicy = NONE }
    owner.sneak("OpponentByProbe<Player1>!, ActorTriggerSignal!")

    other.beginOperation("-ActorTriggerSignal!") {
      game.tasks.extract { it.instruction.toString() }.shouldContainExactly("Heat<Player1>!")
    }
  }

  private fun newGame(): World {
    return Engine.newGame(canonicalPremise(catalog = ProbeCatalog))
  }
}

private object ProbeCatalog : TfmCatalog.Composite(Canon.withPlayers(2), ProbeDeclarations)

private object ProbeDeclarations : TfmCatalog() {
  override val explicitClassDeclarations =
      parseClasses(
              """
              CLASS ActorTriggerSignal
              CLASS OwnedActorTrigger : Owned

              CLASS ActorTriggerProbe {
                ActorTriggerSignal BY Anyone: Plant<Player1>
                -ActorTriggerSignal BY Player: Steel<Player1>
              }

              CLASS ActorBindingProbe {
                -OwnedActorTrigger<Owner(NOT Player)> BY Player: Steel<Player>, Heat<Owner(NOT Player)>
              }

              CLASS RepeatedOwnerProbe : Owned {
                ActorTriggerSignal: Plant<Owner>, Steel<Owner>
              }

              CLASS OwnedByProbe : Owned {
                ActorTriggerSignal BY Owner: Heat<Owner>
                -ActorTriggerSignal BY Owner: Heat<Owner>
              }

              CLASS OwnedTriggerProbe : Owned {
                OwnedActorTrigger<Anyone>: Plant<Owner>
              }

              CLASS OpponentByProbe : Owned {
                ActorTriggerSignal OR -ActorTriggerSignal BY Player(NOT Owner): Heat<Owner>
              }

              """
                  .trimIndent()
          )
          .toSet()
}
