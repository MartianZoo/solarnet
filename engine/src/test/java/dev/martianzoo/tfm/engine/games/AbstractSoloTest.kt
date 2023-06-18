package dev.martianzoo.tfm.engine.games

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay
import org.junit.jupiter.api.BeforeEach

abstract class AbstractSoloTest : AbstractFullGameTest() {
  protected lateinit var me: TfmGameplay
  protected lateinit var opponent: TfmGameplay

  override fun setup() = GameSetup(Canon, "BRHVPX", 2)

  @BeforeEach
  fun soloSetup() {
    me = p1
    opponent = p2

    me.godMode().sneak("-6 TR")

    opponent.godMode().sneak("99, 99 S, 99 T, 99 P, 99 E, 99 H")
    opponent.godMode().sneak("PROD[99, 99 S, 99 T, 99 P, 99 E, 99 H]")
  }

  protected fun assertCounts(vararg pairs: Pair<Int, String>) = me.assertCounts(*pairs)

  protected fun assertProduction(m: Int, s: Int, t: Int, p: Int, e: Int, h: Int) =
      me.assertProduction(m, s, t, p, e, h)
  protected fun assertResources(m: Int, s: Int, t: Int, p: Int, e: Int, h: Int) =
      me.assertResources(m, s, t, p, e, h)
  protected fun assertDashMiddle(played: Int, actions: Int, vp: Int, tr: Int, hand: Int) =
      me.assertDashMiddle(played, actions, vp, tr, hand)

  protected fun assertTags(
      but: Int = 0,
      spt: Int = 0,
      sct: Int = 0,
      pot: Int = 0,
      eat: Int = 0,
      jot: Int = 0,
      vet: Int = 0,
      plt: Int = 0,
      mit: Int = 0,
      ant: Int = 0,
      cit: Int = 0
  ) = me.assertTags(but, spt, sct, pot, eat, jot, vet, plt, mit, ant, cit)

  protected fun assertDashRight(events: Int, tagless: Int, cities: Int) =
      me.assertDashRight(events, tagless, cities)
  protected fun assertVps(expected: Int) = me.assertVps(expected)
  protected fun assertActions(expected: Int) = me.assertActions(expected)
}
