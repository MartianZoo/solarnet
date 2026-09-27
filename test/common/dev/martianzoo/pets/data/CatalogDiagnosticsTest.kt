package dev.martianzoo.pets.data

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Quantifier.OPTIONAL
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import dev.martianzoo.pets.types.loadTypes
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * An author's successive attempts to compile a small catalog. Every rejection checks the exact
 * exception class, explanation, complete rendered message, and authored source span.
 *
 * Stop at the master class table: no premise, World, instruction execution, or component limits.
 */
internal class CatalogDiagnosticsTest {
  @Test
  internal fun authoringACatalog() {

    // Start with a declaration file, its names, delimiters, and property syntax.
    mistake(
        "misspelled keyword",
        "CLAS Garden",
        PetSyntaxException::class,
        "expected `ABSTRACT` or `CLASS` or a newline or quoted text; found `CLAS`",
        1,
        1,
        "CLAS",
    )
    mistake(
        "lowercase keyword",
        "class Garden",
        PetSyntaxException::class,
        "expected `ABSTRACT` or `CLASS` or a newline or quoted text; found `class`",
        1,
        1,
        "class",
    )
    mistake(
        "missing class keyword",
        "ABSTRACT Garden",
        PetSyntaxException::class,
        "expected `CLASS`; found `Garden`",
        1,
        10,
        "Garden",
    )
    mistake(
        "missing class name",
        "CLASS",
        PetSyntaxException::class,
        "expected a class name; found end of input",
        1,
        6,
        "",
    )
    mistake(
        "lowercase class name",
        "CLASS garden",
        PetSyntaxException::class,
        "expected a class name; found `garden`",
        1,
        7,
        "garden",
    )
    mistake(
        "numeric class name",
        "CLASS 2Garden",
        PetSyntaxException::class,
        "expected a class name; found `2`",
        1,
        7,
        "2",
    )
    mistake(
        "keyword as class name",
        "CLASS HAS",
        PetSyntaxException::class,
        "expected a class name; found `HAS`",
        1,
        7,
        "HAS",
    )
    mistake(
        "two class names",
        "CLASS Garden, Plant",
        PetSyntaxException::class,
        "expected `:` or `<` or `ABSTRACT` or `CLASS` or `{` or a newline or quoted text; found `,`",
        1,
        13,
        ",",
    )
    mistake(
        "unfinished final declaration",
        """
        CLASS Plant
        CLASS Garden<
        """
            .trimIndent(),
        PetSyntaxException::class,
        "expected `@` or a class name; found end of input",
        2,
        14,
        "",
    )
    mistake(
        "dangling docstring",
        """
        CLASS Plant
        "My garden"
        """
            .trimIndent(),
        PetSyntaxException::class,
        "expected `ABSTRACT` or `CLASS` or a newline; found end of input",
        2,
        12,
        "",
    )
    mistake(
        "unclosed docstring",
        """
        "My garden
        CLASS Garden
        """
            .trimIndent(),
        PetSyntaxException::class,
        "unterminated quoted text; expected a closing double quote",
        1,
        1,
        "\"My garden\nCLASS Garden",
    )
    mistake(
        "missing dependency bracket",
        "CLASS Garden<Plant",
        PetSyntaxException::class,
        "expected `(` or `,` or `<` or `>` or `@`; found end of input",
        1,
        19,
        "",
    )
    mistake(
        "trailing dependency comma",
        "CLASS Garden<Plant,>",
        PetSyntaxException::class,
        "expected `@` or a class name; found `>`",
        1,
        20,
        ">",
    )
    mistake(
        "missing superclass",
        "CLASS Garden :",
        PetSyntaxException::class,
        "expected `@` or a class name; found end of input",
        1,
        15,
        "",
    )
    mistake(
        "missing body brace",
        """
        CLASS Garden {
          HAS MAX 1 This
        """
            .trimIndent(),
        PetSyntaxException::class,
        "expected `(` or `,` or `.` or `<` or `@` or `MAX` or `OR` or `{` or `}` or a newline; found end of input",
        2,
        17,
        "",
    )
    mistake(
        "extra body brace",
        "CLASS Garden {} }",
        PetSyntaxException::class,
        "expected `ABSTRACT` or `CLASS` or a newline or quoted text; found `}`",
        1,
        17,
        "}",
    )
    mistake(
        "missing body separator",
        "CLASS Garden { cost = 2 score = 3 }",
        PetSyntaxException::class,
        "expected `;` or `}` or a newline; found `score`",
        1,
        25,
        "score",
    )
    mistake(
        "wrong property capitalization",
        "CLASS Garden { Cost = 2 }",
        PetSyntaxException::class,
        "expected `(` or `->` or `/` or `::` or `:` or `<` or `@` or `BY` or `IF` or `OR` or `{`; found `=`",
        1,
        21,
        "=",
    )
    mistake(
        "missing property value",
        "CLASS Garden { cost = }",
        PetSyntaxException::class,
        "expected `COUNT` or `HAS` or `Metric` or `Number` or `Requirement` or a non-negative integer; found `}`",
        1,
        23,
        "}",
    )
    mistake(
        "negative property value",
        "CLASS Garden { cost = -1 }",
        PetSyntaxException::class,
        "expected `COUNT` or `HAS` or `Metric` or `Number` or `Requirement` or a non-negative integer; found `-`",
        1,
        23,
        "-",
    )
    mistake(
        "fractional property value",
        "CLASS Garden { cost = 1.5 }",
        PetSyntaxException::class,
        "expected `;` or `}` or a newline; found `.`",
        1,
        24,
        ".",
    )
    mistake(
        "integer overflow",
        "CLASS Garden { cost = 2147483648 }",
        PetSyntaxException::class,
        "integer `2147483648` exceeds the maximum supported value 2147483647",
        1,
        23,
        "2147483648",
    )
    mistake(
        "unquoted metric",
        "CLASS Garden { score = COUNT Plant }",
        PetSyntaxException::class,
        "expected quoted text; found `Plant`",
        1,
        30,
        "Plant",
    )
    mistake(
        "invalid quoted metric",
        "CLASS Garden { score = COUNT \"Plant +\" }",
        PetSyntaxException::class,
        "expected `(` or `-` or `.` or `<` or `@` or `MAX` or `OR` or `{`; found `+`",
        1,
        37,
        "+",
    )
    mistake(
        "invalid quoted requirement",
        "CLASS Garden { requirement = HAS \"0 Plant\" }",
        PetSyntaxException::class,
        "count must be positive; found 0",
        1,
        35,
        "0",
    )

    // Try signatures and class bodies that violate language rules.
    mistake(
        "refined dependency",
        "CLASS Garden<Plant(HAS Water)>",
        PetSyntaxException::class,
        "class signatures cannot contain refined Types: `Plant(HAS Water)`",
        1,
        14,
        "Plant",
    )
    mistake(
        "refined supertype",
        "CLASS Garden : Plant(NOT Weed)",
        PetSyntaxException::class,
        "class signatures cannot contain refined Types: `Plant(NOT Weed)`",
        1,
        16,
        "Plant",
    )
    mistake(
        "duplicate supertype",
        "CLASS Garden : Plant, Plant",
        PetSyntaxException::class,
        "duplicate supertype `Plant` on `Garden`",
        1,
        23,
        "Plant",
    )
    mistake(
        "duplicate property",
        "CLASS Garden { cost = 2; cost = 3 }",
        PetSyntaxException::class,
        "property `cost` is assigned twice: `2` and `3`",
        1,
        26,
        "cost",
    )
    mistake(
        "duplicate invariant",
        "CLASS Garden { HAS MAX 1 This; HAS MAX 1 This }",
        PetSyntaxException::class,
        "duplicate invariant `HAS MAX 1 This` on `Garden`",
        1,
        36,
        "MAX",
    )
    mistake(
        "defaults for another class",
        "CLASS Garden { DEFAULT +Plant? }",
        PetSyntaxException::class,
        "`Garden` cannot declare defaults for `Plant`; name `Garden` instead",
        1,
        25,
        "Plant",
    )
    mistake(
        "defaults naming different classes",
        "CLASS Garden { DEFAULT Garden; DEFAULT Plant }",
        PetSyntaxException::class,
        "`DEFAULT` clauses name different classes: `Garden, Plant`",
        1,
        40,
        "Plant",
    )
    mistake(
        "conflicting dependency defaults",
        "CLASS Garden { DEFAULT Garden<Plant>; DEFAULT Garden<Water> }",
        PetSyntaxException::class,
        "Garden: conflicting dependency defaults: `[Plant], [Water]`",
        1,
        47,
        "Garden",
    )
    mistake(
        "conflicting quantifier defaults",
        "CLASS Garden { DEFAULT +Garden!; DEFAULT +Garden? }",
        PetSyntaxException::class,
        "Garden: conflicting quantifier defaults: `!, ?`",
        1,
        43,
        "Garden",
    )
    mistake(
        "refined default",
        "CLASS Garden { DEFAULT Garden(HAS Plant) }",
        PetSyntaxException::class,
        "DEFAULT must name an unrefined class expression; found `Garden(HAS Plant)`",
        1,
        24,
        "Garden",
    )
    mistake(
        "zero minimum",
        "CLASS Garden { HAS 0 Plant }",
        PetSyntaxException::class,
        "count must be positive; found 0",
        1,
        20,
        "0",
    )
    mistake(
        "backwards range",
        "CLASS Garden { HAS 3..2 Plant }",
        PetSyntaxException::class,
        "expected `(` or `,` or `;` or `@` or `EVAL` or `OR` or `RANK` or `}` or a class name or a newline or a non-negative integer or a property name; found `.`",
        1,
        21,
        ".",
    )
    mistake(
        "bare instruction number",
        "CLASS Garden { This: 3 }",
        PetSyntaxException::class,
        "money amounts must name `MC` explicitly",
        1,
        22,
        "3",
    )
    mistake(
        "bare requirement number",
        "CLASS Garden { HAS 3 }",
        PetSyntaxException::class,
        "money amounts must name `MC` explicitly",
        1,
        20,
        "3",
    )
    mistake(
        "zero instruction count",
        "CLASS Garden { This: 0 Plant }",
        PetSyntaxException::class,
        "count must be positive; found 0",
        1,
        22,
        "0",
    )
    mistake(
        "repeated alternative",
        "CLASS Garden { This: Plant OR Plant }",
        PetSyntaxException::class,
        "duplicate OR alternative `Plant`; remove the repeated alternative",
        1,
        31,
        "Plant",
    )
    mistake(
        "class literal subscription",
        "CLASS Garden { Class<Plant>: Water }",
        PetSyntaxException::class,
        "effect trigger cannot be a Class type: `Class<Plant>`",
        1,
        16,
        "Class",
    )
    mistake(
        "universe subscription",
        "CLASS Garden { Component: Plant }",
        PetSyntaxException::class,
        "`Component` trigger requires `IF` or `BY`",
        1,
        16,
        "Component",
    )
    mistake(
        "custom invariant",
        "CLASS Garden : Custom { HAS MAX 1 This }",
        PetSyntaxException::class,
        "Custom class `Garden` cannot declare Pets invariants; its behavior comes from its Kotlin implementation",
        1,
        29,
        "MAX",
    )
    mistake(
        "custom effect",
        "CLASS Garden : Custom { This: Plant }",
        PetSyntaxException::class,
        "Custom class `Garden` cannot declare Pets effects or actions; its behavior comes from its Kotlin implementation",
        1,
        25,
        "This",
    )
    mistake(
        "custom default",
        "CLASS Garden : Custom { DEFAULT Garden }",
        PetSyntaxException::class,
        "Custom class `Garden` cannot declare Pets defaults; its behavior comes from its Kotlin implementation",
        1,
        33,
        "Garden",
    )
    mistake(
        "unused type variable",
        """
        ABSTRACT CLASS Plant
        CLASS Garden<P@Plant>
        """
            .trimIndent(),
        PetSyntaxException::class,
        "Type-variable marker P@Plant is not shared; use it again in the same scope or remove the marker",
        2,
        14,
        "P",
    )
    mistake(
        "unsupplied type variable",
        "CLASS Garden { This: P@Plant }",
        PetSyntaxException::class,
        "Type-variable marker P@Plant is not shared in a scope; use it again in that scope or remove the marker",
        1,
        22,
        "P",
    )
    mistake(
        "repeated local class",
        "CLASS Garden { This: Plant<> {}; This: Plant<> {} }",
        PetSyntaxException::class,
        "owner `Garden` declares more than one unnamed derived `Plant` Class",
        1,
        40,
        "Plant",
    )
    mistake(
        "nested local class",
        "CLASS Garden { This: Plant<> { This: Water<> {} } }",
        PetSyntaxException::class,
        "owner-local Classes cannot contain owner-local Classes",
        1,
        22,
        "Plant",
    )

    // Compile the namespace, inheritance, dependency bounds, and properties.
    mistake(
        "duplicate class",
        """
        CLASS Plant
        ABSTRACT CLASS Plant
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "conflicting declarations of `Plant`; first declared at 1:7 as `CLASS Plant`",
        2,
        16,
        "Plant",
    )
    mistake(
        "redefined system class",
        "CLASS Component",
        InvalidPetDefinitionException::class,
        "conflicting declarations of `Component`; first declared as `ABSTRACT CLASS Component { DEFAULT +Component!; DEFAULT -Component! }`",
        1,
        7,
        "Component",
    )
    mistake(
        "reserved contextual class",
        "CLASS This",
        PetSyntaxException::class,
        "`This` refers to the enclosing class and cannot be declared as a class name",
        1,
        7,
        "This",
    )
    mistake(
        "unknown superclass",
        "CLASS Garden : Gardn",
        InvalidPetDefinitionException::class,
        "`Garden` names undeclared Class `Gardn`; declare it or correct the name",
        1,
        16,
        "Gardn",
    )
    mistake(
        "unknown dependency",
        "CLASS Garden<Plnat>",
        InvalidPetDefinitionException::class,
        "`Garden` names undeclared Class `Plnat`; declare it or correct the name",
        1,
        14,
        "Plnat",
    )
    mistake(
        "unknown nested argument",
        "CLASS Garden<Owned<Plnat>>",
        InvalidPetDefinitionException::class,
        "`Garden` names undeclared Class `Plnat`; declare it or correct the name",
        1,
        20,
        "Plnat",
    )
    mistake(
        "unknown effect result",
        "CLASS Garden { This: Plnat }",
        InvalidPetDefinitionException::class,
        "`Garden` names undeclared Class `Plnat`; declare it or correct the name",
        1,
        22,
        "Plnat",
    )
    mistake(
        "unknown trigger",
        "CLASS Garden { Plnat: Ok }",
        InvalidPetDefinitionException::class,
        "`Garden` names undeclared Class `Plnat`; declare it or correct the name",
        1,
        16,
        "Plnat",
    )
    mistake(
        "unknown invariant",
        "CLASS Garden { HAS MAX 1 Plnat }",
        InvalidPetDefinitionException::class,
        "`Garden` names undeclared Class `Plnat`; declare it or correct the name",
        1,
        26,
        "Plnat",
    )
    mistake(
        "unknown property expression",
        "CLASS Garden { score = COUNT \"Plnat\" }",
        InvalidPetDefinitionException::class,
        "`Garden` names undeclared Class `Plnat`; declare it or correct the name",
        1,
        31,
        "Plnat",
    )
    mistake(
        "unknown default",
        "CLASS Garden : Owned { DEFAULT Garden<Plnat> }",
        InvalidPetDefinitionException::class,
        "`Garden` names undeclared Class `Plnat`; declare it or correct the name",
        1,
        39,
        "Plnat",
    )
    mistake(
        "explicit universal superclass",
        "CLASS Garden : Component",
        InvalidPetDefinitionException::class,
        "`Garden` must not name `Component` as a supertype; every class extends it already",
        1,
        16,
        "Component",
    )
    mistake(
        "concrete superclass",
        """
        CLASS Plant
        CLASS Flower : Plant
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "`Flower` cannot extend concrete Classes: Plant; declare the superclass ABSTRACT if it is intended to be extended",
        2,
        16,
        "Plant",
    )
    mistake(
        "concrete enclosing class",
        """
        CLASS Plant {
          CLASS Flower
        }
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "`Flower` cannot extend concrete Classes: Plant; declare the superclass ABSTRACT if it is intended to be extended",
        2,
        9,
        "Flower",
    )
    mistake(
        "self inheritance",
        "ABSTRACT CLASS Plant : Plant",
        InvalidPetDefinitionException::class,
        "inheritance cycle: `Plant` -> `Plant`; a class cannot extend itself",
        1,
        24,
        "Plant",
    )
    mistake(
        "inheritance cycle",
        """
        ABSTRACT CLASS Plant : Garden
        ABSTRACT CLASS Garden : Plant
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "inheritance cycle: `Plant` -> `Garden` -> `Plant`; a class cannot extend itself",
        2,
        25,
        "Plant",
    )
    mistake(
        "self dependency",
        "ABSTRACT CLASS Plant<Plant>",
        InvalidPetDefinitionException::class,
        "`Plant` has a circular dependency: resolving its dependency bounds requires those same bounds",
        1,
        22,
        "Plant",
    )
    mistake(
        "dependency cycle",
        """
        CLASS Plant<Garden>
        CLASS Garden<Plant>
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "`Plant` has a circular dependency: resolving its dependency bounds requires those same bounds",
        1,
        13,
        "Garden",
    )
    mistake(
        "signal dependency",
        "CLASS Garden<Signal>",
        InvalidPetDefinitionException::class,
        "`Garden` dependency `Garden_0` cannot target `Signal`; `Signal` types and `Die` cannot be dependency targets",
        1,
        14,
        "Signal",
    )
    mistake(
        "impossible dependency",
        "CLASS Garden<Die>",
        InvalidPetDefinitionException::class,
        "`Garden` dependency `Garden_0` cannot target `Die`; `Signal` types and `Die` cannot be dependency targets",
        1,
        14,
        "Die",
    )
    mistake(
        "unmatched superclass argument",
        """
        CLASS Plant
        CLASS Garden : Owned<Plant>
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "invalid definition for `Garden`: argument `Plant` does not match an available dependency; declared bounds: `[Owned_0=Anyone]`; already supplied: none",
        2,
        22,
        "Plant",
    )
    mistake(
        "too many arguments",
        """
        CLASS Alice : Owner
        CLASS Garden : Owned<Alice, Alice>
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "invalid definition for `Garden`: argument `Alice` does not match an available dependency; declared bounds: `[Owned_0=Anyone]`; already supplied: `Owned_0 <- Alice`",
        2,
        29,
        "Alice",
    )
    mistake(
        "parameterized class literal",
        "CLASS Garden<Class<Owned<Owner>>>",
        InvalidPetDefinitionException::class,
        "invalid definition for `Garden`: a Class literal accepts one bare class name; found `Class<Owned<Owner>>`",
        1,
        20,
        "Owned",
    )
    mistake(
        "multiple class literal operands",
        "CLASS Garden<Class<Owner, Owned>>",
        InvalidPetDefinitionException::class,
        "invalid definition for `Garden`: a Class literal accepts one bare class name; found `Class<Owner, Owned>`",
        1,
        27,
        "Owned",
    )
    mistake(
        "concrete abstract property",
        "CLASS Garden { cost = Number }",
        InvalidPetDefinitionException::class,
        "`Garden` is concrete but has abstract properties: cost; supply values or declare `Garden` ABSTRACT",
        1,
        16,
        "cost",
    )
    mistake(
        "unfilled inherited property",
        """
        ABSTRACT CLASS Plant { cost = Number }
        CLASS Flower : Plant
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "`Flower` is concrete but has abstract properties: cost; supply values or declare `Flower` ABSTRACT",
        2,
        7,
        "Flower",
    )
    mistake(
        "overriding fixed property",
        """
        ABSTRACT CLASS Plant { cost = 2 }
        CLASS Flower : Plant { cost = 3 }
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "`Flower` cannot override inherited property `cost = 2` from `Plant` with `3`",
        2,
        24,
        "cost",
    )
    mistake(
        "wrong property type",
        """
        ABSTRACT CLASS Plant { cost = Number }
        CLASS Flower : Plant { cost = HAS "Plant" }
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "`Flower` cannot narrow inherited property `cost = Number` from `Plant` with `HAS \"Plant\"`",
        2,
        24,
        "cost",
    )
    mistake(
        "unrelated inherited properties",
        """
        ABSTRACT CLASS Plant { cost = Number }
        ABSTRACT CLASS Water { cost = Number }
        CLASS Garden : Plant, Water { cost = 3 }
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "`Garden` inherits distinct properties named `cost` from `Plant` and `Water`",
        3,
        7,
        "Garden",
    )
    mistake(
        "divergent inherited properties",
        """
        ABSTRACT CLASS Resource { cost = Number }
        ABSTRACT CLASS Plant : Resource { cost = 2 }
        ABSTRACT CLASS Water : Resource { cost = 3 }
        CLASS Garden : Plant, Water
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "`Garden` inherits divergent narrowings for `cost` from `Plant` (2) and `Water` (3)",
        4,
        7,
        "Garden",
    )
    mistake(
        "missing custom implementation",
        "CLASS Garden : Custom",
        InvalidPetDefinitionException::class,
        "custom class implementation not found for `Garden`",
        1,
        7,
        "Garden",
    )
    mistake(
        "unknown transform",
        """
        CLASS Plant
        CLASS Garden { This: TYPO[Plant] }
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "`Garden` uses undefined transform kind `TYPO` in `TYPO[Plant]`; available kinds: none",
        2,
        22,
        "TYPO",
    )
    mistake(
        "identity subscription",
        "CLASS Garden { Ok: Garden }",
        InvalidPetDefinitionException::class,
        "`Garden` effect `Ok: Garden` subscribes to `Ok`, whose root is `Ok` or a nominal supertype of `Ok`",
        1,
        16,
        "Ok",
    )
    mistake(
        "identity supertype subscription",
        "CLASS Garden { Signal: Garden }",
        InvalidPetDefinitionException::class,
        "`Garden` effect `Signal: Garden` subscribes to `Signal`, whose root is `Ok` or a nominal supertype of `Ok`",
        1,
        16,
        "Signal",
    )
    mistake(
        "gaining class representatives",
        """
        CLASS Plant
        CLASS Garden { This: Class<Plant> }
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "class representatives cannot be gained by an effect: `Class<Plant>`",
        2,
        22,
        "Class",
    )

    // Defaults must be checked even if nobody reads them after compilation.
    mistake(
        "conflicting inherited quantifiers",
        """
        ABSTRACT CLASS Eager { DEFAULT +Eager. }
        ABSTRACT CLASS Choosy { DEFAULT +Choosy? }
        CLASS Garden : Eager, Choosy
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "`Garden` inherits conflicting gain quantifier defaults: AMAP, OPTIONAL",
        3,
        7,
        "Garden",
    )
    mistake(
        "default without a dependency",
        """
        CLASS Plant
        CLASS Garden { DEFAULT Garden<Plant> }
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "invalid defaults for `Garden`: argument `Plant` does not match an available dependency; declared bounds: `[]`; already supplied: none",
        2,
        31,
        "Plant",
    )
    mistake(
        "wrong default bound",
        """
        CLASS Plant
        CLASS Garden : Owned { DEFAULT Garden<Plant> }
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "invalid defaults for `Garden`: argument `Plant` does not match an available dependency; declared bounds: `[Owned_0=Anyone]`; already supplied: none",
        2,
        39,
        "Plant",
    )
    mistake(
        "invalid invariant metric",
        "CLASS Garden { HAS MAX 1 COUNT 2 }",
        PetSyntaxException::class,
        "expected `(` or `,` or `;` or `@` or `EVAL` or `OR` or `RANK` or `}` or a class name or a newline or a non-negative integer or a property name; found `COUNT`",
        1,
        26,
        "COUNT",
    )

    // Keep pinpointing mistakes as the author grows and reformats the catalog.
    mistake(
        "unexpected punctuation",
        "CLASS Garden \$",
        PetSyntaxException::class,
        "unrecognized character `\$`",
        1,
        14,
        "\$",
    )
    mistake(
        "nested declaration in semicolon body",
        "ABSTRACT CLASS Garden { cost = Number; CLASS Plant }",
        PetSyntaxException::class,
        "expected `(` or `->` or `-` or `@` or `DEFAULT` or `HAS` or `X` or a class name or a non-negative integer or a property name; found `CLASS`",
        1,
        40,
        "CLASS",
    )
    mistake(
        "unfinished action",
        "CLASS Garden { Plant -> }",
        PetSyntaxException::class,
        "expected `(` or `-` or `=` or `@` or `EACH` or `EVAL` or `MAX` or `X` or a class name or a non-negative integer or a property name; found `}`",
        1,
        25,
        "}",
    )
    mistake(
        "unfinished alternative",
        "CLASS Garden { This: Plant OR }",
        PetSyntaxException::class,
        "expected `(` or `-` or `@` or `EACH` or `X` or a class name or a non-negative integer; found `}`",
        1,
        31,
        "}",
    )
    mistake(
        "repeated equal property",
        "CLASS Garden { cost = 2; cost = 2 }",
        PetSyntaxException::class,
        "property `cost` is assigned twice: `2` and `2`",
        1,
        26,
        "cost",
    )
    mistake(
        "unknown name after comments and CRLF",
        "// A garden\r\nCLASS Plant\r\nCLASS Garden {\r\n\tThis: Plnat\r\n}",
        InvalidPetDefinitionException::class,
        "`Garden` names undeclared Class `Plnat`; declare it or correct the name",
        4,
        8,
        "Plnat",
    )
    mistake(
        "unknown name after continuation",
        "CLASS Plant\nCLASS Garden {\n  This: Plant,\\\n    Plnat\n}",
        InvalidPetDefinitionException::class,
        "`Garden` names undeclared Class `Plnat`; declare it or correct the name",
        4,
        5,
        "Plnat",
    )
    mistake(
        "unknown name in a nested declaration",
        """
        ABSTRACT CLASS Garden {
          CLASS Plant<Plnat>
        }
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "`Plant` names undeclared Class `Plnat`; declare it or correct the name",
        2,
        15,
        "Plnat",
    )
    mistake(
        "unknown name in an owner-local declaration",
        """
        ABSTRACT CLASS Plant
        CLASS Garden { This: Plant<> { HAS MAX 1 Plnat } }
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "`Garden_Plant` names undeclared Class `Plnat`; declare it or correct the name",
        2,
        42,
        "Plnat",
    )
    mistake(
        "unknown transform in a quoted property",
        """
        CLASS Plant
        CLASS Garden { score = COUNT "TYPO[Plant]" }
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "`Garden` uses undefined transform kind `TYPO` in `TYPO[Plant]`; available kinds: none",
        2,
        31,
        "TYPO",
    )
    mistake(
        "unknown name in a multiline body property",
        """
        CLASS Garden {
          score = COUNT "Plnat"
        }
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "`Garden` names undeclared Class `Plnat`; declare it or correct the name",
        2,
        18,
        "Plnat",
    )
    mistake(
        "incompatible inherited bounds",
        """
        ABSTRACT CLASS Resource
        CLASS Plant : Resource
        CLASS Water : Resource
        ABSTRACT CLASS Holder<Resource>
        ABSTRACT CLASS HasPlant : Holder<Plant>
        ABSTRACT CLASS HasWater : Holder<Water>
        CLASS Garden : HasPlant, HasWater
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "`Garden` inherits incompatible bounds for `Holder_0`: `Holder_0=Plant` and `Holder_0=Water`",
        7,
        7,
        "Garden",
    )
    mistake(
        "incompatible inherited defaults",
        """
        ABSTRACT CLASS Resource
        CLASS Plant : Resource
        CLASS Water : Resource
        ABSTRACT CLASS Holder<Resource>
        ABSTRACT CLASS HasPlant : Holder { DEFAULT HasPlant<Plant> }
        ABSTRACT CLASS HasWater : Holder { DEFAULT HasWater<Water> }
        CLASS Garden : HasPlant, HasWater
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "`Garden` inherits incompatible defaults for `Holder_0`: `Holder_0=Plant` and `Holder_0=Water`",
        7,
        7,
        "Garden",
    )
    mistake(
        "nested class name collision",
        """
        ABSTRACT CLASS Garden { CLASS Plant }
        ABSTRACT CLASS Water { ABSTRACT CLASS Plant }
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "conflicting declarations of `Plant`; first declared at 1:31 as `CLASS Plant : Garden`",
        2,
        39,
        "Plant",
    )
    mistake(
        "owner-local class name collision",
        """
        ABSTRACT CLASS Plant
        CLASS Garden { This: Plant<> {} }
        ABSTRACT CLASS Garden_Plant
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "conflicting declarations of `Garden_Plant`; first declared at 2:22 as `CLASS Garden_Plant : Plant`",
        3,
        16,
        "Garden_Plant",
    )
    mistake(
        "concrete type variable bound",
        """
        CLASS Plant
        CLASS Garden<P@Plant> { This: P@Plant }
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "`P@Plant` cannot declare a Class-header Type variable; its bound must be abstract",
        2,
        14,
        "P",
    )
    mistake(
        "mixed anonymous and named variables",
        """
        ABSTRACT CLASS Plant
        CLASS Garden<@Plant, P@Plant>
        """
            .trimIndent(),
        PetSyntaxException::class,
        "Anonymous Type-variable marker @Plant cannot share a scope with a named variable of the same bound Class",
        2,
        14,
        "@",
    )
    mistake(
        "nested class literal",
        "CLASS Garden<Class<Class<Owner>>>",
        InvalidPetDefinitionException::class,
        "invalid definition for `Garden`: a Class literal accepts one bare class name; found `Class<Class<Owner>>`",
        1,
        20,
        "Class",
    )
    mistake(
        "action without its supporting classes",
        """
        CLASS Plant
        CLASS Garden { Plant -> Plant }
        """
            .trimIndent(),
        InvalidPetDefinitionException::class,
        "`Garden` requires undeclared Class `UseAction`",
        2,
        7,
        "Garden",
    )

    // A forward reference must still blame the class that wrote the invalid declaration.
    mistake(
        "invalid default reached through a subclass",
        "CLASS Rose : Plant\nABSTRACT CLASS Plant { DEFAULT Plant<Water> }\nCLASS Water",
        InvalidPetDefinitionException::class,
        "invalid defaults for `Plant`: argument `Water` does not match an available dependency; declared bounds: `[]`; already supplied: none",
        2,
        38,
        "Water",
    )
    mistake(
        "invalid bound reached through a subclass",
        "CLASS Rose : Plant\nABSTRACT CLASS Plant<Holder<Water>>\nABSTRACT CLASS Holder<Soil>\nCLASS Soil\nCLASS Water",
        InvalidPetDefinitionException::class,
        "invalid definition for `Plant`: argument `Water` does not match an available dependency; declared bounds: `[]`; already supplied: none",
        2,
        29,
        "Water",
    )

    // After fixing the declarations, compilation succeeds even with forward references.
    val table =
        loadTypes(
            """
            CLASS Rose : Plant { cost = 2 }
            ABSTRACT CLASS Plant : Owned {
              cost = Number
              DEFAULT +Plant?
            }
            CLASS Garden<Gardener> {
              HAS MAX 1 This
              score = COUNT "Rose<Gardener>"
              This: Rose<Gardener>
            }
            CLASS Gardener : Owner
            """
                .trimIndent()
        )
    val rose = table.getClass(cn("Rose"))
    assertEquals(NumberValue(2), rose.properties[PropertyName("cost")])
    assertEquals(OPTIONAL, rose.defaults.gainOnly.quantifier)
    assertEquals(rose, table.resolve(parse<Expression>("Rose<Gardener>")).rootClass)
  }

