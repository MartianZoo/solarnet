package dev.martianzoo.pets

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

/** Client-selected input-only spellings; a [Vocabulary] never emits these names. */
public class ClassSynonyms private constructor(public val mappings: Map<ClassName, ClassName>) {
  /** Returns the canonical class name for [name], or [name] itself when it has no mapping. */
  public fun canonicalName(name: ClassName): ClassName = mappings[name] ?: name

  public companion object {
    /** No alternate spellings. */
    public val NONE: ClassSynonyms = ClassSynonyms(emptyMap())

    /** Creates mappings such as `"TR" to "TerraformRating"`. */
    public fun of(vararg mappings: Pair<String, String>): ClassSynonyms = of(mappings.asIterable())

    /** Creates mappings from an iterable supplied by a client. */
    public fun of(mappings: Iterable<Pair<String, String>>): ClassSynonyms {
      val pairs = mappings.map { (synonym, canonical) -> cn(synonym) to cn(canonical) }
      require(pairs.map { it.first }.distinct().size == pairs.size) { "Duplicate class synonym" }
      return ClassSynonyms(pairs.toMap())
    }
  }
}
