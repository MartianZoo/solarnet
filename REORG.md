# Repository organization

## Package-first source layout

Package and product ownership, rather than Gradle's default directory conventions, form the primary
filesystem structure. Every Gradle module and dependency relation remains, while production,
test, and benchmark files live in shallow repository-wide trees:

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
        web/
          gameviewer/
      tools/
  jvm/dev/martianzoo/
  js/dev/martianzoo/tfm/web/
    gameviewer/
    webrepl/
    shared/

test/
  common/dev/martianzoo/
  jvm/dev/martianzoo/
    benchmarks/
  js/dev/martianzoo/

modules/
  pets/build.gradle.kts
  engine/build.gradle.kts
  tfm-canon/build.gradle.kts
  ...
```

The `modules` tree contains configuration and generated build output only. Each Gradle subproject
selects the package directories and authored data that it compiles, so module divisions remain real
artifact and dependency divisions without fragmenting the package tree.

Declare each owned package directory as its own non-overlapping source root, such as
`src/common/dev/martianzoo/pets` for `pets` and `src/common/dev/martianzoo/engine` for `engine`.
Do not register `src/common` in several subprojects and rely on include filters: Gradle honors those
filters, but IntelliJ assigns the overlapping root to only one module.

Do not retain `kotlin`, `resources`, `bundles`, or `language` directories merely to describe file
kinds. Foundational Pets declarations belong in Kotlin. Canon's authored `.pets` and `.json5` files
belong directly under their expansion, including `en.json5`; generated Kotlin embeds them in JVM and
JavaScript artifacts without checked-in generated files or runtime resource loading. JVM- and
JavaScript-specific production code remain separate because target selection is meaningful, but
each target gets one shallow repository-wide overlay instead of a parallel tree per module.

Keep tests and benchmarks together, separate from shipped product code, organized first by target
and then by package. The benchmark package remains owned by its dedicated Gradle module. Keep the
Terraforming Mars browser applications together under `dev/martianzoo/tfm/web`: the Web REPL belongs
in `webrepl`, the recorded-game viewer in `gameviewer`, and assets used by both in `shared`.
Configure their packaged paths explicitly. Avoid filename-suffix compilation filters: source-set
membership should be declared through directories and Gradle configuration.

Changes to this layout must preserve public APIs, resource lookup semantics still in use, generated
artifacts, module dependencies, JVM tests, and the representative browser game. Validate IDE module
ownership whenever source-root configuration changes.
