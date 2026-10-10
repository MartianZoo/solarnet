package dev.martianzoo.pets.data

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Quantifier.OPTIONAL
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import dev.martianzoo.pets.types.loadTypes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Parsing and compilation diagnostics for mistakes in a small custom catalog. */
internal class CatalogDiagnosticsTest {
  @Test
  internal fun misspelledKeyword() {
    val source = "CLAS Garden"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "expected `ABSTRACT` or `CLASS` or a newline or quoted text; found `CLAS`",
        error.detail,
    )
    assertEquals(
        """
        |expected `ABSTRACT` or `CLASS` or a newline or quoted text; found `CLAS` at 1:1
        |CLAS Garden
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun lowercaseKeyword() {
    val source = "class Garden"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "expected `ABSTRACT` or `CLASS` or a newline or quoted text; found `class`",
        error.detail,
    )
    assertEquals(
        """
        |expected `ABSTRACT` or `CLASS` or a newline or quoted text; found `class` at 1:1
        |class Garden
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun missingClassKeyword() {
    val source = "ABSTRACT Garden"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("expected `CLASS`; found `Garden`", error.detail)
    assertEquals(
        """
        |expected `CLASS`; found `Garden` at 1:10
        |ABSTRACT Garden
        |         ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun missingClassName() {
    val source = "CLASS"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("expected a class name; found end of input", error.detail)
    assertEquals(
        """
        |expected a class name; found end of input at 1:6
        |CLASS
        |     ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun lowercaseClassName() {
    val source = "CLASS garden"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("expected a class name; found `garden`", error.detail)
    assertEquals(
        """
        |expected a class name; found `garden` at 1:7
        |CLASS garden
        |      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun numericClassName() {
    val source = "CLASS 2Garden"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("expected a class name; found `2`", error.detail)
    // Prefer highlighting the entire invalid name `2Garden`, not only its numeric token.
    assertEquals(
        """
        |expected a class name; found `2` at 1:7
        |CLASS 2Garden
        |      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun keywordAsClassName() {
    val source = "CLASS HAS"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("expected a class name; found `HAS`", error.detail)
    assertEquals(
        """
        |expected a class name; found `HAS` at 1:7
        |CLASS HAS
        |      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun twoClassNames() {
    val source = "CLASS Garden, Plant"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "expected `:` or `<` or `ABSTRACT` or `CLASS` or `{` or a newline or quoted text; found `,`",
        error.detail,
    )
    assertEquals(
        """
        |expected `:` or `<` or `ABSTRACT` or `CLASS` or `{` or a newline or quoted text; found `,` at 1:13
        |CLASS Garden, Plant
        |            ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unfinishedFinalDeclaration() {
    val source =
        """
        CLASS Plant
        CLASS Garden<
        """
            .trimIndent()
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("expected `@` or a class name; found end of input", error.detail)
    assertEquals(
        """
        |expected `@` or a class name; found end of input at 2:14
        |CLASS Garden<
        |             ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun danglingDocstring() {
    val source =
        """
        CLASS Plant
        "My garden"
        """
            .trimIndent()
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("expected `ABSTRACT` or `CLASS` or a newline; found end of input", error.detail)
    // Prefer highlighting the unattached docstring; the caret only marks the end of input.
    assertEquals(
        """
        |expected `ABSTRACT` or `CLASS` or a newline; found end of input at 2:12
        |"My garden"
        |           ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unclosedDocstring() {
    val source =
        """
        "My garden
        CLASS Garden
        """
            .trimIndent()
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("unterminated quoted text; expected a closing double quote", error.detail)
    // Prefer highlighting the unterminated quoted text through the end of input.
    assertEquals(
        """
        |unterminated quoted text; expected a closing double quote at 1:1
        |"My garden
        |^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun missingDependencyBracket() {
    val source = "CLASS Garden<Plant"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("expected `(` or `,` or `<` or `>` or `@`; found end of input", error.detail)
    assertEquals(
        """
        |expected `(` or `,` or `<` or `>` or `@`; found end of input at 1:19
        |CLASS Garden<Plant
        |                  ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun trailingDependencyComma() {
    val source = "CLASS Garden<Plant,>"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("expected `@` or a class name; found `>`", error.detail)
    assertEquals(
        """
        |expected `@` or a class name; found `>` at 1:20
        |CLASS Garden<Plant,>
        |                   ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun missingSuperclass() {
    val source = "CLASS Garden :"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("expected `@` or a class name; found end of input", error.detail)
    assertEquals(
        """
        |expected `@` or a class name; found end of input at 1:15
        |CLASS Garden :
        |              ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun missingBodyBrace() {
    val source =
        """
        CLASS Garden {
          HAS MAX 1 This
        """
            .trimIndent()
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "expected `(` or `,` or `.` or `<` or `@` or `MAX` or `OR` or `{` or `}` or a newline; found end of input",
        error.detail,
    )
    assertEquals(
        """
        |expected `(` or `,` or `.` or `<` or `@` or `MAX` or `OR` or `{` or `}` or a newline; found end of input at 2:17
        |  HAS MAX 1 This
        |                ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun extraBodyBrace() {
    val source = "CLASS Garden {} }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "expected `ABSTRACT` or `CLASS` or a newline or quoted text; found `}`",
        error.detail,
    )
    assertEquals(
        """
        |expected `ABSTRACT` or `CLASS` or a newline or quoted text; found `}` at 1:17
        |CLASS Garden {} }
        |                ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun missingBodySeparator() {
    val source = "CLASS Garden { cost = 2 score = 3 }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("expected `;` or `}` or a newline; found `score`", error.detail)
    assertEquals(
        """
        |expected `;` or `}` or a newline; found `score` at 1:25
        |CLASS Garden { cost = 2 score = 3 }
        |                        ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun wrongPropertyCapitalization() {
    val source = "CLASS Garden { Cost = 2 }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "expected `(` or `->` or `/` or `::` or `:` or `<` or `@` or `BY` or `IF` or `OR` or `{`; found `=`",
        error.detail,
    )
    // Prefer highlighting `Cost`; the parser only discovers the wrong capitalization at `=`.
    assertEquals(
        """
        |expected `(` or `->` or `/` or `::` or `:` or `<` or `@` or `BY` or `IF` or `OR` or `{`; found `=` at 1:21
        |CLASS Garden { Cost = 2 }
        |                    ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun missingPropertyValue() {
    val source = "CLASS Garden { cost = }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "expected `COUNT` or `HAS` or `Metric` or `Number` or `Requirement` or a non-negative integer; found `}`",
        error.detail,
    )
    assertEquals(
        """
        |expected `COUNT` or `HAS` or `Metric` or `Number` or `Requirement` or a non-negative integer; found `}` at 1:23
        |CLASS Garden { cost = }
        |                      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun negativePropertyValue() {
    val source = "CLASS Garden { cost = -1 }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "expected `COUNT` or `HAS` or `Metric` or `Number` or `Requirement` or a non-negative integer; found `-`",
        error.detail,
    )
    assertEquals(
        """
        |expected `COUNT` or `HAS` or `Metric` or `Number` or `Requirement` or a non-negative integer; found `-` at 1:23
        |CLASS Garden { cost = -1 }
        |                      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun fractionalPropertyValue() {
    val source = "CLASS Garden { cost = 1.5 }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("expected `;` or `}` or a newline; found `.`", error.detail)
    // Prefer highlighting the complete unsupported value `1.5`, not only the decimal point.
    assertEquals(
        """
        |expected `;` or `}` or a newline; found `.` at 1:24
        |CLASS Garden { cost = 1.5 }
        |                       ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun integerOverflow() {
    val source = "CLASS Garden { cost = 2147483648 }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "integer `2147483648` exceeds the maximum supported value 2147483647",
        error.detail,
    )
    assertEquals(
        """
        |integer `2147483648` exceeds the maximum supported value 2147483647 at 1:23
        |CLASS Garden { cost = 2147483648 }
        |                      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unquotedMetric() {
    val source = "CLASS Garden { score = COUNT Plant }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("expected quoted text; found `Plant`", error.detail)
    assertEquals(
        """
        |expected quoted text; found `Plant` at 1:30
        |CLASS Garden { score = COUNT Plant }
        |                             ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun invalidQuotedMetric() {
    val source = "CLASS Garden { score = COUNT \"Plant +\" }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "expected `(` or `-` or `.` or `<` or `@` or `MAX` or `OR` or `{`; found `+`",
        error.detail,
    )
    assertEquals(
        """
        |expected `(` or `-` or `.` or `<` or `@` or `MAX` or `OR` or `{`; found `+` at 1:37
        |CLASS Garden { score = COUNT "Plant +" }
        |                                    ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun invalidQuotedRequirement() {
    val source = "CLASS Garden { requirement = HAS \"0 Plant\" }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("count must be positive; found 0", error.detail)
    assertEquals(
        """
        |count must be positive; found 0 at 1:35
        |CLASS Garden { requirement = HAS "0 Plant" }
        |                                  ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun refinedDependency() {
    val source = "CLASS Garden<Plant(HAS Water)>"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("class signatures cannot contain refined types: `Plant(HAS Water)`", error.detail)
    // Prefer highlighting `(HAS Water)`; `Plant` itself is a valid dependency bound.
    assertEquals(
        """
        |class signatures cannot contain refined types: `Plant(HAS Water)` at 1:14
        |CLASS Garden<Plant(HAS Water)>
        |             ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun refinedSupertype() {
    val source = "CLASS Garden : Plant(NOT Weed)"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("class signatures cannot contain refined types: `Plant(NOT Weed)`", error.detail)
    // Prefer highlighting `(NOT Weed)`; `Plant` itself is a valid superclass expression.
    assertEquals(
        """
        |class signatures cannot contain refined types: `Plant(NOT Weed)` at 1:16
        |CLASS Garden : Plant(NOT Weed)
        |               ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun duplicateSupertype() {
    val source = "CLASS Garden : Plant, Plant"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("duplicate supertype `Plant` on `Garden`", error.detail)
    assertEquals(
        """
        |duplicate supertype `Plant` on `Garden` at 1:23
        |CLASS Garden : Plant, Plant
        |                      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun duplicateProperty() {
    val source = "CLASS Garden { cost = 2; cost = 3 }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("property `cost` is assigned twice: `2` and `3`", error.detail)
    assertEquals(
        """
        |property `cost` is assigned twice: `2` and `3` at 1:26
        |CLASS Garden { cost = 2; cost = 3 }
        |                         ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun duplicateInvariant() {
    val source = "CLASS Garden { HAS MAX 1 This; HAS MAX 1 This }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("duplicate invariant `HAS MAX 1 This` on `Garden`", error.detail)
    // Prefer highlighting the entire second `HAS MAX 1 This` clause, starting at `HAS`.
    assertEquals(
        """
        |duplicate invariant `HAS MAX 1 This` on `Garden` at 1:36
        |CLASS Garden { HAS MAX 1 This; HAS MAX 1 This }
        |                                   ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun defaultsForAnotherClass() {
    val source = "CLASS Garden { DEFAULT +Plant? }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "`Garden` cannot declare defaults for `Plant`; name `Garden` instead",
        error.detail,
    )
    assertEquals(
        """
        |`Garden` cannot declare defaults for `Plant`; name `Garden` instead at 1:25
        |CLASS Garden { DEFAULT +Plant? }
        |                        ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun defaultsNamingDifferentClasses() {
    val source = "CLASS Garden { DEFAULT Garden; DEFAULT Plant }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("`DEFAULT` clauses name different classes: `Garden`, `Plant`", error.detail)
    assertEquals(
        """
        |`DEFAULT` clauses name different classes: `Garden`, `Plant` at 1:40
        |CLASS Garden { DEFAULT Garden; DEFAULT Plant }
        |                                       ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun conflictingDependencyDefaults() {
    val source = "CLASS Garden { DEFAULT Garden<Plant>; DEFAULT Garden<Water> }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "invalid defaults for `Garden`: conflicting dependency defaults: `<Plant>`, `<Water>`",
        error.detail,
    )
    // Prefer highlighting `<Water>` and showing the earlier conflicting `<Plant>` default.
    assertEquals(
        """
        |invalid defaults for `Garden`: conflicting dependency defaults: `<Plant>`, `<Water>` at 1:47
        |CLASS Garden { DEFAULT Garden<Plant>; DEFAULT Garden<Water> }
        |                                              ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun conflictingQuantifierDefaults() {
    val source = "CLASS Garden { DEFAULT +Garden!; DEFAULT +Garden? }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "invalid defaults for `Garden`: conflicting quantifier defaults: `!`, `?`",
        error.detail,
    )
    // Prefer pointing at the second `?` and showing the earlier conflicting `!`.
    assertEquals(
        """
        |invalid defaults for `Garden`: conflicting quantifier defaults: `!`, `?` at 1:43
        |CLASS Garden { DEFAULT +Garden!; DEFAULT +Garden? }
        |                                          ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun refinedDefault() {
    val source = "CLASS Garden { DEFAULT Garden(HAS Plant) }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "`DEFAULT` must name an unrefined class expression; found `Garden(HAS Plant)`",
        error.detail,
    )
    // Prefer highlighting `(HAS Plant)`; naming `Garden` in the default is valid.
    assertEquals(
        """
        |`DEFAULT` must name an unrefined class expression; found `Garden(HAS Plant)` at 1:24
        |CLASS Garden { DEFAULT Garden(HAS Plant) }
        |                       ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun zeroMinimum() {
    val source = "CLASS Garden { HAS 0 Plant }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("count must be positive; found 0", error.detail)
    assertEquals(
        """
        |count must be positive; found 0 at 1:20
        |CLASS Garden { HAS 0 Plant }
        |                   ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun backwardsRange() {
    val source = "CLASS Garden { HAS 3..2 Plant }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "expected `(` or `,` or `;` or `@` or `EVAL` or `OR` or `RANK` or `}` or a class name or a newline or a non-negative integer or a property name; found `.`",
        error.detail,
    )
    // Prefer highlighting the unsupported range `3..2`, rather than its first decimal point.
    assertEquals(
        """
        |expected `(` or `,` or `;` or `@` or `EVAL` or `OR` or `RANK` or `}` or a class name or a newline or a non-negative integer or a property name; found `.` at 1:21
        |CLASS Garden { HAS 3..2 Plant }
        |                    ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun bareInstructionNumber() {
    val source = "CLASS Garden { This: 3 }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("money amounts must name `MC` explicitly", error.detail)
    assertEquals(
        """
        |money amounts must name `MC` explicitly at 1:22
        |CLASS Garden { This: 3 }
        |                     ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun bareRequirementNumber() {
    val source = "CLASS Garden { HAS 3 }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("money amounts must name `MC` explicitly", error.detail)
    assertEquals(
        """
        |money amounts must name `MC` explicitly at 1:20
        |CLASS Garden { HAS 3 }
        |                   ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun zeroInstructionCount() {
    val source = "CLASS Garden { This: 0 Plant }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("count must be positive; found 0", error.detail)
    assertEquals(
        """
        |count must be positive; found 0 at 1:22
        |CLASS Garden { This: 0 Plant }
        |                     ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun repeatedAlternative() {
    val source = "CLASS Garden { This: Plant OR Plant }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "duplicate `OR` alternative `Plant`; remove the repeated alternative",
        error.detail,
    )
    assertEquals(
        """
        |duplicate `OR` alternative `Plant`; remove the repeated alternative at 1:31
        |CLASS Garden { This: Plant OR Plant }
        |                              ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun classLiteralSubscription() {
    val source = "CLASS Garden { Class<Plant>: Water }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("effect trigger cannot be a class type: `Class<Plant>`", error.detail)
    assertEquals(
        """
        |effect trigger cannot be a class type: `Class<Plant>` at 1:16
        |CLASS Garden { Class<Plant>: Water }
        |               ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun universeSubscription() {
    val source = "CLASS Garden { Component: Plant }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("`Component` trigger requires `IF` or `BY`", error.detail)
    assertEquals(
        """
        |`Component` trigger requires `IF` or `BY` at 1:16
        |CLASS Garden { Component: Plant }
        |               ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun customInvariant() {
    val source = "CLASS Garden : CustomMetric { HAS MAX 1 This }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "custom metric `Garden` cannot declare Pets invariants; its behavior comes from its Kotlin implementation",
        error.detail,
    )
    // Prefer highlighting the forbidden `HAS MAX 1 This` clause, starting at `HAS`.
    assertEquals(
        """
        |custom metric `Garden` cannot declare Pets invariants; its behavior comes from its Kotlin implementation at 1:35
        |CLASS Garden : CustomMetric { HAS MAX 1 This }
        |                                  ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun customEffect() {
    val source = "CLASS Garden : CustomMetric { This: Plant }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "custom metric `Garden` cannot declare Pets effects or actions; its behavior comes from its Kotlin implementation",
        error.detail,
    )
    assertEquals(
        """
        |custom metric `Garden` cannot declare Pets effects or actions; its behavior comes from its Kotlin implementation at 1:31
        |CLASS Garden : CustomMetric { This: Plant }
        |                              ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun customDefault() {
    val source = "CLASS Garden : CustomMetric { DEFAULT Garden }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "custom metric `Garden` cannot declare Pets defaults; its behavior comes from its Kotlin implementation",
        error.detail,
    )
    // Prefer highlighting the entire forbidden `DEFAULT Garden` clause, starting at `DEFAULT`.
    assertEquals(
        """
        |custom metric `Garden` cannot declare Pets defaults; its behavior comes from its Kotlin implementation at 1:39
        |CLASS Garden : CustomMetric { DEFAULT Garden }
        |                                      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unsuppliedTypeVariable() {
    val source = "CLASS Garden { This: P@Plant }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "type variable marker `P@Plant` is not shared in a scope; use it again in that scope or remove the marker",
        error.detail,
    )
    assertEquals(
        """
        |type variable marker `P@Plant` is not shared in a scope; use it again in that scope or remove the marker at 1:22
        |CLASS Garden { This: P@Plant }
        |                     ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun repeatedLocalClass() {
    val source = "CLASS Garden { This: Plant<> {}; This: Plant<> {} }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "owner `Garden` declares more than one unnamed derived `Plant` class",
        error.detail,
    )
    assertEquals(
        """
        |owner `Garden` declares more than one unnamed derived `Plant` class at 1:40
        |CLASS Garden { This: Plant<> {}; This: Plant<> {} }
        |                                       ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun nestedLocalClass() {
    val source = "CLASS Garden { This: Plant<> { This: Water<> {} } }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("owner-local classes cannot contain owner-local classes", error.detail)
    // Prefer highlighting the inner `Water<> {}` declaration; the outer `Plant<> { ... }` is
    // allowed.
    assertEquals(
        """
        |owner-local classes cannot contain owner-local classes at 1:22
        |CLASS Garden { This: Plant<> { This: Water<> {} } }
        |                     ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun duplicateClass() {
    val source =
        """
        CLASS Plant
        ABSTRACT CLASS Plant
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "conflicting declarations of `Plant`; first declared at 1:7 as `CLASS Plant`",
        error.detail,
    )
    assertEquals(
        """
        |conflicting declarations of `Plant`; first declared at 1:7 as `CLASS Plant` at 2:16
        |ABSTRACT CLASS Plant
        |               ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun redefinedSystemClass() {
    val source = "CLASS Component"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "conflicting declarations of `Component`; first declared as `ABSTRACT CLASS Component { DEFAULT +Component!; DEFAULT -Component! }`",
        error.detail,
    )
    assertEquals(
        """
        |conflicting declarations of `Component`; first declared as `ABSTRACT CLASS Component { DEFAULT +Component!; DEFAULT -Component! }` at 1:7
        |CLASS Component
        |      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun reservedContextualClass() {
    val source = "CLASS This"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "`This` refers to the enclosing class and cannot be declared as a class name",
        error.detail,
    )
    assertEquals(
        """
        |`This` refers to the enclosing class and cannot be declared as a class name at 1:7
        |CLASS This
        |      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unknownSuperclass() {
    val source = "CLASS Garden : Gardn"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` names undeclared class `Gardn`; declare it or correct the name",
        error.detail,
    )
    assertEquals(
        """
        |`Garden` names undeclared class `Gardn`; declare it or correct the name at 1:16
        |CLASS Garden : Gardn
        |               ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unknownDependency() {
    val source = "CLASS Garden<Plnat>"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` names undeclared class `Plnat`; declare it or correct the name",
        error.detail,
    )
    assertEquals(
        """
        |`Garden` names undeclared class `Plnat`; declare it or correct the name at 1:14
        |CLASS Garden<Plnat>
        |             ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unknownNestedArgument() {
    val source = "CLASS Garden<Owned<Plnat>>"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` names undeclared class `Plnat`; declare it or correct the name",
        error.detail,
    )
    assertEquals(
        """
        |`Garden` names undeclared class `Plnat`; declare it or correct the name at 1:20
        |CLASS Garden<Owned<Plnat>>
        |                   ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unknownEffectResult() {
    val source = "CLASS Garden { This: Plnat }"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` names undeclared class `Plnat`; declare it or correct the name",
        error.detail,
    )
    assertEquals(
        """
        |`Garden` names undeclared class `Plnat`; declare it or correct the name at 1:22
        |CLASS Garden { This: Plnat }
        |                     ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unknownTrigger() {
    val source = "CLASS Garden { Plnat: Ok }"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` names undeclared class `Plnat`; declare it or correct the name",
        error.detail,
    )
    assertEquals(
        """
        |`Garden` names undeclared class `Plnat`; declare it or correct the name at 1:16
        |CLASS Garden { Plnat: Ok }
        |               ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unknownInvariant() {
    val source = "CLASS Garden { HAS MAX 1 Plnat }"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` names undeclared class `Plnat`; declare it or correct the name",
        error.detail,
    )
    assertEquals(
        """
        |`Garden` names undeclared class `Plnat`; declare it or correct the name at 1:26
        |CLASS Garden { HAS MAX 1 Plnat }
        |                         ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unknownPropertyExpression() {
    val source = "CLASS Garden { score = COUNT \"Plnat\" }"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` names undeclared class `Plnat`; declare it or correct the name",
        error.detail,
    )
    assertEquals(
        """
        |`Garden` names undeclared class `Plnat`; declare it or correct the name at 1:31
        |CLASS Garden { score = COUNT "Plnat" }
        |                              ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unknownDefault() {
    val source = "CLASS Garden : Owned { DEFAULT Garden<Plnat> }"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` names undeclared class `Plnat`; declare it or correct the name",
        error.detail,
    )
    assertEquals(
        """
        |`Garden` names undeclared class `Plnat`; declare it or correct the name at 1:39
        |CLASS Garden : Owned { DEFAULT Garden<Plnat> }
        |                                      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun explicitUniversalSuperclass() {
    val source = "CLASS Garden : Component"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` must not name `Component` as a supertype; every class extends it already",
        error.detail,
    )
    assertEquals(
        """
        |`Garden` must not name `Component` as a supertype; every class extends it already at 1:16
        |CLASS Garden : Component
        |               ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun concreteSuperclass() {
    val source =
        """
        CLASS Plant
        CLASS Flower : Plant
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Flower` cannot extend concrete classes: `Plant`; declare the superclass `ABSTRACT` if it is intended to be extended",
        error.detail,
    )
    assertEquals(
        """
        |`Flower` cannot extend concrete classes: `Plant`; declare the superclass `ABSTRACT` if it is intended to be extended at 2:16
        |CLASS Flower : Plant
        |               ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun concreteEnclosingClass() {
    val source =
        """
        CLASS Plant {
          CLASS Flower
        }
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Flower` cannot extend concrete classes: `Plant`; declare the superclass `ABSTRACT` if it is intended to be extended",
        error.detail,
    )
    // Prefer also showing `CLASS Plant`; the implicit superclass comes from that enclosing
    // declaration.
    assertEquals(
        """
        |`Flower` cannot extend concrete classes: `Plant`; declare the superclass `ABSTRACT` if it is intended to be extended at 2:9
        |  CLASS Flower
        |        ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun selfInheritance() {
    val source = "ABSTRACT CLASS Plant : Plant"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "inheritance cycle: `Plant` -> `Plant`; a class cannot extend itself",
        error.detail,
    )
    assertEquals(
        """
        |inheritance cycle: `Plant` -> `Plant`; a class cannot extend itself at 1:24
        |ABSTRACT CLASS Plant : Plant
        |                       ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun inheritanceCycle() {
    val source =
        """
        ABSTRACT CLASS Plant : Garden
        ABSTRACT CLASS Garden : Plant
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "inheritance cycle: `Plant` -> `Garden` -> `Plant`; a class cannot extend itself",
        error.detail,
    )
    // Prefer also showing the earlier `Plant : Garden` edge that closes this inheritance cycle.
    assertEquals(
        """
        |inheritance cycle: `Plant` -> `Garden` -> `Plant`; a class cannot extend itself at 2:25
        |ABSTRACT CLASS Garden : Plant
        |                        ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun selfDependency() {
    val source = "ABSTRACT CLASS Plant<Plant>"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Plant` has a circular dependency: resolving its dependency bounds requires those same bounds",
        error.detail,
    )
    assertEquals(
        """
        |`Plant` has a circular dependency: resolving its dependency bounds requires those same bounds at 1:22
        |ABSTRACT CLASS Plant<Plant>
        |                     ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun dependencyCycle() {
    val source =
        """
        CLASS Plant<Garden>
        CLASS Garden<Plant>
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Plant` has a circular dependency: resolving its dependency bounds requires those same bounds",
        error.detail,
    )
    // Prefer showing both dependency bounds in the cycle, including the return from `Garden` to
    // `Plant`.
    assertEquals(
        """
        |`Plant` has a circular dependency: resolving its dependency bounds requires those same bounds at 1:13
        |CLASS Plant<Garden>
        |            ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun signalDependency() {
    val source = "CLASS Garden<Signal>"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` dependency `Garden_0` cannot target `Signal`; `Signal` types and `Die` cannot be dependency targets",
        error.detail,
    )
    assertEquals(
        """
        |`Garden` dependency `Garden_0` cannot target `Signal`; `Signal` types and `Die` cannot be dependency targets at 1:14
        |CLASS Garden<Signal>
        |             ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun impossibleDependency() {
    val source = "CLASS Garden<Die>"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` dependency `Garden_0` cannot target `Die`; `Signal` types and `Die` cannot be dependency targets",
        error.detail,
    )
    assertEquals(
        """
        |`Garden` dependency `Garden_0` cannot target `Die`; `Signal` types and `Die` cannot be dependency targets at 1:14
        |CLASS Garden<Die>
        |             ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unmatchedSuperclassArgument() {
    val source =
        """
        CLASS Plant
        CLASS Garden : Owned<Plant>
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "invalid definition for `Garden`: argument `Plant` does not match an available dependency; declared bounds: `Owned_0=Owner`; already supplied: none",
        error.detail,
    )
    assertEquals(
        """
        |invalid definition for `Garden`: argument `Plant` does not match an available dependency; declared bounds: `Owned_0=Owner`; already supplied: none at 2:22
        |CLASS Garden : Owned<Plant>
        |                     ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun tooManyArguments() {
    val source =
        """
        CLASS Alice : Owner
        CLASS Garden : Owned<Alice, Alice>
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "invalid definition for `Garden`: argument `Alice` does not match an available dependency; declared bounds: `Owned_0=Owner`; already supplied: `Owned_0 <- Alice`",
        error.detail,
    )
    assertEquals(
        """
        |invalid definition for `Garden`: argument `Alice` does not match an available dependency; declared bounds: `Owned_0=Owner`; already supplied: `Owned_0 <- Alice` at 2:29
        |CLASS Garden : Owned<Alice, Alice>
        |                            ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun parameterizedClassLiteral() {
    val source = "CLASS Garden<Class<Owned<Anyone>>>"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "invalid definition for `Garden`: a class literal accepts one bare class name; found `Class<Owned<Anyone>>`",
        error.detail,
    )
    // Prefer highlighting the forbidden `<Anyone>` arguments on the represented class `Owned`.
    assertEquals(
        """
        |invalid definition for `Garden`: a class literal accepts one bare class name; found `Class<Owned<Anyone>>` at 1:20
        |CLASS Garden<Class<Owned<Anyone>>>
        |                   ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun multipleClassLiteralOperands() {
    val source = "CLASS Garden<Class<Anyone, Owned>>"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "invalid definition for `Garden`: a class literal accepts one bare class name; found `Class<Anyone, Owned>`",
        error.detail,
    )
    // Prefer highlighting `, Owned`, including the comma that introduces the extra argument.
    assertEquals(
        """
        |invalid definition for `Garden`: a class literal accepts one bare class name; found `Class<Anyone, Owned>` at 1:28
        |CLASS Garden<Class<Anyone, Owned>>
        |                           ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun concreteAbstractProperty() {
    val source = "CLASS Garden { cost = Number }"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` is concrete but has abstract properties: `cost`; supply values or declare `Garden` `ABSTRACT`",
        error.detail,
    )
    // Prefer highlighting `Number`, which leaves the concrete class property without a value.
    assertEquals(
        """
        |`Garden` is concrete but has abstract properties: `cost`; supply values or declare `Garden` `ABSTRACT` at 1:16
        |CLASS Garden { cost = Number }
        |               ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unfilledInheritedProperty() {
    val source =
        """
        ABSTRACT CLASS Plant { cost = Number }
        CLASS Flower : Plant
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Flower` is concrete but has abstract properties: `cost`; supply values or declare `Flower` `ABSTRACT`",
        error.detail,
    )
    // Prefer also showing the inherited `cost = Number` declaration that still needs a value.
    assertEquals(
        """
        |`Flower` is concrete but has abstract properties: `cost`; supply values or declare `Flower` `ABSTRACT` at 2:7
        |CLASS Flower : Plant
        |      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun overridingFixedProperty() {
    val source =
        """
        ABSTRACT CLASS Plant { cost = 2 }
        CLASS Flower : Plant { cost = 3 }
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Flower` cannot override inherited property `cost = 2` from `Plant` with `3`",
        error.detail,
    )
    // Prefer highlighting the replacement value `3` and showing the inherited fixed value `2`.
    assertEquals(
        """
        |`Flower` cannot override inherited property `cost = 2` from `Plant` with `3` at 2:24
        |CLASS Flower : Plant { cost = 3 }
        |                       ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun wrongPropertyType() {
    val source =
        """
        ABSTRACT CLASS Plant { cost = Number }
        CLASS Flower : Plant { cost = HAS "Plant" }
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Flower` cannot narrow inherited property `cost = Number` from `Plant` with `HAS \"Plant\"`",
        error.detail,
    )
    // Prefer highlighting `HAS "Plant"`, whose kind does not match the inherited `Number` property.
    assertEquals(
        """
        |`Flower` cannot narrow inherited property `cost = Number` from `Plant` with `HAS "Plant"` at 2:24
        |CLASS Flower : Plant { cost = HAS "Plant" }
        |                       ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unrelatedInheritedProperties() {
    val source =
        """
        ABSTRACT CLASS Plant { cost = Number }
        ABSTRACT CLASS Water { cost = Number }
        CLASS Garden : Plant, Water { cost = 3 }
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` inherits distinct properties named `cost` from `Plant` and `Water`",
        error.detail,
    )
    // Prefer highlighting `Plant, Water` and showing both inherited `cost` declarations.
    assertEquals(
        """
        |`Garden` inherits distinct properties named `cost` from `Plant` and `Water` at 3:7
        |CLASS Garden : Plant, Water { cost = 3 }
        |      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun divergentInheritedProperties() {
    val source =
        """
        ABSTRACT CLASS Resource { cost = Number }
        ABSTRACT CLASS Plant : Resource { cost = 2 }
        ABSTRACT CLASS Water : Resource { cost = 3 }
        CLASS Garden : Plant, Water
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` inherits divergent narrowings for `cost` from `Plant` (2) and `Water` (3)",
        error.detail,
    )
    // Prefer highlighting `Plant, Water` and showing their conflicting `cost` values.
    assertEquals(
        """
        |`Garden` inherits divergent narrowings for `cost` from `Plant` (2) and `Water` (3) at 4:7
        |CLASS Garden : Plant, Water
        |      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unknownTransform() {
    val source =
        """
        CLASS Plant
        CLASS Garden { This: TYPO[Plant] }
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` uses undefined transform kind `TYPO` in `TYPO[Plant]`; available kinds: OWN",
        error.detail,
    )
    assertEquals(
        """
        |`Garden` uses undefined transform kind `TYPO` in `TYPO[Plant]`; available kinds: OWN at 2:22
        |CLASS Garden { This: TYPO[Plant] }
        |                     ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun identitySubscription() {
    val source = "CLASS Garden { Ok: Garden }"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` effect `Ok: Garden` subscribes to `Ok`, whose root is `Ok` or a nominal supertype of `Ok`",
        error.detail,
    )
    assertEquals(
        """
        |`Garden` effect `Ok: Garden` subscribes to `Ok`, whose root is `Ok` or a nominal supertype of `Ok` at 1:16
        |CLASS Garden { Ok: Garden }
        |               ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun identitySupertypeSubscription() {
    val source = "CLASS Garden { Signal: Garden }"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` effect `Signal: Garden` subscribes to `Signal`, whose root is `Ok` or a nominal supertype of `Ok`",
        error.detail,
    )
    assertEquals(
        """
        |`Garden` effect `Signal: Garden` subscribes to `Signal`, whose root is `Ok` or a nominal supertype of `Ok` at 1:16
        |CLASS Garden { Signal: Garden }
        |               ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun gainingClassRepresentatives() {
    val source =
        """
        CLASS Plant
        CLASS Garden { This: Class<Plant> }
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "class representatives cannot be gained by an effect: `Class<Plant>`",
        error.detail,
    )
    assertEquals(
        """
        |class representatives cannot be gained by an effect: `Class<Plant>` at 2:22
        |CLASS Garden { This: Class<Plant> }
        |                     ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun conflictingInheritedQuantifiers() {
    val source =
        """
        ABSTRACT CLASS Eager { DEFAULT +Eager. }
        ABSTRACT CLASS Choosy { DEFAULT +Choosy? }
        CLASS Garden : Eager, Choosy
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` inherits conflicting gain quantifier defaults: `.`, `?`",
        error.detail,
    )
    // Prefer highlighting `Eager, Choosy` and showing their conflicting default clauses.
    assertEquals(
        """
        |`Garden` inherits conflicting gain quantifier defaults: `.`, `?` at 3:7
        |CLASS Garden : Eager, Choosy
        |      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun defaultWithoutADependency() {
    val source =
        """
        CLASS Plant
        CLASS Garden { DEFAULT Garden<Plant> }
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "invalid defaults for `Garden`: argument `Plant` does not match an available dependency; declared bounds: none; already supplied: none",
        error.detail,
    )
    assertEquals(
        """
        |invalid defaults for `Garden`: argument `Plant` does not match an available dependency; declared bounds: none; already supplied: none at 2:31
        |CLASS Garden { DEFAULT Garden<Plant> }
        |                              ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun wrongDefaultBound() {
    val source =
        """
        CLASS Plant
        CLASS Garden : Owned { DEFAULT Garden<Plant> }
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "invalid defaults for `Garden`: argument `Plant` does not match an available dependency; declared bounds: `Owned_0=Owner`; already supplied: none",
        error.detail,
    )
    assertEquals(
        """
        |invalid defaults for `Garden`: argument `Plant` does not match an available dependency; declared bounds: `Owned_0=Owner`; already supplied: none at 2:39
        |CLASS Garden : Owned { DEFAULT Garden<Plant> }
        |                                      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun invalidInvariantMetric() {
    val source = "CLASS Garden { HAS MAX 1 COUNT 2 }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "expected `(` or `,` or `;` or `@` or `EVAL` or `OR` or `RANK` or `}` or a class name or a newline or a non-negative integer or a property name; found `COUNT`",
        error.detail,
    )
    assertEquals(
        """
        |expected `(` or `,` or `;` or `@` or `EVAL` or `OR` or `RANK` or `}` or a class name or a newline or a non-negative integer or a property name; found `COUNT` at 1:26
        |CLASS Garden { HAS MAX 1 COUNT 2 }
        |                         ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unexpectedPunctuation() {
    val source = "CLASS Garden \$"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("unrecognized character `\$`", error.detail)
    assertEquals(
        "unrecognized character `\$` at 1:14\nCLASS Garden \$\n             ^",
        error.message,
    )
  }

  @Test
  internal fun nestedDeclarationInSemicolonBody() {
    val source = "ABSTRACT CLASS Garden { cost = Number; CLASS Plant }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "expected `(` or `->` or `-` or `@` or `DEFAULT` or `HAS` or `X` or a class name or a non-negative integer or a property name; found `CLASS`",
        error.detail,
    )
    assertEquals(
        """
        |expected `(` or `->` or `-` or `@` or `DEFAULT` or `HAS` or `X` or a class name or a non-negative integer or a property name; found `CLASS` at 1:40
        |ABSTRACT CLASS Garden { cost = Number; CLASS Plant }
        |                                       ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unfinishedAction() {
    val source = "CLASS Garden { Plant -> }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "expected `(` or `-` or `=` or `@` or `EACH` or `EVAL` or `MAX` or `X` or a class name or a non-negative integer or a property name; found `}`",
        error.detail,
    )
    assertEquals(
        """
        |expected `(` or `-` or `=` or `@` or `EACH` or `EVAL` or `MAX` or `X` or a class name or a non-negative integer or a property name; found `}` at 1:25
        |CLASS Garden { Plant -> }
        |                        ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unfinishedAlternative() {
    val source = "CLASS Garden { This: Plant OR }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "expected `(` or `-` or `@` or `EACH` or `X` or a class name or a non-negative integer; found `}`",
        error.detail,
    )
    assertEquals(
        """
        |expected `(` or `-` or `@` or `EACH` or `X` or a class name or a non-negative integer; found `}` at 1:31
        |CLASS Garden { This: Plant OR }
        |                              ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun repeatedEqualProperty() {
    val source = "CLASS Garden { cost = 2; cost = 2 }"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals("property `cost` is assigned twice: `2` and `2`", error.detail)
    assertEquals(
        """
        |property `cost` is assigned twice: `2` and `2` at 1:26
        |CLASS Garden { cost = 2; cost = 2 }
        |                         ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unknownNameAfterCommentsAndCRLF() {
    val source = "// A garden\r\nCLASS Plant\r\nCLASS Garden {\r\n\tThis: Plnat\r\n}"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` names undeclared class `Plnat`; declare it or correct the name",
        error.detail,
    )
    assertEquals(
        "`Garden` names undeclared class `Plnat`; declare it or correct the name at 4:8\n\tThis: Plnat\n\t      ^",
        error.message,
    )
  }

  @Test
  internal fun unknownNameAfterContinuation() {
    val source =
        """
        CLASS Plant
        CLASS Garden {
          This: Plant,\
            Plnat
        }
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` names undeclared class `Plnat`; declare it or correct the name",
        error.detail,
    )
    assertEquals(
        """
        |`Garden` names undeclared class `Plnat`; declare it or correct the name at 4:5
        |    Plnat
        |    ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unknownNameInANestedDeclaration() {
    val source =
        """
        ABSTRACT CLASS Garden {
          CLASS Plant<Plnat>
        }
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Plant` names undeclared class `Plnat`; declare it or correct the name",
        error.detail,
    )
    assertEquals(
        """
        |`Plant` names undeclared class `Plnat`; declare it or correct the name at 2:15
        |  CLASS Plant<Plnat>
        |              ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unknownNameInAnInlineDeclaration() {
    val source =
        """
        ABSTRACT CLASS Plant
        CLASS Garden { This: Plant<> { HAS MAX 1 Plnat } }
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden_Plant` names undeclared class `Plnat`; declare it or correct the name",
        error.detail,
    )
    assertEquals(
        """
        |`Garden_Plant` names undeclared class `Plnat`; declare it or correct the name at 2:42
        |CLASS Garden { This: Plant<> { HAS MAX 1 Plnat } }
        |                                         ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unknownTransformInAQuotedProperty() {
    val source =
        """
        CLASS Plant
        CLASS Garden { score = COUNT "TYPO[Plant]" }
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` uses undefined transform kind `TYPO` in `TYPO[Plant]`; available kinds: OWN",
        error.detail,
    )
    assertEquals(
        """
        |`Garden` uses undefined transform kind `TYPO` in `TYPO[Plant]`; available kinds: OWN at 2:31
        |CLASS Garden { score = COUNT "TYPO[Plant]" }
        |                              ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun unknownNameInAMultilineBodyProperty() {
    val source =
        """
        CLASS Garden {
          score = COUNT "Plnat"
        }
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` names undeclared class `Plnat`; declare it or correct the name",
        error.detail,
    )
    assertEquals(
        """
        |`Garden` names undeclared class `Plnat`; declare it or correct the name at 2:18
        |  score = COUNT "Plnat"
        |                 ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun incompatibleInheritedBounds() {
    val source =
        """
        ABSTRACT CLASS Resource
        CLASS Plant : Resource
        CLASS Water : Resource
        ABSTRACT CLASS Holder<Resource>
        ABSTRACT CLASS HasPlant : Holder<Plant>
        ABSTRACT CLASS HasWater : Holder<Water>
        CLASS Garden : HasPlant, HasWater
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` inherits incompatible bounds for `Holder_0`: `Holder_0=Plant` and `Holder_0=Water`",
        error.detail,
    )
    // Prefer highlighting `HasPlant, HasWater` and showing the conflicting dependency bounds.
    assertEquals(
        """
        |`Garden` inherits incompatible bounds for `Holder_0`: `Holder_0=Plant` and `Holder_0=Water` at 7:7
        |CLASS Garden : HasPlant, HasWater
        |      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun incompatibleInheritedDefaults() {
    val source =
        """
        ABSTRACT CLASS Resource
        CLASS Plant : Resource
        CLASS Water : Resource
        ABSTRACT CLASS Holder<Resource>
        ABSTRACT CLASS HasPlant : Holder { DEFAULT HasPlant<Plant> }
        ABSTRACT CLASS HasWater : Holder { DEFAULT HasWater<Water> }
        CLASS Garden : HasPlant, HasWater
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` inherits incompatible defaults for `Holder_0`: `Holder_0=Plant` and `Holder_0=Water`",
        error.detail,
    )
    // Prefer highlighting `HasPlant, HasWater` and showing the conflicting default arguments.
    assertEquals(
        """
        |`Garden` inherits incompatible defaults for `Holder_0`: `Holder_0=Plant` and `Holder_0=Water` at 7:7
        |CLASS Garden : HasPlant, HasWater
        |      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun nestedClassNameCollision() {
    val source =
        """
        ABSTRACT CLASS Garden { CLASS Plant }
        ABSTRACT CLASS Water { ABSTRACT CLASS Plant }
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "conflicting declarations of `Plant`; first declared at 1:31 as `CLASS Plant : Garden`",
        error.detail,
    )
    assertEquals(
        """
        |conflicting declarations of `Plant`; first declared at 1:31 as `CLASS Plant : Garden` at 2:39
        |ABSTRACT CLASS Water { ABSTRACT CLASS Plant }
        |                                      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun ownerLocalClassNameCollision() {
    val source =
        """
        ABSTRACT CLASS Plant
        CLASS Garden { This: Plant<> {} }
        ABSTRACT CLASS Garden_Plant
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "conflicting declarations of `Garden_Plant`; first declared at 2:22 as `CLASS Garden_Plant : Plant`",
        error.detail,
    )
    assertEquals(
        """
        |conflicting declarations of `Garden_Plant`; first declared at 2:22 as `CLASS Garden_Plant : Plant` at 3:16
        |ABSTRACT CLASS Garden_Plant
        |               ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun concreteTypeVariableBound() {
    val source =
        """
        CLASS Plant
        CLASS Garden<P@Plant> { This: P@Plant }
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`P@Plant` cannot declare a class-header type variable; its bound must be abstract",
        error.detail,
    )
    // Prefer highlighting the bound name `Plant` in `P@Plant` and showing its concrete declaration.
    assertEquals(
        """
        |`P@Plant` cannot declare a class-header type variable; its bound must be abstract at 2:14
        |CLASS Garden<P@Plant> { This: P@Plant }
        |             ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun mixedAnonymousAndNamedVariables() {
    val source =
        """
        ABSTRACT CLASS Plant
        CLASS Garden<@Plant, P@Plant>
        """
            .trimIndent()
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        "anonymous type variable marker `@Plant` cannot share a scope with a named variable of the same bound class",
        error.detail,
    )
    // Prefer highlighting both `@Plant` and `P@Plant`, whose use in one scope is ambiguous.
    assertEquals(
        """
        |anonymous type variable marker `@Plant` cannot share a scope with a named variable of the same bound class at 2:14
        |CLASS Garden<@Plant, P@Plant>
        |             ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun nestedClassLiteral() {
    val source = "CLASS Garden<Class<Class<Anyone>>>"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "invalid definition for `Garden`: a class literal accepts one bare class name; found `Class<Class<Anyone>>`",
        error.detail,
    )
    // Prefer highlighting the complete nested `Class<Anyone>` operand, which is not a bare class
    // name.
    assertEquals(
        """
        |invalid definition for `Garden`: a class literal accepts one bare class name; found `Class<Class<Anyone>>` at 1:20
        |CLASS Garden<Class<Class<Anyone>>>
        |                   ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun actionWithoutItsSupportingClasses() {
    val source =
        """
        CLASS Plant
        CLASS Garden { Plant -> Plant }
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals("`Garden` requires undeclared class `UseAction`", error.detail)
    // Prefer pointing at `->`, which introduces the action needing `UseAction`.
    assertEquals(
        """
        |`Garden` requires undeclared class `UseAction` at 2:7
        |CLASS Garden { Plant -> Plant }
        |      ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun invalidDefaultReachedThroughASubclass() {
    val source =
        """
        CLASS Rose : Plant
        ABSTRACT CLASS Plant { DEFAULT Plant<Water> }
        CLASS Water
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "invalid defaults for `Plant`: argument `Water` does not match an available dependency; declared bounds: none; already supplied: none",
        error.detail,
    )
    assertEquals(
        """
        |invalid defaults for `Plant`: argument `Water` does not match an available dependency; declared bounds: none; already supplied: none at 2:38
        |ABSTRACT CLASS Plant { DEFAULT Plant<Water> }
        |                                     ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun invalidBoundReachedThroughASubclass() {
    val source =
        """
        CLASS Rose : Plant
        ABSTRACT CLASS Plant<Holder<Water>>
        ABSTRACT CLASS Holder<Soil>
        CLASS Soil
        CLASS Water
        """
            .trimIndent()
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "invalid definition for `Plant`: argument `Water` does not match an available dependency; declared bounds: none; already supplied: none",
        error.detail,
    )
    assertEquals(
        """
        |invalid definition for `Plant`: argument `Water` does not match an available dependency; declared bounds: none; already supplied: none at 2:29
        |ABSTRACT CLASS Plant<Holder<Water>>
        |                            ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun correctedCatalogSupportsForwardReferences() {
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
  internal fun endOfInputShowsTheCaretAfterTheFinalToken() {
    val source = "CLASS"
    val error = assertFailsWith<PetSyntaxException> { loadTypes(source) }

    assertEquals(
        """
        |expected a class name; found end of input at 1:6
        |CLASS
        |     ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun renderedMessagesPreserveTabsWithWindowsNewlines() {
    val source = "// A garden\r\nCLASS Plant\r\nCLASS Garden {\r\n\tThis: Plnat\r\n}"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        "`Garden` names undeclared class `Plnat`; declare it or correct the name at 4:8\n\tThis: Plnat\n\t      ^",
        error.message,
    )
  }

  @Test
  internal fun renderedMessagesPointInsideQuotedPets() {
    val source = "CLASS Garden { score = COUNT \"Missing\" }"
    val error = assertFailsWith<InvalidPetDefinitionException> { loadTypes(source) }

    assertEquals(
        """
        |`Garden` names undeclared class `Missing`; declare it or correct the name at 1:31
        |CLASS Garden { score = COUNT "Missing" }
        |                              ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun sourceSpansAreNotDeclarationIdentity() {
    val first = parseClasses("CLASS Plant")
    val second = parseClasses("// another contribution\n  CLASS Plant")
    assertEquals(first, second)
    assertEquals(first.single(), ClassDeclaration.indexByName(first + second)[cn("Plant")])
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
    assertEquals(
        """
        |conflicting declarations of `Plant`; first declared as `CLASS Plant` at 2:16
        |ABSTRACT CLASS Plant
        |               ^
        """
            .trimMargin(),
        error.message,
    )
  }

  @Test
  internal fun oneLineEntryPointUsesTheSameDiagnostic() {
    val source = "CLASS Garden { cost = 2; cost = 3 }"
    val fileError = assertFailsWith<PetSyntaxException> { parseClasses(source) }
    val oneLineError = assertFailsWith<PetSyntaxException> { parseOneLinerClass(source) }
    assertEquals(fileError.message, oneLineError.message)
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
    assertEquals(
        "`Garden` cannot declare defaults for `Plant`; name `Garden` instead",
        error.message,
    )
  }
}
