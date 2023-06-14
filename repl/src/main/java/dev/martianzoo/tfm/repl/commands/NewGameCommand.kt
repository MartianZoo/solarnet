package dev.martianzoo.tfm.repl.commands

import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.repl.ReplCommand
import dev.martianzoo.tfm.repl.ReplSession
import dev.martianzoo.tfm.repl.ReplSession.UsageException

internal class NewGameCommand(val repl: ReplSession) : ReplCommand("newgame") {
  override val usage = "newgame <bundles> <player count>"
  override val help =
      """
        Erases your current game and starts a new one. You can't undo that (but you can get your
        command history out of ~/.rego_session and replay it.) For <bundles>, jam some letters
        together: B=Base, R=coRpoRate eRa, M=Tharsis, H=Hellas, X=Promos, and the rest are what
        you'd think. The player count can be from 1 to 5, but if you choose 1, you are NOT getting
        any of the actual solo rules!
      """

  override fun withArgs(args: String): List<String> {
    try {
      val (bundleString, players) = args.trim().split(Regex("\\s+"), 2)

      repl.setup = GameSetup(repl.setup.authority, bundleString, players.toInt())
      repl.newGame(repl.setup)

      return listOf("New $players-player game created with bundles: $bundleString") +
          if (players.toInt() == 1) {
            listOf("NOTE: No solo mode rules are implemented.")
          } else {
            listOf()
          }
    } catch (e: RuntimeException) {
      throw UsageException(e.message)
    }
  }
}
