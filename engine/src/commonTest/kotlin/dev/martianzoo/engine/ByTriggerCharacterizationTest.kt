package dev.martianzoo.engine

import dev.martianzoo.data.Actor
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.tfm.api.TfmRuleset
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ByTriggerCharacterizationTest {
  @Test
  fun byAnyoneAcceptsPlayer() {
    assertByAnyone(PLAYER1)
  }

  @Test
  fun byAnyoneAcceptsEngine() {
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
  fun byPlayerAcceptsPlayer() {
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
  fun byPlayerRejectsEngine() {
    val game = newGame()
    val engine = game.gameplay(ENGINE).godMode().also { it.autoExecMode = NONE }
    engine.sneak("ActorTriggerProbe!, ActorTriggerSignal!")

    engine.beginManual("-ActorTriggerSignal!")

    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun byOwnerTestsThePerformerNotTheActorReceivingTheEffect() {
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
  fun anUnownedTriggerDefaultsToTheEffectOwner() {
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
  fun anOwnedTriggerUsesItsAuthoredOwnershipInsteadOfAnImplicitActorFilter() {
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

  private fun newGame(): Game {
    val options = Canon.options("BM", 2)
    return Engine.newGame(GameSetup(ProbeRuleset.resolve(Canon.bundleNames(options)), options))
  }
}

private object ProbeRuleset : TfmRuleset.Composite(Canon, ProbeDeclarations)

private object ProbeDeclarations : TfmRuleset.Empty() {
  override val explicitClassDeclarations =
      parseClasses(
              """
              CLASS ActorTriggerSignal : AutoLoad
              CLASS OwnedActorTrigger : Owned, AutoLoad

              CLASS ActorTriggerProbe : AutoLoad {
                ActorTriggerSignal BY Anyone: Plant<Player1>
                -ActorTriggerSignal BY Player: Steel<Player1>
              }

              CLASS RepeatedOwnerProbe : Owned, AutoLoad {
                ActorTriggerSignal: Plant<Owner>, Steel<Owner>
              }

              CLASS OwnedByProbe : Owned, AutoLoad {
                ActorTriggerSignal BY Owner: Heat<Owner>
                -ActorTriggerSignal BY Owner: Heat<Owner>
              }

              CLASS OwnedTriggerProbe : Owned, AutoLoad {
                OwnedActorTrigger<Anyone>: Plant<Owner>
              }
              """
                  .trimIndent()
          )
          .toSet()
}
