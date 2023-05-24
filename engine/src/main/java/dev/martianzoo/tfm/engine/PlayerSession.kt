package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.api.Exceptions.NotNowException
import dev.martianzoo.tfm.api.Exceptions.RecoverableException
import dev.martianzoo.tfm.api.Exceptions.TaskException
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.engine.Component.Companion.toComponent
import dev.martianzoo.tfm.engine.Operator.OperationBody
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetElement
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Hierarchical.Companion.lub
import dev.martianzoo.util.Multiset

/** A player session adds autoexec, string overloads, prep, blah blah. */
public class PlayerSession(
    // TODO Session?
    private val game: Game,
    public val player: Player,
) : Operator {
  companion object {
    fun Game.session(player: Player) = PlayerSession(this, player)
  }

  public val writer = game.writer(player)
  public val reader = game.reader as GameReaderImpl
  public val tasks by game::tasks
  public val events by game::events
  public val timeline by game::timeline

  public fun asPlayer(player: Player) = game.session(player)

  // in case a shortname is used
  public fun asPlayer(player: ClassName): PlayerSession =
      asPlayer(Player(reader.resolve(player.expression).className))

  // QUERIES

  fun count(metric: Metric): Int = reader.count(preprocess(metric))

  fun count(metric: String): Int = count(parse(metric))
  fun countComponent(component: Component) = reader.countComponent(component.mtype)
  fun list(expression: Expression): Multiset<Expression> {
    val typeToList: MType = reader.resolve(preprocess(expression))
    val allComponents: Multiset<Component> =
        reader.getComponents(reader.resolve(preprocess(expression))).map { it.toComponent() }

    val result = HashMultiset<Expression>()
    typeToList.root.directSubclasses.forEach { sub ->
      val matches = allComponents.filter { it.mtype.isSubtypeOf(sub.baseType) }
      if (matches.any()) {
        val types = matches.elements.map { it.mtype }
        result.add(lub(types)!!.expression, matches.size)
      }
    }
    return result
  }

  fun has(requirement: Requirement): Boolean = reader.evaluate(preprocess(requirement))

  fun has(requirement: String) = has(parse(requirement))

  // EXECUTION

  override fun operation(startingInstruction: String, vararg tasks: String): TaskResult =
      timeline.atomic { operation(startingInstruction) { tasks.forEach(::task) } }

  override fun operation(startingInstruction: String, body: OperationBody.() -> Unit) {
    val instruction: Instruction = parseInContext(startingInstruction)
    require(tasks.isEmpty()) { tasks }
    val cp = timeline.checkpoint()
    initiateOnly(instruction)
    autoExec()

    try {
      OperationBodyImpl().body()
    } catch (e: JustRollBackException) {
      timeline.rollBack(cp)
    } catch (e: Exception) {
      timeline.rollBack(cp)
      throw e
    }

    require(tasks.isEmpty()) {
      "Should be no tasks left, but:\n" + tasks.extract { it }.joinToString("\n")
    }
    require(game.reader.evaluate(parse("MAX 0 Temporary")))
  }

  private class JustRollBackException : Exception("")

  inner class OperationBodyImpl : OperationBody {
    val session = this@PlayerSession
    val tasks by game::tasks

    override fun task(instruction: String) {
      session.task(instruction)
      autoExec()
    }

    override fun matchTask(instruction: String) {
      session.matchTask(instruction)
      autoExec()
    }

    // TODO rename or something, it sounds like you can keep going
    override fun rollItBack() {
      throw JustRollBackException()
    }
  }

  // OTHER

  fun initiateOnly(instruction: Instruction, fakeCause: Cause? = null): TaskResult {
    return timeline.atomic {
      val newTasks = writer.unsafe().addTask(instruction, fakeCause).tasksSpawned
      newTasks.forEach { writer.executeTask(it) }
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
        tasks.preparedTask()?.let(::listOf) ?: tasks.ids().filter(writer::canPrepareTask)

    when (options.size) {
      0 -> writer.prepareTask(tasks.ids().first()).also { error("that should've failed") }
      1 -> {
        val taskId = options.single()
        writer.prepareTask(taskId) ?: return true
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
        writer.executeTask(taskId)
        return true
      } catch (e: RecoverableException) {
        // we're in trouble if ALL of these are NotNowExceptions
        if (e !is NotNowException) recoverable = true
        writer.explainTask(taskId, e.message ?: e::class.simpleName!!)
      }
    }
    if (!recoverable) throw DeadEndException("")

    return false // presumably everything is abstract
  }

  override fun tryTask(taskId: TaskId, narrowed: String?) =
      timeline.atomic {
        try {
          writer.prepareTask(taskId)
          if (taskId !in tasks && narrowed !in setOf(null, "Ok")) {
            throw NarrowingException("$narrowed isn't Ok")
          }
          narrowed?.let { writer.narrowTask(taskId, parse(it)) }
          if (taskId in tasks) writer.executeTask(taskId)
          autoExec()
        } catch (e: RecoverableException) {
          writer.explainTask(taskId, e.message!!)
        }
      }

  // Similar to tryTask, but a NotNowException is unrecoverable in this case
  override fun tryPreparedTask(): Boolean /* did I do stuff? */ {
    val taskId = tasks.preparedTask()!!
    return try {
      writer.executeTask(taskId)
      autoExec()
      true
    } catch (e: NotNowException) {
      throw DeadEndException(e)
    } catch (e: RecoverableException) {
      writer.explainTask(taskId, e.message!!)
      false
    }
  }

  override fun matchTask(revised: String): TaskResult {
    if (tasks.isEmpty()) throw TaskException("no tasks")

    val ins: Instruction = preprocess(parse(revised))
    val matches = tasks.matching { it.owner == player && ins.narrows(it.instruction, game.reader) }

    val id = matches.singleOrNull()

    if (id == null) {
      throw TaskException(
          "${matches.size} matches for $ins among:\n" +
              tasks.extract { "${it.instruction}" }.joinToString("\n"))
    }

    return timeline.atomic {
      writer.prepareTask(id)
      if (id in tasks) writer.narrowTask(id, ins)
      if (id in tasks) writer.executeTask(id)
      autoExec()
    }
  }

  fun ifMatchTask(revised: String): TaskResult {
    val prepped: Instruction = preprocess(parse(revised))
    val id =
        tasks
            .matching { it.owner == player && prepped.narrows(it.instruction, game.reader) }
            .single()

    return timeline.atomic {
      writer.prepareTask(id)
      if (id in tasks) writer.narrowTask(id, prepped)
      if (id in tasks) writer.executeTask(id)
      autoExec()
    }
  }

  fun task(revised: String): TaskResult {
    val id =
        tasks.matching { it.owner == player }.firstOrNull() ?: throw NotNowException("no tasks")
    return timeline.atomic {
      writer.narrowTask(id, preprocess(parse(revised)))
      if (id in tasks) writer.executeTask(id)
      autoExec()
    }
  }

  public fun <P : PetElement> preprocess(node: P) = (writer as GameWriterImpl).preprocess(node)

  private inline fun <reified P : PetElement> parseInContext(text: String): P =
      preprocess(parse(text))
}
