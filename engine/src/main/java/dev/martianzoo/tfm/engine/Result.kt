package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.Task.TaskId

data class Result constructor(
    val changes: List<ChangeEvent>, // TODO Set
    val newTaskIdsAdded: Set<TaskId>,
)
