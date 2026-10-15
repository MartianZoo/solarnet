package dev.martianzoo.pets

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.util.toSetStrict

/** Any object that has a class name. */
public interface HasClassName {
  /** The class name of or for this object. */
  public val className: ClassName

  public companion object {
    /** Returns names in input order, including repeats. */
    public fun Iterable<HasClassName>.classNames(): List<ClassName> = map { it.className }

    /** Lazily produces names in input order, including repeats. */
    public fun Sequence<HasClassName>.classNames(): Sequence<ClassName> = map { it.className }

    /** Copies names into a set, throwing if distinct elements have the same class name. */
    public fun Set<HasClassName>.classNames(): Set<ClassName> = toSetStrict { it.className }
  }
}
