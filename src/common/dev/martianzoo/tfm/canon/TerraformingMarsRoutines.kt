package dev.martianzoo.tfm.canon

import dev.martianzoo.engine.Agent.Companion.parse
import dev.martianzoo.engine.Agent.OperationBody
import dev.martianzoo.engine.BodyLambda
import dev.martianzoo.engine.Routine
import dev.martianzoo.engine.RoutineContext
import dev.martianzoo.engine.RoutineException
import dev.martianzoo.pets.Transforming.bindXTo
import dev.martianzoo.pets.api.Exceptions.AbstractException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.ScaledExpression.Scalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.data.Task
import dev.martianzoo.pets.data.TaskResult
import dev.martianzoo.tfm.engine.TfmApiUtils.standardResourceNames
import dev.martianzoo.tfm.engine.TfmGameplay

internal val terraformingMarsRoutines: Map<String, Routine> =
    mapOf(
        "tasks" to routine(TerraformingMarsRoutineExecutor::tasks),
        "playCard" to routine(TerraformingMarsRoutineExecutor::playCard),
        "useAction" to routine(TerraformingMarsRoutineExecutor::useAction),
        "buyCards" to routine(TerraformingMarsRoutineExecutor::buyCards),
        "endTurn" to routine(TerraformingMarsRoutineExecutor::endTurn),
        "assignWildTag" to routine(TerraformingMarsRoutineExecutor::assignWildTag),
    )

private fun routine(body: TerraformingMarsRoutineExecutor.(List<String>) -> TaskResult): Routine =
    Routine { context, arguments ->
      TerraformingMarsRoutineExecutor(context).body(arguments)
    }

/** Kotlin implementations of the Routines contributed by the core Terraforming Mars bundle. */
internal class TerraformingMarsRoutineExecutor(private val context: RoutineContext) {
  private val game = context.game
  private val agent = context.agent

  internal fun tasks(arguments: List<String>): TaskResult {
    if (arguments.isEmpty()) throw RoutineException("tasks requires instructions")
    if (arguments == listOf("Pass")) return tfm().pass()
    if (arguments == listOf("select")) {
      val task =
          game.tasks.extract { it }.filter { it.assignee == agent.actor }.singleOrNull()
              ?: throw RoutineException("this requires exactly one pending task")
      return agent.selectTask(task.id)
    }
    val numberedSelect = arguments.singleOrNull()?.let(Regex("^([1-9]\\d*) select$")::matchEntire)
    if (numberedSelect != null) {
      val taskNumber = numberedSelect.groupValues[1].toInt()
      val task =
          game.tasks.extract { it }.filter { it.assignee == agent.actor }.getOrNull(taskNumber - 1)
              ?: throw RoutineException("there is no task $taskNumber")
      return agent.selectTask(task.id)
    }
    return executeRoutine { executeChoices(ArrayDeque(arguments)) }
  }

  // These helpers use an OperationBody receiver because task selection exists only inside the
  // active operation; keeping the receiver preserves one task queue and transaction across stages.
  private fun OperationBody.executeChoices(choices: ArrayDeque<String>) {
    while (choices.isNotEmpty()) {
      executeChoice(choices.removeFirst())
      executeSelectedConcreteTasks()
    }
  }

  private fun OperationBody.executeSelectedConcreteTasks() {
    while (true) {
      val selected =
          tasks
              .extract { it }
              .singleOrNull { task ->
                task.selected && task.assignee == agent.actor
              } ?: break
      agent.selectTask(selected.id)
      val stillSelected = tasks.extract { it }.singleOrNull { it.selected }
      if (stillSelected == selected) return
    }
    val tradeBarrierCleanup =
        tasks
            .extract { it }
            .singleOrNull { task ->
              task.assignee == agent.actor &&
                  task.instruction.toString().removeSuffix("!") == "-TradeBarrier"
            }
    if (tradeBarrierCleanup != null) agent.selectTask(tradeBarrierCleanup.id)
  }

