package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.Task.TaskId

/**
 * Returned by a successful execution to indicate what changes were performed and what new tasks
 * were added.
 */
public data class TaskResult(
    public val changes: List<ChangeEvent>,
    public val tasksSpawned: Set<TaskId>,
)
