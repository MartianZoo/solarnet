package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn

internal object SpecialClassNames {
  val STANDARD_ACTION = cn("StandardAction")
  val STANDARD_PROJECT = cn("StandardProject")

  val MARS_MAP = cn("MarsMap")
  val TILE = cn("Tile")

  val MILESTONE = cn("Milestone")

  val CORPORATION_CARD = cn("CorporationCard")
  val PRELUDE_CARD = cn("PreludeCard")
  val PROJECT_CARD = cn("ProjectCard")

  val CARD_FRONT = cn("CardFront")
  val RESOURCE_CARD = cn("ResourceCard")
  val ACTION_CARD = cn("ActionCard")
  val ACTIVE_CARD = cn("ActiveCard")
  val AUTOMATED_CARD = cn("AutomatedCard")
  val EVENT_CARD = cn("EventCard")
}
