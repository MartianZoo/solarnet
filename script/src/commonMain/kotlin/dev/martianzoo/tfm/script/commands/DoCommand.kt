package dev.martianzoo.tfm.script.commands

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.engine.BodyLambda
import dev.martianzoo.engine.Gameplay.Companion.parse
import dev.martianzoo.engine.Gameplay.OperationBody
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
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.script.ScriptSession.ScriptMode.PURPLE
import dev.martianzoo.script.ScriptSession.UsageException
import dev.martianzoo.tfm.canon.ApiUtils.standardResourceNames
import dev.martianzoo.tfm.canon.tfmCatalog
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.script.RoutineCall

/** Temporary REPL entry point for machine-authored Terraforming Mars Routine calls. */
internal class DoCommand(private val repl: ScriptSession) : ScriptCommand("do") {
  override val usage: String = "do <RoutineCall>"
  override val help: String =
      """
        Executes one machine-authored Routine call in purple mode. The initial prototype supports
        `tasks(...)`, `playCard(...)`, `useAction(...)`, `buyCards()`, `endTurn()`, and
        `assignWildTag(...)`.
        `DO` disables player-task autoexecution for the current Actor; Engine-owned workflow
        remains active.
      """

  override fun withArgs(args: String): List<String> {
    if (repl.mode != PURPLE) throw UsageException("DO requires purple mode")
    val call = RoutineCall.parse(args)
    repl.setAutoExecMode(NONE)

    val result =
        when (call.name) {
          "tasks" -> tasks(call)
          "playCard" -> playCard(call)
          "useAction" -> useAction(call)
          "buyCards" -> buyCards(call)
          "endTurn" -> endTurn(call)
          "assignWildTag" -> assignWildTag(call)
          else -> throw UsageException("Unknown Routine: ${call.name}")
        }
    return repl.describeExecutionResults(result)
  }

  private fun tasks(call: RoutineCall): TaskResult {
    if (call.arguments.isEmpty()) throw UsageException("tasks requires instructions")
    if (call.arguments == listOf("select")) {
      return repl.gameplay.selectTask(repl.onlyTask().id)
    }
    val numberedSelect =
        call.arguments.singleOrNull()?.let(Regex("^([1-9]\\d*) select$")::matchEntire)
    if (numberedSelect != null) {
      val taskNumber = numberedSelect.groupValues[1].toInt()
      val task =
          repl.game.tasks
              .extract { it }
              .filter { it.assignee == repl.gameplay.actor }
              .getOrNull(taskNumber - 1) ?: throw UsageException("there is no task $taskNumber")
      return repl.gameplay.selectTask(task.id)
    }
    return executeRoutine { executeChoices(ArrayDeque(call.arguments)) }
  }

  // These helpers use an OperationBody receiver because task selection exists only inside the
  // active operation; keeping the receiver preserves one task queue and transaction across stages.
  private fun OperationBody.executeChoices(choices: ArrayDeque<String>) {
    while (choices.isNotEmpty()) executeChoice(choices.removeFirst())
  }

