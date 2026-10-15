package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName.Companion.cn

internal val fakeEnglishDescriptions =
    mapOf(
        cn("FakeWildTag") to
            ComponentDescriber(
                cardInvariantSentence =
                    "While performing a future action, the wild tag can become any tag of your choice."
            )
    )
