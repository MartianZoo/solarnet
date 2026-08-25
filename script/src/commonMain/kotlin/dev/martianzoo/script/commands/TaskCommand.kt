package dev.martianzoo.script.commands

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.data.TaskResult
import dev.martianzoo.script.PetsCompletionRoot
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession

internal class TaskCommand(private val repl: ScriptSession) : ScriptCommand("task") {
  override val usage = "task [<number>] <Instruction> | task <prepare | drop>"
  override val help =
      """
        Carry out the pending task matched by an instruction, narrowing it when needed. For
        example, `task -2 Plant<Player1>` can resolve a queued
        `-3 StandardResource<Anyone>?`. The instruction must match only one pending task, though
        identical tasks are interchangeable and a prepared task always wins. In the rare
        ambiguous case, prefix the instruction with its current 1-based position in `tasks`, such
        as `task 2 Ok`. Task numbers are temporary positions, not ids. `task prepare` and `task
        drop` are available only when exactly one task is pending.
      """

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> {
    val numbered = context.firstWord.toIntOrNull() != null && context.hasRestAfterFirstWord
    val instructionContext = if (numbered) context.droppingLeadingWords(1) else context
    val actions =
        if (context.argIndex == 0) {
          context.completions("drop", "prepare", group = "task actions")
        } else {
          emptyList()
        }
    return actions + instructionContext.petsWords(PetsCompletionRoot.INSTRUCTION)
  }

  override fun withArgs(args: String): List<String> {
    val numbered = Regex("^([1-9]\\d*)\\s+(.+)$").matchEntire(args)
    val taskNumber = numbered?.groupValues?.get(1)?.toInt()
    val request = numbered?.groupValues?.get(2) ?: args
    val result: TaskResult =
        when (args) {
          "drop" -> {
            repl.access().dropTask(repl.onlyTask().id)
            return listOf("Task deleted")
          }
          "prepare" -> {
            repl.gameplay.prepareTask(repl.onlyTask().id)
            return repl.taskLines()
          }
          else -> {
            if (taskNumber == null) {
              repl.gameplay.tryTask(request)
            } else {
              try {
                repl.gameplay.tryTask(args)
              } catch (_: TaskException) {
                repl.gameplay.tryTask(request, taskNumber)
              } catch (_: NarrowingException) {
                repl.gameplay.tryTask(request, taskNumber)
              }
            }
          }
        }
    return repl.describeExecutionResults(result)
  }
}
