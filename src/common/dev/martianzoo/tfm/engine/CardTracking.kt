package dev.martianzoo.tfm.engine

import dev.martianzoo.engine.AgentInputRequiredException
import dev.martianzoo.engine.AgentTransactionGuard
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.state.Component
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.Player
import dev.martianzoo.state.toComponent
import dev.martianzoo.tfm.canon.cardBack
import dev.martianzoo.tfm.canon.cardTags
import dev.martianzoo.tfm.state.tfmCatalog

/**
 * Opt-in, game-wide record of named cards. Call [start] before the first hand movement, then supply
 * names before the Agent operation that needs them. Every successful Agent operation names all
 * CardBack movements into or out of Hand; missing names fail atomically. When [start] enables
 * Selecting tracking, every Selecting movement must also be named. A discarded card may enter the
 * game again. A card cannot occupy a tracked Hand, Selecting area, or played location twice. Direct
 * plays from Selecting are not checked against previously seen cards when Selecting tracking is
 * disabled. Supplied names must be concrete catalog CardFront classes with the matching CardBack.
 * Each named change gets a `Cards:` event note; wrong names or missing required names roll back the
 * Agent request. Names queued for a later change are consumed in that Player's per-back,
 * per-location event order.
 *
 * The current event history and supplied name sequences are the sources of truth. Validation
 * rebuilds the named locations from surviving events, so timeline rollback also restores tracking.
 * This tracks client-declared identities; it does not deal cards or verify a physical reshuffle.
 */
