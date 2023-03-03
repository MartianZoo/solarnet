package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.util.toSetStrict

public interface HasClassName {
  val className: ClassName
}

fun Iterable<HasClassName>.classNames(): List<ClassName> = map { it.className }
fun Sequence<HasClassName>.classNames(): Sequence<ClassName> = map { it.className }
fun Set<HasClassName>.classNames(): Set<ClassName> = map { it.className }.toSetStrict()
