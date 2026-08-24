# Canon map Pets generation

**Status: experiment.** Canon loads map and area Class Declarations from generated Pets resources.
The JSON map files remain temporarily as the redundant source of `MarsMapDefinition` topology and
bonus metadata used by custom behavior and tools.

`./gradlew :tools:generateMapPets` reads `Canon.marsMapDefinitions` and writes one parse-validated
Pets block per bundle under `tools/build/generated/mapPets/canon/bundles/`. The selected format
puts every map declaration first, followed by one commented generated-area section. Each area stays
on one line, area kinds are aligned, and map rows are separated with blank lines. This block is
maintained at the end of each bundle's sole `classes.pets`. When a structured Definition and an
explicit declaration share a Class Name, the explicit declaration supplies the Class while the
Definition remains available as metadata.

Because the `tools` module depends on `canon`, generation is still an explicit maintenance step and
cannot feed Canon's own resource processing without a build dependency cycle. Removing JSON at
runtime requires deriving topology, bonuses, and map grouping from the Pets declarations first.
