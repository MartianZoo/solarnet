package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

public object TfmClasses {
  internal val END = cn("End")
  internal val START_TOKEN = cn("StartToken")
  internal val AFTER_ME = cn("AfterMe")

  internal val MARS_MAP = cn("MarsMap")
  internal val PLACEMENT = cn("Placement")
  public val TILE: ClassName = cn("Tile")

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
  internal val RESOURCE_CARD = cn("ResourceCard")

  public val STANDARD_RESOURCE: ClassName = cn("StandardResource")
  public val PRODUCTION: ClassName = cn("Production")
  public val MC: ClassName = cn("MC")
  internal val PROD_OFFSET: ClassName = cn("ProdOffset")

  // Okay so it's not really a class name
  public const val PROD: String = "PROD"
}
