package dev.martianzoo.engine

import dev.martianzoo.data.Actor
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.tfm.api.TfmAuthority
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
          .extract { Triple(it.actor, it.triggeredBy, it.instruction.toString()) }
          .shouldContainExactly(Triple(actor, null, "Plant<Player1>!"))
    }
  }

  @Test
  fun byPlayerAcceptsPlayer() {
    val game = newGame()
    val p1 = game.gameplay(PLAYER1).godMode().also { it.autoExecMode = NONE }
    p1.sneak("ActorTriggerProbe!, ActorTriggerSignal!")

    p1.beginManual("-ActorTriggerSignal!") {
      game.tasks
          .extract { Triple(it.actor, it.triggeredBy, it.instruction.toString()) }
          .shouldContainExactly(Triple(PLAYER1, null, "Steel<Player1>!"))
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
  fun repeatedOwnerOccurrencesSpecializeTogether() {
    val game = newGame()
    val p1 = game.gameplay(PLAYER1).godMode().also { it.autoExecMode = NONE }
    p1.sneak("RepeatedOwnerProbe<Player2>!")

    p1.beginManual("ActorTriggerSignal!") {
      game.tasks
          .extract { Triple(it.actor, it.triggeredBy, it.instruction.toString()) }
          .shouldContainExactlyInAnyOrder(
              Triple(PLAYER2, PLAYER1, "Plant<Player2>!"),
              Triple(PLAYER2, PLAYER1, "Steel<Player2>!"),
          )
    }
  }

  @Test
  fun triggeredBySurvivesRevisionAndThenQueueInsertion() {
    val game = newGame()
    val p1 = game.gameplay(PLAYER1).godMode().also { it.autoExecMode = NONE }
    val p2 = game.gameplay(PLAYER2).also { it.autoExecMode = NONE }
    p1.sneak("ThenOwnerProbe<Player2>!")

    p1.beginManual("ThenTriggerSignal!")
    val firstTask = game.tasks.extract { it }.single()
    firstTask.actor shouldBe PLAYER2
    firstTask.triggeredBy shouldBe PLAYER1
    val beforeRevision = game.timeline.checkpoint()

    p2.reviseTask(firstTask.id, "Ok")

    game.tasks
        .extract { Triple(it.actor, it.triggeredBy, it.instruction.toString()) }
        .shouldContainExactly(Triple(PLAYER2, PLAYER1, "Steel<Player2>!"))

    game.timeline.rollBack(beforeRevision)
    game.tasks.extract { it }.shouldContainExactly(firstTask)
  }

  private fun newGame() = Engine.newGame(GameSetup(ProbeAuthority, "BM", 2))
}

private object ProbeAuthority : TfmAuthority() {
  override val explicitClassDeclarations =
      Canon.explicitClassDeclarations +
          parseClasses(
              """
              CLASS ActorTriggerSignal : AutoLoad

              CLASS ActorTriggerProbe : AutoLoad {
                ActorTriggerSignal BY Anyone: Plant<Player1>
                -ActorTriggerSignal BY Player: Steel<Player1>
              }

              CLASS RepeatedOwnerProbe : Owned, AutoLoad {
                ActorTriggerSignal: Plant<Owner>, Steel<Owner>
              }

              CLASS ThenTriggerSignal : AutoLoad

              CLASS ThenOwnerProbe : Owned, AutoLoad {
                ThenTriggerSignal: Plant<Owner>? THEN Steel<Owner>
              }
              """
                  .trimIndent()
          )

  override val cardDefinitions = Canon.cardDefinitions
  override val marsMapDefinitions = Canon.marsMapDefinitions
  override val milestoneDefinitions = Canon.milestoneDefinitions
  override val colonyTileDefinitions = Canon.colonyTileDefinitions
  override val standardActionDefinitions = Canon.standardActionDefinitions
  override val customClasses = Canon.customClasses
}
