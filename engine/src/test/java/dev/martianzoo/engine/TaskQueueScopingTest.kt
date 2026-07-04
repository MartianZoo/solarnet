package dev.martianzoo.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.data.Player
import dev.martianzoo.data.Player.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.Gameplay.GodMode
import dev.martianzoo.tfm.canon.Canon
import org.junit.jupiter.api.Test

class TaskQueueScopingTest {
  private val game = Engine.newGame(Canon.SIMPLE_GAME)
  private val p1 = game.godMode(PLAYER1)
  private val p2 = game.godMode(PLAYER2)
  private val engine = game.godMode(ENGINE)

  @Test
  fun `P1 operation body sees only P1 tasks`() {
    addOneTaskForEachQueue()

    assertThat(operationBodyOwners(PLAYER1)).containsExactly(PLAYER1)
  }

  @Test
  fun `P2 operation body sees only P2 tasks`() {
    addOneTaskForEachQueue()

    assertThat(operationBodyOwners(PLAYER2)).containsExactly(PLAYER2)
  }

  @Test
  fun `Engine operation body sees only Engine tasks`() {
    addOneTaskForEachQueue()

    assertThat(operationBodyOwners(ENGINE)).containsExactly(ENGINE)
  }

  @Test
  fun `doFirstTask acts on caller queue instead of globally oldest task`() {
    p1.addTasks("Plant?")
    p2.addTasks("Heat?")

    p2.doFirstTask("Heat")

    assertThat(p2.count("Heat<Player2>")).isEqualTo(1)
    assertThat(game.tasks.extract { it.owner }).containsExactly(PLAYER1)
    assertThat(game.tasks.extract { "${it.instruction}" }).containsExactly("Plant<Player1>?")
  }

  @Test
  fun `oldest task behavior is unchanged within a caller queue`() {
    p1.addTasks("Plant?")
    p1.addTasks("Heat?")

    p1.doFirstTask("Plant")

    assertThat(p1.count("Plant<Player1>")).isEqualTo(1)
    assertThat(game.tasks.extract { it.owner }).containsExactly(PLAYER1)
    assertThat(game.tasks.extract { "${it.instruction}" }).containsExactly("Heat<Player1>?")
  }

  private fun addOneTaskForEachQueue() {
    p1.addTasks("Plant?")
    p2.addTasks("Heat?")
    engine.addTasks("SetupPhase?")
    assertThat(game.tasks.extract { it.owner }).containsExactly(PLAYER1, PLAYER2, ENGINE).inOrder()
  }

  private fun operationBodyOwners(player: Player): List<Player> {
    val gameplay = game.godMode(player)
    gameplay.autoExecMode = AutoExecMode.NONE

    var owners: List<Player>? = null
    gameplay.continueManual { owners = tasks.extract { it.owner } }

    return owners!!
  }

  private fun Game.godMode(player: Player): GodMode = gameplay(player).godMode()
}
