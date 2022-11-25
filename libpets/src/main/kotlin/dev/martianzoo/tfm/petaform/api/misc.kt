package dev.martianzoo.tfm.petaform.api

fun hasZeroOrOneProd(vararg things: PetaformObject?) = hasZeroOrOneProd(things.toList())

fun hasZeroOrOneProd(things: Iterable<PetaformObject?>) =
    when (things.count { it != null && it.hasProd }) {
      0 -> false
      1 -> true
      else -> error("too many prod boxes")
    }
