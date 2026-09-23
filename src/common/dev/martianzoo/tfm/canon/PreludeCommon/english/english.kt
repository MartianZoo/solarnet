package dev.martianzoo.tfm.text.preludecommon

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.text.ComponentDescriber
import dev.martianzoo.tfm.text.ComponentDescriber.ChangeFrame as Frame

internal val preludeEnglishDeclarations: List<Pair<ClassName, ComponentDescriber>> =
    listOf(
        cn("PreludeCard") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Counted("prelude card", "prelude cards"),
                numericSingularChange = true,
                changeFrame = Frame.Deck,
            )
    )
