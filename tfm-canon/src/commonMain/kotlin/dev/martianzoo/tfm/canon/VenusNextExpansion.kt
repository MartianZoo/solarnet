package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.BundleContentSelection
import dev.martianzoo.tfm.api.BundleContentSelection.Kind.AWARDS
import dev.martianzoo.tfm.api.BundleContentSelection.Kind.CARDS
import dev.martianzoo.tfm.api.BundleContentSelection.Kind.MILESTONES

internal val venusNextExpansionBundle: StandardFormBundle by lazy {
  StandardFormBundle(
      "VenusNextExpansion",
      moduleContentSelections =
          mapOf(
              cn("VenusNextExpansion") to
                  setOf(
                      BundleContentSelection(
                          cn("VenusNextExpansion"),
                          setOf(CARDS, MILESTONES, AWARDS),
                      )
                  )
          ),
  )
}

/** Namespace for Venus Next-specific implementations. */
private object VenusNextExpansion
