package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.Task.TaskId

interface UnsafeGameWriter {
  fun removeTask(taskId: TaskId): TaskRemovedEvent
}
