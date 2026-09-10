package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.ClassDeclaration

internal val attributionProbe = cn("AttributionProbe")
internal val attribution = cn("Attribution")

/** Declarations for one attribution watcher per occupied player seat. */
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
