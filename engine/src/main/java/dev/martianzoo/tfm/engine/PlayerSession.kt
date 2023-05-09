package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.UserException
import dev.martianzoo.tfm.api.UserException.NotNowException
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.PetTransformer.Companion.chain
import dev.martianzoo.tfm.pets.Transforming.replaceOwnerWith
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.split
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetElement
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Hierarchical.Companion.lub
import dev.martianzoo.util.Multiset

/** A player session adds autoexec, string overloads, prep, blah blah. */
public class PlayerSession
internal constructor(
  val game: Game,
  val writer: GameWriter,
  val player: Player,
) {
  public fun asPlayer(player: Player) = game.writer(player).session()

  // in case a shortname is used
  public fun asPlayer(player: ClassName): PlayerSession =
      asPlayer(Player(game.resolve(player.expression).className))

  // QUERIES

  fun count(metric: Metric): Int = game.reader.count(preprocess(metric))
  fun count(metric: String): Int = count(parse(metric))
  fun countComponent(component: Component) = game.reader.countComponent(component.mtype)

  fun list(expression: Expression): Multiset<Expression> { // TODO why not (M)Type?
    val typeToList: MType = game.resolve(preprocess(expression))
    val allComponents: Multiset<Component> = game.getComponents(preprocess(expression))

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

  fun has(requirement: Requirement): Boolean = game.reader.evaluate(preprocess(requirement))
  fun has(requirement: String) = has(parse(requirement))

  // EXECUTION

  /** Action just means "queue empty -> do anything -> queue empty again" */
  fun action(firstInstruction: Instruction): TaskResult {
    return game.doAtomic {
      action(firstInstruction) {}
    }
  }

  fun action(firstInstruction: String): TaskResult = action(parseInContext(firstInstruction))

  fun <T : Any> action(firstInstruction: String, tasker: Tasker.() -> T?) =
      action(parseInContext(firstInstruction), tasker)

  fun <T : Any> action(firstInstruction: Instruction, tasker: Tasker.() -> T?): T? {
    require(game.tasks.isEmpty()) { game.tasks }
    val cp = game.checkpoint()
    initiate(firstInstruction) // try task then try to drain

    val wrapped = Tasker(this)
    val result =
        try {
          wrapped.tasker()
        } catch (e: Exception) {
          game.rollBack(cp)
          throw e
        }

    if (result == wrapped.rollItBack()) {
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

  fun sneakyChange(instruction: Instruction): TaskResult {
    val changes =
        prepAndSplit(instruction).mapNotNull {
          if (it !is Change) throw UserException.badSneak(it)
          val count = it.count
          require(count is ActualScalar)
          writer.sneakyChange(
              count.value,
              game.toComponent(it.gaining),
              game.toComponent(it.removing),
          )
        }
    return TaskResult(changes = changes, tasksSpawned = setOf())
  }

  // OTHER

  // TODO reconcile this with heyItsMe & stuff
  fun <P : PetElement> preprocess(node: P) =
      chain(game.transformers.standardPreprocess(), replaceOwnerWith(player)).transform(node)

  inline fun <reified P : PetElement> parseInContext(text: String): P = preprocess(parse(text))

  fun execute(instruction: String, vararg tasks: String) {
    initiate(instruction)
    tasks.forEach { tryMatchingTask(it) }
  }

  fun initiate(instruction: String) = initiate(parse(instruction))

  fun initiate(instruction: Instruction): TaskResult {
    val prepped: List<Instruction> = prepAndSplit(instruction) // TODO, obvs
    return game.doAtomic {
      prepped.forEach {
        val id = writer.addTask(it)
        writer.doTask(id)
      }
      tryToDrain()
    }
  }

  fun tryToDrain(): TaskResult {
    return game.doAtomic {
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
    return game.doAtomic {
      writer.doTask(id, parseInContext(revised))
      tryToDrain()
    }
  }

  fun tryTask(id: TaskId, narrowed: String? = null): TaskResult {
    return game.doAtomic {
      writer.tryTask(id, narrowed?.let(::parseInContext))
      tryToDrain()
    }
  }

  private fun prepAndSplit(instruction: Instruction) = split(preprocess(instruction))
}
