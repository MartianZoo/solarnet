# Pets and Solarnet builds

This repository contains two independent Gradle builds. `pets/` owns the language, catalog,
static Terraforming Mars content, English, Almanac, and their generation/analysis tools. The root
build owns runtime state, execution, Agents, gameplay, scripts, tests, and runtime applications.
Pets shares `pets/src/{common,jvm,js}` and `pets/test/{common,jvm,js}` across its modules;
the build files under `pets/modules/` select their package directories. Runtime modules retain
the existing root `src/` and `test/` layout. Keep the nested Pets build during the transition;
an external checkout is not required.

## Everyday use

- `./gradlew build`, `test`, `spotlessCheck`, and `spotlessApply` cover both builds when local Pets
  source is selected. Dependencies on the included build's verification tasks are explicit.
- Standalone commands and publication policy live in [Pets build guidance](../../pets/docs/agents/BUILDS.md).
  From this checkout, use `./gradlew -p pets` for those commands.
- `./gradlew webAppsDevelopmentRun` serves Web REPL and packaged Almanac together.
- Open the root Gradle project in the IDE for combined work. Its included Pets build supplies
  source navigation and dependency substitution. Open `pets/` alone for static-model work.

## Source selection and releases

The default local source is the explicit `pets/` subdirectory. Pass `-PpetsSource=/absolute/path`
to select another Pets build. Per-working-copy choices can be stored under `_local/pets-sources/`;
use `./gradlew petsMode` to see the exact selection file and active source/release. A command-line
property overrides the local selection file. An invalid source path fails with instructions.

Use `-PpetsSource=artifacts -PpetsRepository=/absolute/path/to/maven` to disable source substitution.
`petsVersion` in the root `gradle.properties` pins the complete Pets library set. Publishing a local
Pets build uses its own recorded `version`:

```sh
./gradlew -p pets publish -PpetsRepository=/absolute/path/to/maven
./gradlew build -PpetsSource=artifacts -PpetsRepository=/absolute/path/to/maven
```

The Maven repository is exclusive for `dev.martianzoo.pets`; it does not replace the repositories
for third-party dependencies. Hosted releases and their version policy remain future work.

Almanac is a ZIP publication with a runtime outgoing variant for local composite use.
Web consumes the same distribution in both modes and copies it into its resources. No runtime
build script reads another build's output directory or requires its source for artifact consumption.
The combined Web app still adds optional art from Solarnet's `_local/images`; standalone Almanac
accepts `-PlocalImagesDir=/absolute/path` or its own `_local/images`.

The Solarnet wrapper isolates generated files by Gradle build directory as well as concurrent
invocation. The root build, included Pets build, and their convention builds each have separate
output pools, including when Pets is selected with `./gradlew -p pets`.

## Isolation verification

`scripts/verify-build-separation.py` copies current, non-ignored source into fresh directories,
builds/tests Pets and publishes JVM/JS libraries and Almanac to an empty local repository, then
builds/tests/packages Solarnet using only those artifacts. It also exercises independent local
source selections, explicit artifact override, and missing-source diagnostics. CI runs this check
in addition to the normal composite build. Temporary copies and publications are removed afterward.

See [Pets build independence](../../pets/docs/agents/BUILDS.md#build-independence) for its
output layout, convention plugins, and static-analysis policy. Solarnet retains its own
account/worktree isolation script.
