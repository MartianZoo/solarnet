package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Exceptions.AbstractException
import dev.martianzoo.tfm.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.api.Exceptions.NotNowException
import dev.martianzoo.tfm.api.Exceptions.RecoverableException
import dev.martianzoo.tfm.api.Exceptions.TaskException
import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.engine.ComponentGraph.Component.Companion.toComponent
import dev.martianzoo.tfm.engine.Operator.OperationBody
import dev.martianzoo.tfm.engine.Timeline.AbortOperationException
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.PetTransformer.Companion.chain
import dev.martianzoo.tfm.pets.Transforming.replaceOwnerWith
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.PetElement
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.types.Transformers
import javax.inject.Inject

internal class PlayerAgent
@Inject
constructor(
    private val tasks: WritableTaskQueue,
    private val reader: GameReader,
    private val timeline: Timeline,
    private val player: Player,
    val instructor: Instructor,
    private val changer: Changer,
    transformers: Transformers,
) : Operator, UnsafeGameWriter {

  override fun narrowTask(taskId: TaskId, narrowed: String) =
      narrowTask(taskId, preprocess(parse(narrowed)))

  override fun narrowTask(taskId: TaskId, narrowed: Instruction): TaskResult {
    val task = tasks.getTaskData(taskId)
    if (player != task.owner) {
      throw TaskException("$player can't narrow a task owned by ${task.owner}")
    }

    val current = task.instruction
    val fixedNarrowing = preprocess(narrowed)
    if (fixedNarrowing == current) return TaskResult()
    fixedNarrowing.ensureNarrows(current, reader)

    val replacement = if (task.next) instructor.prepare(fixedNarrowing) else fixedNarrowing
    return editButCheckCardinality(tasks.getTaskData(taskId).copy(instructionIn = replacement))
  }

  override fun canPrepareTask(taskId: TaskId): Boolean {
    dontCutTheLine(taskId)
    val unprepared = tasks.getTaskData(taskId).instruction
    return try {
      instructor.prepare(unprepared)
      true
    } catch (e: Exception) {
      false
    }
  }

  override fun prepareTask(taskId: TaskId): TaskId? {
    val result = doPrepare(tasks.getTaskData(taskId))

    // let's just look ahead though
    if (taskId in tasks) {
      try {
        timeline.atomic {
          executeTask(taskId)
          throw AbstractException("just getting this to roll back")
        }
      } catch (ignore: AbstractException) { // the only failure that's expected/normal
      }
    }
    return result
  }

  private fun doPrepare(task: Task): TaskId? {
    dontCutTheLine(task.id)

    val replacement = instructor.prepare(task.instruction)
    editButCheckCardinality(task.copy(instructionIn = replacement, next = true))
    return tasks.preparedTask()
  }

  // Use this to edit a task if the replacement instruction might be NoOp, in which case the
  // task is handleTask'd instead.
  private fun editButCheckCardinality(replacement: Task): TaskResult {
    return timeline.atomic {
      val split = Instruction.split(replacement.instruction)
      if (split.size == 1) {
        val reason = replacement.whyPending?.let { "(was: $it)" }
        val one = split.instructions[0]
        tasks.editTask(replacement.copy(instructionIn = one, whyPending = reason))
      } else {
        tasks.addTasks(split, replacement.owner, replacement.cause)
        handleTask(replacement.id)
      }
    }
  }

  override fun executeTask(taskId: TaskId): TaskResult {
    return timeline.atomic {
      val prepared = doPrepare(tasks.getTaskData(taskId)) ?: return@atomic
      val preparedTask = tasks.getTaskData(prepared)
      val newTasks = instructor.execute(preparedTask.instruction, preparedTask.cause)
      newTasks.forEach(tasks::addTasks)
      handleTask(taskId)
    }
  }

  override fun explainTask(taskId: TaskId, reason: String) {
    tasks.editTask(tasks.getTaskData(taskId).copy(whyPending = reason))
  }

  override fun operation(
      starting: String,
      vararg taskInstructions: String,
      body: OperationBody.() -> Unit
  ): TaskResult {
    val instruction: Instruction = preprocess(parse(starting))
    require(tasks.isEmpty()) { tasks }

    return timeline.atomic {
      val newTasks = addTask(instruction).tasksSpawned
      newTasks.forEach { executeTask(it) }
      autoExec()
      taskInstructions.forEach(::task)

      OperationBodyImpl().body()

      require(tasks.isEmpty()) {
        "Should be no tasks left, but:\n" + tasks.extract { it }.joinToString("\n")
      }
      require(reader.evaluate(parse("MAX 0 Temporary")))
    }
  }

  inner class OperationBodyImpl : OperationBody {
    override fun task(instruction: String) {
      task(instruction)
      autoExec()
    }
    override fun matchTask(instruction: String) {
      matchTask(instruction)
      autoExec()
    }

    override fun abortAndRollBack() {
      throw AbortOperationException()
    }
  }

  @Suppress("ControlFlowWithEmptyBody")
  override fun autoExec(safely: Boolean): TaskResult { // TODO invert default or something
    return timeline.atomic { while (autoExecOneTask(safely)) {} }
  }

  fun autoExecOneTask(safely: Boolean): Boolean /* should we continue */ {
    if (tasks.isEmpty()) return false

    // see if we can prepare a task (choose only from our own)
    val options: List<TaskId> =
        tasks.preparedTask()?.let(::listOf) ?: tasks.ids().filter(::canPrepareTask)

    when (options.size) {
      0 -> prepareTask(tasks.ids().first()).also { error("that should've failed") }
      1 -> {
        val taskId = options.single()
        prepareTask(taskId) ?: return true
        try {
          if (tryPreparedTask()) return true // if this fails we should fail too
        } catch (e: DeadEndException) {
          throw e.cause ?: e
        }
      }
      else -> if (safely) return false // impasse: we can't choose for you
    }

    var recoverable = false

    // we're in unsafe mode. last resort: do the first task that executes
    for (taskId in options) {
      try {
        executeTask(taskId)
        return true
      } catch (e: RecoverableException) {
        // we're in trouble if ALL of these are NotNowExceptions
        if (e !is NotNowException) recoverable = true
        explainTask(taskId, e.message ?: e::class.simpleName!!)
      }
    }
    if (!recoverable) throw DeadEndException("")

    return false // presumably everything is abstract
  }

  private fun task(revised: String): TaskResult {
    val id =
        tasks.matching { it.owner == player }.firstOrNull() ?: throw NotNowException("no tasks")
    return timeline.atomic {
      narrowTask(id, preprocess(parse(revised)))
      if (id in tasks) executeTask(id)
      autoExec()
    }
  }

  override fun tryPreparedTask(): Boolean /* did I do stuff? */ {
    val taskId = tasks.preparedTask()!!
    return try {
      executeTask(taskId)
      autoExec()
      true
    } catch (e: NotNowException) {
      throw DeadEndException(e)
    } catch (e: RecoverableException) {
      explainTask(taskId, e.message!!)
      false
    }
  }

  override fun unsafe(): UnsafeGameWriter = this

  // UnsafeGameWriter interface

  override fun executeFully(instruction: Instruction, fakeCause: Cause?) {
    addTask(instruction, fakeCause)
    do {
      executeTask(tasks.ids().first())
    } while (tasks.ids().any())
  }

  override fun addTask(instruction: Instruction, firstCause: Cause?) =
      timeline.atomic {
        val prepped = Instruction.split(preprocess(instruction))
        tasks.addTasks(prepped, player, firstCause)
      }

  override fun dropTask(taskId: TaskId) = tasks.removeTask(taskId)

  override fun sneak(changes: String, cause: Cause?) =
      sneak(preprocess(Parsing.parse(changes)), cause)

  // TODO: in theory any instruction would be sneakable, and it only means disabling triggers
  override fun sneak(changes: Instruction, cause: Cause?): TaskResult {
    return timeline.atomic {
      Instruction.split(changes).map {
        val count = (it as Change).count as ActualScalar
        changer.change(
            count.value,
            it.gaining?.toComponent(reader),
            it.removing?.toComponent(reader),
            cause,
            orRemoveOneDependent = false,
        )
      }
    }
  }

  /**
   * Remove a task because its [Task.instruction] has been handled; any [Task.then] instructions are
   * automatically enqueued.
   */
  private fun handleTask(taskId: TaskId) {
    val task = tasks.getTaskData(taskId)
    task.then?.let { tasks.addTasks(Instruction.split(it), task.owner, task.cause) }
    dropTask(taskId)
  }

  private fun dontCutTheLine(taskId: TaskId) {
    val already = tasks.preparedTask()
    if (already != null && already != taskId) {
      throw TaskException("task $already is already prepared and must be executed first")
    }
  }

  private val xer = chain(transformers.standardPreprocess(), replaceOwnerWith(player))

  internal fun <P : PetElement> preprocess(node: P) = xer.transform(node)
}
