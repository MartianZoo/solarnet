package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.SpecialClassNames.RAW
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.Component
import dev.martianzoo.tfm.engine.EventLog.Checkpoint
import dev.martianzoo.tfm.engine.Game
import dev.martianzoo.tfm.engine.PlayerAgent
import dev.martianzoo.tfm.engine.Result
import dev.martianzoo.tfm.pets.Parsing.parseInput
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.PureTransformers.transformInSeries
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
 * It accepts `RAW[...]` nodes and (TODO explain).
 */
// TODO most of this doesn't seem to belong only in `repl`
public class InteractiveSession(
    val game: Game,
    val player: Player = Player.ENGINE,
    var defaultAutoExec: Boolean = true, // TODO 3 policies
) {
  public val agent: PlayerAgent = agent(player)

  public fun asPlayer(player: Player) = InteractiveSession(game, player)
  public fun asPlayer(player: ClassName): InteractiveSession {
    // in case a shortname is used
    return asPlayer(Player(game.resolve(player.expr).className))
  }

  // QUERIES

  fun count(metric: Metric): Int = agent.count(prep(metric))
  fun count(metric: String) = count(parseInput(metric))
  fun countComponent(component: Component) = game.reader.countComponent(component.mtype)

  fun list(expression: Expression): Multiset<Expression> { // TODO y not (M)Type?
    val typeToList: MType = game.resolve(prep(expression))
    val allComponents: Multiset<Component> = agent.getComponents(prep(expression))

    // TODO decide more intelligently how to break it down

    // Note: when I try to do this right, add a test for CapitalTile but don't worry that much
    // about how it comes out...
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

  fun has(requirement: Requirement): Boolean = agent.evaluate(prep(requirement))
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
    return Result(changes = changes, newTaskIdsAdded = setOf())
  }

  fun execute(instruction: String, vararg tasks: String) {
    execute(instruction, true)
    tasks.forEach(::doTask)
  }

  fun execute(instruction: String, autoExec: Boolean = defaultAutoExec) =
      execute(prep(parseInput(instruction)), autoExec)

  fun execute(instruction: Instruction, autoExec: Boolean = defaultAutoExec): Result {
    val instrs = split(prep(instruction))
    return agent.doAtomic {
      for (instr in instrs) { // TODO
        val tasks = ArrayDeque<TaskId>()
        val firstResult = agent.initiate(instr)
        if (autoExec) {
          tasks += firstResult.newTaskIdsAdded
          while (tasks.any()) {
            val task = tasks.removeFirst()
            tasks += doTaskAndAutoExec(task).newTaskIdsAdded
          }
        }
      }
    }
  }

  fun doTask(instruction: String) =
      doTask(agent.tasks().keys.min(), instruction)

  fun doTask(initialTaskId: String, narrowedInstruction: String? = null) =
      doTask(TaskId(initialTaskId), narrowedInstruction)

  fun doTask(initialTaskId: TaskId, narrowedInstruction: String? = null): Result {
    val narrowed = narrowedInstruction?.let<String, Instruction> { parseInput(it) }
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
    taskIdsToAutoExec += firstResult.newTaskIdsAdded - initialTaskId

    while (taskIdsToAutoExec.any()) {
      val thisTaskId: TaskId = taskIdsToAutoExec.removeFirst()
      val results: Result = agent.doTask(thisTaskId)
      taskIdsToAutoExec += results.newTaskIdsAdded - thisTaskId // TODO better
    }

    return game.eventLog.activitySince(checkpoint)
  }

  fun doTaskOnly(
      taskId: TaskId,
      narrowedInstruction: Instruction? = null,
  ) = agent.doTask(taskId, narrowedInstruction?.let { prep(it) })

  // OTHER

  fun dropTask(id: String) = agent.removeTask(TaskId(id))

  fun rollBack(ordinal: Int) = game.rollBack(ordinal)
  fun rollBack(checkpoint: Checkpoint) = game.rollBack(checkpoint)

  fun <P : PetElement> prep(node: P): P {
    val xers = game.loader.transformers
    val xer =
        transformInSeries(
            useFullNames(),
            xers.atomizer(),
            xers.insertDefaults(THIS.expr),
            xers.deprodify(),
            TransformNode.unwrapper(RAW))
    return xer.transform(node)
  }

  fun agent(player: Player) = game.asPlayer(player)

  public fun useFullNames() =
      object : PetTransformer() {
        override fun <P : PetNode> transform(node: P): P {
          return if (node is ClassName) {
            @Suppress("UNCHECKED_CAST")
            game.loader.getClass(node).className as P
          } else {
            node
          }
        }
      }
}
