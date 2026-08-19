package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.types.Class

/** English component descriptions, inherited independently one fact at a time. */
internal object Describers {
  internal operator fun get(className: ClassName): ComponentDescriber {
    val componentClass = Canon.classTable.findClass(className) ?: return ComponentDescriber()
    return effectiveDescribers.getValue(componentClass)
  }

  private val declarations: Map<Class, ComponentDescriber> by lazy {
    mapOf(
        klass("Component") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.ClassName,
                deriveBottomText = true,
            ),
        klass("StandardResource") to ComponentDescriber(standardResource = true),
        klass("Megacredit") to ComponentDescriber(noun = ComponentDescriber.Noun.Fixed("M€")),
        klass("Plant") to
            ComponentDescriber(noun = ComponentDescriber.Noun.Counted("plant", "plants")),
        klass("CardResource") to
            ComponentDescriber(cardResource = ComponentDescriber.CardResource.SUFFIXED),
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
                track = ComponentDescriber.Track("your TR"),
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
                placement = ComponentDescriber.Placement("a", "city tile", "city tiles"),
                requirement = ComponentDescriber.Requirement.CITY_TILES_IN_PLAY,
            ),
        klass("Colony") to
            ComponentDescriber(
                placement = ComponentDescriber.Placement("a", "colony", "colonies"),
                requirement = ComponentDescriber.Requirement.COLONIES,
            ),
        klass("ReserveTradeFleet") to
            ComponentDescriber(directGain = ComponentDescriber.DirectGain("Trade Fleet", 1)),
        klass("VictoryPoint") to ComponentDescriber(victoryPoint = true),
        klass("End") to ComponentDescriber(endTrigger = true),
        klass("Card005F") to ComponentDescriber(deriveBottomText = false),
    )
  }

  private val effectiveDescribers: Map<Class, ComponentDescriber> by lazy {
    Canon.classTable.allClasses().associateWith(::resolve)
  }

  private fun resolve(componentClass: Class): ComponentDescriber =
      ComponentDescriber(
          noun = resolveFact(componentClass, ComponentDescriber::noun),
          standardResource = resolveFact(componentClass, ComponentDescriber::standardResource),
          cardResource = resolveFact(componentClass, ComponentDescriber::cardResource),
          tag = resolveFact(componentClass, ComponentDescriber::tag),
          track = resolveFact(componentClass, ComponentDescriber::track),
          placement = resolveFact(componentClass, ComponentDescriber::placement),
          requirement = resolveFact(componentClass, ComponentDescriber::requirement),
          directGain = resolveFact(componentClass, ComponentDescriber::directGain),
          victoryPoint = resolveFact(componentClass, ComponentDescriber::victoryPoint),
          endTrigger = resolveFact(componentClass, ComponentDescriber::endTrigger),
          deriveBottomText = resolveFact(componentClass, ComponentDescriber::deriveBottomText),
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
      providers.none { (other) ->
        other !== provider && other.isSubtypeOf(provider)
      }
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
