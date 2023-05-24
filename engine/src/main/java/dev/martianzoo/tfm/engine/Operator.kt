package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.TaskResult

interface Operator {
  fun autoExec(safely: Boolean = false): TaskResult

  fun operation(startingInstruction: String, vararg tasks: String): TaskResult
  fun operation(startingInstruction: String, body: OperationBody.() -> Unit)

  fun tryTask(taskId: Task.TaskId, narrowed: String? = null): TaskResult
  fun tryPreparedTask(): Boolean

  fun matchTask(revised: String): TaskResult

  interface OperationBody {
    fun task(instruction: String)
    fun matchTask(instruction: String)
    fun rollItBack()
  }
}