public class CardTracking
private constructor(
    private val world: World,
    private val requireSelectingNames: Boolean,
) : AgentTransactionGuard {
  private val arrivals =
      mutableMapOf<Triple<Player, ClassName, ClassName>, MutableList<ClassName>>()
  private val departures =
      mutableMapOf<Triple<Player, ClassName, ClassName>, MutableList<ClassName>>()

  /**
   * Supplies names in the order cards of [back] next enter [player]'s [location]. Omit [back] for
   * project cards and [location] for Hand. Selecting names require Selecting tracking at [start].
   */
  public fun expectArrivals(
      player: Player,
      vararg cards: ClassName,
      back: ClassName = PROJECT_CARD,
      location: ClassName = HAND,
  ) {
    requirePlayer(player)
    requireBack(back)
    requireLocation(location)
    requireCards(cards, back)
    arrivals.getOrPut(Triple(player, back, location)) { mutableListOf() }.addAll(cards)
  }

  /** Replaces unused arrival names for this Player, back, and location. */
  public fun replacePendingArrivals(
      player: Player,
      vararg cards: ClassName,
      back: ClassName = PROJECT_CARD,
      location: ClassName = HAND,
  ) {
    requirePlayer(player)
    requireBack(back)
    requireLocation(location)
    requireCards(cards, back)
    val key = Triple(player, back, location)
    val used = snapshot().arrivalOffsets.getOrDefault(key, 0)
    val names = arrivals.getOrPut(key) { mutableListOf() }
    names.subList(used, names.size).clear()
    names.addAll(cards)
  }

  /**
   * Supplies names for upcoming anonymous departures of [back] from [player]'s [location]. Omit
   * [back] for project cards and [location] for Hand.
   */
  public fun expectDepartures(
      player: Player,
      vararg cards: ClassName,
      back: ClassName = PROJECT_CARD,
      location: ClassName = HAND,
  ) {
    requirePlayer(player)
    requireBack(back)
    requireLocation(location)
    requireCards(cards, back)
    departures.getOrPut(Triple(player, back, location)) { mutableListOf() }.addAll(cards)
  }

  /** Replaces unused departure names for this Player, back, and location. */
  public fun replacePendingDepartures(
      player: Player,
      vararg cards: ClassName,
      back: ClassName = PROJECT_CARD,
      location: ClassName = HAND,
  ) {
    requirePlayer(player)
    requireBack(back)
    requireLocation(location)
    requireCards(cards, back)
    val key = Triple(player, back, location)
    val used = snapshot().departureOffsets.getOrDefault(key, 0)
    val names = departures.getOrPut(key) { mutableListOf() }
    names.subList(used, names.size).clear()
    names.addAll(cards)
  }

  /** Snapshot of the named cards currently in [player]'s hand, optionally limited to [back]. */
  public fun hand(player: Player, back: ClassName? = null): Set<ClassName> {
    requirePlayer(player)
    if (back != null) requireBack(back)
    return snapshot()
        .locations
        .filterValues {
          it.location == Hand(player) && (back == null || it.back == back)
        }
        .keys
  }

  /** Requires every supplied name to have been used; movement checks run per Agent request. */
  public fun requireComplete() {
    val result = snapshot()
    arrivals.forEach { (key, names) ->
      check(result.arrivalOffsets.getOrDefault(key, 0) == names.size) {
        "unused ${key.second} ${key.third} arrivals for ${key.first}: " +
            names.drop(result.arrivalOffsets.getOrDefault(key, 0))
      }
    }
    departures.forEach { (key, names) ->
      check(result.departureOffsets.getOrDefault(key, 0) == names.size) {
        "unused ${key.second} ${key.third} departures for ${key.first}: " +
            names.drop(result.departureOffsets.getOrDefault(key, 0))
      }
    }
  }

  override fun validate() {
    snapshot()
  }

  private fun snapshot(): Snapshot {
    val locations = linkedMapOf<ClassName, CardState>()
    val arrivalOffsets = mutableMapOf<Triple<Player, ClassName, ClassName>, Int>()
    val departureOffsets = mutableMapOf<Triple<Player, ClassName, ClassName>, Int>()
    val notes = mutableMapOf<ChangeEvent, String>()

    fun takeNames(
        supplied: Map<Triple<Player, ClassName, ClassName>, List<ClassName>>,
        offsets: MutableMap<Triple<Player, ClassName, ClassName>, Int>,
        player: Player,
        back: ClassName,
        location: ClassName,
        count: Int,
        event: ChangeEvent,
        direction: String,
    ): List<ClassName> {
      val key = Triple(player, back, location)
      val names = supplied[key].orEmpty()
      val start = offsets.getOrDefault(key, 0)
      val end = start + count
      if (end > names.size) {
        throw AgentInputRequiredException(
            "$player must name $count $back $direction at event ${event.ordinal}"
        )
      }
      offsets[key] = end
      return names.subList(start, end)
    }

    for (event in world.events.changesSinceSetup()) {
      val change = event.change
      val removing = change.removing
      val gaining = change.gaining
      val removingBack = removing.cardBackAt(HAND)
      val gainingBack = gaining.cardBackAt(HAND)
      val playedFace =
          gaining?.className?.takeIf { name ->
            world.classTable.getClass(name).isSubtypeOf(world.classTable.getClass(CARD_FRONT))
          }
      val selectingRemovalBack = removing.cardBackAt(SELECTING).takeIf { requireSelectingNames }
      val selectingGainBack = gaining.cardBackAt(SELECTING).takeIf { requireSelectingNames }
      val handRemoval = removingBack != null
      val handGain = gainingBack != null
      var removedNames = emptyList<ClassName>()
      var gainedNames = emptyList<ClassName>()
      var selectedRemovedNames = emptyList<ClassName>()
      var selectedGainedNames = emptyList<ClassName>()

      if (handRemoval) {
        val player = checkNotNull(removing?.owningPlayer)
        removedNames =
            if (playedFace != null) {
              check(change.count == 1) { "one played face cannot consume ${change.count} cards" }
              listOf(playedFace)
            } else {
              takeNames(
                  departures,
                  departureOffsets,
                  player,
                  checkNotNull(removingBack),
                  HAND,
                  change.count,
                  event,
                  "departures",
              )
            }
        check(removedNames.size == removedNames.distinct().size) {
          "the same card leaves $player's hand twice in $event"
        }
        removedNames.forEach { name ->
          check(locations[name] == CardState(checkNotNull(removingBack), Hand(player))) {
            "$player moved $name from Hand, but its tracked location is ${locations[name]} at $event"
          }
          val location =
              when {
                gaining.cardBackAt(REVEALED) == removingBack -> Revealed(player)
                requireSelectingNames && gaining.cardBackAt(SELECTING) == removingBack ->
                    Selecting(player)
                playedFace != null -> Played(player)
                else -> Outside
              }
          locations[name] = CardState(checkNotNull(removingBack), location)
        }
      }

      if (handGain) {
        val player = checkNotNull(gaining?.owningPlayer)
        gainedNames =
            when {
              handRemoval -> removedNames
              removing?.className == PLAYED_EVENT -> {
                check(change.count == 1) { "one played event cannot return ${change.count} cards" }
                listOf(checkNotNull(removing.trackedName(locations.keys)))
              }
              removing.cardBackAt(REVEALED) != null -> {
                val revealed =
                    locations
                        .filterValues {
                          it == CardState(checkNotNull(gainingBack), Revealed(player))
                        }
                        .keys
                        .toList()
                if (revealed.size == change.count) revealed
                else
                    takeNames(
                        arrivals,
                        arrivalOffsets,
                        player,
                        gainingBack,
                        HAND,
                        change.count,
                        event,
                        "returns",
                    )
              }
              else ->
                  takeNames(
                      arrivals,
                      arrivalOffsets,
                      player,
                      checkNotNull(gainingBack),
                      HAND,
                      change.count,
                      event,
                      "arrivals",
                  )
            }
        check(gainedNames.size == gainedNames.distinct().size) {
          "the same card enters $player's hand twice in $event"
        }
        gainedNames.forEach { name ->
          val previous = locations[name]
          check(
              when {
                removing?.className == PLAYED_EVENT ->
                    previous == CardState(gainingBack, Played(player))
                removing.cardBackAt(REVEALED) != null ->
                    previous == CardState(gainingBack, Revealed(player))
                removing.cardBackAt(SELECTING) != null && requireSelectingNames ->
                    previous == CardState(gainingBack, Selecting(player))
                else -> previous == null || previous == CardState(gainingBack, Outside)
              }
          ) {
            "$name cannot enter $player's hand from its tracked location $previous at $event"
          }
          locations[name] = CardState(checkNotNull(gainingBack), Hand(player))
        }
        val context = event.cause?.context
        if (gainingBack == PROJECT_CARD && context?.className in FILTERED_HAND_ARRIVALS) {
          val filter =
              context!!.arguments.single {
                world.classTable
                    .getClass(it.className)
                    .isSubtypeOf(world.classTable.getClass(CARD_FILTER))
              }
          gainedNames.forEach { name ->
            check(matchesFilter(name, filter)) {
              "$name does not satisfy $filter at event ${event.ordinal}"
            }
          }
        }
      }

      if (selectingRemovalBack != null) {
        val player = checkNotNull(removing?.owningPlayer)
        val back = selectingRemovalBack
        if (handGain) check(gainingBack == back) { "a selected card changed backs at $event" }
        selectedRemovedNames =
            when {
              handGain -> gainedNames
              playedFace != null -> {
                check(change.count == 1) { "one played face cannot consume ${change.count} cards" }
                requireCards(arrayOf(playedFace), back)
                listOf(playedFace)
              }
              else ->
                  takeNames(
                      departures,
                      departureOffsets,
                      player,
                      back,
                      SELECTING,
                      change.count,
                      event,
                      "Selecting departures",
                  )
            }
        check(selectedRemovedNames.size == selectedRemovedNames.distinct().size) {
          "the same card leaves $player's Selecting twice in $event"
        }
        selectedRemovedNames.forEach { name ->
          if (!handGain) {
            check(locations[name] == CardState(back, Selecting(player))) {
              "$name was not in $player's Selecting at $event"
            }
            locations[name] = CardState(back, if (playedFace == name) Played(player) else Outside)
          }
        }
      }

      if (selectingGainBack != null) {
        val player = checkNotNull(gaining?.owningPlayer)
        val back = selectingGainBack
        selectedGainedNames =
            if (handRemoval) removedNames
            else
                takeNames(
                    arrivals,
                    arrivalOffsets,
                    player,
                    back,
                    SELECTING,
                    change.count,
                    event,
                    "Selecting arrivals",
                )
        check(selectedGainedNames.size == selectedGainedNames.distinct().size) {
          "the same card enters $player's Selecting twice in $event"
        }
        selectedGainedNames.forEach { name ->
          if (!handRemoval) {
            val previous = locations[name]
            check(previous == null || previous == CardState(back, Outside)) {
              "$name cannot enter $player's Selecting from $previous at $event"
            }
            locations[name] = CardState(back, Selecting(player))
          }
        }
      }

      if (
          handGain ||
              handRemoval ||
              selectedGainedNames.isNotEmpty() ||
              selectedRemovedNames.isNotEmpty()
      ) {
        val gained = if (handGain) gainedNames else selectedGainedNames
        val removed = if (handRemoval) removedNames else selectedRemovedNames
        check(gained.isEmpty() || removed.isEmpty() || gained == removed) {
          "a card changed identity at $event: $removed became $gained"
        }
        notes[event] = "Cards: ${(gained.ifEmpty { removed }).joinToString()}"
      }
      // PlayedEvent already names its face. Keep the location of a previously held card current
      // for later Hand checks; no name needs to be supplied or written on this event.
      if (!handGain && removing?.className == PLAYED_EVENT) {
        removing.trackedName(locations.keys)?.let { name ->
          val current = checkNotNull(locations[name])
          check(current.location == Played(checkNotNull(removing.owningPlayer))) {
            "$name cannot leave PlayedEvent from ${locations[name]} at $event"
          }
          locations[name] = current.copy(location = Outside)
        }
      } else if (!handGain && gaining?.className == PLAYED_EVENT) {
        gaining.trackedName(locations.keys)?.let { name ->
          val player = checkNotNull(gaining.owningPlayer)
          val current = checkNotNull(locations[name])
          check(current.location == Outside || current.location is Played) {
            "$name cannot enter PlayedEvent from ${locations[name]} at $event"
          }
          locations[name] = current.copy(location = Played(player))
        }
      } else if (!handRemoval && gaining?.className != PLAYED_EVENT) {
        removing
            ?.className
            ?.takeIf { it in locations }
            ?.let { name ->
              val current = checkNotNull(locations[name])
              check(current.location == Played(checkNotNull(removing.owningPlayer))) {
                "$name cannot leave play from ${locations[name]} at $event"
              }
              locations[name] = current.copy(location = Outside)
            }
      }
    }

    val modeled =
        world.reader
            .getComponents(CARD_BACK.expression)
            .entries
            .filter { (type, _) -> type.expressionFull.arguments.any { it.className == HAND } }
            .groupBy { (type, _) -> type.toComponent().owningPlayer to type.className }
            .mapValues { (_, entries) -> entries.sumOf { it.value } }
    val named =
        locations.values
            .filter { it.location is Hand }
            .groupingBy {
              (it.location as Hand).player to it.back
            }
            .eachCount()
    check(named == modeled) { "named hand cards $named differ from modeled hand cards $modeled" }
    if (requireSelectingNames) {
      val modeledSelecting =
          world.reader
              .getComponents(CARD_BACK.expression)
              .entries
              .filter { (type, _) ->
                type.expressionFull.arguments.any { it.className == SELECTING }
              }
              .groupBy { (type, _) -> type.toComponent().owningPlayer to type.className }
              .mapValues { (_, entries) -> entries.sumOf { it.value } }
      val namedSelecting =
          locations.values
              .filter { it.location is Selecting }
              .groupingBy { (it.location as Selecting).player to it.back }
              .eachCount()
      check(namedSelecting == modeledSelecting) {
        "named Selecting cards $namedSelecting differ from modeled Selecting cards $modeledSelecting"
      }
    }
    notes.forEach { (event, cardNote) ->
      var inserted = false
      val lines =
          event.notes
              ?.lineSequence()
              ?.mapNotNull { line ->
                if (!line.startsWith("Cards: ")) line
                else if (!inserted) {
                  inserted = true
                  cardNote
                } else null
              }
              ?.toList()
              .orEmpty()
      event.notes = (lines + if (inserted) emptyList() else listOf(cardNote)).joinToString("\n")
    }
    return Snapshot(locations, arrivalOffsets, departureOffsets)
  }

  private fun matchesFilter(cardName: ClassName, filter: Expression): Boolean {
    val catalog = world.reader.tfmCatalog
    val card = catalog.card(cardName)
    return when (filter.className) {
      cn("TagFilter") -> {
        val tag =
            catalog.classTable.getClass(filter.arguments.single().arguments.single().className)
        cardTags(card).elements.any { catalog.classTable.getClass(it).isSubtypeOf(tag) }
      }
      cn("NoTagsFilter") -> cardTags(card).isEmpty()
      cn("ReferenceFilter") -> {
        val reference =
            catalog.classTable.getClass(filter.arguments.single().arguments.single().className)
        card.declaration.allNodes.any { node ->
          node.descendantsOfType<ClassName>().any {
            catalog.classTable.findClass(it)?.isSubtypeOf(reference) == true
          }
        }
      }
      else -> error("unrecognized card filter: $filter")
    }
  }

  private fun requirePlayer(player: Player) {
    require(player in world.actors) { "$player is not seated in this game" }
  }

  private fun requireBack(back: ClassName) {
    require(world.classTable.getClass(back).isSubtypeOf(world.classTable.getClass(CARD_BACK))) {
      "$back is not a CardBack"
    }
  }

  private fun requireLocation(location: ClassName) {
    require(location == HAND || location == SELECTING) {
      "$location must be Hand or Selecting"
    }
    require(location != SELECTING || requireSelectingNames) {
      "start Selecting tracking before supplying Selecting names"
    }
  }

  private fun requireCards(cards: Array<out ClassName>, back: ClassName) {
    // A replay may name a genuine source card excluded from this game's playable pool.
    val classes = world.reader.tfmCatalog.classTable
    val expectedBack = classes.getClass(back)
    val cardFront = classes.getClass(CARD_FRONT)
    cards.forEach { name ->
      val card = classes.findClass(name)
      require(
          card != null &&
              !card.abstract &&
              card.isSubtypeOf(cardFront) &&
              cardBack(card)?.isSubtypeOf(expectedBack) == true
      ) {
        "$name is not a concrete CardFront with a $back back in the catalog"
      }
    }
  }

  private fun Component?.cardBackAt(location: ClassName): ClassName? =
      this?.className?.takeIf { back ->
        expressionFull.arguments.any { it.className == location } &&
            world.classTable.getClass(back).isSubtypeOf(world.classTable.getClass(CARD_BACK))
      }

  private fun Component.trackedName(known: Set<ClassName>): ClassName? =
      expressionFull.descendantsOfType<ClassName>().firstOrNull { it in known }

  private sealed interface Location

  private data class CardState(val back: ClassName, val location: Location)

  private data class Hand(val player: Player) : Location

  private data class Selecting(val player: Player) : Location

  private data class Revealed(val player: Player) : Location

  private data class Played(val player: Player) : Location

  private data object Outside : Location

  private data class Snapshot(
      val locations: Map<ClassName, CardState>,
      val arrivalOffsets: Map<Triple<Player, ClassName, ClassName>, Int>,
      val departureOffsets: Map<Triple<Player, ClassName, ClassName>, Int>,
  )

  public companion object {
    private val CARD_BACK = cn("CardBack")
    private val PROJECT_CARD = cn("ProjectCard")
    private val PLAYED_EVENT = cn("PlayedEvent")
    private val CARD_FRONT = cn("CardFront")
    private val CARD_FILTER = cn("CardFilter")
    private val HAND = cn("Hand")
    private val SELECTING = cn("Selecting")
    private val REVEALED = cn("Revealed")
    private val FILTERED_HAND_ARRIVALS = setOf(cn("SearchForCard"), cn("TakeSelectedCard"))

    /**
     * Starts mandatory Hand tracking before the first hand movement. When [requireSelectingNames]
     * is true, start before the first Selecting movement too; every such movement must be named.
     * Otherwise Selecting movements are outside tracking and Selecting names cannot be supplied.
     */
    public fun start(world: World, requireSelectingNames: Boolean = false): CardTracking {
      val tracker = CardTracking(world, requireSelectingNames)
      check(
          world.events.changesSinceSetup().none { event ->
            with(tracker) {
              event.change.gaining.cardBackAt(HAND) != null ||
                  event.change.removing.cardBackAt(HAND) != null ||
                  (requireSelectingNames &&
                      (event.change.gaining.cardBackAt(SELECTING) != null ||
                          event.change.removing.cardBackAt(SELECTING) != null))
            }
          }
      ) {
        "tracked card movements already occurred before tracking started"
      }
      world.installAgentTransactionGuard(tracker)
      return tracker
    }
  }
}
