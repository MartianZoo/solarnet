package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy
import dev.martianzoo.tfm.engine.DecisionSearch
import dev.martianzoo.tfm.engine.DecisionSearch.Verdict.COMPLETES
import dev.martianzoo.tfm.engine.DecisionSearch.Verdict.DEAD_END
import dev.martianzoo.tfm.engine.DecisionSearch.Verdict.UNKNOWN
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.time.Duration

/** The search finishes card work without naming cards: unplayed cards are anonymous backs. */
internal class DecisionSearchTest : TfmSandboxTest() {
  @Test
  internal fun `Invention Contest completes whatever cards it reveals and keeps`() {
    inventionContestVerdict(money = 2) shouldBe COMPLETES
  }

  @Test
  internal fun `Invention Contest is a dead end without its cost`() {
    inventionContestVerdict(money = 1) shouldBe DEAD_END
  }

  @Test
  internal fun `Tycho Magnetics completes with one Energy`() {
    tychoMagneticsVerdict(energy = 1) shouldBe COMPLETES
  }

  @Test
  internal fun `Tycho Magnetics without Energy remains undecided when amounts are unbounded`() {
    tychoMagneticsVerdict(energy = 0) shouldBe UNKNOWN
  }

  @Test
  internal fun `a larger unbounded amount can finish when the first amount cannot`() {
    newTestGame()
    kim.setToExMachina(0, "MC")
    val gain = kim.addTasks("X MC, -2 MC").first()
    val form = kim.fillInTask(gain)
    form.options(form.decisions().single()).take(101).count() shouldBe 101

    DecisionSearch(agents, tracker = null, playerPolicy = AutoExecPolicy.NONE).verdict {
      it[kim.actor].selectTask(gain)
    } shouldBe COMPLETES
  }

  @Test
  internal fun `a high amount is tried before exhausting smaller amounts`() {
    newTestGame()
    kim.setToExMachina(0, "MC")
    val gain = kim.addTasks("X MC, -90 MC").first()

    DecisionSearch(agents, tracker = null, budget = 130, playerPolicy = AutoExecPolicy.NONE)
        .verdict { it[kim.actor].selectTask(gain) } shouldBe COMPLETES
  }

  @Test
  internal fun `expired search leaves a choice undecided`() {
    newTestGame()
    val gain = kim.addTasks("MC").first()

    DecisionSearch(agents, tracker = null, maxDuration = Duration.ZERO).verdict {
      it[kim.actor].selectTask(gain)
    } shouldBe UNKNOWN
  }

  @Test
  internal fun `Nitrogen-Rich Asteroid completes when only all ten Titanium cover its cost`() {
    nitrogenRichAsteroidVerdict(money = 1) shouldBe COMPLETES
  }

  @Test
  internal fun `Nitrogen-Rich Asteroid is a dead end one MC short`() {
    nitrogenRichAsteroidVerdict(money = 0) shouldBe DEAD_END
  }

  @Test
  internal fun `an unfinished TradeAction keeps its payment choices in a speculative form`() {
    newTestGame(addOptions = "Luna, Ceres, Triton, Ganymede, Callisto", playerCount = 2)
    kim.setToExMachina(0, "MC")
    kim.setToExMachina(0, "Titanium")
    kim.setToExMachina(3, "Energy")
    kim.beginOperation("OfferAction")
    val task = kim.tasks.extract { it }.single()
    val form = kim.fillInTask(task.id)
    form.choose(form.decisions().single(), "StandardAction<Kim>!")
    form.choose(form.decisions().single(), "TradeAction")
    val unfinishedTrade = form.instruction

    val fork = agents.fork()[kim.actor].fillInTask(task.id, unfinishedTrade)
    fork.options(fork.decisions().single()).toList() shouldBe
        listOf("Action1", "Action2", "Action3")
    DecisionSearch(agents, tracker = null).verdictForm(task.id, unfinishedTrade) shouldBe COMPLETES
    DecisionSearch(agents, tracker = null).verdict {
      val draft = it[kim.actor].fillInTask(task.id, unfinishedTrade)
      draft.choose(draft.decisions().single(), "Action2")
      draft.commit()
    } shouldBe COMPLETES
  }

  private fun nitrogenRichAsteroidVerdict(money: Int): DecisionSearch.Verdict {
    newTestGame(addOptions = "$NitrogenRichAsteroid")
    kim.exMachina("ProjectCard")
    kim.setToExMachina(10, "Titanium")
    kim.setToExMachina(money, "MC")
    kim.beginOperation("OfferAction")
    return DecisionSearch(agents, tracker = null).verdict {
      it[kim.actor].doTask("PlayProject<Class<$NitrogenRichAsteroid>>")
    }
  }

  private fun tychoMagneticsVerdict(energy: Int): DecisionSearch.Verdict {
    newTestGame()
    kim.exMachina("$TychoMagnetics")
    kim.exMachina("ProjectCard")
    kim.setToExMachina(energy, "Energy")
    kim.beginOperation("OfferAction")
    return DecisionSearch(agents, tracker = null).verdict {
      it[kim.actor].doTask("UseCardAction<$TychoMagnetics, Action1>")
    }
  }

  private fun inventionContestVerdict(money: Int): DecisionSearch.Verdict {
    newTestGame(addOptions = "$InventionContest")
    kim.exMachina("ProjectCard")
    kim.setToExMachina(money, "MC")
    kim.beginOperation("OfferAction")
    return DecisionSearch(agents, tracker = null).verdict {
      it[kim.actor].doTask("PlayProject<Class<$InventionContest>>")
    }
  }
}
