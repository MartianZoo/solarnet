package dev.martianzoo.tfm.text.prelude1cardpack

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.text.ComponentDescriber

internal val prelude1EnglishDeclarations: List<Pair<ClassName, ComponentDescriber>> =
    listOf(cn("NonNegativeIconsOf") to ComponentDescriber(printedIconCount = true))
