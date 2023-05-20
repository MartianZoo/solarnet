package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.api.Exceptions.NotNowException
import dev.martianzoo.tfm.api.Exceptions.RecoverableException
import dev.martianzoo.tfm.api.Exceptions.TaskException
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.engine.Component.Companion.toComponent
import dev.martianzoo.tfm.engine.Game.EventLog.Checkpoint
import dev.martianzoo.tfm.engine.Game.GameWriterImpl
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.split
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetElement
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Hierarchical.Companion.lub
import dev.martianzoo.util.Multiset

/** A player session adds autoexec, string overloads, prep, blah blah. */
public class PlayerSession(
    private val game: Game,
    public val player: Player,
) {
  companion object {
    fun Game.session(player: Player) = PlayerSession(this, player)
  }

  public val writer = game.writer(player)
  public val reader by game::reader
  public val tasks by game::tasks
  public val events by game::events

  fun myTasks(): List<TaskId> =
      tasks.filter { player == ENGINE || it.owner == player }.map { it.id }.sorted()

  // TODO get rid
  public fun asPlayer(player: Player) = game.session(player)

  // in case a shortname is used
  public fun asPlayer(player: ClassName): PlayerSession =
      asPlayer(Player(reader.resolve(player.expression).className))

  // QUERIES

  fun count(metric: Metric): Int = reader.count(preprocess(metric))
  fun count(metric: String): Int = count(parse(metric))
  fun countComponent(component: Component) = reader.countComponent(component.mtype)

  fun list(expression: Expression): Multiset<Expression> { // TODO why not (M)Type?
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

  /** See [Game.atomic]. */
  public fun atomic(block: () -> Unit) = game.atomic(block)

  internal val theTasker = Tasker(this)

  fun turn(initial: String? = null) = turn(initial) {}

  fun <T : Any> turn(initial: String? = null, tasker: Tasker.() -> T?): T? {
    return action("NewTurn") {
      initial?.let(this::task)
      theTasker.tasker()
    }
  }

  /** Here action just means "queue empty -> do safe, draining safely -> queue empty again" */
  fun action(manual: Instruction, vararg tasks: String): TaskResult {
    return atomic { action(manual) { tasks.forEach(::task) } }
  }

  fun action(firstInstruction: String, vararg tasks: String): TaskResult =
      action(parseInContext(firstInstruction), *tasks)

  fun <T : Any> action(firstInstruction: String, tasker: Tasker.() -> T?): T? =
      action(parseInContext(firstInstruction), tasker)

  fun <T : Any> action(firstInstruction: Instruction, tasker: Tasker.() -> T?): T? {
    require(game.tasks.isEmpty()) { game.tasks }
    val cp = game.checkpoint()
    initiate(firstInstruction) // try task then try to drain
    autoExec()

    val result =
        try {
          theTasker.tasker()
        } catch (e: Exception) {
          game.rollBack(cp)
          throw e
        }

    if (result == theTasker.rollItBack()) {
      game.rollBack(cp)
    } else {
      require(game.tasks.isEmpty()) {
        "Should be no tasks left, but:\n" + game.tasks.joinToString("\n")
      }
      require(game.reader.evaluate(parse("MAX 0 Temporary")))
    }
    return result
  }

  class Tasker(val session: PlayerSession) {
    fun tasks() = session.game.tasks

    fun task(instr: String) {
      session.task(instr)
      session.autoExec()
    }

    fun matchTask(instr: String) {
      session.matchTask(instr)
      session.autoExec()
    }

    fun rollItBack() = null
  }

  // OTHER

  // TODO hmmm
  public fun <P : PetElement> preprocess(node: P) = (writer as GameWriterImpl).preprocess(node)

  private inline fun <reified P : PetElement> parseInContext(text: String): P =
      preprocess(parse(text))

  fun execute(instruction: String, vararg tasks: String) {
    val task = initiate(instruction).tasksSpawned.single()
    writer.executeTask(task)
    tasks.forEach { matchTask(it) } // TODO do first??
  }

  fun initiate(manual: String) = initiate(parse(manual))

  fun initiate(manual: Instruction): TaskResult {
    return atomic { writer.unsafe().initiateTask(manual) }
  }

  @Suppress("ControlFlowWithEmptyBody")
  fun autoExec(safely: Boolean = false): TaskResult { // TODO invert default or something
    return atomic {
      while (autoExecOneTask(safely)) {
      }
    }
  }

  fun autoExecOneTask(safely: Boolean = true): Boolean /* should we continue */ {
    if (myTasks().none()) return false

    // see if we can prepare a task (choose only from our own)
    val options: List<TaskId> =
        if (tasks.hasPreparedTask()) {
          listOf(tasks.preparedTask()!!) // we'll prepare it again
        } else {
          myTasks().filter(writer::canPrepareTask)
        }

    when (options.size) {
      0 -> writer.prepareTask(myTasks().first()) // this will throw
      1 -> {
        val taskId = options.single()
        writer.prepareTask(taskId)
        if (taskId !in tasks) return true
        if (tryPreparedTask()) return true // if this fails we should fail too
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

  fun tryTask(taskId: TaskId, narrowed: String? = null) = atomic {
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
  private fun tryPreparedTask(): Boolean /* did I do stuff? */ {
    val taskId = tasks.preparedTask()!!
    return try {
      writer.executeTask(taskId)
      autoExec()
      true
    } catch (e: NotNowException) {
      throw DeadEndException(e) // has to be rolled back
    } catch (e: RecoverableException) {
      writer.explainTask(taskId, e.message!!)
      false
    }
  }

  fun matchTask(revised: String): TaskResult {
    if (game.tasks.none()) throw TaskException("no tasks")

    val ins: Instruction = preprocess(parse(revised))
    val matches =
        game.tasks.filter { it.owner == player && ins.narrows(it.instruction, game.reader) }

    if (matches.size == 0) {
      throw TaskException("no matches for $ins among:\n${game.tasks.joinToString("")}")
    }
    if (matches.size > 1) {
      throw TaskException("multiple matches for $ins among:\n${game.tasks.joinToString("")}")
    }
    val id = matches.single().id

    return atomic {
      writer.prepareTask(id)
      if (id in tasks) writer.narrowTask(id, ins)
      if (id in tasks) writer.executeTask(id)
      autoExec()
    }
  }

  fun ifMatchTask(revised: String): TaskResult {
    val prepped: Instruction = preprocess(parse(revised))
    val id =
        game.tasks
            .filter { it.owner == player }
            .singleOrNull { prepped.narrows(it.instruction, game.reader) }
            ?.id
          ?: return TaskResult()

    return atomic {
      writer.prepareTask(id)
      if (id in tasks) writer.narrowTask(id, prepped)
      if (id in tasks) writer.executeTask(id)
      autoExec()
    }
  }

  fun task(revised: String): TaskResult {
    val id = game.tasks.firstOrNull { it.owner == player }?.id ?: throw NotNowException("no tasks")
    return atomic {
      writer.narrowTask(id, preprocess(parse(revised)))
      if (id in tasks) writer.executeTask(id)
      autoExec()
    }
  }

  private fun prepAndSplit(instruction: Instruction) = split(preprocess(instruction))

  fun rollBack(checkpoint: Checkpoint) = game.rollBack(checkpoint)
  fun rollBack(position: Int) = rollBack(Checkpoint(position))
}
