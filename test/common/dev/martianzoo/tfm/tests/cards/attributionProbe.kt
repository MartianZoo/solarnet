package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.ClassDeclaration

internal val attributionProbe = cn("AttributionProbe")
internal val attribution = cn("Attribution")

/**
 * A card recording which actor was credited for each victory point removed. It watches Admin and
 * the occupied seats, since a trigger may only name a player that game actually seats.
 */
internal fun attributionProbeDeclarations(seats: Int): Set<ClassDeclaration> {
  val watchers =
      (listOf("Admin") + (1..seats).map { "Player$it" }).joinToString("\n  ") { actor ->
        "-X VictoryPoint<Anyone> BY $actor: Attribution<$actor>"
      }
  return parseClasses(
          """
          CLASS Attribution<Actor> : Hidden

          CLASS AttributionProbe : ActiveCard {
            cost = 0
            HAS MAX 1 AttributionProbe

            $watchers
          }
          """
              .trimIndent()
      )
      .toSet()
}
