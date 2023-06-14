package dev.martianzoo.pets

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.util.toSetStrict

/** Any object that has a class name. */
public interface HasClassName {
  /** The class name of or for this object. */
  val className: ClassName

  public companion object {
    fun Iterable<HasClassName>.classNames(): List<ClassName> = map { it.className }
    fun Sequence<HasClassName>.classNames(): Sequence<ClassName> = map { it.className }
    fun Set<HasClassName>.classNames(): Set<ClassName> = toSetStrict { it.className }
  }
}
