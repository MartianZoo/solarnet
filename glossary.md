# Glossary

This is a growing vocabulary for concepts that need consistent names across Solarnet's language,
engine, tests, and documentation.

## Effect representations

An authored effect passes through several materially different representations. Use these terms to
identify which stage is under discussion:

- **Source effect:** The effect as authored in a `.pets` file or JSON data, before engine
  transformations. `ClassDeclaration.effects` and `MClass.declaredEffects` hold its parsed AST.
- **Class effect:** The loaded-class template produced after applying defaults, atomization,
  production rewriting, and other class-level transformations. It may still contain contextual
  placeholders such as `This` or `Owner`. `MClass.classEffects` holds this representation.
- **Component effect:** A class effect specialized for one concrete component type. Its concrete
  dependencies, contextual `Owner`, and `This` have been substituted as far as the component allows.
  `Component.effects` holds this representation.
- **Active effect:** The runtime matching representation derived from a component effect. It is no
  longer exactly a Pets `Effect`: `Effector.ActiveEffect` stores a compiled trigger subscription
  separately from its instruction. Effects concerning other components are registered according to
  how many copies of their carrying component exist; self effects are also consulted directly when
  that component is gained or removed.
- **Triggered instruction:** The consequence produced when an active effect matches one particular
  change event. Trigger-derived substitutions, contextual Owner binding, and scaling have been
  applied. The result is an instruction to execute immediately or place in a task, not another
  persistent effect form.

A triggered instruction may still be abstract. Task preparation and narrowing belong to the later
instruction lifecycle, not the effect-representation lifecycle.

These form names are separate from an effect's semantic properties. For example, a triggered
effect can be discussed in source, class, component, or active form, and automaticness remains a
property of the effect rather than of the triggered instruction it produces.
