package dev.martianzoo.tfm.engine

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.Agent.OperationScope
import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agent.OperationBlock
import dev.martianzoo.engine.World
import dev.martianzoo.pets.Transforming.bindXTo
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.ScaledExpression.Scalar
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.state.Player
import dev.martianzoo.state.Task
import dev.martianzoo.state.TaskResult

private val MC: ClassName = cn("MC")

/**
 * Wraps and extends an [Agent] to provide much more convenient functions specific to *Terraforming
 * Mars*.
 */
public class TfmGameplay(
    private val agents: Agents,
    override val actor: Actor,
) : Agent by agents[actor] {
  private val agent: Agent = agents[actor]
  private val game: World = agents.world

  private var explicitPaymentChoicesRequired = false
  private var explicitUnusedActionCardsRequired = false
  private var allowNondefaultPayment = false

  private fun asActor(actor: Actor) =
      TfmGameplay(agents, actor).also {
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

  /** Keeps [count] of the ten project cards offered during setup. */
  public fun keepStartingProjects(count: Int): TaskResult = agent.continueOperation {
    discardUnwantedCards(count)
  }

  /**
   * Plays a standard corporation and buys the project cards retained during setup. This convenience
   * chooses fixed corporation effects before paying for the starting cards; the underlying tasks
   * remain independently selectable by other clients.
   */
  public fun playCorp(cardName: ClassName, body: OperationBlock = {}): TaskResult {
    if (count("CorporationPhase") == 0) {
      return inTurn {
        doTask("PlayCard<Class<CorporationCard>, Class<$cardName>>")
        body()
      }
    }
    val previousPolicy = autoExecPolicy
    autoExecPolicy = NONE
    return try {
      inTurn {
        doTask("PlayCard<Class<CorporationCard>, Class<$cardName>>")
        payAllMc()
        chooseConcreteCorporationEffectsBeforePurchase()
        body()
        chooseConcreteCorporationEffectsBeforePurchase()
        buySelectedCards()
      }
    } finally {
      autoExecPolicy = previousPolicy
    }
  }

  /** Buys the selected number of project cards and pays their adjusted M€ cost. */
  public fun buyCards(count: Int): TaskResult = agent.continueOperation {
    buyOfferedCards(count)
  }

  /** Shares the operation-scoped discard, payment, and hand transfer sequence across all buys. */
  private fun OperationScope.buyOfferedCards(count: Int) {
    discardUnwantedCards(count)
    buySelectedCards()
  }

  private fun OperationScope.discardUnwantedCards(count: Int) {
    val offered = this@TfmGameplay.count("ProjectCard<Selecting>")
    require(count in 0..offered) { "Cannot buy $count of $offered offered project cards" }
    val discarded = offered - count
    doTask(if (discarded == 0) "Ok" else "-$discarded ProjectCard<Selecting>")
  }

  private fun OperationScope.buySelectedCards() {
    autoExecNow()
    payAllMc()
    val purchased = this@TfmGameplay.count("ProjectCard<Selecting>")
    if (purchased > 0) doTask("$purchased ProjectCard<Hand FROM Selecting>")
  }

  private fun OperationScope.chooseConcreteCorporationEffectsBeforePurchase() {
    while (true) {
      val next =
          tasks
              .extract { it }
              .filter { task ->
                task.selectionAssignee == actor && asActor(task.assignee).canSelectTask(task.id)
              }
              .firstOrNull { task -> !task.instruction.isAbstract(reader) } ?: return
      selectTaskForActor(next)
    }
  }

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

  private fun passWithoutUnusedActionCardCheck(): TaskResult = inTurn { doTask("Pass") }

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
    return inTurn {
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
    val trigger = game.events.changeAt(origin.triggerEvent)
    return trigger?.change?.gaining?.className == cn("SecondAction")
  }

  /**
   * Selects [option] from the current turn offer, runs [payment], then completes its [body]. For
   * example, use `ClaimMilestone<Class<Builder>>` or `TradeAction<Action2>`. When a required action
   * is pending, select `RequiredActionsSignal` instead.
   *
   * The default payment spends the bill's resource when it is the sole accepted resource. Supply
   * [payment] for mixed payments and [body] for remaining choices. Selection must narrow an offered
   * task; failures roll back this operation through [inTurn].
   */
  public fun stdAction(
      option: String,
      payment: OperationBlock = { payInvoiceFromItsResourceIfOffered() },
      body: OperationBlock = {},
  ): TaskResult = inTurn { useStdAction(option, payment, body) }

  /**
   * Selects [option] from an action granted within the enclosing operation, then runs [payment] and
   * [body]. Uses the same option syntax and default payment as [stdAction]; the enclosing operation
   * owns completion and rollback.
   */
  public fun OperationScope.useStdAction(
      option: String,
      payment: OperationBlock = { payInvoiceFromItsResourceIfOffered() },
      body: OperationBlock = {},
  ) {
    doTask(option)
    payment()
    body()
  }

  public fun claimMilestone(milestone: ClassName): TaskResult =
      stdAction("ClaimMilestone<Class<$milestone>>")

  public fun fundAward(award: ClassName, amountPaid: Int): TaskResult =
      stdAction("FundAward<Class<$award>>", payment = { pay(amountPaid) })

  private fun OperationScope.payInvoiceFromItsResourceIfOffered() {
    val billingCause = openPendingBilling()
    val resource = acceptedResources().singleOrNull()
    if (resource != null) {
      doTask("-$resource / Owed<Class<$resource>>")
    }
    if (this@TfmGameplay.count("Owed") == 0) finishBilling(billingCause)
  }

  /** The standard resources this Actor's live billing accepts. */
  private fun acceptedResources(): List<ClassName> =
      reader.getComponents(resolve("Accepting<$actor>")).elements.mapNotNull {
        it.typeDependencies.firstNotNullOfOrNull { dependency ->
          dependency.boundType.representedClass?.className
        }
      }

  public fun convertPlants(body: OperationBlock = {}): TaskResult {
    return stdAction("ConvertPlants", body = body)
  }

  public fun convertHeat(body: OperationBlock = {}): TaskResult {
    return stdAction("ConvertHeat", body = body)
  }

  public fun stdProject(
      stdProject: String,
      payment: OperationBlock = {
        payAllMc()
      },
      body: OperationBlock = {},
  ): TaskResult {
    return inTurn { useStdProject(stdProject, payment, body) }
  }

  /** Uses a granted standard-action slot for a standard project within an enclosing operation. */
  public fun OperationScope.useStdProject(
      stdProject: String,
      payment: OperationBlock = {
        payAllMc()
      },
      body: OperationBlock = {},
  ) {
    useStdAction("UseStandardProject<$stdProject>", payment, body)
  }

  public fun playPrelude(
      cardName: ClassName,
      body: OperationBlock = {},
  ): TaskResult {
    return inTurn { playPrelude(cardName, body) }
  }

  public fun OperationScope.playPrelude(
      cardName: ClassName,
      body: OperationBlock = {},
  ) {
    playCardWithinOperation(cn("PreludeCard"), cardName, body)
  }

  public fun OperationScope.playCorp(
      cardName: ClassName,
      body: OperationBlock = {},
  ) {
    playCardWithinOperation(cn("CorporationCard"), cardName, body)
  }

  private fun OperationScope.playCardWithinOperation(
      cardBack: ClassName,
      cardName: ClassName,
      body: OperationBlock,
  ) {
    doTask("PlayCard<Class<$cardBack>, Class<$cardName>>")
    body()
  }

  // In the method after this, all the cost parameters are optional,
  // but you've gotta provide ONE of them.
  public fun playProject(unused1: ClassName, unused2: OperationBlock = {}): Nothing =
      error("you must specify some cost")

  public fun playProject(
      cardName: ClassName,
      mc: Int = 0,
      steel: Int = 0,
      titanium: Int = 0,
      plants: Int = 0,
      energy: Int = 0,
      heat: Int = 0,
      payment: OperationBlock = { pay(mc, steel, titanium, plants, energy, heat) },
      body: OperationBlock = {},
  ): TaskResult {
    return inTurn {
      playProjectWithinOperation(cardName, payment, body)
    }
  }

  public fun OperationScope.playProject(
      cardName: ClassName,
      mc: Int = 0,
      steel: Int = 0,
      titanium: Int = 0,
      plants: Int = 0,
      energy: Int = 0,
      heat: Int = 0,
      payment: OperationBlock = { pay(mc, steel, titanium, plants, energy, heat) },
      body: OperationBlock = {},
  ) {
    playProjectWithinOperation(cardName, payment, body)
  }

  private fun OperationScope.playProjectWithinOperation(
      cardName: ClassName,
      payment: OperationBlock,
      body: OperationBlock,
  ) {
    val instruction =
        if (
            tasks
                .matching { cn("PlayCard") in it.instruction.descendantsOfType<ClassName>() }
                .isNotEmpty()
        )
            "PlayCard<Class<ProjectCard>, Class<$cardName>>"
        else "PlayProject<Class<$cardName>>"
    doTask(instruction)

    payment()
    body()
    if (this@TfmGameplay.count("Owed") == 0) declineUnusedPaymentOffers(fromCards = true)
    autoExecNow()
  }

  /**
   * Pays the open billing component and rejects any allocation containing a unit that could be
   * returned without leaving the debt underpaid.
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
    val previousAutoExecPolicy = autoExecPolicy
    if (autoExecPolicy != NONE) autoExecPolicy = CONCRETE

    return try {
      continueOperation {
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
            doTask("-$units $currency")
          }
        }
        if (count("Owed") == 0) finishBilling(billingCause)
      }
    } finally {
      autoExecPolicy = previousAutoExecPolicy
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

  private fun OperationScope.openPendingBilling(): Cause? {
    if (this@TfmGameplay.count("Owed") != 0) return null
    val billing =
        game.tasks
            .extract { it }
            .filter { task ->
              task.selectionAssignee == actor &&
                  task.instruction.descendantsOfType<Change>().any { change ->
                    change.gaining?.className == cn("Owed")
                  }
            }
            .singleOrNull() ?: return null
    selectTaskForActor(billing)
    advanceSingleConcreteTask(billing.cause)
    return billing.cause
  }

  private fun OperationScope.payAllMc() {
    val billingCause = openPendingBilling()
    val owed = this@TfmGameplay.count("Owed")
    if (owed > 0) doTask("-$owed MC")
    if (this@TfmGameplay.count("Owed") == 0) finishBilling(billingCause)
  }

  private fun OperationScope.finishBilling(billingCause: Cause?) {
    declineUnusedPaymentOffers()
    autoExecNow()
    advanceSingleConcreteTask(billingCause)
  }

  /** Declines unused payment offers after their bill has been settled. */
  private fun OperationScope.declineUnusedPaymentOffers(fromCards: Boolean = false) {
    while (true) {
      val offer = pendingPaymentOffers(fromCards = fromCards).firstOrNull() ?: return
      selectTaskForActor(offer)
      if (offer.id in game.tasks) narrowTask("Ok")
    }
  }

  private fun OperationScope.advanceSingleConcreteTask(cause: Cause?) {
    if (cause == null) return
    while (true) {
      val next =
          game.tasks
              .extract { it }
              .filter { task ->
                task.selectionAssignee == actor &&
                    task.cause == cause &&
                    !task.instruction.isAbstract(reader) &&
                    asActor(task.assignee).canSelectTask(task.id)
              }
              .singleOrNull() ?: return
      selectTaskForActor(next)
    }
  }

  private fun preparePayment(currency: String) {
    val matching = pendingPaymentOffers(currency)
    val selectedOffer = pendingPaymentOffers().singleOrNull { it.selected }
    if (selectedOffer != null && matching.none { it.id == selectedOffer.id }) {
      asActor(selectedOffer.assignee).narrowTask("Ok")
    }
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
            task.selectionAssignee == actor &&
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

  private fun OperationScope.selectTaskForActor(task: Task) {
    while (task.id in game.tasks) {
      val current = game.tasks.getTaskData(task.id)
      if (current.selected && current.instruction.isAbstract(reader)) return
      if (current.assignee == actor) selectTask(task.id)
      else asActor(current.assignee).selectTask(task.id)
    }
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
   * How much of the open billing component one unit of [currency] settles: one when its
   * denomination is [currency], plus one per [ResourceValue] the payer owns for it.
   */
  private fun paymentValue(currency: String): Int =
      count("ResourceValue<Class<$currency>>") + if (count("Owed<Class<$currency>>") > 0) 1 else 0

  public fun cardAction1(cardName: ClassName, body: OperationBlock = {}): TaskResult =
      cardAction(1, cardName, body = body)

  /** Selects the action's variable task and binds its X to positive [x]. */
  public fun cardAction1(
      cardName: ClassName,
      x: Int,
      body: OperationBlock = {},
  ): TaskResult = cardAction(1, cardName, x, body)

  public fun cardAction2(cardName: ClassName, body: OperationBlock = {}): TaskResult =
      cardAction(2, cardName, body = body)

  /** Binds the action's X to positive [x] without directly executing the resulting task. */
  public fun cardAction2(
      cardName: ClassName,
      x: Int,
      body: OperationBlock = {},
  ): TaskResult = cardAction(2, cardName, x, body)

  public fun OperationScope.cardAction1(cardName: ClassName, body: OperationBlock = {}) {
    useCardAction(1, cardName, body = body)
  }

  /** Binds the action's X to positive [x] without directly executing the resulting task. */
  public fun OperationScope.cardAction1(cardName: ClassName, x: Int, body: OperationBlock = {}) {
    useCardAction(1, cardName, x, body)
  }

  public fun OperationScope.cardAction2(cardName: ClassName, body: OperationBlock = {}) {
    useCardAction(2, cardName, body = body)
  }

  /** Binds the action's X to positive [x] without directly executing the resulting task. */
  public fun OperationScope.cardAction2(cardName: ClassName, x: Int, body: OperationBlock = {}) {
    useCardAction(2, cardName, x, body)
  }

  private fun cardAction(
      which: Int,
      cardName: ClassName,
      x: Int? = null,
      body: OperationBlock = {},
  ): TaskResult {
    return inTurn {
      useCardAction(which, cardName, x, body)
    }
  }

  private fun OperationScope.useCardAction(
      which: Int,
      cardName: ClassName,
      x: Int? = null,
      body: OperationBlock = {},
  ) {
    val signal =
        if (
            tasks
                .matching { cn("UseAction") in it.instruction.descendantsOfType<ClassName>() }
                .isNotEmpty()
        )
            "UseAction"
        else "UseCardAction"
    doTask("$signal<$cardName, ${whichAction(which)}>")
    x?.let { chooseVariableAmount(this, it) }
    payInvoiceFromItsResourceIfOffered()
    body()
  }

  private fun chooseVariableAmount(operation: OperationScope, x: Int) {
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
    selectTask(variableTask.id)
    narrowTask(bound.toString())
    operation.autoExecNow()
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
        doTask("$count MC FROM ProjectCard!")
      }

  public fun phase(phase: String, body: OperationBlock = {}) {
    if (count("Phase") != 1) {
      throw NotNowException(
          "No current Phase; start SetupPhase through TfmWorkflow before changing phases"
      )
    }
    asActor(ADMIN).runOperation("${phase}Phase FROM Phase", body)
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
    /** Creates Terraforming Mars conveniences for this world's [actor]. */
    public fun Agents.tfm(actor: Actor): TfmGameplay = TfmGameplay(this, actor)
  }
}
