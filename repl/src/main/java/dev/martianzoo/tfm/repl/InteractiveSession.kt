package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.SpecialClassNames.ANYONE
import dev.martianzoo.tfm.data.Actor
import dev.martianzoo.tfm.data.Actor.Companion.ENGINE
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.Component
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.EventLog.Checkpoint
import dev.martianzoo.tfm.engine.Game
import dev.martianzoo.tfm.engine.SingleExecution.ExecutionResult
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.tfm.types.Transformers.AtomizeGlobalParameterGains
import dev.martianzoo.tfm.types.Transformers.CompositeTransformer
import dev.martianzoo.tfm.types.Transformers.Deprodify
import dev.martianzoo.tfm.types.Transformers.InsertDefaults
import dev.martianzoo.tfm.types.Transformers.ReplaceOwnerWith
import dev.martianzoo.tfm.types.Transformers.UseFullNames
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Hierarchical.Companion.lub
import dev.martianzoo.util.Multiset

/**
 * A convenient interface for functional tests; basically, [ReplSession] is just a more texty
 * version of this.
 */
class InteractiveSession(initialGame: GameSetup) {
  public lateinit var game: Game
    internal set
  internal var gameNumber: Int = -1 // TODO
  internal var defaultPlayer: ClassName? = null
  internal var actor: Actor = ENGINE
  internal var start: Checkpoint = Checkpoint(0) // will be overwritten

  init {
    newGame(initialGame)
  }

  fun newGame(setup: GameSetup) {
    game = Engine.newGame(setup)
    start = game.eventLog.checkpoint()
    gameNumber++
    becomeNoOne()
  }

  fun becomePlayer(player: ClassName) {
    val p = game.resolve(player.expr)
    require(!p.abstract)
    require(p.isSubtypeOf(game.resolve(ANYONE.expr)))
    defaultPlayer = p.mclass.className
    actor = Actor(p.mclass.className)
  }

  fun becomeNoOne() {
    defaultPlayer = null
    actor = ENGINE
  }

  // QUERIES

  fun count(metric: Metric) = game.count(prep(metric))

  fun list(expression: Expression): Multiset<Expression> { // TODO y not (M)Type?
    val typeToList: MType = game.resolve(prep(expression))
    val allComponents: Multiset<Component> = game.getComponents(typeToList)

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

  fun has(requirement: Requirement) = game.evaluate(prep(requirement))

  // EXECUTION

  fun doIgnoringEffects(instruction: Instruction) =
      game.executeWithoutEffects(prep(instruction), actor)

  fun initiateAndQueue(instruction: Instruction): ExecutionResult {
    return game.initiate(prep(instruction), actor, fakeCause = null)
  }

  fun initiateAndAutoExec(
      instruction: Instruction,
      requireFullSuccess: Boolean = true,
  ): ExecutionResult {
    val checkpoint = game.eventLog.checkpoint()
    val instrs = Instruction.split(prep(instruction))
    var success = true
    for (instr in instrs) {
      val result: ExecutionResult = game.initiate(instr, actor, fakeCause = null)
      success = success &&
          result.newTaskIdsAdded.all {
            doTaskAndAutoExec(it, requireFullSuccess = requireFullSuccess).fullSuccess
          }
    }
    return game.eventLog.resultsSince(checkpoint, success)
  }

  fun doTaskOnly(taskId: TaskId, narrowedInstruction: Instruction? = null) =
      game.tryOneExistingTask(taskId, actor, prep(narrowedInstruction))

  fun doTaskAndAutoExec(
      initialTaskId: TaskId,
      narrowedInstruction: Instruction? = null,
      requireFullSuccess: Boolean = false,
  ): ExecutionResult {
    val taskIdsToAutoExec: ArrayDeque<TaskId> = ArrayDeque()
    val checkpoint = game.eventLog.checkpoint()
    var success = true
    val narrowed = prep(narrowedInstruction)

    fun doTask(initialTaskId: TaskId, instr: Instruction? = null) =
        if (requireFullSuccess) {
          game.doOneExistingTask(initialTaskId, actor, instr)
        } else {
          game.tryOneExistingTask(initialTaskId, actor, instr).also {
            success = success && it.fullSuccess
          }
        }

    val firstResult: ExecutionResult = doTask(initialTaskId, narrowed)
    taskIdsToAutoExec += firstResult.newTaskIdsAdded - initialTaskId

    while (taskIdsToAutoExec.any()) {
      val thisTaskId: TaskId = taskIdsToAutoExec.removeFirst()
      val results: ExecutionResult = doTask(thisTaskId)
      taskIdsToAutoExec += results.newTaskIdsAdded - thisTaskId // TODO better
    }

    return game.eventLog.resultsSince(checkpoint, success)
  }

  fun enqueueTasks(instruction: Instruction, taskOwner: Actor? = null) =
      game.enqueueTasks(prep(instruction), taskOwner ?: actor)

  // OTHER

  fun rollBack(ordinal: Int) = game.rollBack(Checkpoint(ordinal))

  // TODO somehow do this with Type not Expression?
  // TODO Let game take care of this itself?
  fun <P : PetNode?> prep(node: P): P {
    if (node == null) return node
    val loader = game.loader
    return CompositeTransformer(
            UseFullNames(loader),
            AtomizeGlobalParameterGains(loader),
            InsertDefaults(loader),
            ReplaceOwnerWith(defaultPlayer),
            Deprodify(loader),
            // not needed: ReplaceThisWith, FixEffectForUnownedContext
        )
        .transform(node)
  }
}
