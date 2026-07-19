package dev.martianzoo.tfm.data

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

internal fun parseBundleNames(text: String?): Set<ClassName> =
    text
        ?.split(',')
        ?.map(String::trim)
        ?.onEach { require(it.isNotEmpty()) }
        ?.map(::cn)
        ?.toSet()
        .orEmpty()
