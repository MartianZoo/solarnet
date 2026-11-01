package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.LegacyTfmTestSupport
import kotlin.test.BeforeTest

/** Retains the old action helpers for replays that have not migrated to plain Agents. */
internal abstract class AbstractFullGameTest : FullGameTest(EAGER), LegacyTfmTestSupport {
  protected open val requireExplicitPaymentChoices: Boolean = true
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
    fun gameplay(seat: Int) =
        player(seat).also {
          if (requireExplicitPaymentChoices) it.requireExplicitPaymentChoices()
        }
    p1 = gameplay(1)
    if (players.size > 1) p2 = gameplay(2)
    if (players.size > 2) p3 = gameplay(3)
  }

  override fun player(seat: Int): TfmGameplay = agents.tfm(super.player(seat).actor)
}
