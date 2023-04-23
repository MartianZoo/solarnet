package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.engine.InteractiveSession

// Eventually we may want a usability layer, but for now interacting with the engine is a PITA, so
// we have a few test-only libraries to smooth that out a bit.
object TestHelpers {

  fun InteractiveSession.turn(vararg tasks: String) {
    execute("Turn")
    tasks.forEach(::doTask)
  }

  fun InteractiveSession.playCard(cost: Int, cardName: String, vararg tasks: String) {
    turn("UseAction1<PlayCardFromHand>", "PlayCard<Class<$cardName>>")
    if (cost > 0) doTask("$cost Pay<Class<M>> FROM M")
    tasks.forEach(::doTask)
  }

  fun InteractiveSession.useCardAction1(cardName: String, vararg tasks: String) =
      turn(
          "UseAction1<UseActionFromCard>",
          "UseAction1<$cardName> THEN ActionUsedMarker<$cardName>",
          *tasks)

  fun InteractiveSession.stdProject(spName: String, vararg tasks: String) =
      turn("UseAction1<UseStandardProject>", "UseAction1<$spName>", *tasks)

  fun InteractiveSession.counts(s: String) = s.split(",").map(::count)

  fun InteractiveSession.assertCounts(vararg pairs: Pair<Int, String>) =
      assertThat(pairs.map { count(it.second) })
          .containsExactlyElementsIn(pairs.map { it.first })
          .inOrder()
}
