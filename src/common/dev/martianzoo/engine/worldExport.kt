package dev.martianzoo.engine

import dev.martianzoo.engine.RoutineReplayEncoder.Entry.Call
import dev.martianzoo.engine.RoutineReplayEncoder.Entry.Correction
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.GamePremise

/** Serializes an idle World to the versioned native REgo format. */
internal fun exportWorld(world: World, premise: GamePremise): String {
  require(world.isIdle()) { "only an idle world can currently be exported" }
  val config =
      requireNotNull(premise.sourceConfig) { "the unresolved game config was not retained" }
  val provider =
      premise.catalog as? RoutineProvider
          ?: error("this Catalog does not provide readable replay encoding")
  val lines =
      mutableListOf(
          "// solarnet world export 1",
          "auto none",
          "",
          "newgame \"$config\" ${config.playerNames.joinToString(" ")} purple",
          "",
      )
  var actor: Actor? = null
  var red = false
  fun leaveRedMode() {
    if (red) {
      lines += "mode purple"
      red = false
    }
  }
  provider.replayEncoder.encode(world, world.events.entriesSinceSetup()).forEach { entry ->
    if (entry.actor != actor) {
      leaveRedMode()
      lines += "BECOME ${world.vocabulary.petsName(entry.actor)}"
      actor = entry.actor
    }
    when (entry) {
      is Call -> {
        leaveRedMode()
        lines += "DO ${entry.name}(${entry.arguments.joinToString()})"
      }
      is Correction -> {
        if (!red) {
          lines += "mode red"
          red = true
        }
        lines += "exec ${entry.instruction}"
      }
    }
  }
  leaveRedMode()
  return lines.joinToString("\n", postfix = "\n")
}
