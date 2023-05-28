package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.TaskResult

interface Operator : GameWriter {
  fun operation(
      starting: String,
      vararg taskInstructions: String,
      body: OperationBody.() -> Unit
  ): TaskResult

  interface OperationBody {
    fun task(instruction: String)
    fun matchTask(instruction: String)
    fun abortAndRollBack()
  }

//  fun tryTask(taskId: Task.TaskId, narrowed: String? = null): TaskResult
//
//  fun matchTask(revised: String): TaskResult
//
  fun tryPreparedTask(): Boolean

  fun autoExec(safely: Boolean = false): TaskResult
}
