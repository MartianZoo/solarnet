package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

public object TfmClasses {
  internal val STANDARD_ACTION = cn("StandardAction")

  internal val END = cn("End")

  internal val MARS_MAP = cn("MarsMap")
  public val TILE: dev.martianzoo.pets.ast.ClassName = cn("Tile")

  internal val MILESTONE = cn("Milestone")
  internal val AWARD = cn("Award")

  internal val CORPORATION_CARD = cn("CorporationCard")
  internal val PRELUDE_CARD = cn("PreludeCard")
  internal val PROJECT_CARD = cn("ProjectCard")
  internal val CARD_RESOURCE = cn("CardResource")
  internal val CARD_FRONT = cn("CardFront")
  internal val TAG = cn("Tag")
  internal val EVENT_TAG = cn("EventTag")
  internal val ACTIVE_CARD = cn("ActiveCard")
  internal val AUTOMATED_CARD = cn("AutomatedCard")
  internal val EVENT_CARD = cn("EventCard")
  internal val ACTION_CARD = cn("ActionCard")
  internal val RESOURCE_CARD = cn("ResourceCard")

  public val STANDARD_RESOURCE: dev.martianzoo.pets.ast.ClassName = cn("StandardResource")
  public val PRODUCTION: dev.martianzoo.pets.ast.ClassName = cn("Production")
  public val MC: dev.martianzoo.pets.ast.ClassName = cn("MC")
  public val STANDARD_RESOURCE_CLASSES: Set<ClassName> =
      setOf(MC, cn("Steel"), cn("Titanium"), cn("Plant"), cn("Energy"), cn("Heat"))

  // Okay so it's not really a class name
  public const val PROD: String = "PROD"
}
