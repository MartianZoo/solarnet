package dev.martianzoo.data

import dev.martianzoo.pets.ast.PropertyName

/** Standard Pets properties controlling how Modules participate in a game premise. */
public object ModuleProperties {
  /** Optional condition under which an unmentioned Module is selected automatically. */
  public val AUTO_SELECT_WHEN: PropertyName = PropertyName("autoSelectWhen")

  /** Optional condition that the resolved premise must satisfy when the Module is selected. */
  public val PREMISE_REQUIREMENT: PropertyName = PropertyName("premiseRequirement")
}
