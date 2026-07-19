package dev.martianzoo.tfm.canon.bundles

import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.TfmRuleset
import dev.martianzoo.tfm.canon.CanonResources
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.JsonReader
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.MilestoneDefinition
import dev.martianzoo.tfm.data.StandardActionDefinition
import dev.martianzoo.util.toSetStrict

/** Shared resource loading for one canonical bundle directory. */
internal abstract class CanonicalBundle(
    name: String,
    legacyCode: String?,
    alwaysIncluded: Boolean = false,
    cards: Boolean = false,
    actions: Boolean = false,
    maps: Boolean = false,
    milestones: Boolean = false,
) : TfmRuleset.Bundle(cn(name), legacyCode, alwaysIncluded) {
  private val directory = name

  override val explicitClassDeclarations: Set<ClassDeclaration> =
      setOf(parseOneLinerClass("CLASS $name : System { HAS =1 This }"))

  override val cardDefinitions: Set<CardDefinition> by lazy {
    if (cards) {
      JsonReader.readCards(read("cards.json5"), requireNotNull(legacyCode))
          .toSetStrict(::CardDefinition)
    } else emptySet()
  }

  override val standardActionDefinitions: Set<StandardActionDefinition> by lazy {
    if (actions) {
      JsonReader.readActions(read("actions.json5"), requireNotNull(legacyCode)).toSetStrict()
    } else emptySet()
  }

  override val marsMapDefinitions: Set<MarsMapDefinition> by lazy {
    if (maps) JsonReader.readMaps(read("maps.json5"), requireNotNull(legacyCode)).toSetStrict()
    else emptySet()
  }

  override val milestoneDefinitions: Set<MilestoneDefinition> by lazy {
    if (milestones) {
      JsonReader.readMilestones(read("milestones.json5"), requireNotNull(legacyCode)).toSetStrict()
    } else emptySet()
  }

  protected fun read(filename: String): String = CanonResources.read("bundles/$directory/$filename")
}
