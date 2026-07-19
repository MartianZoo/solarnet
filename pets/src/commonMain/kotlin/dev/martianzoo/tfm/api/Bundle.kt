package dev.martianzoo.tfm.api

import dev.martianzoo.pets.ast.ClassName

/** One raw grouping of declarations, definitions, and custom implementations. */
public abstract class Bundle(
    public val bundleName: ClassName,
) : TfmRuleset.Empty() {
  final override val bundles: List<Bundle> = listOf(this)

  final override val classDeclarationBundles: Map<ClassName, Set<ClassName>> by lazy {
    val contributedNames = contributedClassDeclarations.map { it.className }.toSet()
    allClassNames.associateWith { name ->
      if (name in contributedNames) setOf(bundleName) else emptySet()
    }
  }
}
