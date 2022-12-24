package dev.martianzoo.tfm.data

import com.squareup.moshi.Json
import dev.martianzoo.tfm.pets.ComponentDef
import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.SpecialComponent.OK
import dev.martianzoo.tfm.pets.SpecialComponent.THIS
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGain
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression

data class MilestoneDefinition(
    val id: String,
    val bundle: String? = null,
    val replaces: String? = null,

    @Json(name = "requirement")
    val requirementText: String,
) : Definition {

  init {
    require(id.isNotEmpty())
    require(bundle?.isEmpty() != true)
    require(requirementText.isNotEmpty())
    require(replaces?.isEmpty() != true)
  }

  val requirement: Requirement by lazy { parse(requirementText) }

  override val toComponentDef: ComponentDef by lazy {
    ComponentDef(
        "Milestone$id",
        abstract = false,
        supertypes = setOf(TypeExpression("Milestone")),
        effects = setOf(
            Effect(OnGain(THIS.type), Gated(requirement, Gain(OK.type)))
        )
    )
  }
}
