package dev.martianzoo.tfm.data

import com.squareup.moshi.Json
import dev.martianzoo.tfm.pets.PetParser.parsePets
import dev.martianzoo.tfm.pets.SpecialComponent.OK
import dev.martianzoo.tfm.pets.SpecialComponent.THIS
import dev.martianzoo.tfm.pets.ast.ClassName
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

  val requirement: Requirement by lazy { parsePets(requirementText) }

  override val className = ClassName("Milestone$id")

  override val asClassDeclaration: ClassDeclaration by lazy {
    ClassDeclaration(
        className,
        abstract = false,
        supertypes = setOf(gte("Milestone")),
        effectsRaw = setOf(Effect(
            OnGain(THIS.baseType),
            Gated(requirement, Gain(QuantifiedExpression(OK.baseType))),
            automatic = true,
        ))
    )
  }
}
