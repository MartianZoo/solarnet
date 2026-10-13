package dev.martianzoo.tfm.web.playground

import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.SystemClasses.REAL
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.Component
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameRecording
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.canon.TfmClasses.MC
import dev.martianzoo.tfm.state.ApiUtils.mapDefinition

private val resourceNames = listOf("MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
private val tagNames =
    listOf(
        "BuildingTag",
        "SpaceTag",
        "ScienceTag",
        "PowerTag",
        "EarthTag",
        "JovianTag",
        "VenusTag",
        "PlantTag",
        "MicrobeTag",
        "AnimalTag",
        "CityTag",
        "WildTag",
        "EventTag",
    )

/** Reads the world at a completed recording position into the existing workspace's display data. */
internal fun liveSpecimen(
    name: String,
    recording: GameRecording.Playback,
    changes: List<ChangeEvent>,
): Specimen {
  val game = recording.world
  val reader = game.reader
  val queries = GameQueries(reader)
  val allClasses = reader.classTable.allClasses()
  val real = reader.resolve(REAL.expression)
  val system = reader.resolve(cn("System").expression)
  val signal = reader.resolve(cn("Signal").expression)
  val componentEntries = reader.getComponents(cn("Component").expression).entries

  fun types(name: String): List<Type> = runCatching {
    reader.getComponents(cn(name).expression).elements.toList()
  }
      .getOrDefault(emptyList())
  fun count(name: String): Int = runCatching {
    reader.count(reader.resolve(cn(name).expression))
  }
      .getOrDefault(0)
  fun firstDependency(name: String): String? =
      types(name).singleOrNull()?.typeDependencies?.firstOrNull()?.boundType?.className?.toString()
  fun playerCount(player: Player, name: String): Int =
      try {
        queries.count(player, name)
      } catch (_: ExpressionException) {
        0
      }

  val players =
      game.actors.filterIsInstance<Player>().map { player ->
        val playedCards = playedCards(game, player)
        val played = playedCards.map { it.className.toString() }
        Specimen.PlayerState(
            name = player.className.toString(),
            terraformRating = playerCount(player, "TerraformRating"),
            handCards = playerCount(player, "ProjectCard"),
            resources =
                resourceNames.associateWith { resource ->
                  listOf(
                      playerCount(player, resource),
                      runCatching { queries.production(player, cn(resource)) }.getOrDefault(0),
                  )
                },
            tags = tagNames.associateWith { playerCount(player, it) },
            played = played,
            playedWithArt = played,
            usedActionCards =
                playedCards
                    .filter { hasActionUsedMarker(reader, player, it) }
                    .mapTo(mutableSetOf()) { it.className.toString() },
        )
      }

  val phase = types("Phase").singleOrNull()?.className?.toString()?.removeSuffix("Phase")
  val onTurn =
      if (phase == "Action") {
        game.events
            .changesSinceSetup()
            .asReversed()
            .firstOrNull {
              it.change.gaining?.className?.toString() == "NewTurn"
            }
            ?.change
            ?.gaining
            ?.let { Component(it.type).owningPlayer?.className?.toString() }
      } else null
  val global =
      Specimen.GlobalState(
          generation = count("Generation"),
          phase = phase,
          playerOnTurn = onTurn,
          startingPlayer =
              types("StartToken").singleOrNull()?.let {
                Component(it).owningPlayer?.className?.toString()
              },
          temperatureSteps = count("TemperatureStep"),
          oxygenSteps = count("OxygenStep"),
          oceans = count("OceanTile"),
          venusSteps = count("VenusStep"),
          ruling = firstDependency("Ruling"),
          dominant = firstDependency("Dominant"),
          chairman = firstDependency("Chairman"),
          currentGlobalEvent = firstDependency("Current"),
          comingGlobalEvent = firstDependency("Coming"),
          distantGlobalEvent = firstDependency("Distant"),
      )

  fun concreteSubclasses(name: String): List<Class> {
    val root = reader.classTable.findClass(cn(name)) ?: return emptyList()
    return allClasses
        .filter { !it.abstract && it.isSubtypeOf(root) }
        .sortedBy { it.className.toString() }
  }
  val milestones =
      concreteSubclasses("Milestone").map { candidate ->
        val owner =
            componentEntries
                .firstOrNull { (type, _) -> type.className == candidate.className }
                ?.key
                ?.let { Component(it).owningPlayer?.className?.toString() }
        Specimen.Milestone(candidate.className.toString(), owner)
      }
  val awards =
      concreteSubclasses("Award").map { candidate ->
        Specimen.Award(
            candidate.className.toString(),
            componentEntries.any { (type, _) ->
              type.className == candidate.className
            },
        )
      }

  val parties = concreteSubclasses("Party").map { it.className.toString() }
  val delegates =
      types("PartyDelegate").map { type ->
        val deps = type.typeDependencies.map { it.boundType.className.toString() }
        Specimen.Turmoil.Delegates(deps.last(), deps.first(), reader.countComponent(type))
      }
  val leaders =
      types("PartyLeader").associate { type ->
        val deps = type.typeDependencies.map { it.boundType.className.toString() }
        deps.last() to deps.first()
      }

  val tasks =
      game.tasks.extract { task ->
        Specimen.PendingTask(
            id = task.id.ordinal,
            instruction = task.instruction.toString(),
            then = task.then?.toString(),
            givenBy = task.cause?.context?.toString(),
            because = task.cause?.triggerEvent,
            assignee = task.assignee.toString(),
            selected = task.selected,
        )
      }
  val events = changes.map { event ->
    val change = event.change
    Specimen.LoggedChange(
        ordinal = event.ordinal,
        actor = event.actor.toString(),
        count = change.count,
        gaining = change.gaining?.toString(),
        removing = change.removing?.toString(),
        real =
            listOfNotNull(change.gaining, change.removing).any {
              it.type.isSubtypeOf(real)
            },
        signal = change.gaining?.type?.isSubtypeOf(signal) == true,
        givenBy = event.cause?.context?.toString(),
        because = event.cause?.triggerEvent,
    )
  }

  val components = componentEntries.map { (type, amount) ->
    Specimen.PresentComponent(
        type = type.toString(),
        count = amount,
        className = type.className.toString(),
        nonReal = !type.isSubtypeOf(real),
        system = type.isSubtypeOf(system),
        dependencies =
            type.typeDependencies.associate { dep ->
              "${dep.key.declaringClass}_${dep.key.index}" to dep.boundType.toString()
            },
    )
  }
  val classes = allClasses.map { candidate ->
    Specimen.ClassInfo(
        name = candidate.className.toString(),
        abstract = candidate.abstract,
        superclasses = candidate.directSuperclasses.map { it.className.toString() },
        baseType = candidate.baseType.toString(),
        dependencyKeys = candidate.dependencies.keys.map { "${it.declaringClass}_${it.index}" },
    )
  }

  val tiles = types("Tile")
  val map = runCatching {
    mapDefinition(reader)
  }
      .getOrNull()
      ?.areas
      .orEmpty()
      .map { area ->
        val tile = tiles.firstOrNull { type ->
          type.typeDependencies.any { it.boundType.className == area.className }
        }
        Specimen.MapArea(
            row = area.row,
            column = area.column,
            kind = area.kind.toString(),
            tile = tile?.className?.toString(),
            owner = tile?.let { Component(it).owningPlayer?.className?.toString() },
            bonus =
                area.bonus?.instructions.orEmpty().flatMap { instruction ->
                  val change = instruction as? Change ?: return@flatMap emptyList()
                  val amount = (change.count as? ActualScalar)?.value ?: return@flatMap emptyList()
                  when {
                    change.gaining?.className == MC && amount in 1..49 ->
                        listOf(
                            Specimen.MapBonus(image = "MC/MC${amount.toString().padStart(2, '0')}")
                        )
                    change.gaining != null ->
                        List(amount) {
                          Specimen.MapBonus(image = change.gaining!!.className.toString())
                        }
                    change.removing?.className == MC && amount in 1..9 ->
                        listOf(Specimen.MapBonus(image = "MC/MC-$amount"))
                    change.removing?.className == MC ->
                        listOf(Specimen.MapBonus(label = "−$amount"))
                    else -> emptyList()
                  }
                },
        )
      }
  val colonyProduction = types("ColonyProduction")
  val playerColonies = types("Colony")
  val colonies =
      types("ColonyTile").map { tile ->
        Specimen.ColonyState(
            name = tile.className.toString(),
            production =
                colonyProduction.sumOf { marker ->
                  if (marker.typeDependencies.any { it.boundType.className == tile.className })
                      reader.countComponent(marker)
                  else 0
                },
            owners =
                playerColonies
                    .filter { colony ->
                      colony.typeDependencies.any { it.boundType.className == tile.className }
                    }
                    .mapNotNull { Component(it).owningPlayer?.className?.toString() },
        )
      }

  return Specimen(
      source = Specimen.Source(name, recording.positions[recording.positionIndex].ordinal),
      players = players,
      global = global,
      milestones = milestones,
      awards = awards,
      turmoil = Specimen.Turmoil(parties, delegates, leaders),
      tasks = tasks,
      events = events,
      components = components,
      classes = classes,
      map = map,
      colonies = colonies,
  )
}
