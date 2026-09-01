package dev.martianzoo.tfm.canon

import dev.martianzoo.engine.RoutineReplayEncoder
import dev.martianzoo.engine.RoutineReplayEncoder.Entry
import dev.martianzoo.engine.RoutineReplayEncoder.Entry.Call
import dev.martianzoo.engine.RoutineReplayEncoder.Entry.Correction
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.GameEvent
import dev.martianzoo.pets.data.GameEvent.ChangeEvent
import dev.martianzoo.pets.data.GameEvent.GameplayInputEvent
import dev.martianzoo.pets.data.GameEvent.GameplayInputEvent.Kind.DIRECT_CHANGES
import dev.martianzoo.pets.data.GameEvent.GameplayInputEvent.Kind.SELECT_TASK
import dev.martianzoo.pets.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.pets.data.Player

/** Terraforming Mars interpretation of generic Player-input history. */
internal object TerraformingMarsReplayEncoder : RoutineReplayEncoder {
  override fun encode(world: World, events: List<GameEvent>): List<Entry> {
    val inputs = events.filterIsInstance<GameplayInputEvent>()
    val preludeStart =
        events
            .filterIsInstance<ChangeEvent>()
            .firstOrNull { it.change.gaining?.className == cn("PreludePhase") }
            ?.ordinal ?: Int.MAX_VALUE
    val result = mutableListOf<Entry>()
    result += encodeCorporationSetup(world, inputs.filter { it.ordinal < preludeStart })

    val gameplayInputs = inputs.filter { it.ordinal >= preludeStart }
    val starts = gameplayInputs.map(GameplayInputEvent::operationStartOrdinal).distinct().sorted()
    val eventRanges = starts.associateWith { start ->
      val end = starts.firstOrNull { it > start } ?: Int.MAX_VALUE
      events.filter { it.ordinal in start until end }
    }
    gameplayInputs
        .groupBy { it.operationStartOrdinal to it.actor }
        .values
        .sortedBy { group -> group.minOf(GameplayInputEvent::ordinal) }
        .forEach { group ->
          result +=
              encodeOperation(
                  world,
                  group,
                  eventRanges.getValue(group.first().operationStartOrdinal),
              )
        }
    return result
  }

  private fun encodeCorporationSetup(
      world: World,
      inputs: List<GameplayInputEvent>,
  ): List<Entry> = buildList {
    world.actors.filterIsInstance<Player>().forEach { actor ->
      val actorInputs = inputs.filter { it.actor == actor }
      val selecting =
          actorInputs.firstOrNull { Regex("Selecting<[^>]+>!").matches(it.source) }
              ?: return@forEach
      val offer = actorInputs.first {
        Regex("\\d+ ProjectCard<Selecting<[^>]+>>!").matches(it.source)
      }
      val play = actorInputs.first { it.source.startsWith("PlayCard<Class<CorporationCard>") }
      val discard = actorInputs.first {
        it.ordinal > play.ordinal && "ProjectCard<Selecting>" in it.source
      }
      val card = checkNotNull(Regex("Class<([^>]+)>>$").find(play.source)?.groupValues?.get(1))
      val offerCount = offer.source.substringBefore(' ')
      val consequenceInputs = actorInputs.filter { input ->
        input.ordinal in (play.ordinal + 1) until discard.ordinal &&
            isSetupConsequence(input.source)
      }
      add(Call(actor, "tasks", listOf("${selecting.taskNumber} select")))
      add(
          Call(
              actor,
              "tasks",
              listOf(
                  "$offerCount ProjectCard<Selecting>",
                  normalizeChoice(world, actor, discard.source),
              ),
          )
      )
      add(Call(actor, "playCard", listOf(card)))
      add(Call(actor, "tasks", consequenceInputs.map { normalizeChoice(world, actor, it.source) }))
      add(Call(actor, "buyCards"))
    }
  }

  private fun isSetupConsequence(source: String): Boolean =
      source.startsWith("ProjectCard<") ||
          Regex("\\d+ MC<[^>]+>!").matches(source) ||
          source.startsWith("Production<") ||
          "_Mandate<" in source

