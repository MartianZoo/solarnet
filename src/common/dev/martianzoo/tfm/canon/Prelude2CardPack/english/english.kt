package dev.martianzoo.tfm.text.prelude2cardpack

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.text.ComponentDescriber
import dev.martianzoo.tfm.text.ComponentDescriber.ChangeFrame as Frame

internal val prelude2EnglishDeclarations: List<Pair<ClassName, ComponentDescriber>> =
    listOf(
        cn("Director") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Counted("director resource", "director resources")
            ),
        cn("FrontierTownBonus") to
            ComponentDescriber(
                changeFrame = Frame.ScopedInstruction("and gain its placement bonus twice")
            ),
    )
