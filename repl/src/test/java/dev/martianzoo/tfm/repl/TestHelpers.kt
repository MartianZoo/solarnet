package dev.martianzoo.tfm.repl

object TestHelpers {

  fun InteractiveSession.playCard(cost: Int, cardName: String, vararg tasks: String) {
    execute("Turn", "UseAction1<PlayCardFromHand>", "PlayCard<Class<$cardName>>")
    if (cost > 0) doTask("$cost Pay<Class<M>> FROM M")
    tasks.forEach(::doTask)
  }

  fun InteractiveSession.useCardAction1(cardName: String, vararg tasks: String) =
      execute(
          "Turn",
          "UseAction1<UseActionFromCard>",
          "UseAction1<$cardName> THEN ActionUsedMarker<$cardName>",
          *tasks)

  fun InteractiveSession.useSp(spName: String, vararg tasks: String) =
      execute("Turn", "UseAction1<UseStandardProject>", "UseAction1<$spName>", *tasks)

  fun InteractiveSession.counts(s: String) = s.split(",").map(::count)
}
