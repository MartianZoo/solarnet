package dev.martianzoo.engine

import dev.martianzoo.engine.Exceptions.IncompleteAnalysisException
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Motivation
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskQueue

/** Executes mandatory game work, retaining priority and retrying after progress. */
internal class AdminTasks(
    private val allTasks: TaskQueue,
    private val timeline: Timeline,
    private val engine: () -> ActorEngine,
    private val validateApplication: () -> Unit,
) {
  /**
   * Attempts Admin work in FIFO order, retrying waiting tasks after each success. A selected Player
   * task retains priority. Every nonempty pass must succeed at a task; otherwise its retained
   * failure is thrown, including choices still needing narrowing and work waiting on a Player. An
   * unfinished proof is retried after progress and takes precedence over gameplay rejection when
   * the pass stalls. Missing application input is also retried after progress and reported before
   * returning to Player work.
   *
   * Complete enumeration of legal narrowings remains outside this implementation.
   */
  internal fun run() {
    val waiting = mutableListOf<TaskId>()
    while (true) {
      val selected = allTasks.selectedTask()
      // A failing selected Admin task cannot wait for competing Player work: selection blocks it.
      if (selected != null && !isAdmin(selected)) return
      val pending =
          (selected?.let(::listOf) ?: allTasks.ids().toList()).filter {
            isAdmin(it) && !waitsForEarlierWork(it)
          }
      waiting.retainAll(pending.toSet())
      waiting.addAll(pending.filterNot { it in waiting })
      if (waiting.isEmpty()) return
      var rejection: Exception? = null
      var incomplete: IncompleteAnalysisException? = null
      var inputRequired: InputRequiredException? = null
      var progressed = false
      repeat(waiting.size) {
        if (!progressed) {
          val taskId = waiting.removeAt(0)
          try {
            timeline.atomic {
              val engine = engine()
              engine.transaction(Motivation.ADMIN_EAGER_POLICY) {
                engine.doTask(taskId)
              }
              validateApplication()
            }
            progressed = true
          } catch (e: InputRequiredException) {
            waiting += taskId
            inputRequired = inputRequired ?: e
          } catch (e: IncompleteAnalysisException) {
            waiting += taskId
            incomplete = incomplete ?: e
          } catch (e: NotFullySpecifiedException) {
            waiting += taskId
            rejection = e
          } catch (e: RequirementException) {
            waiting += taskId
            rejection = e
          } catch (e: NotNowException) {
            waiting += taskId
            rejection = e
          } catch (e: DeadEndException) {
            waiting += taskId
            rejection = e
          }
        }
      }
      if (!progressed) {
        inputRequired?.let { throw it }
        incomplete?.let { throw it }
        throw checkNotNull(rejection) { "Admin made no progress without retaining a failure" }
      }
    }
  }

  private fun isAdmin(id: TaskId): Boolean = allTasks.getTaskData(id).assignee == ADMIN

  private fun waitsForEarlierWork(id: TaskId): Boolean =
      allTasks.getTaskData(id).deferral > allTasks.earliestDeferral
}
