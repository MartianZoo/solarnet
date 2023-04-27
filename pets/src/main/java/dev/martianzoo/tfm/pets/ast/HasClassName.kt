package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.util.toSetStrict

public interface HasClassName {
  val className: ClassName

  public companion object {
    fun Iterable<HasClassName>.classNames(): List<ClassName> = map { it.className }
    fun Sequence<HasClassName>.classNames(): Sequence<ClassName> = map { it.className }
    fun Set<HasClassName>.classNames(): Set<ClassName> = toSetStrict { it.className }
  }
}
