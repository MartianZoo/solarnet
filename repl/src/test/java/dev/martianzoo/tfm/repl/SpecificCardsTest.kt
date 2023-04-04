package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.ResourceUtils.lookUpProductionLevels
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.engine.Engine
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SpecificCardsTest {

  @Test
  fun localHeatTrapping() {
    val game = Engine.newGame(GameSetup(Canon, "BM", 2))
    val p1 = InteractiveSession(game, Player.PLAYER1)

    p1.execute("4 Heat, 3 ProjectCard, Pets")
    assertThat(p1.counts("Card, Heat, CardBack, CardFront, Animal, PlayedEvent"))
        .containsExactly(3, 4, 2, 1, 1, 0)
        .inOrder()

    val cp1 = p1.game.checkpoint()
    p1.execute("LocalHeatTrapping")
    assertThat(p1.counts("Card, Heat, CardBack, CardFront, Animal, PlayedEvent"))
        .containsExactly(3, 4, 1, 1, 1, 1)
        .inOrder()

    val tasks = p1.agent.tasks()
    assertThat(tasks.values.any { it.whyPending == "can't gain/remove 5 instances, only 4" })
    p1.game.rollBack(cp1)

    p1.execute("2 Heat")
    assertThat(p1.counts("Card, Heat, CardBack, CardFront, Animal, PlayedEvent"))
        .containsExactly(3, 6, 2, 1, 1, 0)
        .inOrder()

    val nextTask = p1.execute("LocalHeatTrapping").newTaskIdsAdded.single()
    assertThat(p1.counts("Card, Heat, CardBack, CardFront, Animal, PlayedEvent"))
        .containsExactly(3, 1, 1, 1, 1, 1)
        .inOrder()

    val cp2 = p1.game.checkpoint()
    assertThrows<Exception>("2") { p1.doTask(nextTask, "2 Animal") }
    assertThat(p1.game.checkpoint()).isEqualTo(cp2)

    assertThrows<Exception>("3") { p1.doTask(nextTask, "2 Animal<Fish>") }
    assertThat(p1.game.checkpoint()).isEqualTo(cp2)
    assertThat(p1.count("Animal")).isEqualTo(1)

    p1.doTask(nextTask, "4 Plant")
    assertThat(p1.counts("Card, Heat, Plant, Animal")).containsExactly(3, 1, 4, 1).inOrder()
    p1.game.rollBack(cp2)

    p1.doTask(nextTask, "2 Animal<Pets>")
    assertThat(p1.counts("Card, Heat, Plant, Animal")).containsExactly(3, 1, 0, 3).inOrder()
    p1.game.rollBack(cp2)
  }

  @Test
  fun manutech() {
    val game = Engine.newGame(GameSetup(Canon, "BMV", 2))
    val p1 = InteractiveSession(game, Player.PLAYER1)

    p1.execute("CorporationCard, Manutech")
    assertThat(p1.count("Production<Class<S>>")).isEqualTo(1)
    assertThat(p1.count("Steel")).isEqualTo(1)

    p1.execute("PROD[8, 6T, 7P, 5E, 3H]")
    val prods = lookUpProductionLevels(p1.game.reader, p1.agent.player.expression)

    // Being very lazy and depending on declaration order!
    assertThat(prods.values).containsExactly(8, 1, 6, 7, 5, 3).inOrder()
    assertThat(p1.counts("M, S, T, P, E, H")).containsExactly(43, 1, 6, 7, 5, 3)
  }

  fun InteractiveSession.counts(s: String) = s.split(",").map(::count)

  @Test
  fun sulphurEatingBacteria() {
    val game = Engine.newGame(GameSetup(Canon, "BMV", 2))
    val p1 = InteractiveSession(game, Player.PLAYER1)

    p1.execute("ProjectCard THEN SulphurEatingBacteria")
    p1.execute("UseAction1<SulphurEatingBacteria>")
    assertThat(p1.count("Microbe")).isEqualTo(1)

    p1.execute("UseAction2<SulphurEatingBacteria>")
    p1.doTask("A", "-Microbe<SulphurEatingBacteria> THEN 3")
    assertThat(p1.count("Microbe")).isEqualTo(0)

    p1.execute("4 UseAction1<SulphurEatingBacteria>")
    assertThat(p1.count("Microbe")).isEqualTo(4)

    p1.execute("UseAction2<SulphurEatingBacteria>")

    fun assertTaskFails(task: String, desc: String) =
        assertThrows<Exception>(desc) { p1.doTask("A", task) }

    assertTaskFails("-Microbe<C251> THEN 4", "greed")
    assertTaskFails("-Microbe<C251> THEN 2", "shortchanged")
    assertTaskFails("-Microbe<C251>", "no get paid")
    assertTaskFails("-3 Microbe THEN 9", "which microbe")
    assertTaskFails("-5 Microbe<C251> THEN 15", "more than have")
    assertTaskFails("-0 Microbe<C251> THEN 0", "x can't be zero")
    assertTaskFails("-3 Resource<C251> THEN 9", "what kind")
    assertTaskFails("9 THEN -3 Microbe<C251>", "out of order")
    assertTaskFails("2 Microbe<C251> THEN -6", "inverse")

    assertThat(p1.count("Microbe")).isEqualTo(4)

    p1.doTask("A", "-3 Microbe<C251> THEN 9")
    assertThat(p1.count("Microbe")).isEqualTo(1)
  }
}