  private fun encodeOperation(
      world: World,
      inputs: List<GameplayInputEvent>,
      operationEvents: List<GameEvent>,
  ): List<Entry> {
    val actor = inputs.first().actor
    inputs
        .filter { it.kind == DIRECT_CHANGES }
        .takeIf(List<GameplayInputEvent>::isNotEmpty)
        ?.let {
          return it.map { input -> Correction(actor, normalizeDirect(world, actor, input.source)) }
        }
    val relevant = inputs.filterNot { it.kind == SELECT_TASK }
    if (relevant.isEmpty()) {
      return inputs.map { input ->
        val selection =
            input.taskNumber?.let { "$it select" } ?: normalizeChoice(world, actor, input.source)
        Call(actor, "tasks", listOf(selection))
      }
    }
    if (relevant.all { it.agent != null } && operationChangesPhase(operationEvents)) {
      return emptyList()
    }

    val play = relevant.firstOrNull { it.source.startsWith("PlayCard<Class<") }
    if (play != null) {
      val precedingUse = relevant.lastOrNull {
        it.ordinal < play.ordinal &&
            it.agent == null &&
            it.source.startsWith("UseAction<") &&
            !it.source.startsWith("UseAction<UseCardActionSA") &&
            !it.source.startsWith("UseAction<PlayCardSA")
      }
      if (precedingUse != null) {
        return encodeUseAction(
            world,
            actor,
            relevant.filter { it.ordinal < play.ordinal },
            operationEvents,
            precedingUse,
        ) +
            encodePlayCard(
                world,
                actor,
                relevant.filter { it.ordinal >= play.ordinal },
                operationEvents,
                play,
            )
      }
      return encodePlayCard(world, actor, relevant, operationEvents, play)
    }

    val use = relevant.lastOrNull {
      it.agent == null &&
          it.source.startsWith("UseAction<") &&
          !it.source.startsWith("UseAction<UseCardActionSA") &&
          !it.source.startsWith("UseAction<PlayCardSA")
    }
    if (use != null) return encodeUseAction(world, actor, relevant, operationEvents, use)

    if (relevant.any { it.source.startsWith("BuySelectedCards<") }) {
      val discard = relevant.firstOrNull { "ProjectCard<Selecting>" in it.source }
      return buildList {
        if (discard != null && !discard.source.endsWith(" Ok")) {
          add(Call(actor, "tasks", listOf(normalizeChoice(world, actor, discard.source))))
        }
        add(Call(actor, "buyCards"))
      }
    }
    relevant
        .firstOrNull { it.source == "Pass" }
        ?.let {
          return listOf(Call(actor, "tasks", listOf("Pass")))
        }
    relevant
        .firstOrNull { it.source.endsWith("! BY Engine") }
        ?.let {
          return listOf(Call(actor, "tasks", listOf(normalizeChoice(world, actor, it.source))))
        }
    relevant
        .firstOrNull { "<WildTagUse<" in it.source }
        ?.let {
          return listOf(Call(actor, "assignWildTag", listOf(it.source.substringBefore('<'))))
        }
    val explicitDecline = relevant.firstOrNull { it.agent == null && isOkInput(it) }
    if (explicitDecline != null && declinedFinalGreenery(operationEvents)) {
      return listOf(Call(actor, "tasks", listOf("${explicitDecline.taskNumber} Ok")))
    }
    if (relevant.all(::isOkInput)) return listOf(Call(actor, "endTurn"))

    val choices =
        relevant
            .filterNot { isPlumbing(it.source) }
            .map {
              normalizeChoice(world, actor, it.source)
            }
    return if (choices.isEmpty()) emptyList() else listOf(Call(actor, "tasks", choices))
  }

