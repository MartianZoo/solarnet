package dev.martianzoo.tfm.data

import com.squareup.moshi.Json
import dev.martianzoo.tfm.data.SpecialClassNames.MILESTONE
import dev.martianzoo.tfm.pets.SpecialClassNames.OK
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.Companion.requirement
import dev.martianzoo.tfm.pets.ast.ScalarAndType.Companion.sat

data class MilestoneDefinition(
    override val id: ClassName,
    override val bundle: String,
    val replaces: String? = null,
    @Json(name = "requirement") val requirementText: String,
) : Definition {

  init {
    require(bundle.isNotEmpty())
    require(requirementText.isNotEmpty())
    require(replaces?.isEmpty() != true)
  }

  val requirement: Requirement by lazy { requirement(requirementText) }

  override val className = englishHack(id)

  override val asClassDeclaration: ClassDeclaration by lazy {
    ClassDeclaration(
        className,
        id,
        abstract = false,
        supertypes = setOf(MILESTONE.type),
        effectsRaw =
            setOf(
                Effect(
                    WhenGain,
                    Gated(requirement, Gain(sat(1, OK.type))),
                    automatic = true,
                )))
  }
}