  private fun OperationBody.executeChoice(source: String) {
    try {
      executeCurrentChoice(source)
    } catch (failure: TaskException) {
      if (selectDirectTaskContaining(source)) return
      if (!advanceFixedPrefixToward(source)) throw failure
      executeCurrentChoice(source)
    } catch (failure: NarrowingException) {
      if (selectDirectTaskContaining(source)) return
      if (!advanceFixedPrefixToward(source)) throw failure
      executeCurrentChoice(source)
    }
  }

  private fun OperationBody.selectDirectTaskContaining(source: String): Boolean {
    val normalized = agent.parse<InstructionTree>(source).toString()
    val names = Regex("[A-Z][A-Za-z0-9_]*").findAll(normalized).map { it.value }.toSet()
    val task =
        tasks
            .extract { it }
            .filter { candidate ->
              candidate.assignee == agent.actor &&
                  names.isNotEmpty() &&
                  names.all { it in candidate.instruction.toString() }
            }
            .singleOrNull() ?: return false
    agent.selectTask(task.id)
    return true
  }

  private fun OperationBody.executeCurrentChoice(source: String) {
    if (Regex("^[1-9]\\d*\\s+.+$").matches(source)) {
      doTaskLikeRepl(source)
      return
    }
    val lowered = InstructionGroup.of(agent.parse<InstructionTree>(source)).instructions
    if (lowered.size == 1) {
      doTaskLikeRepl(source)
    } else {
      lowered.forEach { instruction ->
        doTaskLikeRepl(withoutDefaultIntensity(instruction).toString())
      }
    }
  }

  /** Opens a fixed task prefix whose authored continuation contains the requested later choice. */
  private fun OperationBody.advanceFixedPrefixToward(source: String): Boolean {
    val names = Regex("[A-Z][A-Za-z0-9_]*").findAll(source).map { it.value }.toSet()
    val prefix =
        tasks
            .extract { it }
            .filter { task ->
              task.assignee == agent.actor &&
                  task.then?.toString()?.let { continuation ->
                    names.isNotEmpty() && names.all { it in continuation }
                  } == true
            }
            .singleOrNull() ?: return false
    agent.selectTask(prefix.id)
    while (true) {
      val fixed =
          tasks
              .extract { it }
              .filter { task ->
                task.assignee == agent.actor &&
                    !task.instruction.isAbstract(reader) &&
                    agent.canSelectTask(task.id)
              }
      if (fixed.size != 1) return true
      agent.selectTask(fixed.single().id)
    }
  }

  private fun OperationBody.doTaskLikeRepl(source: String) {
    val numbered = Regex("^([1-9]\\d*)\\s+(.+)$").matchEntire(source)
    if (numbered == null) {
      doTask(source)
      return
    }
    val taskNumber = numbered.groupValues[1].toInt()
    val request = numbered.groupValues[2]
    if (request == "Ok") {
      doTask(request, taskNumber)
      return
    }
    try {
      doTask(source)
    } catch (_: TaskException) {
      doTask(request, taskNumber)
    } catch (_: NarrowingException) {
      doTask(request, taskNumber)
    }
  }

  private fun withoutDefaultIntensity(instruction: Instruction): Instruction =
      when (instruction) {
        is Gain -> Gain.gain(instruction.scaledEx, intensity = null)
        is Remove -> Remove.remove(instruction.scaledEx, intensity = null)
        is Transmute -> instruction.copy(intensity = null)
        else -> instruction
      }

  internal fun playCard(arguments: List<String>): TaskResult {
    if (arguments.isEmpty()) throw RoutineException("playCard requires a card")
    val cardName = canonicalName(arguments.first())
    val card = game.reader.tfmCatalog.card(cardName)
    val deck =
        cardBack(card)?.className ?: throw RoutineException("$cardName is not a playable card")

    return executeRoutine {
      val choices = ArrayDeque(arguments.drop(1))
      val selectingOffer = openSelectingOffer(deck)
      if (tasks.matching { "StandardAction" in it.instruction.toString() }.any()) {
        doTask("UseAction<PlayCardSA, First>")
      }
      doTask("PlayCard<Class<$deck>, Class<$cardName>>")
      settleBilling(choices)
      requireNoUnusedCosts("playCard", choices)
      finishPlayedCard(cardName)
      if (selectingOffer) doTask("-Selecting")
    }
  }