  private fun encodePlayCard(
      world: World,
      actor: Actor,
      inputs: List<GameplayInputEvent>,
      operationEvents: List<GameEvent>,
      play: GameplayInputEvent,
  ): List<Entry> {
    val card = checkNotNull(Regex("Class<([^>]+)>>$").find(play.source)?.groupValues?.get(1))
    val purchaseStart = purchaseStart(inputs)
    val headlineInputs = inputs.filter { purchaseStart == null || it.ordinal < purchaseStart }
    val costs = paymentCosts(world, actor, headlineInputs, operationEvents)
    val choices =
        headlineInputs
            .filter { it.ordinal > play.ordinal }
            .filterNot { isPlumbing(it.source) || isPayment(it.source) }
            .sortedBy { if (it.agent == null) 0 else 1 }
            .map { normalizeChoice(world, actor, it.source) }
            .filterNot(::isEmptyChoice)
    return buildList {
      add(Call(actor, "playCard", listOf(card) + costs))
      if (choices.isNotEmpty()) add(Call(actor, "tasks", canonicalChoiceOrder(choices)))
      addAll(encodePurchase(world, actor, inputs))
    }
  }

  private fun encodeUseAction(
      world: World,
      actor: Actor,
      inputs: List<GameplayInputEvent>,
      operationEvents: List<GameEvent>,
      use: GameplayInputEvent,
  ): List<Entry> {
    val purchaseStart = purchaseStart(inputs)
    val headlineInputs = inputs.filter { purchaseStart == null || it.ordinal < purchaseStart }
    val match =
        requireNotNull(
            Regex("UseAction<(?:[^,>]+, )?([^,>]+), (First|Second|Third)>").matchEntire(use.source)
        ) {
          "cannot encode action input: ${use.source}"
        }
    val provider = match.groupValues[1]
    val number = listOf("First", "Second", "Third").indexOf(match.groupValues[2]) + 1
    val costs = paymentCosts(world, actor, headlineInputs, operationEvents).toMutableList()
    val directCost =
        headlineInputs
            .dropWhile { it != use }
            .drop(1)
            .firstOrNull { it.source.startsWith('-') && provider in it.source }
    if (directCost != null) costs += normalizeChoice(world, actor, directCost.source)
    val choices =
        if (provider == "HandleMandates") {
          emptyList()
        } else {
          headlineInputs
              .filter { it.ordinal > use.ordinal }
              .filterNot {
                isPlumbing(it.source) ||
                    isPayment(it.source) ||
                    it == directCost ||
                    (it.agent != null && it.source.startsWith("UseAction<"))
              }
              .sortedBy { if (it.agent == null) 0 else 1 }
              .map { normalizeChoice(world, actor, it.source) }
              .filterNot(::isEmptyChoice)
        }
    return buildList {
      add(Call(actor, "useAction", listOf("$number", provider) + costs))
      if (choices.isNotEmpty()) {
        val ordered = if (provider == "TradeSA") orderTradeChoices(choices) else choices
        add(Call(actor, "tasks", canonicalChoiceOrder(ordered)))
      }
      addAll(encodePurchase(world, actor, inputs))
    }
  }

  private fun purchaseStart(inputs: List<GameplayInputEvent>): Int? =
      inputs
          .firstOrNull { input ->
            input.source.startsWith("Selecting<") ||
                ("ProjectCard<" in input.source && "Selecting" in input.source)
          }
          ?.ordinal

  private fun encodePurchase(
      world: World,
      actor: Actor,
      inputs: List<GameplayInputEvent>,
  ): List<Call> {
    if (inputs.none { it.source.startsWith("BuySelectedCards<") }) return emptyList()
    val discard = inputs.firstOrNull { input ->
      input.source.startsWith('-') && "ProjectCard<" in input.source && "Selecting" in input.source
    }
    return buildList {
      if (discard != null) {
        add(Call(actor, "tasks", listOf(normalizeChoice(world, actor, discard.source))))
      }
      add(Call(actor, "buyCards", emptyList()))
    }
  }

