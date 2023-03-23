package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.Exceptions.UserException
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.data.Actor
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.Component
import dev.martianzoo.tfm.engine.Game
import dev.martianzoo.tfm.engine.PlayerAgent
import dev.martianzoo.tfm.engine.Result
import dev.martianzoo.tfm.pets.Parsing.parseInput
import dev.martianzoo.tfm.pets.PetFeature.ATOMIZE
import dev.martianzoo.tfm.pets.PetFeature.DEFAULTS
import dev.martianzoo.tfm.pets.PetFeature.PROD_BLOCKS
import dev.martianzoo.tfm.pets.PetFeature.SHORT_NAMES
import dev.martianzoo.tfm.pets.PureTransformers.transformInSeries
import dev.martianzoo.tfm.pets.Raw
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

/**
 * A convenient interface for functional tests; basically, [ReplSession] is just a more texty
 * version of this.
 */
public class InteractiveSession(
    val game: Game,
    actor: Actor = Actor.ENGINE,
    var defaultAutoExec: Boolean = true,
) {
  internal val agent: PlayerAgent = agent(actor)

  public fun asActor(actor: Actor) = InteractiveSession(game, actor)

  // QUERIES

  internal val features = setOf(ATOMIZE, DEFAULTS, PROD_BLOCKS, SHORT_NAMES)

  fun count(metric: Metric): Int = agent.count(metric)
  fun count(metric: Raw<Metric>) = count(prep(metric))
  fun count(metric: String) = count(parseInput(metric, features))

  fun list(expression: Raw<Expression>): Multiset<Expression> { // TODO y not (M)Type?
    val typeToList: MType = game.resolve(prep(expression))
    val allComponents: Multiset<Component> = agent.getComponents(prep(expression))

    // BIGTODO decide more intelligently how to break it down

    // ugh capital tile TODO
    val result = HashMultiset<Expression>()
    typeToList.mclass.directSubclasses.forEach { sub ->
      val matches = allComponents.filter { it.hasType(sub.baseType) }
      if (matches.any()) {
        val types = matches.elements.map { it.mtype }
        result.add(lub(types)!!.expression, matches.size)
      }
    }
    return result
  }

  fun has(requirement: Requirement): Boolean = agent.evaluate(requirement)
  fun has(requirement: Raw<Requirement>) = has(prep(requirement))
  fun has(requirement: String) = has(parseInput(requirement, features))

  // EXECUTION

  fun sneakyChange(instruction: Instruction): Result {
    val changes =
        split(instruction).mapNotNull {
          if (it !is Change) throw UserException("can only sneak simple changes")
          val count = it.count
          require(count is ActualScalar)
          agent.quietChange(count.value, it.gaining, it.removing)
        }
    return Result(changes = changes, newTaskIdsAdded = setOf())
  }
  fun sneakyChange(raw: Raw<Instruction>) = sneakyChange(prep(raw))
  fun sneakyChange(raw: String) = sneakyChange(parseInput(raw, features))

  fun execute(instruction: String, autoExec: Boolean = defaultAutoExec) =
      execute(parseInput(instruction, features), autoExec)

  fun execute(instruction: Raw<Instruction>, autoExec: Boolean = defaultAutoExec) =
      execute(prep(instruction), autoExec)

  fun execute(instruction: Instruction, autoExec: Boolean = defaultAutoExec): Result {
    val instrs = split(instruction)
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

  fun doTask(id: String, s: String? = null): Result {
    val initialTaskId = TaskId(id)
    val narrowedInstruction = s?.let<String, Raw<Instruction>> { parseInput(it) }
    return if (defaultAutoExec) {
      doTaskAndAutoExec(initialTaskId, narrowedInstruction)
    } else {
      doTaskOnly(initialTaskId, narrowedInstruction)
    }
  }

  fun doTaskAndAutoExec(
      initialTaskId: TaskId,
      narrowedInstruction: Raw<Instruction>? = null,
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
      narrowedInstruction: Raw<Instruction>? = null,
  ) = agent.doTask(taskId, narrowedInstruction?.let { prep(it) })

  // OTHER

  fun dropTask(id: String) = agent.removeTask(TaskId(id))

  fun rollBack(ordinal: Int) = game.rollBack(ordinal)

  // TODO somehow do this with Type not Expression?
  // TODO Let game take care of this itself?
  fun <P : PetElement> prep(node: Raw<P>): P {
    val xers = game.loader.transformers
    return transformInSeries(
        xers.useFullNames(),
        xers.atomizer(),
            xers.insertDefaults(THIS.expr), // TODO: context??
            xers.deprodify(),
            // not needed: ReplaceThisWith, ReplaceOwnerWith, FixEffectForUnownedContext
        )
        .transform(node.unprocessed) // TODO
  }

  fun agent(actor: ClassName) = agent(Actor(actor))
  fun agent(actor: Actor) = game.agent(actor)
}
