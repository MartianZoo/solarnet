package dev.martianzoo.tfm.carddata

import kotlinx.serialization.Serializable

/** Pure authored data for one Terraforming Mars card. Pets fragments remain source strings. */
@Serializable
public data class CardDefinition(
    val name: String,
    val deck: String? = null,
    val tags: List<String> = emptyList(),
    val immediate: String? = null,
    val actions: List<String> = emptyList(),
    val effects: List<String> = emptyList(),
    val components: Set<String> = emptySet(),
    val invariants: Set<String> = emptySet(),
    val requirement: String? = null,
    val autoSelectWhen: String? = null,
    val cost: Int = 0,
    val projectKind: String? = null,
) {
  init {
    require(NAME.matches(name)) { "Invalid card name: $name" }
    require(deck == null || deck in DECKS) { "Invalid card deck: $deck" }
    require(projectKind == null || projectKind in PROJECT_KINDS) {
      "Invalid project kind: $projectKind"
    }
    require(invariants.none(String::isEmpty))
    require(requirement?.isNotEmpty() != false)
    require(autoSelectWhen?.isNotEmpty() != false)
    require(cost >= 0)
    if (deck == "ProjectCard") {
      require(projectKind != null) { "Project card has no kind: $name" }
    } else {
      require(projectKind == null) { "Not a project: $name" }
      require(requirement == null) { "Non-project card has a requirement: $name" }
      require(cost == 0) { "Non-project card has a cost: $name" }
    }
  }

  private companion object {
    val NAME = Regex("[A-Za-z][A-Za-z0-9]*")
    val DECKS = setOf("CorporationCard", "PreludeCard", "ProjectCard")
    val PROJECT_KINDS = setOf("AutomatedCard", "EventCard", "ActiveCard")
  }
}