  private fun paymentCosts(
      world: World,
      actor: Actor,
      inputs: List<GameplayInputEvent>,
      operationEvents: List<GameEvent>,
  ): List<String> {
    val costs =
        inputs
            .filter { isPayment(it.source) }
            .mapNotNull { input ->
              val direct = Regex("^(\\d+) Pay<Class<([^>]+)>> FROM ([^ ]+)").find(input.source)
              if (direct != null) {
                val count = direct.groupValues[1]
                val resource = longResourceName(direct.groupValues[3])
                "-${if (count == "1" && resource != "MC") "" else "$count "}$resource"
              } else if (Regex("^(\\d+ )?PayFromCard<").containsMatchIn(input.source)) {
                val match =
                    checkNotNull(
                        Regex("^(\\d+ )?PayFromCard<([^>]+)> FROM ([^<]+)<([^>]+)>")
                            .find(input.source)
                    )
                "-${match.groupValues[1]}${longResourceName(match.groupValues[3])}" +
                    "<${match.groupValues[4]}>"
              } else {
                inferredPayment(world, actor, operationEvents, input)
              }
            }
    return costs
  }

  private fun inferredPayment(
      world: World,
      actor: Actor,
      events: List<GameEvent>,
      input: GameplayInputEvent,
  ): String? {
    val resource =
        Regex("Pay<Class<([^>]+)>> FROM ([^ /]+)").find(input.source)?.groupValues?.get(2)
            ?: return null
    val previousInputOrdinal =
        events
            .filterIsInstance<GameplayInputEvent>()
            .filter { it.ordinal < input.ordinal }
            .maxOfOrNull(GameplayInputEvent::ordinal) ?: input.operationStartOrdinal
    val count =
        events
            .filterIsInstance<ChangeEvent>()
            .filter { it.ordinal in (previousInputOrdinal + 1) until input.ordinal }
            .filter { it.actor == actor && it.change.removing?.className == cn(resource) }
            .sumOf { it.change.count }
    if (count == 0) return null
    val rendered = longResourceName(resource)
    return "-${if (count == 1 && rendered != "MC") "" else "$count "}$rendered"
  }

  private fun isPayment(source: String): Boolean =
      "Pay<Class<" in source || Regex("^(\\d+ )?PayFromCard<").containsMatchIn(source)

  private fun isPlumbing(source: String): Boolean =
      source.startsWith("UseAction<PlayCardSA") ||
          source.startsWith("UseAction<UseCardActionSA") ||
          source.startsWith("ActionUsedMarker<") ||
          source.startsWith("Owed<") ||
          source.contains(" Owed<") ||
          source.startsWith("HandleCardTags<") ||
          Regex("^(\\d+ )?PlayTag<").containsMatchIn(source) ||
          source.startsWith("CardInvoice<") ||
          source.startsWith("Invoice<") ||
          source.startsWith("MAX 0 Invoice<") ||
          source.startsWith("MAX 0 Barrier:") ||
          " FROM CorporationCard<Hand<" in source ||
          " FROM PreludeCard<Hand<" in source ||
          (source.contains(" FROM ProjectCard<Hand<") && !source.startsWith("PlayedEvent<")) ||
          source.startsWith("BuySelectedCards<") ||
          source.startsWith("BuyCard<") ||
          source.startsWith("-Selecting<") ||
          isOkInputSource(source)

  private fun isOkInput(input: GameplayInputEvent): Boolean = isOkInputSource(input.source)

  private fun isOkInputSource(source: String): Boolean = source == "Ok" || source.endsWith(" Ok")

  private fun isEmptyChoice(source: String): Boolean = source == "Ok"

  private fun normalizeDirect(world: World, actor: Actor, source: String): String =
      replacePlayers(world, source).replaceResourceAliases().let {
        if (it == "TR") "TerraformRating" else it
      }

