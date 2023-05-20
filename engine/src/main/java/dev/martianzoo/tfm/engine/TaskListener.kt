package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent.TaskAddedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskEditedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.Task

interface TaskListener {
  fun taskAdded(task: Task): TaskAddedEvent
  fun taskRemoved(task: Task): TaskRemovedEvent
  fun taskReplaced(oldTask: Task, newTask: Task): TaskEditedEvent
}
