package dev.martianzoo.engine

import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.pets.data.Player.Companion.PLAYER2
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
  internal fun byAnyoneAcceptsEngine() {
    assertByAnyone(ENGINE)
  }

  private fun assertByAnyone(actor: Actor) {
    val game = newGame()
    val gameplay = game.gameplay(actor).godMode().also { it.autoExecMode = NONE }
    gameplay.sneak("ActorTriggerProbe!")

    gameplay.beginManual("ActorTriggerSignal!") {
      game.tasks
          .extract { it.assignee to it.instruction.toString() }
          .shouldContainExactly(actor to "Plant<Player1>!")
    }
  }

  @Test
  internal fun byPlayerAcceptsPlayer() {
    val game = newGame()
    val p1 = game.gameplay(PLAYER1).godMode().also { it.autoExecMode = NONE }
    p1.sneak("ActorTriggerProbe!, ActorTriggerSignal!")

    p1.beginManual("-ActorTriggerSignal!") {
      game.tasks
          .extract { it.assignee to it.instruction.toString() }
          .shouldContainExactly(PLAYER1 to "Steel<Player1>!")
    }
  }

  @Test
  internal fun byPlayerBindsTheConcreteActorInTheTriggerAndInstruction() {
    val game = newGame()
    val p2 = game.gameplay(PLAYER2).godMode().also { it.autoExecMode = NONE }
    p2.sneak("ActorBindingProbe!, OwnedActorTrigger<Player1>!")

    p2.beginManual("-OwnedActorTrigger<Player1>!") {
      game.tasks
          .extract { it.assignee to it.instruction.toString() }
          .shouldContainExactlyInAnyOrder(
              PLAYER1 to "Steel<Player2>!",
              PLAYER1 to "Heat<Player1>!",
          )
    }
  }

  @Test
  internal fun byPlayerRejectsEngine() {
    val game = newGame()
    val engine = game.gameplay(ENGINE).godMode().also { it.autoExecMode = NONE }
    engine.sneak("ActorTriggerProbe!, ActorTriggerSignal!")

    engine.beginManual("-ActorTriggerSignal!")

    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun byOwnerTestsThePerformerNotTheActorReceivingTheEffect() {
    val game = newGame()
    val p1 = game.gameplay(PLAYER1).godMode().also { it.autoExecMode = NONE }
    val p2 = game.gameplay(PLAYER2).godMode().also { it.autoExecMode = NONE }
    p1.sneak("OwnedByProbe<Player2>!")

    p1.manual("ActorTriggerSignal!")
    game.tasks.isEmpty() shouldBe true

    p2.beginManual("-ActorTriggerSignal!") {
      game.tasks
          .extract { it.assignee to it.instruction.toString() }
          .shouldContainExactly(PLAYER2 to "Heat<Player2>!")
    }
  }

  @Test
  internal fun anUnownedTriggerDefaultsToTheEffectOwner() {
    val game = newGame()
    val p1 = game.gameplay(PLAYER1).godMode().also { it.autoExecMode = NONE }
    val p2 = game.gameplay(PLAYER2).godMode().also { it.autoExecMode = NONE }
    p1.sneak("RepeatedOwnerProbe<Player2>!")
    val checkpoint = game.timeline.checkpoint()

    p1.manual("ActorTriggerSignal!")
    game.tasks.isEmpty() shouldBe true

    p2.beginManual("ActorTriggerSignal!") {
      game.tasks
          .extract { it.assignee to it.instruction.toString() }
          .shouldContainExactlyInAnyOrder(
              PLAYER2 to "Plant<Player2>!",
              PLAYER2 to "Steel<Player2>!",
          )
    }

    p2.autoExecMode = FIRST

    game.tasks.isEmpty() shouldBe true
    p1.count("Plant<Player2>") shouldBe 1
    p1.count("Steel<Player2>") shouldBe 1
    game.events.changesSince(checkpoint).takeLast(2).all { it.actor == PLAYER2 } shouldBe true
  }

  @Test
  internal fun anOwnedTriggerUsesItsAuthoredOwnershipInsteadOfAnImplicitActorFilter() {
    val game = newGame()
    val p1 = game.gameplay(PLAYER1).godMode().also { it.autoExecMode = NONE }
    val p2 = game.gameplay(PLAYER2).godMode().also { it.autoExecMode = NONE }
    p1.sneak("OwnedTriggerProbe<Player1>!")

    p2.beginManual("OwnedActorTrigger<Player2>!") {
      game.tasks
          .extract { it.assignee to it.instruction.toString() }
          .shouldContainExactly(PLAYER1 to "Plant<Player1>!")
    }
  }

  @Test
  internal fun anOwnedTriggerRetainsItsSelectorWhenItsEffectOwnerIsBound() {
    val table = ProbeCatalog.classTable
    val component = Component(table.resolve(parse("OwnedTriggerProbe<Player1>")))
    val transformers = Transformers(table)
    val sourceEffect = transformers.classEffects(component.type.rootClass).single()

    sourceEffect.typeVariables.variables.associate { variable ->
      variable.declaration.expression.toString() to
          sourceEffect.typeVariables.expressionsOf(variable).map(Any::toString).toSet()
    } shouldBe emptyMap()

    LiveEffect.compile(component, transformers)
        .map { it.effect.toString() }
        .shouldContainExactly("OwnedActorTrigger<Anyone>: Plant<Player1>!")
  }

  @Test
  internal fun byNotOwnerAcceptsOtherPlayersButRejectsTheOwnerAndEngine() {
    val game = newGame()
    val owner = game.gameplay(PLAYER1).godMode().also { it.autoExecMode = NONE }
    val other = game.gameplay(PLAYER2).godMode().also { it.autoExecMode = NONE }
    val engine = game.gameplay(ENGINE).godMode().also { it.autoExecMode = NONE }
    owner.sneak("OpponentByProbe<Player1>!")

    owner.manual("ActorTriggerSignal!")
    engine.manual("ActorTriggerSignal!")
    game.tasks.isEmpty() shouldBe true

    other.beginManual("ActorTriggerSignal!") {
      game.tasks
          .extract { it.assignee to it.instruction.toString() }
          .shouldContainExactly(PLAYER1 to "Heat<Player1>!")
    }
  }

  @Test
  internal fun orTriggerMatchesItsRemovalAlternative() {
    val game = newGame()
    val owner = game.gameplay(PLAYER1).godMode().also { it.autoExecMode = NONE }
    val other = game.gameplay(PLAYER2).godMode().also { it.autoExecMode = NONE }
    owner.sneak("OpponentByProbe<Player1>!, ActorTriggerSignal!")

    other.beginManual("-ActorTriggerSignal!") {
      game.tasks
          .extract { it.assignee to it.instruction.toString() }
          .shouldContainExactly(PLAYER1 to "Heat<Player1>!")
    }
  }

  private fun newGame(): World {
    return Engine.newGame(canonicalPremise(catalog = ProbeCatalog))
  }
}

private object ProbeCatalog : TfmCatalog.Composite(Canon, ProbeDeclarations)

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
                -OwnedActorTrigger<!Player> BY Player: Steel<Player>, Heat<!Player>
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
                ActorTriggerSignal OR -ActorTriggerSignal BY !Owner: Heat<Owner>
              }

              """
                  .trimIndent()
          )
          .toSet()
}
