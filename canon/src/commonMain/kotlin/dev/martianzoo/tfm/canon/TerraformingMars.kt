package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.tfm.canon.CanonResources
import dev.martianzoo.util.toSetStrict

/** The core Terraforming Mars rules and shared game vocabulary. */
internal object TerraformingMars :
    CanonicalBundle(
        name = "TerraformingMars",
        legacyCode = "B",
        cards = true,
        actions = true,
    ) {
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

  override val customClasses: Set<CustomClass> =
      setOf(
          CreateAdjacencies,
          CheckCardDeck,
          CheckCardRequirement,
          HandleCardCost,
          GetEventVps,
          PassLeft,
      )
}
