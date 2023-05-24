package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.engine.Game.SnReader
import dev.martianzoo.tfm.engine.WritableComponentGraph.Limiter
import javax.inject.Inject

internal class GameWriterFactory @Inject constructor(
    private val reader: SnReader,
    private val updater: Updater,
    private val logger: ChangeLogger,
    private val effector: Effector,
    private val limiter: Limiter,
    private val timeline: TimelineImpl,
    private val tasks: WritableTaskQueue
) {
  public fun writer(player: Player): GameWriter {
    val changer = Changer(reader, updater, logger, player)
    val instructor = Instructor(reader, effector, limiter, changer)
    return GameWriterImpl(tasks, reader, timeline, player, instructor, changer)
  }

}
