package dev.martianzoo.tfm.data

import com.squareup.moshi.Json
import dev.martianzoo.tfm.pets.ComponentDef
import dev.martianzoo.tfm.pets.Effect
import dev.martianzoo.tfm.pets.Effect.Trigger.OnGain
import dev.martianzoo.tfm.pets.Instruction.Gain
import dev.martianzoo.tfm.pets.Instruction.Gated
import dev.martianzoo.tfm.pets.Parser.parse
import dev.martianzoo.tfm.pets.Requirement
import dev.martianzoo.tfm.pets.TypeExpression

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

  val requirement: Requirement by lazy { parse(requirementText) }

  override val toComponentDef: ComponentDef by lazy {
    ComponentDef(
        "Milestone$id",
        abstract = false,
        supertypes = setOf(TypeExpression("Milestone")),
        effects = setOf(Effect(
            OnGain(TypeExpression("This")),
            Gated(requirement, Gain(TypeExpression("Ok")))))
    )
  }
}
