package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.types.Class

/** Terraforming Mars component descriptions supplied to the structural English renderer. */
internal object TerraformingMarsDescribers {
  internal val descriptions: Map<Class, ComponentDescriber> by lazy {
    Canon.classTable.allClasses().associateWith(::resolve)
  }

  private val declarations: Map<Class, ComponentDescriber> by lazy {
    mapOf(
        klass("Component") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.ClassName,
            ),
        klass("StandardResource") to ComponentDescriber(standardResource = true),
        klass("Megacredit") to ComponentDescriber(noun = ComponentDescriber.Noun.Fixed("M€")),
        klass("Plant") to
            ComponentDescriber(noun = ComponentDescriber.Noun.Counted("plant", "plants")),
        klass("CardResource") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Counted("resource", "resources"),
                cardResource = ComponentDescriber.CardResource.SUFFIXED,
            ),
        klass("CardFront") to ComponentDescriber(cardResourceHolder = "card", playedCard = true),
        klass("MarsArea") to ComponentDescriber(metricLocation = "on Mars"),
        klass("Animal") to
            ComponentDescriber(cardResource = ComponentDescriber.CardResource.ORDINARY),
        klass("Asteroid") to
            ComponentDescriber(cardResource = ComponentDescriber.CardResource.ORDINARY),
        klass("Floater") to
            ComponentDescriber(cardResource = ComponentDescriber.CardResource.ORDINARY),
        klass("Microbe") to
            ComponentDescriber(cardResource = ComponentDescriber.CardResource.ORDINARY),
        klass("Tag") to ComponentDescriber(tag = ComponentDescriber.Tag.ORDINARY),
        klass("PlanetTag") to ComponentDescriber(tag = ComponentDescriber.Tag.PLANET),
        klass("OxygenStep") to
            ComponentDescriber(
                track = ComponentDescriber.Track("oxygen"),
                requirement =
                    requirement(
                        minimum = { expression, target ->
                          ifSimple(expression) { "Requires $target% oxygen." }
                        },
                        maximum = { expression, target ->
                          ifSimple(expression) { "Oxygen must be $target% or less." }
                        },
                    ),
            ),
        klass("TemperatureStep") to
            ComponentDescriber(
                track = ComponentDescriber.Track("temperature"),
                requirement =
                    requirement(
                        minimum = { expression, target ->
                          ifSimple(expression) { "Requires ${temperature(target)} or warmer." }
                        },
                        maximum = { expression, target ->
                          ifSimple(expression) {
                            "Temperature must be ${temperature(target)} or colder."
                          }
                        },
                    ),
            ),
        klass("VenusStep") to
            ComponentDescriber(
                track = ComponentDescriber.Track("Venus"),
                requirement =
                    requirement(
                        minimum = { expression, target ->
                          ifSimple(expression) { "Requires Venus ${target * 2}%." }
                        },
                        maximum = { expression, target ->
                          ifSimple(expression) { "Venus must be ${target * 2}% or less." }
                        },
                    ),
            ),
        klass("TerraformRating") to
            ComponentDescriber(
                track = ComponentDescriber.Track("your terraform rating"),
                requirement =
                    requirement(
                        minimum = { expression, target ->
                          ifSimple(expression) {
                            "Requires that you have at least $target terraform rating."
                          }
                        }
                    ),
            ),
        klass("OceanTile") to
            ComponentDescriber(
                placement = ComponentDescriber.Placement("an", "ocean tile", "ocean tiles"),
                requirement =
                    requirement(
                        minimum = { expression, target ->
                          ifSimple(expression) {
                            "Requires $target ocean ${if (target == 1) "tile" else "tiles"}."
                          }
                        },
                        maximum = { expression, target ->
                          ifSimple(expression) {
                            "There must be $target or fewer ocean tiles."
                          }
                        },
                    ),
            ),
        klass("GreeneryTile") to
            ComponentDescriber(
                placement =
                    ComponentDescriber.Placement(
                        "a",
                        "greenery tile",
                        "greenery tiles",
                        consequence = "and raise oxygen 1 step",
                        allowsMultiple = false,
                    ),
                requirement =
                    requirement(
                        minimum = { expression, target ->
                          ifSimple(expression) {
                            "Requires that you have $target greenery ${if (target == 1) "tile" else "tiles"}."
                          }
                        }
                    ),
            ),
        klass("CityTile") to
            ComponentDescriber(
                placement =
                    ComponentDescriber.Placement(
                        "a",
                        "city tile",
                        "city tiles",
                        anyoneMetricOwner = ComponentDescriber.MetricOwner.ANY_PLAYER,
                    ),
                requirement =
                    requirement(
                        minimum = { expression, target ->
                          val tiles = "$target city ${if (target == 1) "tile" else "tiles"}"
                          when {
                            ownedByAnyPlayer(expression) -> "Requires $tiles."
                            expression.simple -> "Requires that you have $tiles."
                            else -> null
                          }
                        },
                        ownedCount = { target ->
                          "$target city ${if (target == 1) "tile" else "tiles"}"
                        },
                    ),
            ),
        klass("Colony") to
            ComponentDescriber(
                placement =
                    ComponentDescriber.Placement(
                        "a",
                        "colony",
                        "colonies",
                        unqualifiedMetricOwner = ComponentDescriber.MetricOwner.YOU,
                        anyoneMetricOwner = ComponentDescriber.MetricOwner.ANY_PLAYER,
                    ),
                requirement =
                    requirement(
                        minimum = { expression, target ->
                          ifSimple(expression) {
                            "Requires $target ${if (target == 1) "colony" else "colonies"}."
                          }
                        },
                        maximum = { expression, target ->
                          ifSimple(expression) {
                            "You must have no more than $target ${if (target == 1) "colony" else "colonies"}."
                          }
                        },
                        ownedCount = { target ->
                          "$target ${if (target == 1) "colony" else "colonies"}"
                        },
                    ),
            ),
        klass("ReserveTradeFleet") to
            ComponentDescriber(directGain = ComponentDescriber.DirectGain("Trade Fleet", 1)),
        klass("VictoryPoint") to ComponentDescriber(score = ComponentDescriber.Score("VP", "VPs")),
        klass("End") to ComponentDescriber(endTrigger = true),
        klass("PlayCard") to ComponentDescriber(playTrigger = ComponentDescriber.PlayTrigger.CARD),
        klass("PlayTag") to ComponentDescriber(playTrigger = ComponentDescriber.PlayTrigger.TAG),
        klass("Pay") to ComponentDescriber(spentResourceTrigger = true),
        klass("Owed") to ComponentDescriber(owedPayment = true),
    )
  }

  private fun resolve(componentClass: Class): ComponentDescriber =
      ComponentDescriber(
          noun = resolveFact(componentClass, ComponentDescriber::noun),
          standardResource = resolveFact(componentClass, ComponentDescriber::standardResource),
          cardResource = resolveFact(componentClass, ComponentDescriber::cardResource),
          cardResourceHolder = resolveFact(componentClass, ComponentDescriber::cardResourceHolder),
          metricLocation = resolveFact(componentClass, ComponentDescriber::metricLocation),
          tag = resolveFact(componentClass, ComponentDescriber::tag),
          track = resolveFact(componentClass, ComponentDescriber::track),
          placement = resolveFact(componentClass, ComponentDescriber::placement),
          requirement = resolveFact(componentClass, ComponentDescriber::requirement),
          directGain = resolveFact(componentClass, ComponentDescriber::directGain),
          score = resolveFact(componentClass, ComponentDescriber::score),
          endTrigger = resolveFact(componentClass, ComponentDescriber::endTrigger),
          playTrigger = resolveFact(componentClass, ComponentDescriber::playTrigger),
          playedCard = resolveFact(componentClass, ComponentDescriber::playedCard),
          spentResourceTrigger =
              resolveFact(componentClass, ComponentDescriber::spentResourceTrigger),
          owedPayment = resolveFact(componentClass, ComponentDescriber::owedPayment),
      )

  private fun <T> resolveFact(
      componentClass: Class,
      fact: (ComponentDescriber) -> T?,
  ): T? {
    val providers =
        componentClass.allSuperclasses().mapNotNull { superclass ->
          declarations[superclass]?.let(fact)?.let { superclass to it }
        }
    val nearest = providers.filter { (provider) ->
      providers.none { (other) -> other !== provider && other.isSubtypeOf(provider) }
    }
    val values = nearest.map { (_, value) -> value }.distinct()
    check(values.size <= 1) {
      "${componentClass.className} inherits conflicting English component knowledge from " +
          nearest.joinToString { (provider) -> provider.className.toString() }
    }
    return values.singleOrNull()
  }

  private fun klass(name: String): Class = Canon.classTable.getClass(cn(name))

  private fun requirement(
      minimum: (Expression, Int) -> String?,
      maximum: (Expression, Int) -> String? = { _, _ -> null },
      ownedCount: ((Int) -> String)? = null,
  ): ComponentDescriber.Requirement =
      object : ComponentDescriber.Requirement {
        override fun renderMinimum(expression: Expression, target: Int): String? =
            minimum(expression, target)

        override fun renderMaximum(expression: Expression, target: Int): String? =
            maximum(expression, target)

        override fun renderOwnedCount(target: Int): String? = ownedCount?.invoke(target)
      }

  private fun ifSimple(expression: Expression, render: () -> String): String? =
      if (expression.simple) render() else null

  private fun ownedByAnyPlayer(expression: Expression): Boolean =
      expression.arguments == listOf(anyoneExpression) &&
          expression.refinement == null &&
          !expression.complement

  private fun temperature(steps: Int): String {
    val degreesCelsius = -30 + 2 * steps
    return "${if (degreesCelsius > 0) "+" else ""}${degreesCelsius}°C"
  }

  private val anyoneExpression = cn("Anyone").expression
}
