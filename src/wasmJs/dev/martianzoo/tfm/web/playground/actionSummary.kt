package dev.martianzoo.tfm.web.playground

import dev.martianzoo.catalog.Catalog
import dev.martianzoo.catalog.displayName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.GameEvent.ChangeEvent

/** A short description of the recorded choice, with card names supplied by the catalog. */
internal fun actionSummary(
    action: ChangeEvent,
    following: List<ChangeEvent>,
    catalog: Catalog,
): String {
  val chosen = requireNotNull(action.change.gaining)
  val table = chosen.type.classTable
  val descendants = mutableSetOf(action.ordinal)
  val consequences = following.filter { event ->
    (event.cause?.triggerEvent in descendants).also { if (it) descendants += event.ordinal }
  }
  val component =
      if (chosen.className == cn("DoRequiredActions")) {
        consequences
            .firstOrNull { it.change.gaining?.className == cn("UseAction") }
            ?.change
            ?.gaining ?: chosen
      } else chosen
  val subjects = component.type.typeDependencies.map { it.boundType }
  val cardFront = table.getClass(cn("CardFront"))
  val card = subjects.mapNotNull { it.representedClass }.firstOrNull { it.isSubtypeOf(cardFront) }
  val player = requireNotNull(chosen.owningPlayer).className.toString()
  fun subject(root: String): String =
      displayName(
          catalog,
          subjects.first { it.rootClass.isSubtypeOf(table.getClass(cn(root))) }.className,
      )
  val description =
      when (component.className.toString()) {
        "PlayProject",
        "PlayCard" -> "plays ${displayName(catalog, requireNotNull(card).className)}"
        "UseCardAction" -> "uses ${subject("ActionCard")}"
        "UseAction" -> "uses ${subject("HasActions")}"
        "ConvertHeat" -> "converts heat"
        "ConvertPlants" -> "converts plants"
        "ClaimMilestone",
        "FundAward" -> {
          val target = subjects.mapNotNull { it.representedClass }.single()
          val verb = if (component.className == cn("ClaimMilestone")) "claims" else "funds"
          "$verb ${displayName(catalog, target.className)}"
        }
        "UseStandardProject" -> {
          if (subjects.any { it.className == cn("SellPatentsProject") }) {
            val projectCard = table.getClass(cn("ProjectCard"))
            val count = consequences.sumOf { event ->
              if (event.change.removing?.type?.rootClass?.isSubtypeOf(projectCard) == true)
                  event.change.count
              else 0
            }
            "sells $count ${if (count == 1) "patent" else "patents"}"
          } else "uses ${subject("StandardProject")}"
        }
        else -> "uses ${displayName(catalog, component.className)}"
      }
  return "$player $description"
}