  @Test
  internal fun renderedMessagesShowAuthoredContext() {
    val examples =
        listOf(
            "CLASS" to "expected a class name; found end of input at 1:6\nCLASS\n     ^",
            "// A garden\r\nCLASS Plant\r\nCLASS Garden {\r\n\tThis: Plnat\r\n}" to
                "`Garden` names undeclared Class `Plnat`; declare it or correct the name at 4:8\n\tThis: Plnat\n\t      ^",
            "CLASS Garden { score = COUNT \"Missing\" }" to
                "`Garden` names undeclared Class `Missing`; declare it or correct the name at 1:31\nCLASS Garden { score = COUNT \"Missing\" }\n                              ^",
        )
    examples.forEach { (source, expected) ->
      assertEquals(expected, assertFailsWith<PetException> { loadTypes(source) }.message)
    }
  }

  @Test
  internal fun sourceSpansAreNotDeclarationIdentity() {
    val first = parseClasses("CLASS Plant")
    val second = parseClasses("// another contribution\n  CLASS Plant")
    assertEquals(first, second)
    assertEquals(first.single(), ClassDeclaration.indexByName(first + second)[cn("Plant")])
    assertEquals(1, first.single().className.sourceLocation?.line)
    assertEquals(2, second.single().className.sourceLocation?.line)
  }

