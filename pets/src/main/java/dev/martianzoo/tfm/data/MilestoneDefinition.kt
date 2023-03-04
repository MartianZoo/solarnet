package dev.martianzoo.tfm.data

import com.squareup.moshi.Json
import dev.martianzoo.tfm.data.EnglishHack.englishHack
import dev.martianzoo.tfm.data.SpecialClassNames.MILESTONE
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect.Companion.effect
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.Companion.requirement

data class MilestoneDefinition(
    val id: String,
    override val bundle: String,
    val replaces: String? = null,
    @Json(name = "requirement") val requirementText: String,
) : Definition {

  init {
    require(bundle.isNotEmpty())
    require(requirementText.isNotEmpty())
    require(replaces?.isEmpty() != true)
  }
  override val shortName = cn(id)

  val requirement: Requirement = requirement(requirementText)

  override val className = englishHack(id)

  override val asClassDeclaration: ClassDeclaration by lazy {
    ClassDeclaration(
        className,
        shortName,
        abstract = false,
        supertypes = setOf(MILESTONE.expr),
        effectsIn = setOf(effect("This:: ($requirement: Ok)")))
  }
}
