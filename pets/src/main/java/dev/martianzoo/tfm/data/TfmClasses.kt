package dev.martianzoo.tfm.data

import dev.martianzoo.pets.ast.ClassName.Companion.cn

public object TfmClasses {
  val STANDARD_ACTION = cn("StandardAction")
  val STANDARD_PROJECT = cn("StandardProject")

  val END = cn("End")

  val MARS_MAP = cn("MarsMap")
  val TILE = cn("Tile")

  val MILESTONE = cn("Milestone")

  val CORPORATION_CARD = cn("CorporationCard")
  val PRELUDE_CARD = cn("PreludeCard")
  val PROJECT_CARD = cn("ProjectCard")
  val CARD_FRONT = cn("CardFront")
  val ACTIVE_CARD = cn("ActiveCard")
  val AUTOMATED_CARD = cn("AutomatedCard")
  val EVENT_CARD = cn("EventCard")
  val ACTION_CARD = cn("ActionCard")
  val RESOURCE_CARD = cn("ResourceCard")

  val STANDARD_RESOURCE = cn("StandardResource")
  val PRODUCTION = cn("Production")
  val MEGACREDIT = cn("Megacredit")

  // Okay so it's not really a class name
  val PROD = "PROD"
}
