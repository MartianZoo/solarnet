package dev.martianzoo.tfm.engine

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.engine.AutoExecMode.SAFE
import dev.martianzoo.engine.BodyLambda
import dev.martianzoo.engine.Gameplay
import dev.martianzoo.engine.Gameplay.OperationBody
import dev.martianzoo.engine.Gameplay.TurnLayer
import dev.martianzoo.engine.TaskQueue
import dev.martianzoo.engine.World
import dev.martianzoo.pets.Transforming.bindXTo
import dev.martianzoo.pets.api.Exceptions.AbstractException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.ScaledExpression.Scalar
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.GameEvent.ChangeEvent
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.data.Task
import dev.martianzoo.pets.data.TaskResult
import dev.martianzoo.tfm.canon.ApiUtils.standardResourceNames
import dev.martianzoo.tfm.canon.TfmClasses.MC
import dev.martianzoo.tfm.canon.TfmClasses.STANDARD_RESOURCE_CLASSES

/**
 * Wraps and extends a [Gameplay] instance to provide much more convenient functions specific to
 * *Terraforming Mars*.
 */
public class TfmGameplay(
    private val game: World,
    override val actor: Actor,
    internal val gameplay: TurnLayer = game.gameplay(actor) as TurnLayer,
) : TurnLayer by gameplay {
  public val reader: GameReader by game::reader

  private var explicitPaymentChoicesRequired = false
  private var allowUnderpayment = false
  private var expectedOverpaymentWaste: Int? = null

  private fun asActor(actor: Actor) =
      TfmGameplay(game, actor).also {
        if (explicitPaymentChoicesRequired) it.requireExplicitPaymentChoices()
      }

  public fun asPlayer(player: Player): TfmGameplay = asActor(player)

  public fun nextGeneration(vararg cardsBought: Int) {
    phase("Production")
    asActor(ENGINE).godMode().manual("Generation")
    phase("Research") {
      for ((cards, player) in cardsBought.zip(game.actors.filterIsInstance<Player>())) {
        asPlayer(player).buyCards(cards)
      }
    }
    phase("Action")
  }

  public fun playCorp(cardName: ClassName, buyCards: Int, body: BodyLambda = {}): TaskResult {
    return inTurn {
      doTask("PlayCard<Class<CorporationCard>, Class<$cardName>>")
      buySelectedCards(buyCards)
      body()
    }
  }

  /** Buys the selected number of offered project cards and settles their M€ invoice. */
  public fun buyCards(count: Int): TaskResult =
      gameplay.godMode().continueManual { buySelectedCards(count) }

  private fun OperationBody.buySelectedCards(count: Int) {
    closeUnusedPaymentOffers()
    openPendingProjectCardOffer()
    val offered = this@TfmGameplay.count("ProjectCard<Selecting>")
    require(count in 0..offered) { "cannot buy $count of $offered selected project cards" }
    val discarded = offered - count
    val selectionTaskNumber =
        tasks
            .extract { it }
            .withIndex()
            .single { (_, task) ->
              val instruction = task.instruction.toString()
              "ProjectCard" in instruction && "Selecting" in instruction
            }
            .index + 1
    doTask(
        if (discarded == 0) "Ok" else "-$discarded ProjectCard<Selecting>",
        selectionTaskNumber,
    )
    if (hasPendingBuySelectedCards(tasks)) doTask("BuySelectedCards")
    if (count > 0) {
      while (hasPendingBuyCard(tasks)) doTask("BuyCard / ProjectCard<Selecting>")
      if (hasPendingBuyCardsInvoice(tasks)) doTask("Invoice<BuyCards, First>")
      payAllMc()
      completePurchasedCards()
    }
    if (this@TfmGameplay.count("Selecting") != 0) doTask("-Selecting")
    closeUnusedPaymentOffers()
  }

  private fun OperationBody.openPendingProjectCardOffer() {
    if (this@TfmGameplay.count("ProjectCard<Selecting>") != 0) return
    val offer =
        tasks
            .extract { it }
            .filter { task ->
              task.assignee == actor &&
                  task.instruction.descendantsOfType<Change>().any { change ->
                    change.gaining?.let { gaining ->
                      gaining.className == cn("ProjectCard") &&
                          cn("Selecting") in gaining.descendantsOfType<ClassName>()
                    } == true
                  }
            }
            .singleOrNull() ?: return
    executeTask(offer)
  }

  private fun OperationBody.closeUnusedPaymentOffers() {
    if (this@TfmGameplay.count("Owed") != 0) return
    while (true) {
      autoExecNow()
      val offer =
          tasks
              .extract { it }
              .withIndex()
              .firstOrNull { (_, task) -> task.cause?.context?.className == cn("Accept") } ?: return
      doTask("Ok", offer.index + 1)
    }
  }

  private fun hasPendingBuyCardsInvoice(tasks: TaskQueue): Boolean =
      tasks
          .extract { it }
          .any { task ->
            task.instruction.toString().let { it.startsWith("Invoice<") && "BuyCards" in it }
          }

  private fun hasPendingBuySelectedCards(tasks: TaskQueue): Boolean =
      tasks.extract { it }.any { it.instruction.toString().startsWith("BuySelectedCards") }

  private fun hasPendingBuyCard(tasks: TaskQueue): Boolean =
      tasks.extract { it }.any { it.instruction.toString().startsWith("BuyCard<") }

  private fun OperationBody.completePurchasedCards() {
    val transfer =
        tasks
            .extract { it }
            .filter { task ->
              task.instruction.descendantsOfType<Change>().any { change ->
                change.gaining?.let { gaining ->
                  gaining.className == cn("ProjectCard") &&
                      cn("Hand") in gaining.descendantsOfType<ClassName>()
                } == true &&
                    change.removing?.let { removing ->
                      removing.className == cn("ProjectCard") &&
                          cn("Selecting") in removing.descendantsOfType<ClassName>()
                    } == true
              }
            }
            .singleOrNull() ?: return
    executeTask(transfer)
  }

  public fun pass(): TaskResult = inTfmTurn { doTask("Pass") }

  /**
   * Performs the actions in one test-level turn, declining an unused second action when needed. If
   * every other player has passed, the workflow offers `NewTurn` rather than a second action; that
   * offer is deliberately left in place so this block can contain the rest of the generation.
   */
  // TODO: Contract temporary tfm-tests gameplay seams.
  public fun turn(body: TfmGameplay.() -> Unit) {
    body()
    if (secondActionOffer() != null) declineSecondAction()
  }

  public fun declineSecondAction(): TaskResult {
    return inTfmTurn {
      val secondAction =
          secondActionOffer()
              ?: throw TaskException("$actor is not waiting on exactly one second-action offer")
      doTask("Ok", secondAction.index + 1)
    }
  }

  private fun secondActionOffer(): IndexedValue<Task>? =
      game.tasks
          .extract { it }
          .filter { it.assignee == actor }
          .withIndex()
          .filter { (_, task) -> task.isActionPhaseSecondAction() }
          .singleOrNull()

  private fun Task.isActionPhaseSecondAction(): Boolean {
    val origin = cause ?: return false
    if (origin.context.className != cn("ActionPhase")) return false
    val trigger = game.events.entryAt(origin.triggerEvent) as? ChangeEvent
    return trigger?.change?.gaining?.className == cn("SecondAction")
  }

  public fun stdAction(
      stdAction: String,
      which: Int = 1,
      payment: BodyLambda = { payInvoiceFromItsResourceIfOffered() },
      body: BodyLambda = {},
  ): TaskResult {
    // TODO: Reject providers that are not StandardAction; generic HasActions need a distinct API.
    return inTfmTurn {
      doTask("UseAction<$stdAction, ${whichAction(which)}>")
      payment()
      body()
    }
  }

  public fun claimMilestone(milestone: ClassName): TaskResult =
      stdAction("ClaimMilestoneSA") { doTask("$milestone") }

  public fun fundAward(award: ClassName, amountPaid: Int): TaskResult {
    val which = count("Award") + 1
    return stdAction("FundAwardSA", which, payment = { pay(amountPaid) }) { doTask("$award") }
  }

  private fun OperationBody.payInvoiceFromItsResourceIfOffered() {
    val billingCause = openPendingBilling()
    val offeredResource = STANDARD_RESOURCE_CLASSES.singleOrNull { resource ->
      game.tasks
          .extract { it }
          .filter { it.assignee == actor }
          .flatMap { it.instruction.descendantsOfType<Change>() }
          .any { change ->
            change.gaining?.let { gaining ->
              gaining.className == cn("Pay") && resource in gaining.descendantsOfType<ClassName>()
            } == true
          }
    }
    if (offeredResource != null) {
      doTask("Pay<Class<$offeredResource>> FROM $offeredResource / Owed<Class<$offeredResource>>")
    }
    if (this@TfmGameplay.count("Owed") == 0) finishBilling(billingCause)
  }

  public fun convertPlants(body: BodyLambda = {}): TaskResult {
    return stdAction("ConvertPlantsSA", body = body)
  }

  public fun convertHeat(body: BodyLambda = {}): TaskResult {
    return stdAction("ConvertHeatSA", body = body)
  }

  public fun stdProject(
      stdProject: String,
      payment: BodyLambda = {
        payAllMc()
      },
      body: BodyLambda = {},
  ): TaskResult {
    return stdAction(stdProject, payment = payment, body = body)
  }

  public fun playPrelude(cardName: ClassName, body: BodyLambda = {}): TaskResult {
    return inTfmTurn {
      doTask("PlayCard<Class<PreludeCard>, Class<$cardName>>")
      body()
    }
  }

  // In the method after this, all the cost parameters are optional,
  // but you've gotta provide ONE of them.
  public fun playProject(unused1: ClassName, unused2: BodyLambda = {}): Nothing =
      error("you must specify some cost")

  public fun playProject(
      cardName: ClassName,
      mc: Int = 0,
      steel: Int = 0,
      titanium: Int = 0,
      payment: BodyLambda = { pay(mc, steel, titanium) },
      body: BodyLambda = {},
  ): TaskResult {
    return inTfmTurn { playProjectWithinOperation(cardName, payment, body) }
  }

  public fun OperationBody.playProject(
      cardName: ClassName,
      mc: Int = 0,
      steel: Int = 0,
      titanium: Int = 0,
      payment: BodyLambda = { pay(mc, steel, titanium) },
      body: BodyLambda = {},
  ) {
    playProjectWithinOperation(cardName, payment, body)
  }

  private fun OperationBody.playProjectWithinOperation(
      cardName: ClassName,
      payment: BodyLambda,
      body: BodyLambda,
  ) {
    if (tasks.matching { "${it.instruction}".contains("StandardAction") }.any()) {
      doTask("UseAction<PlayCardSA, First>")
    }
    doTask("PlayCard<Class<ProjectCard>, Class<$cardName>>")

    payment()
    body()
    if (this@TfmGameplay.count("Owed") == 0) {
      tasks
          .matching { it.cause?.context?.className in setOf(cn("Accept"), cn("AcceptFromCard")) }
          .forEach {
            selectTask(it)
            if (it in tasks) narrowTask("Ok")
          }
    }
    autoExecNow()
  }

  private fun inTfmTurn(body: BodyLambda): TaskResult {
    return inTurn {
      val preexistingTasks = game.tasks.extract { it }.associateBy { it.id }
      body()
      autoExecNow()
      val newPendingTasks =
          game.tasks
              .extract { it }
              .filter { task ->
                val previous = preexistingTasks[task.id]
                previous == null ||
                    previous.copy(selected = task.selected, whyPending = task.whyPending) != task
              }
      if (newPendingTasks.isNotEmpty()) {
        if (newPendingTasks.any { it.whyPending == "abstract" }) {
          throw AbstractException("pending abstract tasks:\n${newPendingTasks.joinToString("\n")}")
        }
        throw TaskException("pending tasks:\n${newPendingTasks.joinToString("\n")}")
      }
      declineWildTagOffers()
      removeWildTagUses()
    }
  }

  private fun declineWildTagOffers() {
    while (true) {
      val offer =
          game.tasks
              .extract { it }
              .filter { it.assignee == actor }
              .withIndex()
              .firstOrNull { (_, task) -> task.cause?.context?.className == cn("WildTagUse") }
              ?: return
      doTask("Ok", offer.index + 1)
    }
  }

  private fun removeWildTagUses() {
    val uses = reader.getComponents("WildTagUse<$actor>")
    if (uses.isEmpty()) return
    val removals = uses.elements.joinToString(", ") { "-${it.expression}" }
    godMode().manual(removals)
  }

  public fun pay(
      mc: Int = 0,
      steel: Int = 0,
      titanium: Int = 0,
      plant: Int = 0,
      energy: Int = 0,
      heat: Int = 0,
  ): TaskResult {
    val underpaymentAllowed = allowUnderpayment
    val expectedWaste = expectedOverpaymentWaste
    allowUnderpayment = false
    expectedOverpaymentWaste = null
    // Billing effects are queued; safely advance them until the payment choices are available.
    val previousAutoExecMode = autoExecMode
    if (autoExecMode != NONE) autoExecMode = SAFE

    return try {
      godMode().continueManual {
        val billingCause = openPendingBilling()
        var observedWaste = 0

        fun payNonMoneyResource(cost: Int, currency: String) {
          val accepted =
              tasks
                  .extract { it }
                  .any {
                    val context = it.cause?.context
                    context?.className == cn("Accept") && "Class<$currency>" in context.toString()
                  }
          if (!accepted) {
            if (cost > 0) doTask("$cost Pay<Class<$currency>> FROM $currency")
            return@payNonMoneyResource
          }

          val value = paymentValue(currency)
          val owed = count("Owed")
          val available = count(currency)
          val maximumFullValuePayment = minOf(available, owed / value)
          if (
              explicitPaymentChoicesRequired &&
                  cost < maximumFullValuePayment &&
                  !underpaymentAllowed
          ) {
            throw IllegalArgumentException(
                "$actor paid $cost $currency but could pay $maximumFullValuePayment at full value; " +
                    "call intentionalUnderpay() immediately before paying if this is sourced"
            )
          }
          val squanderedValue = (cost * value - owed).coerceAtLeast(0)
          if (explicitPaymentChoicesRequired && squanderedValue > 0) {
            if (expectedWaste == null) {
              throw IllegalArgumentException(
                  "$actor paid $cost $currency worth ${cost * value} against $owed owed; " +
                      "call intentionalOverpay($squanderedValue) immediately before paying if this is sourced"
              )
            }
            observedWaste += squanderedValue
          }
          if (cost > 0) doTask("$cost Pay<Class<$currency>> FROM $currency")
        }

        payNonMoneyResource(plant, "Plant")
        payNonMoneyResource(energy, "Energy")
        payNonMoneyResource(heat, "Heat")
        payNonMoneyResource(titanium, "Titanium")
        payNonMoneyResource(steel, "Steel")

        if (explicitPaymentChoicesRequired && expectedWaste != null) {
          require(expectedWaste == observedWaste) {
            "$actor declared $expectedWaste M€ of intentional overpayment but squandered $observedWaste M€"
          }
        }

        val owed = count("Owed")
        if (mc > owed) {
          throw LimitsException("Overpaying $mc MC when only $owed is owed")
        }
        if (mc > 0) {
          doTask("$mc Pay<Class<MC>> FROM MC")
        }

        if (count("Owed") == 0) {
          finishBilling(billingCause)
        }
      }
    } finally {
      autoExecMode = previousAutoExecMode
    }
  }

  private fun OperationBody.openPendingBilling(): Cause? {
    if (this@TfmGameplay.count("Owed") != 0) return null
    val billing =
        tasks
            .extract { it }
            .filter { task ->
              task.instruction.descendantsOfType<Change>().any { change ->
                change.gaining?.className == cn("Owed")
              }
            }
            .singleOrNull() ?: return null
    executeTask(billing)
    advanceSingleConcreteTask(billing.cause)
    return billing.cause
  }

  private fun OperationBody.payAllMc() {
    val billingCause = openPendingBilling()
    val owed = this@TfmGameplay.count("Owed")
    if (owed > 0) doTask("$owed Pay<Class<MC>> FROM MC")
    if (this@TfmGameplay.count("Owed") == 0) finishBilling(billingCause)
  }

  private fun OperationBody.finishBilling(billingCause: Cause?) {
    tasks
        .matching { it.cause?.context?.className == cn("Accept") }
        .forEach {
          selectTask(it)
          if (it in tasks) narrowTask("Ok")
        }
    autoExecNow()
    advanceSingleConcreteTask(billingCause)
  }

  private fun OperationBody.advanceSingleConcreteTask(cause: Cause?) {
    if (cause == null) return
    while (true) {
      val next =
          tasks
              .extract { it }
              .filter { task ->
                task.cause == cause &&
                    !task.instruction.isAbstract(reader) &&
                    this@TfmGameplay.canSelectTask(task.id)
              }
              .singleOrNull() ?: return
      executeTask(next)
    }
  }

  private fun executeTask(task: Task) {
    selectTask(task.id)
  }

  /** Allows the next [pay] call to leave usable accepted non-money resources unspent. */
  public fun intentionalUnderpay() {
    allowUnderpayment = true
  }

  /**
   * Allows the next [pay] call to squander exactly [monetaryValueSquandered] through overpayment.
   */
  public fun intentionalOverpay(monetaryValueSquandered: Int) {
    require(monetaryValueSquandered > 0) {
      "Intentional overpayment must squander a positive value"
    }
    expectedOverpaymentWaste = monetaryValueSquandered
  }

  public fun requireExplicitPaymentChoices(): TfmGameplay = apply {
    explicitPaymentChoicesRequired = true
  }

  private fun paymentValue(currency: String): Int {
    val checkpoint = game.timeline.checkpoint()
    return try {
      godMode().sneak("100 Owed<>, $currency")
      val owed = count("Owed")
      doTask("Pay<Class<$currency>> FROM $currency")
      owed - count("Owed")
    } finally {
      game.timeline.rollBack(checkpoint)
    }
  }

  public fun cardAction1(cardName: ClassName, body: BodyLambda = {}): TaskResult =
      cardAction(1, cardName, body = body)

  public fun cardAction1(cardName: ClassName, x: Int, body: BodyLambda = {}): TaskResult =
      cardAction(1, cardName, x, body)

  public fun cardAction2(cardName: ClassName, body: BodyLambda = {}): TaskResult =
      cardAction(2, cardName, body = body)

  public fun cardAction2(cardName: ClassName, x: Int, body: BodyLambda = {}): TaskResult =
      cardAction(2, cardName, x, body)

  public fun OperationBody.cardAction1(cardName: ClassName, body: BodyLambda = {}) {
    useCardAction(1, cardName, body = body)
  }

  public fun OperationBody.cardAction1(cardName: ClassName, x: Int, body: BodyLambda = {}) {
    useCardAction(1, cardName, x, body)
  }

  public fun OperationBody.cardAction2(cardName: ClassName, body: BodyLambda = {}) {
    useCardAction(2, cardName, body = body)
  }

  public fun OperationBody.cardAction2(cardName: ClassName, x: Int, body: BodyLambda = {}) {
    useCardAction(2, cardName, x, body)
  }

  private fun cardAction(
      which: Int,
      cardName: ClassName,
      x: Int? = null,
      body: BodyLambda = {},
  ): TaskResult {
    return stdAction("UseCardActionSA") {
      doTask("ActionUsedMarker<$cardName>")
      useCardAction(which, cardName, x, body)
    }
  }

  private fun OperationBody.useCardAction(
      which: Int,
      cardName: ClassName,
      x: Int? = null,
      body: BodyLambda = {},
  ) {
    doTask("UseAction<$cardName, ${whichAction(which)}>")
    x?.let { chooseVariableAmount(this, it) }
    payInvoiceFromItsResourceIfOffered()
    body()
  }

  private fun chooseVariableAmount(operation: OperationBody, x: Int) {
    require(x > 0) { "An action's X must be positive: $x" }
    val variableTasks =
        game.tasks
            .extract { it }
            .filter { task ->
              task.assignee == actor &&
                  task.instruction.descendantsOfType<Scalar>().any(Scalar::abstract)
            }
    val variableTask =
        variableTasks.singleOrNull { task ->
          task.instruction.descendantsOfType<Change>().any { change ->
            change.gaining?.className == cn("Owed")
          }
        } ?: variableTasks.single()
    val bound = bindXTo(x).transformInstructionTree(variableTask.instruction)
    val firstStage = if (bound is Then) bound.first else bound
    operation.doTask(firstStage.toString())
  }

  private fun whichAction(which: Int): String =
      when (which) {
        1 -> "First"
        2 -> "Second"
        3 -> "Third"
        else -> throw IllegalArgumentException("A component can offer only three actions: $which")
      }

  public fun sellPatents(count: Int): TaskResult =
      stdAction("SellPatents") {
        doTask("$count MC FROM ProjectCard<Hand>!")
      }

  public fun phase(phase: String, body: BodyLambda = {}) {
    if (count("Phase") != 1) {
      throw NotNowException(
          "No current Phase; start SetupPhase through TfmWorkflow before changing phases"
      )
    }
    asActor(ENGINE).godMode().manual("${phase}Phase FROM Phase", body)
  }

  public fun production(): Map<ClassName, Int> =
      standardResourceNames(reader).associateWith { production(it) }

  public fun production(kind: ClassName): Int =
      count("PROD[$kind]") - if (kind == MC || kind == cn("M")) 5 else 0

  public fun oxygenPercent(): Int = count("OxygenStep")

  public fun temperatureC(): Int = -30 + count("TemperatureStep") * 2

  public fun venusPercent(): Int = count("VenusStep") * 2

  public companion object {
    public fun World.tfm(actor: Actor): TfmGameplay = TfmGameplay(this, actor)
  }
}
