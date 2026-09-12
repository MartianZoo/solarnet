package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.ClassDeclaration

internal val attributionProbe = cn("AttributionProbe")
internal val attribution = cn("Attribution")

/**
 * A card recording which player was credited for each victory point removed. It watches one seat
 * per seat the game occupies, since a trigger may only name a player that game actually seats.
 */
internal fun attributionProbeDeclarations(seats: Int): Set<ClassDeclaration> {
  val watchers =
      (1..seats).joinToString("\n  ") { seat ->
        "-X VictoryPoint<Anyone> BY Player$seat: Attribution<Player$seat>"
      }
  return parseClasses(
          """
          CLASS Attribution<Player> : Hidden

          CLASS AttributionProbe : ActiveCard {
            cost = 0

            $watchers
          }
          """
              .trimIndent()
      )
      .toSet()
}
