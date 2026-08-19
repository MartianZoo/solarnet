package dev.martianzoo.tfm.language

/** Sparse English-language facts declared for one component Class. */
internal data class ComponentDescriber(
    val noun: Noun? = null,
    val standardResource: Boolean? = null,
    val cardResource: CardResource? = null,
    val tag: Tag? = null,
    val track: Track? = null,
    val placement: Placement? = null,
    val requirement: Requirement? = null,
    val directGain: DirectGain? = null,
    val victoryPoint: Boolean? = null,
    val endTrigger: Boolean? = null,
) {
  internal sealed interface Noun {
    data object ClassName : Noun

    data class Fixed(val text: String) : Noun

    data class Counted(val singular: String, val plural: String) : Noun
  }

  internal enum class CardResource {
    ORDINARY,
    SUFFIXED,
  }

  internal enum class Tag {
    ORDINARY,
    PLANET,
  }

  internal data class Track(val subject: String)

  internal data class Placement(
      val article: String,
      val singular: String,
      val plural: String,
      val consequence: String? = null,
      val allowsMultiple: Boolean = true,
  )

  internal enum class Requirement {
    CITY_TILES_IN_PLAY,
    COLONIES,
    GREENERY_TILES,
    OCEAN_TILES,
    OXYGEN_PERCENT,
    TEMPERATURE,
    TERRAFORM_RATING,
    VENUS_PERCENT,
  }

  internal data class DirectGain(val noun: String, val count: Int)
}