  private fun OperationBody.finishPlayedCard(cardName: ClassName) {
    val transferPending =
        tasks
            .extract { it }
            .any { task ->
              task.instruction.descendantsOfType<Change>().any { change ->
                change.gaining?.className == cardName &&
                    change.removing?.let { removing ->
                      removing.className == cn("ProjectCard") &&
                          cn("Hand") in removing.descendantsOfType<ClassName>()
                    } == true
              }
            }
    if (transferPending) doTask("$cardName FROM ProjectCard<Hand>")
  }

  private fun OperationBody.openSelectingOffer(deck: ClassName): Boolean {
    val offerExists =
        tasks
            .extract { it }
            .any { task ->
              val gains =
                  (task.instruction.descendantsOfType<Change>() +
                          task.then?.descendantsOfType<Change>().orEmpty())
                      .mapNotNull(Change::gaining)
              gains.any { gain ->
                gain.className == deck && cn("Selecting") in gain.descendantsOfType<ClassName>()
              } && gains.any { it.className == cn("PlayCard") }
            }
    if (!offerExists) return false

    doTask("Selecting")
    val offered =
        tasks
            .extract { it.instruction }
            .filterIsInstance<Gain>()
            .single { gain ->
              gain.gaining.className == deck &&
                  cn("Selecting") in gain.gaining.descendantsOfType<ClassName>()
            }
    doTask(offered.toString())

    val retained =
        tasks
            .extract { it.instruction }
            .filterIsInstance<Transmute>()
            .single { transfer ->
              transfer.gaining.className == deck &&
                  cn("Hand") in transfer.gaining.descendantsOfType<ClassName>() &&
                  transfer.removing.className == deck &&
                  cn("Selecting") in transfer.removing.descendantsOfType<ClassName>()
            }
    doTask(retained.toString())
    return true
  }

  internal fun useAction(arguments: List<String>): TaskResult {
    if (arguments.size < 2) {
      throw RoutineException("useAction requires an action number and provider")
    }
    val actionNumber = arguments[0].toIntOrNull() ?: throw RoutineException("Invalid action number")
    val selector =
        listOf("First", "Second", "Third").getOrNull(actionNumber - 1)
            ?: throw RoutineException("Action number must be 1, 2, or 3")
    val provider = canonicalName(arguments[1])
    val providerIsCard = game.reader.tfmCatalog.cards.any { it.className == provider }

    return executeRoutine {
      val choices = ArrayDeque(arguments.drop(2))
      if (providerIsCard) {
        if (tasks.matching { "StandardAction" in it.instruction.toString() }.any()) {
          doTask("UseAction<UseCardActionSA, First>")
        }
        doTask("ActionUsedMarker<$provider>")
      }
      doTask("UseAction<$provider, $selector>")
      prepareVariableBilling(choices)
      settleBilling(choices)
      if (provider == cn("HandleMandates")) advanceMandateScaffolding()
      consumeLinkedActionCost(choices)
      requireNoUnusedCosts("useAction", choices)
    }
  }

  private fun OperationBody.consumeLinkedActionCost(choices: ArrayDeque<String>) {
    val source = choices.firstOrNull() ?: return
    val instruction =
        runCatching {
              InstructionGroup.of(agent.parse<InstructionTree>(source)).instructions.singleOrNull()
            }
            .getOrNull()
            ?.let(::withoutDefaultIntensity) as? Change ?: return
    if (instruction is Gain) return

    val names = instruction.descendantsOfType<ClassName>().map(ClassName::toString).toSet()
    val matchingTasks =
        tasks
            .extract { it }
            .filter { task ->
              task.assignee == agent.actor &&
                  task.then != null &&
                  (runCatching { instruction.narrows(task.instruction, reader) }
                      .getOrDefault(false) || names.all { it in task.instruction.toString() })
            }
    if (matchingTasks.size != 1) return

    choices.removeFirst()
    executeChoice(source)
    executeSelectedConcreteTasks()
  }

  private fun requireNoUnusedCosts(routine: String, choices: ArrayDeque<String>) {
    if (choices.isNotEmpty()) {
      throw RoutineException(
          "$routine accepts only cost arguments; use tasks(...) for consequences: " +
              choices.joinToString()
      )
    }
  }

