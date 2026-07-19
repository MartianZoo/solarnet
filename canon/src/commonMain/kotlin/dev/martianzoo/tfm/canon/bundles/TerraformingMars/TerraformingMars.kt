package dev.martianzoo.tfm.canon.bundles.TerraformingMars

import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.tfm.api.TfmRuleset
import dev.martianzoo.tfm.canon.CanonResources
import dev.martianzoo.util.toSetStrict

/** The core Terraforming Mars rules and shared game vocabulary. */
internal object TerraformingMars : TfmRuleset.Empty() {
  private val petsFilenames =
      setOf(
          "global.pets",
          "maps-tiles.pets",
          "player.pets",
          "cards.pets",
          "actions.pets",
          "payment.pets",
      )

  override val explicitClassDeclarations: Set<ClassDeclaration> by lazy {
    petsFilenames
        .flatMap { parseClasses(CanonResources.read("bundles/TerraformingMars/$it")) }
        .toSetStrict()
  }
}
