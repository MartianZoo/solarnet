package dev.martianzoo.tfm.data

import com.squareup.moshi.Json
import dev.martianzoo.tfm.petaform.PetaformParser.parse
import dev.martianzoo.tfm.petaform.Predicate

data class MilestoneDefinition(
    val id: String,
    val bundle: String? = null,
    val replaces: String? = null,

    @Json(name = "requirement")
    val requirementText: String,
) : Definition {

  init {
    require(id.isNotEmpty())
    require(bundle?.isNotEmpty() ?: true)
    require(requirementText.isNotEmpty())
    require(replaces?.isNotEmpty() ?: true)
  }

  val requirement: Predicate by lazy { parse(requirementText) }

  override val asComponentDefinition: ComponentDefinition by lazy {
    ComponentDefinition(
        "Milestone$id",
        abstract = false,
        supertypesPetaform = setOf("Milestone"),
        immediatePetaform = "$requirementText: Ok")
  }
}
