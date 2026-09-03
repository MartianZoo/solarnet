package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.data.ClassDeclaration

/** Published Terraforming Mars Catalog with typed Terraforming Mars definition registries. */
public object Canon :
    TfmCatalog.Composite(
        terraformingMarsBundle, // 2016
        hellasElysiumExpansionBundle, // 2017
        venusNextExpansionBundle, // 2017
        preludeExpansionBundle, // 2018
        coloniesExpansionBundle, // 2018
        turmoilCardPackBundle, // 2019
        prelude2ExpansionBundle, // 2024
        milestonesAwardsExpansionBundle, // 2024
        utopiaCimmeriaExpansionBundle, // 2024
        promoCardPackBundle,
    ) {
  /** Returns Canon extended with replay- or scenario-specific class declarations. */
  public fun withNonstandardClasses(declarations: Collection<ClassDeclaration>): TfmCatalog =
      TfmCatalog.compose(
          this,
          object : TfmCatalog() {
            override val explicitClassDeclarations: Set<ClassDeclaration> = declarations.toSet()
          },
      )
}