  @Test
  internal fun conflictingContributionsIdentifyTheSecondSource() {
    val first = parseClasses("// first file\nCLASS Plant")
    val source = "// second file\nABSTRACT CLASS Plant"
    val error =
        assertFailsWith<InvalidPetDefinitionException> {
          ClassDeclaration.indexByName(first + parseClasses(source))
        }
    assertEquals(
        "conflicting declarations of `Plant`; first declared as `CLASS Plant`",
        error.detail,
    )
    assertEquals(source, error.sourceLocation?.source)
    assertEquals(2, error.sourceLocation?.line)
    assertEquals(16, error.sourceLocation?.column)
  }

  @Test
  internal fun oneLineEntryPointUsesTheSameDiagnostic() {
    val source = "CLASS Garden { cost = 2; cost = 3 }"
    val fileError = assertFailsWith<PetSyntaxException> { parseClasses(source) }
    val oneLineError = assertFailsWith<PetSyntaxException> { parseOneLinerClass(source) }
    assertEquals(fileError.message, oneLineError.message)
    assertEquals(fileError.sourceLocation, oneLineError.sourceLocation)
  }

  @Test
  internal fun programmaticDeclarationsDoNotInventASourceLocation() {
    val error =
        assertFailsWith<PetSyntaxException> {
          ClassDeclaration(
              cn("Garden"),
              ClassDeclaration.ClassKind.CONCRETE,
              defaultsDeclaration = ClassDeclaration.DefaultsDeclaration(forClass = cn("Plant")),
          )
        }
    assertNull(error.sourceLocation)
    assertEquals(error.detail, error.message)
  }

  private fun mistake(
      name: String,
      source: String,
      expectedType: KClass<out PetException>,
      detail: String,
      line: Int,
      column: Int,
      token: String,
  ) {
    val error = assertFailsWith<PetException>(name) { loadTypes(source) }
    assertEquals(expectedType, error::class, name)
    assertEquals(detail, error.detail, name)
    val location = assertNotNull(error.sourceLocation, name)
    assertEquals(source, location.source, name)
    assertEquals(line, location.line, name)
    assertEquals(column, location.column, name)
    assertEquals(token, location.text, name)
    assertEquals(token.length, location.length, name)

    val lines = source.split('\n')
    val offset = lines.take(line - 1).sumOf { it.length + 1 } + column - 1
    assertEquals(offset, location.offset, name)
    val excerpt = lines[line - 1].trimEnd('\r')
    val caret = excerpt.take(column - 1).map { if (it == '\t') '\t' else ' ' }.joinToString("")
    assertEquals("$detail at $line:$column\n$excerpt\n$caret^", error.message, name)
  }
}
