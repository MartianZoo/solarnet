package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Exceptions.NotNowException
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.engine.Component.Companion.ofType
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
        reader.getComponents(reader.resolve(preprocess(expression))).map(::ofType)

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

  /** Action just means "queue empty -> do anything -> queue empty again" */
  fun action(firstInstruction: Instruction, vararg tasks: String): TaskResult {
    return game.atomic { action(firstInstruction) { tasks.forEach(::doFirstTask) } }
  }

  fun action(firstInstruction: String, vararg tasks: String): TaskResult =
      action(parseInContext(firstInstruction), *tasks)

  fun <T : Any> action(firstInstruction: String, tasker: Tasker.() -> T?): T? =
      action(parseInContext(firstInstruction), tasker)

  fun <T : Any> action(firstInstruction: Instruction, tasker: Tasker.() -> T?): T? {
    require(game.tasks.isEmpty()) { game.tasks }
    val cp = game.checkpoint()
    initiate(firstInstruction) // try task then try to drain

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
    }
    return result
  }

  class Tasker(val session: PlayerSession) {

    fun tasks() = session.game.tasks

    fun doFirstTask(instr: String) {
      session.doFirstTask(instr)
      session.tryToDrain()
    }

    fun tryMatchingTask(instr: String) {
      session.tryMatchingTask(instr)
      session.tryToDrain()
    }

    fun rollItBack() = null
  }

  // OTHER

  // TODO hmmm
  public fun <P : PetElement> preprocess(node: P) = (writer as GameWriterImpl).preprocess(node)

  private inline fun <reified P : PetElement> parseInContext(text: String): P =
      preprocess(parse(text))

  fun execute(instruction: String, vararg tasks: String) {
    initiate(instruction)
    tasks.forEach { tryMatchingTask(it) }
  }

  fun initiate(instruction: String) = initiate(parse(instruction))

  fun initiate(instruction: Instruction): TaskResult {
    val prepped: List<Instruction> = prepAndSplit(instruction) // TODO, obvs
    return game.atomic {
      prepped.forEach(writer::doTask)
      tryToDrain()
    }
  }

  fun tryToDrain(): TaskResult {
    return game.atomic {
      var anySuccess = true
      while (anySuccess) {
        val allTasks = game.tasks.ids()
        if (allTasks.none()) break
        anySuccess =
            allTasks
                .filter {
                  writer.tryTask(it)
                  it !in game.tasks.ids()
                }
                .any() // filter-any, not any, because we want a full trip
      }
    }
  }

  fun tryMatchingTask(revised: String): TaskResult {
    val ids = game.tasks.ids()
    if (ids.none()) throw NotNowException("no tasks")
    var ex: Exception? = null
    for (id in ids) {
      try {
        return tryTask(id, revised)
      } catch (e: Exception) {
        if (ex == null) {
          ex = e
        } else {
          ex.addSuppressed(e)
        }
      }
    }
    throw ex!! // it must have been set for us to get here
  }

  /** Like try but throws if it doesn't succeed */
  fun doFirstTask(revised: String): TaskResult {
    val id = game.tasks.firstOrNull { it.owner == player }?.id ?: throw NotNowException("no tasks")
    return game.atomic {
      writer.doTask(id, parseInContext(revised))
      tryToDrain()
    }
  }

  fun tryTask(id: TaskId, narrowed: String? = null): TaskResult {
    return game.atomic {
      writer.tryTask(id, narrowed?.let(::parseInContext))
      tryToDrain()
    }
  }

  private fun prepAndSplit(instruction: Instruction) = split(preprocess(instruction))

  fun rollBack(checkpoint: Checkpoint) = game.rollBack(checkpoint)
  fun rollBack(position: Int) = rollBack(Checkpoint(position))
}
