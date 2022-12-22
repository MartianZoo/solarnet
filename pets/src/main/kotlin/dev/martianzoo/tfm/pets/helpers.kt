package dev.martianzoo.tfm.pets

fun <T> Collection<T>.toSetCarefulP() = toSet().also { if (it.size != size) throw PetsException("{$this}") }
