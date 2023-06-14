package dev.martianzoo.tfm.repl.commands

import dev.martianzoo.data.Player
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.repl.ReplCommand
import dev.martianzoo.tfm.repl.ReplSession

internal class BecomeCommand(val repl: ReplSession) : ReplCommand("become") {
  override val usage = "become [PlayerN]"
  override val help =
      """
        Type `become Player2` or whatever and your prompt will change accordingly; everything you
        do now will be done as if it's player 2 doing it. You can also `become Engine` to do
        engine things.
      """

  override fun noArgs(): List<String> {
    repl.tfm = repl.game.tfm(Player.ENGINE)
    return listOf("Okay, you are the game engine now")
  }

  override fun withArgs(args: String): List<String> {
    repl.tfm = repl.game.tfm(repl.player(args))
    return listOf("Hi, ${repl.tfm.player}")
  }
}
