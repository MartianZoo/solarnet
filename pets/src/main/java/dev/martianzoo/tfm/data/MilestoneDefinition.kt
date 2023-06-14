package dev.martianzoo.tfm.data

import com.squareup.moshi.Json
import dev.martianzoo.api.SystemClasses.OK
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.tfm.data.EnglishHack.englishHack
import dev.martianzoo.tfm.data.TfmClasses.MILESTONE

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

  val requirement: Requirement = parse(requirementText)

  override val className = englishHack(id)

  override val asClassDeclaration: ClassDeclaration by lazy {
    ClassDeclaration(
        className,
        shortName,
        kind = CONCRETE,
        supertypes = setOf(MILESTONE.expression),
        effects = setOf(parse("This:: ($requirementText: $OK)")),
    )
  }
}