  private fun normalizeChoice(world: World, actor: Actor, sourceIn: String): String {
    val actorName = world.vocabulary.petsName(actor)
    var source = replacePlayers(world, sourceIn).replaceResourceAliases()
    source = source.removeSuffix("!").removeSuffix("?").removeSuffix(".")
    source = source.replace(Regex("^1 "), "")
    source = source.replace(Regex("^-1 "), "-")
    source =
        Regex("(-?)(\\d+ )?([A-Za-z][A-Za-z0-9_]*)<([A-Za-z][A-Za-z0-9_]*)<$actorName>>").replace(
            source
        ) { match ->
          match.groupValues[1] +
              match.groupValues[2] +
              match.groupValues[3] +
              "<$actorName, ${match.groupValues[4]}>"
        }
    source =
        Regex("^CopyPrelude<[^,>]+, ([^>]+)>$").replace(source) { match ->
          "CopyPrelude<${match.groupValues[1]}>"
        }
    source =
        Regex("^PlayedEvent<$actorName, (Class<[^>]+>)> FROM ([A-Za-z0-9_]+)<$actorName>$").replace(
            source
        ) { match ->
          "PlayedEvent<${match.groupValues[1]}> FROM ${match.groupValues[2]}"
        }
    source =
        Regex("^(WildTag<$actorName, [A-Za-z0-9_]+)<$actorName>>$").replace(source) { match ->
          match.groupValues[1] + ">"
        }
    source =
        Regex("^(-?)(\\d+ )?Production<$actorName, Class<([^>]+)>>$").replace(source) { match ->
          val amount = match.groupValues[2].trim()
          val sign = match.groupValues[1]
          "PROD[$sign${if (amount.isEmpty() || amount == "1") "" else "$amount "}${match.groupValues[3]}]"
        }
    source =
        Regex("^(\\d+ )?ProjectCard<$actorName, Hand>$").replace(source) { match ->
          match.groupValues[1] + "ProjectCard"
        }
    source =
        Regex("^(\\d+ )?ProjectCard<$actorName, Selecting>$").replace(source) { match ->
          match.groupValues[1] + "ProjectCard<Selecting>"
        }
    source =
        Regex("^(\\d+ )?ProjectCard<Hand<$actorName>>$").replace(source) { match ->
          match.groupValues[1] + "ProjectCard"
        }
    source =
        Regex("^(-?)(\\d+ )?([A-Za-z][A-Za-z0-9_]*)<$actorName>$").replace(source) { match ->
          match.groupValues[1] + match.groupValues[2] + match.groupValues[3]
        }
    source =
        Regex("^(-?)(\\d+ )?([A-Za-z][A-Za-z0-9_]*)<([^,<>]+)>$").replace(source) { match ->
          val provider = match.groupValues[4]
          val playerNames =
              world.actors.filterIsInstance<Player>().map {
                world.vocabulary.petsName(it).toString()
              }
          if (
              provider.startsWith("Utopia_") ||
                  provider in setOf("Selecting", "Hand") ||
                  provider in playerNames
          ) {
            match.value
          } else {
            match.groupValues[1] +
                match.groupValues[2] +
                match.groupValues[3] +
                "<$actorName, $provider>"
          }
        }
    source = source.replace(Regex("^-ProjectCard$"), "-ProjectCard<Hand>")
    source =
        Regex("^(CityTile|GreeneryTile|OceanTile)<$actorName, ([^>]+)>$").replace(source) { match ->
          "${match.groupValues[1]}<${match.groupValues[2]}>"
        }
    source =
        Regex("^(CityTile|GreeneryTile|OceanTile)<([^,>]+), $actorName>$").replace(source) { match
          ->
          "${match.groupValues[1]}<${match.groupValues[2]}>"
        }
    source = source.replace("Trade<$actorName, ", "Trade<")
    source = source.replace("ResetColonyProduction<$actorName, ", "ResetColonyProduction<")
    source =
        Regex("(-?\\d* ?ColonyProduction)<$actorName, ([^>]+)>").replace(source) { match ->
          "${match.groupValues[1]}<${match.groupValues[2]}>"
        }
    if (source.startsWith("CopyPrelude<") && ", " in source) {
      source = "CopyPrelude<${source.substringAfter(", ")}"
    }
    return source
  }

  private fun replacePlayers(world: World, source: String): String =
      world.actors.filterIsInstance<Player>().fold(source) { current, player ->
        current.replace(player.toString(), world.vocabulary.petsName(player).toString())
      }

