package dev.martianzoo.tfm.engine

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.Agent.OperationBody
import dev.martianzoo.agent.AutoExecMode.NONE
import dev.martianzoo.agent.AutoExecMode.SAFE
import dev.martianzoo.agent.BodyLambda
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
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.ScaledExpression.Scalar
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.GameEvent.ChangeEvent
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.data.Task
import dev.martianzoo.pets.data.TaskResult

private val MC: ClassName = cn("MC")

/**
 * Wraps and extends an [Agent] to provide much more convenient functions specific to *Terraforming
 * Mars*.
 */
public class TfmGameplay(
    private val game: World,
    override val actor: Actor,
    private val agent: Agent = game.agent(actor),
) : Agent by agent {
  override val reader: GameReader
    get() = game.reader

  private var explicitPaymentChoicesRequired = false
  private var explicitUnusedActionCardsRequired = false
  private var allowNondefaultPayment = false

  private fun asActor(actor: Actor) =
      TfmGameplay(game, actor).also {
        if (explicitPaymentChoicesRequired) it.requireExplicitPaymentChoices()
        if (explicitUnusedActionCardsRequired) it.requireExplicitUnusedActionCards()
      }

  public fun asPlayer(player: Player): TfmGameplay = asActor(player)

  public fun nextGeneration(vararg cardsBought: Int) {
    phase("Production")
    phase("Research") {
      for ((cards, player) in cardsBought.zip(game.actors.filterIsInstance<Player>())) {
        asPlayer(player).buyCards(cards)
      }
    }
    phase("Action")
  }

  public fun playCorp(cardName: ClassName, buyCards: Int, body: BodyLambda = {}): TaskResult {
    return inTurn {
      doTask("PlayCard<Class<CorporationCard>, Class<$cardName>, Hand>")
      buySelectedCards(buyCards)
      body()
    }
  }

  /** Buys the selected number of offered project cards and settles their M€ invoice. */
  public fun buyCards(count: Int): TaskResult = agent.continueManual { buySelectedCards(count) }

  private fun OperationBody.buySelectedCards(count: Int) {
    openPendingProjectCardOffer()
    val offered = this@TfmGameplay.count("ProjectCard<Selecting>")
    require(count in 0..offered) { "cannot buy $count of $offered selected project cards" }
    val discarded = offered - count
    selectTask(tasks.extract { it }.single { it.discardsSelectedProjectCards() }.id)
    narrowTask(if (discarded == 0) "Ok" else "-$discarded ProjectCard<Selecting>")
    if (hasPendingBuySelectedCards(tasks)) doTask("BuySelectedCards")
    if (count > 0) payAllMc()
  }

  /**
   * Selects the pending task that puts project cards on offer. A task that deals them directly is
   * preferred over one that only leads to a deal through its continuation.
   */
  private fun OperationBody.openPendingProjectCardOffer() {
    if (this@TfmGameplay.count("ProjectCard<Selecting>") != 0) return
    val offers = tasks.extract { it }.filter { it.assignee == actor && it.offersProjectCards() }
    if (offers.isEmpty()) return
    val dealsNow = offers.filter { it.instruction.dealsSelectedProjectCards() }
    selectTask((dealsNow.singleOrNull() ?: offers.single()).id)
  }

  /** Whether this task, now or through its continuation, puts project cards on offer. */
  private fun Task.offersProjectCards(): Boolean =
      instruction.dealsSelectedProjectCards() ||
          then?.instructions.orEmpty().any { it.dealsSelectedProjectCards() }

  /** Whether this task discards from the cards already on offer. */
  private fun Task.discardsSelectedProjectCards(): Boolean =
      instruction.descendantsOfType<Change>().any { it.removing.isSelectedProjectCard() }

  private fun InstructionTree.dealsSelectedProjectCards(): Boolean =
      descendantsOfType<Change>().any { it.gaining.isSelectedProjectCard() }

  /** Whether this instruction gains a component of class [className]. */
  private fun InstructionTree.gains(className: ClassName): Boolean =
      descendantsOfType<Change>().any { it.gaining?.className == className }

  /** Whether this instruction offers `UseAction` against a provider of class [provider]. */
  private fun InstructionTree.offersAction(provider: ClassName): Boolean =
      descendantsOfType<Change>().any { change ->
        change.gaining?.className == cn("UseAction") &&
            change.gaining!!.arguments.any { it.className == provider }
      }

  private fun Expression?.isSelectedProjectCard(): Boolean =
      this != null &&
          className == cn("ProjectCard") &&
          cn("Selecting") in descendantsOfType<ClassName>()

  private fun hasPendingBuySelectedCards(tasks: TaskQueue): Boolean =
      tasks.extract { it }.any { it.instruction.gains(cn("BuySelectedCards")) }

  public fun pass(): TaskResult {
    return if (explicitUnusedActionCardsRequired) pass(unused = emptySet())
    else passWithoutUnusedActionCardCheck()
  }

  public fun pass(unused: ClassName, vararg additionallyUnused: ClassName): TaskResult =
      pass(unused = setOf(unused, *additionallyUnused))

  public fun pass(unused: Set<ClassName>): TaskResult {
    val actual =
        reader
            .getComponents(resolve("ActionCard"))
            .elements
            .filter { count("ActionUsedMarker<${it.className}>") == 0 }
            .map { it.className }
            .toSet()
    require(actual == unused) { "$actor has unused action cards $actual, not $unused" }
    return passWithoutUnusedActionCardCheck()
  }

  private fun passWithoutUnusedActionCardCheck(): TaskResult = inTfmTurn { doTask("Pass") }

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
      doTask("Ok", secondAction.id)
    }
  }

  private fun secondActionOffer(): Task? =
      game.tasks
          .extract { it }
          .filter { it.assignee == actor }
          .filter { task -> task.isActionPhaseSecondAction() }
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
      stdAction("ClaimMilestoneAction") { doTask("$milestone") }

  public fun fundAward(award: ClassName, amountPaid: Int): TaskResult {
    val which = count("Award") + 1
    return stdAction("FundAwardAction", which, payment = { pay(amountPaid) }) { doTask("$award") }
  }

  private fun OperationBody.payInvoiceFromItsResourceIfOffered() {
    val billingCause = openPendingBilling()
    val resource = acceptedResources().singleOrNull()
    if (resource != null) {
      doTask("Pay<Class<$resource>> FROM $resource / Owed<Class<$resource>>")
    }
    if (this@TfmGameplay.count("Owed") == 0) finishBilling(billingCause)
  }

  /** The standard resources this Actor's live billing accepts. */
  private fun acceptedResources(): List<ClassName> =
      reader.getComponents(resolve("Accepting<$actor>")).elements.mapNotNull {
        it.expression.arguments.lastOrNull()?.arguments?.singleOrNull()?.className
      }

  public fun convertPlants(body: BodyLambda = {}): TaskResult {
    return stdAction("ConvertPlantsAction", body = body)
  }

  public fun convertHeat(body: BodyLambda = {}): TaskResult {
    return stdAction("ConvertHeatAction", body = body)
  }

  public fun stdProject(
      stdProject: String,
      payment: BodyLambda = {
        payAllMc()
      },
      body: BodyLambda = {},
  ): TaskResult {
    return stdAction("UseStandardProjectAction", payment = {}) {
      doTask("UseAction<$stdProject, Action1>")
      payment()
      body()
    }
  }

  public fun playPrelude(cardName: ClassName, body: BodyLambda = {}): TaskResult {
    return inTfmTurn { playPreludeWithinOperation(cardName, body) }
  }

  public fun OperationBody.playPrelude(cardName: ClassName, body: BodyLambda = {}) {
    playPreludeWithinOperation(cardName, body)
  }

  private fun OperationBody.playPreludeWithinOperation(cardName: ClassName, body: BodyLambda) {
    playCardWithinOperation(cn("PreludeCard"), cardName, body)
  }

  public fun OperationBody.playCorp(cardName: ClassName, body: BodyLambda = {}) {
    playCardWithinOperation(cn("CorporationCard"), cardName, body)
  }

  private fun OperationBody.playCardWithinOperation(
      cardBack: ClassName,
      cardName: ClassName,
      body: BodyLambda,
  ) {
    val location = if (this@TfmGameplay.count("$cardBack<Selecting>") > 0) "Selecting" else "Hand"
    doTask("PlayCard<Class<$cardBack>, Class<$cardName>, $location>")
    body()
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
      plants: Int = 0,
      energy: Int = 0,
      heat: Int = 0,
      payment: BodyLambda = { pay(mc, steel, titanium, plants, energy, heat) },
      body: BodyLambda = {},
  ): TaskResult {
    return inTfmTurn { playProjectWithinOperation(cardName, payment, body) }
  }

  public fun OperationBody.playProject(
      cardName: ClassName,
      mc: Int = 0,
      steel: Int = 0,
      titanium: Int = 0,
      plants: Int = 0,
      energy: Int = 0,
      heat: Int = 0,
      payment: BodyLambda = { pay(mc, steel, titanium, plants, energy, heat) },
      body: BodyLambda = {},
  ) {
    playProjectWithinOperation(cardName, payment, body)
  }

  private fun OperationBody.playProjectWithinOperation(
      cardName: ClassName,
      payment: BodyLambda,
      body: BodyLambda,
  ) {
    if (tasks.matching { it.instruction.offersAction(cn("StandardAction")) }.any()) {
      doTask("UseAction<PlayCardFromHandAction, Action1>")
    }
    doTask("PlayCard<Class<ProjectCard>, Class<$cardName>, Hand>")

    payment()
    body()
    if (this@TfmGameplay.count("Owed") == 0) declineUnusedPaymentOffers(fromCards = true)
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
                previous == null || previous.copy(selection = task.selection) != task
              }
      if (newPendingTasks.isNotEmpty()) {
        if (newPendingTasks.any { it.instruction.isAbstract(game.reader) }) {
          throw AbstractException("pending abstract tasks:\n${newPendingTasks.joinToString("\n")}")
        }
        throw TaskException("pending tasks:\n${newPendingTasks.joinToString("\n")}")
      }
    }
  }

  /**
   * Pays the open invoice and rejects any allocation containing a unit that could be returned
   * without leaving the invoice underpaid.
   */
  public fun pay(
      mc: Int = 0,
      steel: Int = 0,
      titanium: Int = 0,
      plants: Int = 0,
      energy: Int = 0,
      heat: Int = 0,
  ): TaskResult {
    val nondefaultPaymentAllowed = allowNondefaultPayment
    allowNondefaultPayment = false
    // Billing effects are queued; safely advance them until the payment choices are available.
    val previousAutoExecMode = autoExecMode
    if (autoExecMode != NONE) autoExecMode = SAFE

    return try {
      continueManual {
        val billingCause = openPendingBilling()
        val tender =
            linkedMapOf(
                "Plant" to plants,
                "Energy" to energy,
                "Heat" to heat,
                "Titanium" to titanium,
                "Steel" to steel,
                MC.toString() to mc,
            )
        rejectReturnableUnit(tender)
        if (explicitPaymentChoicesRequired && !nondefaultPaymentAllowed) auditSourcedTender(tender)
        for ((currency, units) in tender) {
          if (units > 0) {
            preparePayment(currency)
            doTask("$units Pay<Class<$currency>> FROM $currency")
          }
        }
        if (count("Owed") == 0) finishBilling(billingCause)
      }
    } finally {
      autoExecMode = previousAutoExecMode
    }
  }

  /**
   * Applies the payment rule once to the complete tender. Rounding excess is legal when every
   * selected unit is necessary; a unit is illegal when removing it would still cover the debt.
   */
  private fun rejectReturnableUnit(tender: Map<String, Int>) {
    val debt = count("Owed<Class<MC>>")
    if (debt == 0) return
    val values =
        tender
            .filterValues { it > 0 }
            .mapValues { (currency, _) -> paymentValue(currency) }
            .filterValues { it > 0 }
    val total = values.entries.sumOf { (currency, value) -> tender.getValue(currency) * value }
    values.forEach { (currency, value) ->
      if (total - value >= debt) {
        throw LimitsException(
            "Illegal payment by $actor: removing one $currency still covers $debt owed"
        )
      }
    }
  }

  /** Audits sourced legal payments against the default resource allocation. */
  private fun auditSourcedTender(tender: Map<String, Int>) {
    var remainingDebt = count("Owed<Class<MC>>")
    if (remainingDebt == 0) return
    for ((currency, units) in tender) {
      if (currency == MC.toString() || pendingPaymentOffers(currency).isEmpty()) continue
      val value = paymentValue(currency)
      if (value > 1) {
        val fullValueUnits = minOf(count(currency), remainingDebt / value)
        if (units < fullValueUnits) {
          throw IllegalArgumentException(
              "$actor paid $units $currency but could pay $fullValueUnits at full value; " +
                  "call intentionalUnderpay() immediately before paying if this is sourced"
          )
        }
      } else if (
          value == 1 &&
              units > 0 &&
              pendingPaymentOffers(MC.toString()).isNotEmpty() &&
              count(MC.toString()) >= remainingDebt
      ) {
        throw IllegalArgumentException(
            "$actor paid $units $currency while $remainingDebt M€ could settle the bill; " +
                "call intentionalUnderpay() immediately before paying if this is sourced"
        )
      }
      remainingDebt = (remainingDebt - units * value).coerceAtLeast(0)
    }
  }

  private fun OperationBody.openPendingBilling(): Cause? {
    if (this@TfmGameplay.count("Owed") != 0) return null
    val billing =
        game.tasks
            .extract { it }
            .filter { task ->
              task.actor == actor &&
                  task.instruction.descendantsOfType<Change>().any { change ->
                    change.gaining?.className == cn("Owed")
                  }
            }
            .singleOrNull() ?: return null
    selectTaskForActor(billing)
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
    declineUnusedPaymentOffers()
    autoExecNow()
    advanceSingleConcreteTask(billingCause)
  }

  /** Declines unused payment offers after their bill has been settled. */
  private fun OperationBody.declineUnusedPaymentOffers(fromCards: Boolean = false) {
    while (true) {
      val offer = pendingPaymentOffers(fromCards = fromCards).firstOrNull() ?: return
      selectTaskForActor(offer)
      if (offer.id in game.tasks) narrowTask("Ok")
    }
  }

  private fun OperationBody.advanceSingleConcreteTask(cause: Cause?) {
    if (cause == null) return
    while (true) {
      val next =
          game.tasks
              .extract { it }
              .filter { task ->
                task.actor == actor &&
                    task.cause == cause &&
                    !task.instruction.isAbstract(reader) &&
                    asActor(task.assignee).canSelectTask(task.id)
              }
              .singleOrNull() ?: return
      selectTaskForActor(next)
    }
  }

  private fun preparePayment(currency: String) {
    val offer = pendingPaymentOffers(currency).singleOrNull() ?: return
    if (offer.assignee != actor) asActor(offer.assignee).selectTask(offer.id)
  }

  private fun pendingPaymentOffers(
      currency: String? = null,
      fromCards: Boolean = false,
  ): List<Task> =
      game.tasks
          .extract { it }
          .filter { task ->
            val context = task.cause?.context
            task.actor == actor &&
                when (context?.className) {
                  cn("Accepting") ->
                      currency == null ||
                          context.arguments.any {
                            it.arguments.singleOrNull()?.className == cn(currency)
                          }
                  cn("AcceptingFromCard") -> fromCards && currency == null
                  else -> false
                }
          }

  private fun OperationBody.selectTaskForActor(task: Task) {
    if (task.assignee == actor) selectTask(task.id) else asActor(task.assignee).selectTask(task.id)
  }

  /** Exempts the next [pay] call from the default-allocation audit for a sourced legal payment. */
  public fun intentionalUnderpay() {
    allowNondefaultPayment = true
  }

  public fun requireExplicitPaymentChoices(): TfmGameplay = apply {
    explicitPaymentChoicesRequired = true
  }

  public fun requireExplicitUnusedActionCards(): TfmGameplay = apply {
    explicitUnusedActionCardsRequired = true
  }

  /**
   * How much of the open invoice one unit of [currency] settles: one when the invoice uses that
   * denomination, plus one per [ResourceValue] the payer owns for it.
   */
  private fun paymentValue(currency: String): Int =
      count("ResourceValue<Class<$currency>>") + if (count("Owed<Class<$currency>>") > 0) 1 else 0

  public fun cardAction1(cardName: ClassName, body: BodyLambda = {}): TaskResult =
      cardAction(1, cardName, body = body)

  public fun cardAction1(
      cardName: ClassName,
      x: Int,
      body: BodyLambda = {},
  ): TaskResult = cardAction(1, cardName, x, body)

  public fun cardAction2(cardName: ClassName, body: BodyLambda = {}): TaskResult =
      cardAction(2, cardName, body = body)

  public fun cardAction2(
      cardName: ClassName,
      x: Int,
      body: BodyLambda = {},
  ): TaskResult = cardAction(2, cardName, x, body)

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
    return stdAction("UseActionOnCardAction") {
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
    val variableTask = variableTasks.single()
    val bound = bindXTo(x).transformInstructionTree(variableTask.instruction)
    val firstStage = if (bound is Then) bound.first else bound
    operation.doTask(firstStage.toString())
  }

  private fun whichAction(which: Int): String =
      when (which) {
        1 -> "Action1"
        2 -> "Action2"
        3 -> "Action3"
        else -> throw IllegalArgumentException("A component can offer only three actions: $which")
      }

  public fun sellPatents(count: Int): TaskResult =
      stdProject("SellPatentsProject") {
        doTask("$count MC FROM ProjectCard<Hand>!")
      }

  public fun phase(phase: String, body: BodyLambda = {}) {
    if (count("Phase") != 1) {
      throw NotNowException(
          "No current Phase; start SetupPhase through TfmWorkflow before changing phases"
      )
    }
    asActor(ADMIN).manual("${phase}Phase FROM Phase", body)
  }

  public fun production(kind: ClassName): Int =
      count("PROD[$kind]") - count("ProdOffset<Class<$kind>>")

  public fun oxygenPercent(): Int = count("OxygenStep")

  public fun temperatureC(): Int = -30 + count("TemperatureStep") * 2

  public fun venusPercent(): Int {
    val count = count("VenusStep")
    return if (count <= 15) count * 2 else count + 15 // thanks Amazonis
  }

  public companion object {
    public fun World.tfm(actor: Actor): TfmGameplay = TfmGameplay(this, actor)
  }
}
