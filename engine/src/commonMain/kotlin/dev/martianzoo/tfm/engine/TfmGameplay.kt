package dev.martianzoo.tfm.engine

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NotNowException
import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.api.GameReader
import dev.martianzoo.data.Actor
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GameEvent.ChangeEvent
import dev.martianzoo.data.Player
import dev.martianzoo.data.Task
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.BodyLambda
import dev.martianzoo.engine.Gameplay
import dev.martianzoo.engine.Gameplay.OperationBody
import dev.martianzoo.engine.Gameplay.TurnLayer
import dev.martianzoo.engine.World
import dev.martianzoo.pets.Transforming.bindXTo
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.ScaledExpression.Scalar
import dev.martianzoo.tfm.api.ApiUtils.standardResourceNames
import dev.martianzoo.tfm.data.TfmClasses.MEGACREDIT
import dev.martianzoo.tfm.data.TfmClasses.STANDARD_RESOURCE_CLASSES

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
  private var allowOverpayment = false

  internal fun asActor(actor: Actor) =
      TfmGameplay(game, actor).also {
        if (explicitPaymentChoicesRequired) it.requireExplicitPaymentChoices()
      }

  public fun asPlayer(player: Player): TfmGameplay = asActor(player)

  public fun nextGeneration(vararg cardsBought: Int) {
    phase("Production")
    asActor(ENGINE).godMode().manual("Generation")
    phase("Research") {
      for ((cards, player) in cardsBought.zip(game.actors.filterIsInstance<Player>())) {
        asPlayer(player).doTask(if (cards > 0) "$cards BuyCard" else "Ok")
      }
    }
    phase("Action")
  }

  public fun playCorp(cardName: ClassName, buyCards: Int, body: BodyLambda = {}): TaskResult {
    return inTurn {
      doTask("PlayCard<Class<CorporationCard>, Class<$cardName>>")
      doTask(if (buyCards == 0) "Ok" else "$buyCards BuyCard")
      body()
    }
  }

  public fun pass(): TaskResult = inTfmTurn { doTask("Pass") }

  /**
   * Performs the actions in one test-level turn, declining an unused second action when needed. If
   * every other player has passed, the workflow offers `NewTurn` rather than a second action; that
   * offer is deliberately left in place so this block can contain the rest of the generation.
   */
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
      payment: BodyLambda = {
        payInvoiceFromItsResourceIfOffered()
      },
      body: BodyLambda = {},
  ): TaskResult {
    return inTfmTurn {
      doTask("UseAction<$stdAction, ${whichAction(which)}>")
      payment()
      body()
    }
  }

  private fun OperationBody.payInvoiceFromItsResourceIfOffered() {
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
        doTask("Pay<Class<Megacredit>> FROM Megacredit / Owed<>")
      },
      body: BodyLambda = {},
  ): TaskResult {
    return stdAction("UseStandardProjectSA") {
      doTask("UseAction<$stdProject, First>")
      payment()
      body()
    }
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
      megacredits: Int = 0,
      steel: Int = 0,
      titanium: Int = 0,
      body: BodyLambda = {},
  ): TaskResult {
    return inTfmTurn {
      if (tasks.matching { "${it.instruction}".contains("StandardAction") }.any()) {
        doTask("UseAction<PlayCardSA, First>")
      }
      doTask("PlayCard<Class<ProjectCard>, Class<$cardName>>")

      pay(megacredits, steel, titanium)
      body()
      autoExecNow()
    }
  }

  private fun inTfmTurn(body: BodyLambda): TaskResult {
    declineWildTagOffers()
    return inTurn {
      declineWildTagOffers()
      body()
      autoExecNow()
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
      megacredits: Int = 0,
      steel: Int = 0,
      titanium: Int = 0,
      plant: Int = 0,
      energy: Int = 0,
      heat: Int = 0,
  ): TaskResult {
    val underpaymentAllowed = allowUnderpayment
    val overpaymentAllowed = allowOverpayment
    allowUnderpayment = false
    allowOverpayment = false

    return godMode().continueManual {
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
            explicitPaymentChoicesRequired && cost < maximumFullValuePayment && !underpaymentAllowed
        ) {
          throw IllegalArgumentException(
              "$actor paid $cost $currency but could pay $maximumFullValuePayment at full value; " +
                  "call intentionalUnderpay() immediately before paying if this is sourced"
          )
        }
        if (explicitPaymentChoicesRequired && cost * value > owed && !overpaymentAllowed) {
          throw IllegalArgumentException(
              "$actor paid $cost $currency worth ${cost * value} against $owed owed; " +
                  "call intentionalOverpay() immediately before paying if this is sourced"
          )
        }
        if (cost > 0) doTask("$cost Pay<Class<$currency>> FROM $currency")
      }

      payNonMoneyResource(plant, "Plant")
      payNonMoneyResource(energy, "Energy")
      payNonMoneyResource(heat, "Heat")
      payNonMoneyResource(titanium, "Titanium")
      payNonMoneyResource(steel, "Steel")

      val owed = count("Owed")
      if (megacredits > owed) {
        throw LimitsException("Overpaying $megacredits MC when only $owed is owed")
      }
      if (megacredits > 0) {
        doTask("$megacredits Pay<Class<Megacredit>> FROM Megacredit")
      }

      // Take care of other Accepts we didn't need
      tasks
          .matching { it.cause?.context?.className == cn("Accept") }
          .forEach { reviseTask(it, "Ok") } // "executes" automatically
      autoExecNow()
    }
  }

  /** Allows the next [pay] call to leave usable accepted non-money resources unspent. */
  public fun intentionalUnderpay() {
    allowUnderpayment = true
  }

  /** Allows the next [pay] call to spend a non-money resource for less than its full value. */
  public fun intentionalOverpay() {
    allowOverpayment = true
  }

  internal fun requireExplicitPaymentChoices(): TfmGameplay = apply {
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
    x?.let { chooseVariableInvoiceAmount(this, it) }
    payInvoiceFromItsResourceIfOffered()
    body()
  }

  private fun chooseVariableInvoiceAmount(operation: OperationBody, x: Int) {
    require(x > 0) { "An action's X must be positive: $x" }
    val invoice =
        game.tasks
            .extract { it }
            .filter { it.assignee == actor }
            .single { task ->
              task.instruction.descendantsOfType<Change>().any { change ->
                change.gaining?.className == cn("Owed")
              } && task.instruction.descendantsOfType<Scalar>().any(Scalar::abstract)
            }
    operation.doTask(bindXTo(x).transformInstructionTree(invoice.instruction).toString())
  }

  private fun whichAction(which: Int): String =
      when (which) {
        1 -> "First"
        2 -> "Second"
        3 -> "Third"
        else -> throw IllegalArgumentException("A component can offer only three actions: $which")
      }

  public fun sellPatents(count: Int): TaskResult =
      stdAction("SellPatents") { doTask("-$count ProjectCard THEN $count") }

  public fun phase(phase: String, body: BodyLambda = {}) {
    if (count("Phase") != 1) {
      throw NotNowException(
          "No current Phase; start SetupPhase through TfmWorkflow before changing phases"
      )
    }
    asActor(ENGINE).godMode().manual("${phase}Phase FROM Phase", body)
  }

  internal fun production(): Map<ClassName, Int> =
      standardResourceNames(reader).associateWith { production(it) }

  public fun production(kind: ClassName): Int =
      count("PROD[$kind]") - if (kind == MEGACREDIT || kind == cn("M")) 5 else 0

  public fun oxygenPercent(): Int = count("OxygenStep")

  public fun temperatureC(): Int = -30 + count("TemperatureStep") * 2

  public fun venusPercent(): Int = count("VenusStep") * 2

  public companion object {
    public fun World.tfm(actor: Actor): TfmGameplay = TfmGameplay(this, actor)
  }
}