  private fun String.replaceResourceAliases(): String =
      replace(Regex("(?<![A-Za-z])M(?=([<,>\\] ]|$))"), "MC")
          .replace(Regex("(?<![A-Za-z])S(?=([<,>\\] ]|$))"), "Steel")
          .replace(Regex("(?<![A-Za-z])T(?=([<,>\\] ]|$))"), "Titanium")
          .replace(Regex("(?<![A-Za-z])P(?=([<,>\\] ]|$))"), "Plant")
          .replace(Regex("(?<![A-Za-z])E(?=([<,>\\] ]|$))"), "Energy")
          .replace(Regex("(?<![A-Za-z])H(?=([<,>\\] ]|$))"), "Heat")

  private fun longResourceName(name: String): String =
      when (name) {
        "M" -> "MC"
        "S" -> "Steel"
        "T" -> "Titanium"
        "P" -> "Plant"
        "E" -> "Energy"
        "H" -> "Heat"
        else -> name
      }

  private fun canonicalChoiceOrder(choices: List<String>): List<String> {
    val result = mutableListOf<String>()
    val ordered =
        choices.filterNot { it.startsWith("PlayedEvent<") } +
            choices.filter { it.startsWith("PlayedEvent<") }
    ordered.forEach { choice ->
      val production = Regex("PROD\\[([^]]+)]").matchEntire(choice)
      val previous = result.lastOrNull()?.let { Regex("PROD\\[([^]]+)]").matchEntire(it) }
      if (production != null && previous != null) {
        val parts =
            listOf(previous.groupValues[1], production.groupValues[1]).sortedBy {
              if (it.trimStart().startsWith('-')) 0 else 1
            }
        result[result.lastIndex] = "PROD[${parts.joinToString()}]"
      } else {
        result += choice
      }
    }
    return interleaveGlobalSteps(result)
  }

  private fun interleaveGlobalSteps(choices: List<String>): List<String> {
    val firstStep = choices.indexOfFirst { it.endsWith("Step") }
    val steps = choices.filter { it.endsWith("Step") }
    val ratings = choices.filter { it == "TerraformRating" }
    if (firstStep >= 0 && steps.size > 1 && steps.size == ratings.size) {
      val prefix = choices.take(firstStep)
      val remainder =
          choices.drop(firstStep).filterNot { it.endsWith("Step") || it == "TerraformRating" }
      val productions = remainder.filter { it.startsWith("PROD[") }
      val playedEvents = remainder.filter { it.startsWith("PlayedEvent<") }
      val others = remainder - productions.toSet() - playedEvents.toSet()
      return prefix +
          steps.flatMap { listOf(it, "TerraformRating") } +
          productions +
          others +
          playedEvents
    }
    val result = mutableListOf<String>()
    var index = 0
    while (index < choices.size) {
      val stepCount = choices.drop(index).takeWhile { it.endsWith("Step") }.size
      val ratingCount = choices.drop(index + stepCount).takeWhile { it == "TerraformRating" }.size
      if (stepCount > 1 && stepCount == ratingCount) {
        repeat(stepCount) { offset ->
          result += choices[index + offset]
          result += "TerraformRating"
        }
        index += stepCount + ratingCount
      } else {
        result += choices[index++]
      }
    }
    return result
  }

  private fun orderTradeChoices(choices: List<String>): List<String> {
    val choosesColonyProduction = choices.any { it.startsWith("ColonyProduction<") }
    return choices.sortedBy { choice ->
      when {
        choice.startsWith("Trade<") -> 0
        choosesColonyProduction && choice.startsWith("ColonyProduction<") -> 1
        choice.startsWith("ResetColonyProduction<") -> 4
        choice.contains("ColonyProduction<") -> 5
        choosesColonyProduction -> 3
        else -> 2
      }
    }
  }

  private fun operationChangesPhase(events: List<GameEvent>): Boolean =
      events.filterIsInstance<ChangeEvent>().any { event ->
        event.change.gaining?.className?.toString()?.let {
          it == "Generation" || it.endsWith("Phase")
        } == true
      }

  private fun declinedFinalGreenery(events: List<GameEvent>): Boolean =
      events.filterIsInstance<TaskRemovedEvent>().any { event ->
        event.task.cause?.context?.className == cn("FinalGreeneryPhase")
      }
}
