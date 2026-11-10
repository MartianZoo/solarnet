package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.LegacyTfmTestSupport
import kotlin.test.BeforeTest

/** Retains the old action helpers for replays that have not migrated to plain Agents. */
internal abstract class LegacyCardTrackingFullGameTest :
    CardTrackingFullGameTest(EAGER), LegacyTfmTestSupport {
  override val admin: TfmGameplay
    get() = agents.tfm(ADMIN)

  final override lateinit var p1: TfmGameplay
    private set

  final override lateinit var p2: TfmGameplay
    private set

  final override lateinit var p3: TfmGameplay
    private set

  @BeforeTest
  override fun commonSetup() {
    super.commonSetup()
    p1 = player(1)
    if (players.size > 1) p2 = player(2)
    if (players.size > 2) p3 = player(3)
  }

  override fun player(seat: Int): TfmGameplay = agents.tfm(super.player(seat).actor)

  protected fun TfmGameplay.buyCards(vararg cardClasses: ClassName): TaskResult {
    draw(*cardClasses)
    return buyCards(cardClasses.size)
  }

  protected fun TfmGameplay.sellPatents(vararg cardClasses: ClassName): TaskResult {
    discard(*cardClasses)
    return sellPatents(cardClasses.size)
  }
}
