package dev.martianzoo.data

import dev.martianzoo.pets.ast.PropertyName

/** Standard Pets properties controlling how Classes participate in premise projection. */
public object ClassProperties {
  /** Optional condition applied when bundle policy would select content referencing this Class. */
  public val AUTOMATIC_SELECTION_REQUIREMENT: PropertyName =
      PropertyName("automaticSelectionRequirement")

  /** Optional condition that must hold before a hard reference may activate this Class. */
  public val ACTIVATION_REQUIREMENT: PropertyName = PropertyName("activationRequirement")
}
