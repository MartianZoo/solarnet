package dev.martianzoo.tfm.text.corporateeraexpansion

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.text.ComponentDescriber

internal val corporateEraEnglishDeclarations: List<Pair<ClassName, ComponentDescriber>> =
    listOf(
        cn("Fighter") to
            ComponentDescriber(
                noun = ComponentDescriber.Noun.Counted("fighter resource", "fighter resources")
            )
    )
