package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.engine.Game.SnReader
import dev.martianzoo.tfm.engine.WritableComponentGraph.Limiter
import javax.inject.Inject

internal class GameWriterFactory @Inject constructor(
    val reader: SnReader,
    val updater: Updater,
    val logger: ChangeLogger,
    val effector: Effector,
    val limiter: Limiter,
    val timeline: TimelineImpl,
    val tasks: WritableTaskQueue
) {
  public fun writer(player: Player): GameWriter {
    val changer = Changer(reader, updater, logger, player)
    val instructor = Instructor(reader, effector, limiter, changer)
    return GameWriterImpl(tasks, reader, timeline, player, instructor, changer)
  }

}
