package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn

internal val attributionProbe = cn("AttributionProbe")
internal val attribution = cn("Attribution")

internal val attributionProbeDeclarations =
    parseClasses(
            """
            CLASS Attribution<Player> : Hidden

            CLASS AttributionProbe : ActiveCard<Class<ProjectCard>> {
              cost = 0

              -X VictoryPoint<Anyone> BY Player1: Attribution<Player1>
              -X VictoryPoint<Anyone> BY Player2: Attribution<Player2>
              -X VictoryPoint<Anyone> BY Player3: Attribution<Player3>
            }
            """
                .trimIndent()
        )
        .toSet()
