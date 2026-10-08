package dev.martianzoo.state

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

/**
 * Shared identity of a Kotlin-backed Pets metric or instruction. By default its Pets class name is
 * the implementation's Kotlin simple name.
 */
public abstract class CustomClass(name: String? = null) : HasClassName {
  public constructor(className: ClassName) : this(className.toString())

  final override val className: ClassName = cn(name ?: requireNotNull(this::class.simpleName))
}
