package dev.martianzoo.repl.commands

import dev.martianzoo.data.Player
import dev.martianzoo.repl.ReplCommand
import dev.martianzoo.repl.ReplSession
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm

internal class BecomeCommand(val repl: ReplSession) : ReplCommand("become") {
  override val usage = "become [PlayerN]"
  override val help =
      """
        Type `become Player2` or whatever and your prompt will change accordingly; everything you
        do now will be done as if it's player 2 doing it. You can also `become Engine` to do
        engine things.
      """

  // TODO don't depend on `tfm`?
  override fun noArgs(): List<String> {
    repl.tfm = repl.game.tfm(Player.ENGINE)
    return listOf("Okay, you are the game engine now")
  }

  override fun withArgs(args: String): List<String> {
    repl.tfm = repl.game.tfm(repl.player(args))
    return listOf("Hi, ${repl.tfm.player}")
  }
}
