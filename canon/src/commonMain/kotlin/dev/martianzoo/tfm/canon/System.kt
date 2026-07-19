package dev.martianzoo.tfm.canon

import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.TfmRuleset
import dev.martianzoo.tfm.canon.CanonResources
import dev.martianzoo.util.toSetStrict

/** The Pets runtime declarations that every canonical game uses. */
internal object System :
    TfmRuleset.Bundle(
        bundleName = cn("System"),
        legacyCode = null,
        alwaysIncluded = true,
        hasComponent = false,
    ) {
  override val explicitClassDeclarations: Set<ClassDeclaration> by lazy {
    parseClasses(CanonResources.read("bundles/System/system.pets")).toSetStrict()
  }
}
