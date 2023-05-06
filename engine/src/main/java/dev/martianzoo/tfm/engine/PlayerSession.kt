package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.UserException
import dev.martianzoo.tfm.api.UserException.NotNowException
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.engine.Game.PlayerAgentImpl
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.PetTransformer.Companion.chain
import dev.martianzoo.tfm.pets.Transforming.replaceOwnerWith
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.split
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetElement
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Hierarchical.Companion.lub
import dev.martianzoo.util.Multiset
import dev.martianzoo.util.toStrings

/** A player session adds autoexec, string overloads, prep, blah blah. */
public class PlayerSession
internal constructor(
    val game: Game,
    val agent: PlayerAgent,
) {
  val player by agent::player

  public fun asPlayer(player: Player) = agent.asPlayer(player).session()

  // in case a shortname is used
  public fun asPlayer(player: ClassName) =
      asPlayer(Player(game.resolve(player.expression).className))

  // QUERIES

  fun count(metric: Metric): Int = agent.reader.count(prep(metric))
  fun count(metric: String): Int = count(parse(metric))
  fun countComponent(component: Component) = agent.reader.countComponent(component.mtype)

  fun list(expression: Expression): Multiset<Expression> { // TODO why not (M)Type?
    val typeToList: MType = agent.reader.resolve(prep(expression)) as MType
    val allComponents: Multiset<Component> = agent.getComponents(prep(expression))

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

  fun has(requirement: Requirement): Boolean = agent.reader.evaluate(prep(requirement))
  fun has(requirement: String) = has(parse(requirement))

  // EXECUTION

  fun sneakyChange(instruction: Instruction): TaskResult {
    val changes =
        prepAndSplit(instruction).mapNotNull {
          if (it !is Change) throw UserException.badSneak(it)
          val count = it.count
          require(count is ActualScalar)
          agent.sneakyChange(
              count.value,
              game.toComponent(it.gaining),
              game.toComponent(it.removing),
          )
        }
    return TaskResult(changes = changes, tasksSpawned = setOf())
  }

  // OTHER

  fun <P : PetElement> prep(node: P): P {
    val xers = game.transformers
    return chain(
            useFullNames(),
            xers.atomizer(),
            xers.insertDefaults(),
            xers.deprodify(),
            replaceOwnerWith(player),
        )
        .transform(node)
  }

  public fun useFullNames() =
      object : PetTransformer() {
        override fun <P : PetNode> transform(node: P): P {
          return if (node is ClassName) {
            @Suppress("UNCHECKED_CAST")
            agent.reader.resolve(node.expression).className as P
          } else {
            transformChildren(node)
          }
        }
      }

  fun execute(instruction: String) = execute(parse(instruction))

  fun execute(instruction: Instruction): TaskResult {
    return game.doAtomic {
      initiate(instruction)
      mustDrain()
    }
  }

  fun execute(instruction: String, vararg tasks: String) {
    initiate(instruction)
    tasks.forEach { tryMatchingTask(it) }
    mustDrain()
  }

  fun initiate(instruction: String) = initiate(parse(instruction))

  fun initiate(instruction: Instruction): TaskResult {
    val prepped: List<Instruction> = prepAndSplit(instruction) // TODO, obvs
    return game.doAtomic {
      prepped.forEach {
        val id = agent.addTask(it)
        agent.doTask(id)
      }
      tryToDrain()
    }
  }

  fun initiate(instruction: String, vararg tasks: String) {
    initiate(instruction)
    tasks.forEach(::tryMatchingTask)
  }

  fun mustDrain(): TaskResult {
    val result = tryToDrain()
    if (agent.tasks().any()) error(agent.tasks().values.toStrings().joinToString("\n"))
    return result
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
                  agent.tryTask(it)
                  it !in agent.tasks()
                }
                .any() // filter-any, not any, because we want a full trip
      }
    }
  }

  fun executeMatchingTask(revised: String): TaskResult {
    val ids = agent.tasks().filterValues { it.owner == player }.keys
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

  fun tryMatchingTask(revised: String): TaskResult {
    val ids = agent.tasks().keys
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
    val id = agent.tasks().keys.firstOrNull() ?: throw NotNowException("no tasks")
    return agent.doTask(id, prep(parse<Instruction>(revised)))
  }

  fun tryTask(id: TaskId, narrowed: String? = null) =
      agent.tryTask(id, narrowed?.let { prep(parse(it)) })

  private fun prepAndSplit(instruction: Instruction) =
      split((agent as PlayerAgentImpl).heyItsMe(prep(instruction))) // TODO, obvs
}
