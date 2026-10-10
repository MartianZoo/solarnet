package dev.martianzoo.tfm.text.preludecommon

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.tfm.text.ComponentDescriber
import dev.martianzoo.tfm.text.ComponentDescriber.ChangeFrame as Frame

internal val preludeEnglishDeclarations: List<Pair<ClassName, ComponentDescriber>> =
    listOf(
        cn("PreludeCard") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Counted("Prelude card", "Prelude cards"),
                numericSingularChange = true,
                changeFrame = Frame.Deck,
            ),
        cn("PlayOrFizzle") to
            ComponentDescriber(
                cardProcedure = ComponentDescriber.CardProcedure.PLAY_OR_FIZZLE,
                cardLocationDependency = Key(cn("PlayOrFizzle"), 0),
            ),
    )
