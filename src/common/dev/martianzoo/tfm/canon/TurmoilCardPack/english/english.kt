package dev.martianzoo.tfm.text.turmoilcardpack

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.text.ComponentDescriber

internal val turmoilCardPackEnglishDeclarations: List<Pair<ClassName, ComponentDescriber>> =
    listOf(
        cn("Preservation") to
            ComponentDescriber(
                noun =
                    ComponentDescriber.Noun.Counted(
                        "preservation resource",
                        "preservation resources",
                    )
            )
    )
