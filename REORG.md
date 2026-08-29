# Repository reorganization

## Package-first source layout

Make package and product ownership, rather than Gradle's default directory conventions, the
primary filesystem structure. Keep every current Gradle module and dependency relation, but store
their authored files in one repository-wide source tree:

```text
src/
  common/
    dev/martianzoo/
      engine/
      pets/
      script/
      tfm/
        canon/
          PreludeExpansion/
            cards.pets
            classes.pets
            en.json5
          TerraformingMars/
            ...
        engine/
        script/
      tools/
  jvm/dev/martianzoo/
  js/dev/martianzoo/
  test/
    common/dev/martianzoo/
    jvm/dev/martianzoo/
    js/dev/martianzoo/
  benchmark/jvm/dev/martianzoo/

modules/
  pets/build.gradle.kts
  engine/build.gradle.kts
  tfm-canon/build.gradle.kts
  ...
```

The `modules` tree contains configuration and generated build output only. Each Gradle subproject
selects the package directories and authored data that it compiles, so module divisions remain real
artifact and dependency divisions without fragmenting the package tree.

Do not retain `kotlin`, `resources`, `bundles`, or `language` directories merely to describe file
kinds. Foundational Pets declarations belong in Kotlin. Canon's authored `.pets` and `.json5` files
belong directly under their expansion, including `en.json5`; generated Kotlin embeds them in JVM and
JavaScript artifacts without checked-in generated files or runtime resource loading. JVM- and
JavaScript-specific production code remain separate because target selection is meaningful, but
each target gets one shallow repository-wide overlay instead of a parallel tree per module.

Keep tests separate from shipped product code, organized first by target and then by package. Keep
web assets with `dev/martianzoo/web`, configuring their packaged paths explicitly where necessary.
Avoid filename-suffix compilation filters: source-set membership should be declared through
directories and Gradle configuration.

Perform the move only after the generated Canon registry and remaining resource consumers no longer
depend on the old paths. Validate IDE module ownership with the shared source roots before moving
everything. The completed move must preserve public APIs, resource lookup semantics still in use,
generated artifacts, module dependencies, JVM tests, and the representative browser game.