  private fun OperationBody.executeChoice(source: String) {
    if (Regex("^[1-9]\\d*\\s+.+$").matches(source)) {
      doTaskLikeRepl(source)
      return
    }
    val lowered = InstructionGroup.of(repl.gameplay.parse<InstructionTree>(source)).instructions
    if (lowered.size == 1) {
      doTaskLikeRepl(source)
    } else {
      lowered.forEach { instruction ->
        doTaskLikeRepl(withoutDefaultIntensity(instruction).toString())
      }
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

  private fun playCard(call: RoutineCall): TaskResult {
    if (call.arguments.isEmpty()) throw UsageException("playCard requires a card")
    val cardName = canonicalName(call.arguments.first())
    val card = repl.game.reader.tfmCatalog.card(cardName)
    val deck = card.deck?.className ?: throw UsageException("$cardName is not a playable card")

    return executeRoutine {
      val choices = ArrayDeque(call.arguments.drop(1))
      val selectingOffer = openSelectingOffer(deck)
      if (tasks.matching { "StandardAction" in it.instruction.toString() }.any()) {
        doTask("UseAction<PlayCardSA, First>")
      }
      doTask("PlayCard<Class<$deck>, Class<$cardName>>")
      settleBilling(choices)
      finishPlayedCard(cardName)
      executeChoices(choices)
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

  private fun useAction(call: RoutineCall): TaskResult {
    if (call.arguments.size < 2) {
      throw UsageException("useAction requires an action number and provider")
    }
    val actionNumber =
        call.arguments[0].toIntOrNull() ?: throw UsageException("Invalid action number")
    val selector =
        listOf("First", "Second", "Third").getOrNull(actionNumber - 1)
            ?: throw UsageException("Action number must be 1, 2, or 3")
    val provider = canonicalName(call.arguments[1])
    val providerIsCard =
        repl.game.reader.tfmCatalog.cardDefinitions.any { it.className == provider }

    return executeRoutine {
      val choices = ArrayDeque(call.arguments.drop(2))
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
      executeChoices(choices)
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
              ?: throw UsageException("Routine ended before Billing was settled")
      val payment =
          try {
            settlement(listOf(source))
          } catch (_: UsageException) {
            throw UsageException("Billing requires a payment choice before consequences: $source")
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

  private fun buyCards(call: RoutineCall): TaskResult {
    if (call.arguments.isNotEmpty()) throw UsageException("buyCards takes no arguments")
    return tfm().buyCards()
  }

  private fun endTurn(call: RoutineCall): TaskResult {
    if (call.arguments.isNotEmpty()) throw UsageException("endTurn takes no arguments")
    return tfm().declineSecondAction()
  }

  private fun executeRoutine(body: BodyLambda): TaskResult =
      try {
        repl.gameplay.godMode().finish {
          body()
          settleFinishedWildTagUses()
        }
      } catch (_: AbstractException) {
        repl.gameplay.godMode().continueManual {
          body()
          settleFinishedWildTagUses()
        }
      } catch (_: TaskException) {
        repl.gameplay.godMode().continueManual {
          body()
          settleFinishedWildTagUses()
        }
      }

  // TODO: Replace this bridge with action-scoped completion in the game model.
  private fun OperationBody.settleFinishedWildTagUses() {
    val uses = reader.getComponents("WildTagUse<${repl.gameplay.actor}>")
    if (uses.isEmpty()) return
    val actorTasks =
        tasks
            .extract { it }
            .withIndex()
            .filter { (_, task) -> task.assignee == repl.gameplay.actor }
    if (actorTasks.any { (_, task) -> task.cause?.context?.className != cn("WildTagUse") }) return
    actorTasks.reversed().forEach { task ->
      doTask("Ok", task.index + 1)
    }
    val removals = uses.elements.joinToString(", ") { "-${it.expression}" }
    repl.gameplay.godMode().manual(removals)
  }

  private fun assignWildTag(call: RoutineCall): TaskResult {
    if (call.arguments.size != 1) throw UsageException("assignWildTag requires one tag")
    val tag = canonicalName(call.arguments.single())
    return executeRoutine {
      val use =
          tasks
              .extract { it }
              .asSequence()
              .flatMap { it.instruction.descendantsOfType<Expression>().asSequence() }
              .firstOrNull { it.className == cn("WildTagUse") }
              ?: throw UsageException("No pending wild-tag task")
      val card =
          use.arguments.lastOrNull()?.className ?: throw UsageException("Invalid wild-tag task")
      doTask("$tag<WildTagUse<$card>>")
    }
  }

  private fun canonicalName(text: String): ClassName = repl.game.vocabulary.canonicalName(cn(text))

  private fun settlement(arguments: List<String>): Settlement {
    if (arguments.isEmpty()) return Settlement(emptyMap(), emptyList(), emptyList())
    val standardResources = standardResourceNames(repl.game.reader)
    val cardNames = repl.game.reader.tfmCatalog.cardDefinitions.map { it.className }.toSet()
    val instructions =
        InstructionGroup.of(repl.gameplay.parse<InstructionTree>(arguments.joinToString(", ")))
            .instructions
    val standardPayments = mutableMapOf<ClassName, Int>()
    val standardRemovals = mutableListOf<Remove>()
    val cardPayments = mutableListOf<String>()
    instructions.forEach { instruction ->
      val removal =
          instruction as? Remove
              ?: throw UsageException("Settlement arguments must remove payment resources")
      val count =
          (removal.count as? ActualScalar)?.value
              ?: throw UsageException("Settlement amounts must be concrete")
      val resource = removal.removing.className
      if (resource in standardResources) {
        standardPayments[resource] = standardPayments.getOrElse(resource) { 0 } + count
        standardRemovals += removal
      } else {
        val card =
            removal.removing.descendantsOfType<ClassName>().firstOrNull { it in cardNames }
                ?: throw UsageException("$resource is not a supported payment resource")
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

  private fun tfm(): TfmGameplay = TfmGameplay(repl.game, repl.gameplay.actor)
}
