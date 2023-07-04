package dev.martianzoo.repl.commands

import dev.martianzoo.data.Player
import dev.martianzoo.engine.Gameplay.TurnLayer
import dev.martianzoo.repl.ReplCommand
import dev.martianzoo.repl.ReplSession

internal class BecomeCommand(val repl: ReplSession) : ReplCommand("become") {
  override val usage = "become [PlayerN]"
  override val help =
      """
        Type `become Player2` or whatever and your prompt will change accordingly; everything you
        do now will be done as if it's player 2 doing it. You can also `become Engine` to do
        engine things.
      """

  override fun noArgs(): List<String> {
    repl.gameplay = repl.game.gameplay(Player.ENGINE) as TurnLayer
    return listOf("Okay, you are the game engine now")
  }

  override fun withArgs(args: String): List<String> {
    repl.gameplay = repl.game.gameplay(repl.player(args)) as TurnLayer
    return listOf("Hi, ${repl.gameplay.player}")
  }
}