  private fun OperationBody.prepareVariableBilling(choices: ArrayDeque<String>) {
    val source = choices.firstOrNull() ?: return
    val payment = runCatching { settlement(listOf(source)) }.getOrNull() ?: return
    val removals = payment.standardRemovals
    val variableTasks =
        tasks
            .extract { it }
            .filter { task ->
              task.instruction.descendantsOfType<Scalar>().any(Scalar::abstract)
            }
    variableTasks.forEach { task ->
      val maximumRecordedPayment =
          removals.maxOfOrNull { (it.count as ActualScalar).value } ?: return@forEach
      val choices =
          (1..maximumRecordedPayment).mapNotNull { x ->
            val bound = bindXTo(x).transformInstructionTree(task.instruction)
            val firstStage = if (bound is Then) bound.first else bound
            val owed =
                firstStage.descendantsOfType<Gain>().singleOrNull { gain ->
                  gain.gaining.className == cn("Owed")
                } ?: return@mapNotNull null
            val owedCount = (owed.count as? ActualScalar)?.value
            val matchesPayment = removals.any { removal ->
              removal.removing.className in owed.gaining.descendantsOfType<ClassName>() &&
                  (removal.count as ActualScalar).value == owedCount
            }
            if (matchesPayment) firstStage else null
          }
      val firstStage = choices.singleOrNull() ?: return@forEach
      doTask(firstStage.toString())
      val invoice =
          tasks
              .extract { it }
              .singleOrNull { pending ->
                pending.instruction.toString().startsWith("Invoice<")
              }
      if (invoice != null) doTask(invoice.instruction.toString())
    }
  }

  private fun OperationBody.settleBilling(choices: ArrayDeque<String>) {
    val billingPending =
        tfm().count("Owed") != 0 ||
            tasks
                .extract { it }
                .any { task ->
                  task.instruction.descendantsOfType<Change>().any { change ->
                    change.gaining?.className == cn("Owed")
                  }
                }
    if (billingPending) settle(Settlement(emptyMap(), emptyList(), emptyList()))

    while (tfm().count("Owed") != 0) {
      val source =
          choices.removeFirstOrNull()
              ?: throw RoutineException("Routine ended before Billing was settled")
      val payment =
          try {
            settlement(listOf(source))
          } catch (_: RoutineException) {
            throw RoutineException("Billing requires a payment choice before consequences: $source")
          }
      settle(payment)
    }
  }

  // TODO: Replace this bridge with mandate-owned completion in the game model.
  private fun OperationBody.advanceMandateScaffolding() {
    doTask("MandateSignal")
    val mandateClass = reader.catalog.classTable.getClass(cn("Mandate"))
    fun fromMandate(task: Task): Boolean =
        task.cause?.context?.let(reader::resolve)?.rootClass?.isSubtypeOf(mandateClass) == true

    val useAction =
        tasks
            .extract { it }
            .withIndex()
            .single { (_, task) ->
              fromMandate(task) &&
                  task.instruction.descendantsOfType<Gain>().any {
                    it.gaining.className == cn("UseAction")
                  }
            }
    doTask(useAction.value.instruction.toString(), useAction.index + 1)

    val removal =
        tasks
            .extract { it }
            .withIndex()
            .single { (_, task) ->
              fromMandate(task) && task.instruction.descendantsOfType<Remove>().isNotEmpty()
            }
    doTask(removal.value.instruction.toString(), removal.index + 1)

    while (true) {
      val cleanup =
          tasks
              .extract { it }
              .withIndex()
              .firstOrNull { (_, task) ->
                task.instruction.toString().startsWith("MAX 0 Mandate")
              } ?: return
      doTask("Ok", cleanup.index + 1)
    }
  }

  internal fun buyCards(arguments: List<String>): TaskResult {
    if (arguments.isNotEmpty()) throw RoutineException("buyCards takes no arguments")
    return tfm().buyCards()
  }

  internal fun endTurn(arguments: List<String>): TaskResult {
    if (arguments.isNotEmpty()) throw RoutineException("endTurn takes no arguments")
    return tfm().declineSecondAction()
  }

