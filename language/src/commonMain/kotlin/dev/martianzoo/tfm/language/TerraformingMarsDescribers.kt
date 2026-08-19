package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName.Companion.cn
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
        klass("CardFront") to ComponentDescriber(cardResourceHolder = "card"),
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
                requirement = ComponentDescriber.Requirement.OXYGEN_PERCENT,
            ),
        klass("TemperatureStep") to
            ComponentDescriber(
                track = ComponentDescriber.Track("temperature"),
                requirement = ComponentDescriber.Requirement.TEMPERATURE,
            ),
        klass("VenusStep") to
            ComponentDescriber(
                track = ComponentDescriber.Track("Venus"),
                requirement = ComponentDescriber.Requirement.VENUS_PERCENT,
            ),
        klass("TerraformRating") to
            ComponentDescriber(
                track = ComponentDescriber.Track("your terraform rating"),
                requirement = ComponentDescriber.Requirement.TERRAFORM_RATING,
            ),
        klass("OceanTile") to
            ComponentDescriber(
                placement = ComponentDescriber.Placement("an", "ocean tile", "ocean tiles"),
                requirement = ComponentDescriber.Requirement.OCEAN_TILES,
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
                requirement = ComponentDescriber.Requirement.GREENERY_TILES,
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
                requirement = ComponentDescriber.Requirement.CITY_TILES,
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
                requirement = ComponentDescriber.Requirement.COLONIES,
            ),
        klass("ReserveTradeFleet") to
            ComponentDescriber(directGain = ComponentDescriber.DirectGain("Trade Fleet", 1)),
        klass("VictoryPoint") to ComponentDescriber(score = ComponentDescriber.Score("VP", "VPs")),
        klass("End") to ComponentDescriber(endTrigger = true),
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
}
