package dev.martianzoo.tfm.canon.bundles.SoloMode

import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.tfm.canon.bundles.CanonicalBundle
import dev.martianzoo.util.toSetStrict

/** The one-player variant. */
internal object SoloMode : CanonicalBundle(name = "SoloMode", legacyCode = null) {
  override val explicitClassDeclarations: Set<ClassDeclaration> by lazy {
    parseClasses(read("solo.pets")).toSetStrict()
  }
}
