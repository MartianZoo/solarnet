package dev.martianzoo.tfm.data

import com.squareup.moshi.Json
import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.SpecialComponent.Ok
import dev.martianzoo.tfm.pets.SpecialComponent.This
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGain
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.QuantifiedExpression
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte

data class MilestoneDefinition(
    val id: String,
    override val bundle: String,
    val replaces: String? = null,

    @Json(name = "requirement")
    val requirementText: String,
) : Definition {

  init {
    require(id.isNotEmpty())
    require(bundle.isNotEmpty())
    require(requirementText.isNotEmpty())
    require(replaces?.isEmpty() != true)
  }

  val requirement: Requirement by lazy { parse(requirementText) }

  override val className = "Milestone$id"

  override val asClassDeclaration: ClassDeclaration by lazy {
    ClassDeclaration(
        className,
        abstract = false,
        supertypes = setOf(gte("Milestone")),
        effectsRaw = setOf(Effect(
            OnGain(This.type),
            Gated(requirement, Gain(QuantifiedExpression(Ok.type))),
            automatic = true,
        ))
    )
  }
}