  private fun executeRoutine(body: BodyLambda): TaskResult =
      try {
        agent.finish {
          body()
          settleFinishedWildTagUses()
        }
      } catch (_: AbstractException) {
        agent.continueManual {
          body()
          settleFinishedWildTagUses()
        }
      } catch (_: TaskException) {
        agent.continueManual {
          body()
          settleFinishedWildTagUses()
        }
      }

  // TODO: Replace this bridge with action-scoped completion in the game model.
  private fun OperationBody.settleFinishedWildTagUses() {
    val uses = reader.getComponents("WildTagUse<${agent.actor}>")
    if (uses.isEmpty()) return
    val actorTasks =
        tasks.extract { it }.withIndex().filter { (_, task) -> task.assignee == agent.actor }
    if (actorTasks.any { (_, task) -> task.cause?.context?.className != cn("WildTagUse") }) return
    actorTasks.reversed().forEach { task ->
      doTask("Ok", task.index + 1)
    }
    val removals = uses.elements.joinToString(", ") { "-${it.expression}" }
    agent.manual(removals)
  }

  internal fun assignWildTag(arguments: List<String>): TaskResult {
    if (arguments.size != 1) throw RoutineException("assignWildTag requires one tag")
    val tag = canonicalName(arguments.single())
    return executeRoutine {
      val use =
          tasks
              .extract { it }
              .asSequence()
              .flatMap { it.instruction.descendantsOfType<Expression>().asSequence() }
              .firstOrNull { it.className == cn("WildTagUse") }
              ?: throw RoutineException("No pending wild-tag task")
      val card =
          use.arguments.lastOrNull()?.className ?: throw RoutineException("Invalid wild-tag task")
      doTask("$tag<WildTagUse<$card>>")
    }
  }

  private fun canonicalName(text: String): ClassName = game.vocabulary.canonicalName(cn(text))

  private fun settlement(arguments: List<String>): Settlement {
    if (arguments.isEmpty()) return Settlement(emptyMap(), emptyList(), emptyList())
    val standardResources = standardResourceNames(game.reader)
    val cardNames = game.reader.tfmCatalog.cards.map { it.className }.toSet()
    val instructions =
        InstructionGroup.of(agent.parse<InstructionTree>(arguments.joinToString(", "))).instructions
    val standardPayments = mutableMapOf<ClassName, Int>()
    val standardRemovals = mutableListOf<Remove>()
    val cardPayments = mutableListOf<String>()
    instructions.forEach { instruction ->
      val removal =
          instruction as? Remove
              ?: throw RoutineException("Settlement arguments must remove payment resources")
      val count =
          (removal.count as? ActualScalar)?.value
              ?: throw RoutineException("Settlement amounts must be concrete")
      val resource = removal.removing.className
      if (resource in standardResources) {
        standardPayments[resource] = standardPayments.getOrElse(resource) { 0 } + count
        standardRemovals += removal
      } else {
        val card =
            removal.removing.descendantsOfType<ClassName>().firstOrNull { it in cardNames }
                ?: throw RoutineException("$resource is not a supported payment resource")
        val prefix = if (count == 1) "" else "$count "
        cardPayments += prefix + "PayFromCard<$card> FROM ${removal.removing}"
      }
    }
    return Settlement(standardPayments, standardRemovals, cardPayments)
  }

  private fun OperationBody.settle(settlement: Settlement) {
    if (settlement.cardPayments.isNotEmpty()) tfm().pay()
    settlement.cardPayments.forEach(::doTask)
    with(settlement.standardPayments) {
      tfm()
          .pay(
              mc = get(cn("MC")) ?: 0,
              steel = get(cn("Steel")) ?: 0,
              titanium = get(cn("Titanium")) ?: 0,
              plant = get(cn("Plant")) ?: 0,
              energy = get(cn("Energy")) ?: 0,
              heat = get(cn("Heat")) ?: 0,
          )
    }
  }

  private data class Settlement(
      val standardPayments: Map<ClassName, Int>,
      val standardRemovals: List<Remove>,
      val cardPayments: List<String>,
  )

  private fun tfm(): TfmGameplay = TfmGameplay(game, agent.actor, agent)
}
