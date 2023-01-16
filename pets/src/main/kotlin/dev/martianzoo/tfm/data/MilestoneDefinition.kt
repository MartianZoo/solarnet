package dev.martianzoo.tfm.data

import com.squareup.moshi.Json
import dev.martianzoo.tfm.data.SpecialClassNames.MILESTONE
import dev.martianzoo.tfm.pets.Parsing.parsePets
import dev.martianzoo.tfm.pets.SpecialClassNames.OK
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGain
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.ScalarAndType

data class MilestoneDefinition(
    override val id: ClassName,
    override val bundle: String,
    val replaces: String? = null,

    @Json(name = "requirement")
    val requirementText: String,
) : Definition {

  init {
    require(bundle.isNotEmpty())
    require(requirementText.isNotEmpty())
    require(replaces?.isEmpty() != true)
  }

  val requirement: Requirement by lazy { parsePets(requirementText) }

  override val name = englishHack(id.asString)

  override val asClassDeclaration: ClassDeclaration by lazy {
    ClassDeclaration(
        id,
        name,
        abstract = false,
        supertypes = setOf(MILESTONE.type),
        effectsRaw = setOf(Effect(
            OnGain(THIS.type),
            Gated(requirement, Gain(ScalarAndType(type = OK.type))),
            automatic = true,
        ))
    )
  }
}
