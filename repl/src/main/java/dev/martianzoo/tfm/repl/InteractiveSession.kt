package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.Exceptions.UserException
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.data.Actor
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.Component
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.EventLog.Checkpoint
import dev.martianzoo.tfm.engine.Game
import dev.martianzoo.tfm.engine.PlayerAgent
import dev.martianzoo.tfm.engine.Result
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.HasClassName
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.split
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.tfm.types.Transformers.AtomizeGlobalParameters
import dev.martianzoo.tfm.types.Transformers.Deprodify
import dev.martianzoo.tfm.types.Transformers.InsertDefaults
import dev.martianzoo.tfm.types.Transformers.UseFullNames
import dev.martianzoo.tfm.types.Transformers.transformInSeries
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Hierarchical.Companion.lub
import dev.martianzoo.util.Multiset

/**
 * A convenient interface for functional tests; basically, [ReplSession] is just a more texty
 * version of this.
 */
class InteractiveSession(initialSetup: GameSetup) {
  public var game: Game = Engine.newGame(initialSetup)
    internal set
  internal var gameNumber: Int = 0
    private set
  internal var showLogSince: Checkpoint = game.eventLog.checkpoint()
    private set
  internal var agent: PlayerAgent = agent(Actor.ENGINE)
    private set

  fun newGame(setup: GameSetup) {
    game = Engine.newGame(setup)
    showLogSince = game.eventLog.checkpoint()
    gameNumber++
    become(Actor.ENGINE)
  }

  fun become(actor: Actor) {
    agent = agent(actor)
  }

  // QUERIES

  fun count(metric: Metric) = agent.count(prep(metric))

  fun list(expression: Expression): Multiset<Expression> { // TODO y not (M)Type?
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

  fun has(requirement: Requirement) = agent.evaluate(prep(requirement))

  // EXECUTION

  fun sneakyChange(instruction: Instruction): Result {
    val changes =
        split(prep(instruction)).mapNotNull {
          if (it !is Change) throw UserException("can only sneak simple changes")
          val count = it.count
          require(count is ActualScalar)
          agent.quietChange(count.value, it.gaining, it.removing)
        }
    return Result(changes = changes, newTaskIdsAdded = setOf())
  }

  fun initiateOnly(instruction: Instruction) = agent.initiate(prep(instruction))

  fun initiateAndAutoExec(instruction: Instruction): Result {
    val checkpoint = game.eventLog.checkpoint()
    for (instr in split(prep(instruction))) {
      val tasks = ArrayDeque<TaskId>()
      tasks += agent.initiate(instr).newTaskIdsAdded
      while (tasks.any()) {
        val task = tasks.removeFirst()
        tasks += doTaskAndAutoExec(task).newTaskIdsAdded
      }
    }
    return game.eventLog.resultsSince(checkpoint)
  }

  fun doTaskAndAutoExec(
      initialTaskId: TaskId,
      narrowedInstruction: Instruction? = null,
  ): Result {
    val taskIdsToAutoExec: ArrayDeque<TaskId> = ArrayDeque()
    val checkpoint = game.eventLog.checkpoint()

    val firstResult: Result = agent.doTask(initialTaskId, prep(narrowedInstruction))
    taskIdsToAutoExec += firstResult.newTaskIdsAdded - initialTaskId

    while (taskIdsToAutoExec.any()) {
      val thisTaskId: TaskId = taskIdsToAutoExec.removeFirst()
      val results: Result = agent.doTask(thisTaskId)
      taskIdsToAutoExec += results.newTaskIdsAdded - thisTaskId // TODO better
    }

    return game.eventLog.resultsSince(checkpoint)
  }

  // OTHER

  fun rollBack(ordinal: Int) = game.rollBack(Checkpoint(ordinal))

  // TODO somehow do this with Type not Expression?
  // TODO Let game take care of this itself?
  fun <P : PetNode?> prep(node: P): P {
    if (node == null) return node
    val loader = game.loader
    return transformInSeries(
            UseFullNames(loader),
            AtomizeGlobalParameters(loader),
            InsertDefaults(loader),
            Deprodify(loader),
            // not needed: ReplaceThisWith, ReplaceOwnerWith, FixEffectForUnownedContext
        )
        .transform(node)
  }

  fun agent(player: String) = agent(cn(player))
  fun agent(actor: ClassName) = agent(Actor(actor))
  fun agent(actor: HasClassName) = agent(actor.className)
  fun agent(actor: Actor) = game.agent(actor)

  fun <T> doAs(actor: Actor, function: () -> T): T {
    val current = agent
    become(actor)
    return function().also { agent = current }
  }
}
