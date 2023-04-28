package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.SpecialClassNames.RAW
import dev.martianzoo.tfm.api.UserException
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Result
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.Exceptions.InteractiveException
import dev.martianzoo.tfm.engine.Game.PlayerAgent
import dev.martianzoo.tfm.pets.Parsing.parseInput
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.PetTransformer.Companion.chain
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
import dev.martianzoo.tfm.pets.ast.TransformNode
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Hierarchical.Companion.lub
import dev.martianzoo.util.Multiset

/**
 * A convenient interface for functional tests; basically, [ReplSession] is just a more texty
 * version of this.
 *
 * It accepts `RAW[...]` nodes and (explain).
 */
public class PlayerSession
internal constructor(
    val agent: PlayerAgent,
    var defaultAutoExec: Boolean = true, // TODO 3 policies (what were they?)
) {
  val game by agent::game
  val player by agent::player

  public fun asPlayer(player: Player) =
      PlayerSession(PlayerAgent(agent.game, player), defaultAutoExec)

  // in case a shortname is used
  public fun asPlayer(player: ClassName) =
      asPlayer(Player(game.resolve(player.expression).className))

  // QUERIES

  fun count(metric: Metric): Int = agent.reader.count(prep(metric))
  fun count(metric: String) = count(parseInput(metric))
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
  fun has(requirement: String) = has(parseInput(requirement))

  // EXECUTION

  fun sneakyChange(instruction: Instruction): Result {
    val changes =
        split(prep(instruction)).mapNotNull {
          if (it !is Change) throw InteractiveException.badSneak(it)
          val count = it.count
          require(count is ActualScalar)
          agent.sneakyChange(count.value, it.gaining, it.removing)
        }
    return Result(changes = changes, tasksSpawned = setOf())
  }

  fun execute(instruction: String, vararg tasks: String) {
    execute(instruction, true)
    tasks.forEach(::doTask)
  }

  fun execute(instruction: String, autoExec: Boolean = defaultAutoExec) =
      execute(prep(parseInput(instruction)), autoExec)

  fun execute(instruction: Instruction, autoExec: Boolean = defaultAutoExec): Result {
    val instrs = split(prep(instruction))
    return game.doAtomic {
      for (instr in instrs) {
        val tasks = ArrayDeque<TaskId>()
        val firstResult = agent.initiate(instr)
        if (autoExec) {
          tasks += firstResult.tasksSpawned
          while (tasks.any()) {
            val task = tasks.removeFirst()
            tasks += doTaskAndAutoExec(task).tasksSpawned
          }
        }
      }
    }
  }

  fun doTask(instruction: String): Result {
    val taskKeys = agent.tasks().keys
    if (taskKeys.none()) throw UserException("No tasks in queue when trying to do `$instruction`")
    return doTask(taskKeys.min(), instruction)
  }

  fun doTask(initialTaskId: String, narrowedInstruction: String? = null) =
      doTask(TaskId(initialTaskId), narrowedInstruction)

  fun doTask(initialTaskId: TaskId, narrowedInstruction: String? = null): Result {
    val narrowed = narrowedInstruction?.let { prep(parseInput<Instruction>(it)) }
    return if (defaultAutoExec) {
      doTaskAndAutoExec(initialTaskId, narrowed)
    } else {
      doTaskOnly(initialTaskId, narrowed)
    }
  }

  fun doTaskAndAutoExec(
      initialTaskId: TaskId,
      narrowedInstruction: Instruction? = null,
  ): Result {
    val taskIdsToAutoExec: ArrayDeque<TaskId> = ArrayDeque()
    val checkpoint = game.checkpoint()

    val firstResult: Result = agent.doTask(initialTaskId, narrowedInstruction?.let { prep(it) })
    taskIdsToAutoExec += firstResult.tasksSpawned - initialTaskId

    while (taskIdsToAutoExec.any()) {
      val thisTaskId: TaskId = taskIdsToAutoExec.removeFirst()
      val results: Result = agent.doTask(thisTaskId)
      taskIdsToAutoExec += results.tasksSpawned - thisTaskId
    }

    return game.events.activitySince(checkpoint)
  }

  fun doTaskOnly(
      taskId: TaskId,
      narrowedInstruction: Instruction? = null,
  ) = agent.doTask(taskId, narrowedInstruction?.let { prep(it) })

  // OTHER

  fun <P : PetElement> prep(node: P): P {
    val xers = game.transformers
    return chain(
        useFullNames(),
        xers.atomizer(),
        xers.insertDefaults(),
        xers.deprodify(),
        TransformNode.unwrapper(RAW),
    ).transform(node)
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
}
