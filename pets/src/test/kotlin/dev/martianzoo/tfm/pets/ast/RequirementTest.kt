package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Requirement.Companion.requirement
import dev.martianzoo.tfm.pets.ast.Requirement.Max
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.ScalarAndType.Companion.sat
import dev.martianzoo.tfm.pets.testRoundTrip
import dev.martianzoo.tfm.pets.testSampleStrings
import org.junit.jupiter.api.Test

// Most testing is done by AutomatedTest
private class RequirementTest {

  val inputs =
      """
    0
    11
    Bar
    1, 5
    0 Ahh
    11 Foo
    PROD[1]
    PROD[11]
    PROD[Ahh]
    MAX 11 Eep
    PROD[0 Qux]
    PROD[11 Bar]
    =1 Megacredit
    =11 Megacredit
    PROD[MAX 1 Ooh]
    =0 Ooh<Ooh, Eep>
    MAX 11 Megacredit
    =1 Abc OR PROD[11]
    =11 Xyz(HAS =0 Abc)
    PROD[MAX 1 Abc<Bar>]
    PROD[PROD[MAX 1 Qux]]
    PROD[PROD[0 Foo<Qux>]]
    PROD[5 Qux<Bar> OR Ahh]
    PROD[Foo<Ahh<Foo, Ahh>>]
    PROD[PROD[=1 Megacredit]]
    (5 Foo, MAX 0 Foo, 0), Ahh
    =5 Xyz<Foo>, MAX 1 Xyz<Foo>
    0 Bar, 5 Eep<Foo>, MAX 1 Qux
    MAX 1 Xyz<Eep<Qux<Ahh>>, Wau>
    MAX 11 Bar<Ooh<Xyz, Foo<Bar>>>
    Bar<Foo>(HAS MAX 1 Qux), =1 Bar
    5 Foo<Ahh, Foo, Foo<Qux>(HAS 0)>
    (Abc OR MAX 0 Qux) OR PROD[0 Abc]
    MAX 1 Xyz<Eep<Ooh<Foo<Bar<Bar>>>>>
    MAX 1 Megacredit, PROD[0], Wau<Ahh>
    PROD[0 Abc, Wau, (=0 Megacredit, 1)]
    11 Abc<Bar<Ooh<Bar>, Abc>(HAS 5 Foo)>
    PROD[=5 Megacredit, =0 Xyz, MAX 0 Xyz]
    MAX 1 Eep<Ooh>, =11 Qux, MAX 0 Eep<Ooh>
    (MAX 5 Megacredit, Foo), PROD[MAX 1 Bar]
    PROD[(0 Xyz OR ((0 OR 0 Bar) OR 1)) OR 0]
    PROD[0 OR Abc], ((=1 Eep, Foo), =0 Xyz, 1)
    (MAX 1 Qux, Ooh<Foo>, 0 Abc<Ahh, Qux>), Bar
    Ahh<Xyz, Xyz>, PROD[Qux<Bar<Bar<Foo>>, Bar>]
    (MAX 1 Megacredit, PROD[0]), MAX 0 Megacredit
    MAX 1 Xyz OR (MAX 1 Megacredit, (0 Abc, 0)), 1
    ((5 Bar<Xyz>, Ooh), 1) OR ((Bar, 0 Foo), 5 Bar)
    PROD[(1 OR MAX 1 Ooh, 1), (0, MAX 0 Qux OR Qux)]
    PROD[Foo, =1 Megacredit] OR PROD[1 OR 0 Qux<Bar>]
    MAX 11 Ahh<Wau<Ahh<Ooh, Bar>, Ahh<Qux, Eep>>, Ahh>
    PROD[0 OR MAX 1 Foo] OR PROD[=1 Ooh OR =0 Qux<Qux>]
    (Xyz OR (1 OR Qux), 0 Qux), ((Bar, 0), (Bar, 0 Bar))
    (5, PROD[MAX 1 Foo<Foo>]), ((0, Foo<Foo>), MAX 1 Abc)
    PROD[(0 Abc OR MAX 5 Megacredit) OR (0 Qux OR 0), Foo]
    MAX 1 Eep OR PROD[1] OR MAX 0 Eep OR (1 OR 0, Abc<Foo>)
    PROD[Qux OR MAX 1 Megacredit OR MAX 1 Foo OR 1] OR 0 Xyz
    (MAX 1 Ooh OR MAX 1 Foo<Ooh> OR =1 Foo) OR (1, MAX 1 Ooh)
    (5 Abc OR MAX 5 Qux) OR (Bar OR 0 Bar) OR (MAX 0 Xyz, Abc)
    (MAX 1 Foo OR (Bar OR (0 OR 0 Foo))) OR (=1 Abc, 1, (1, 1))
    (MAX 1 Megacredit OR Qux) OR =1 Megacredit, =11 Abc OR 0 Qux
  """
          .trimIndent()

  @Test
  fun testSampleStrings() {
    val pass = testSampleStrings<Requirement>(inputs)
    assertThat(pass).isTrue()
  }

  @Test
  fun simpleSourceToApi() {
    assertThat(requirement("Foo")).isEqualTo(Min(sat(typeExpr = cn("Foo").ptype)))
    assertThat(requirement("3 Foo")).isEqualTo(Min(sat(3, cn("Foo").ptype)))
    assertThat(requirement("MAX 3 Foo")).isEqualTo(Max(sat(3, cn("Foo").ptype)))
  }

  @Test
  fun simpleApiToSource() {
    assertThat(Min(sat(typeExpr = cn("Foo").ptype)).toString()).isEqualTo("Foo")
    assertThat(Min(sat(1, cn("Foo").ptype)).toString()).isEqualTo("Foo")
    assertThat(Min(sat(3, cn("Foo").ptype)).toString()).isEqualTo("3 Foo")
    assertThat(Min(sat(scalar = 3)).toString()).isEqualTo("3")
    assertThat(Min(sat(scalar = 3, cn("Megacredit").ptype)).toString()).isEqualTo("3")
    assertThat(Min(sat(typeExpr = cn("Megacredit").ptype)).toString()).isEqualTo("1")
    assertThat(Max(sat(0, cn("Foo").ptype)).toString()).isEqualTo("MAX 0 Foo")
    assertThat(Max(sat(typeExpr = cn("Foo").ptype)).toString()).isEqualTo("MAX 1 Foo")
    assertThat(Max(sat(1, cn("Foo").ptype)).toString()).isEqualTo("MAX 1 Foo")
    assertThat(Max(sat(3, cn("Foo").ptype)).toString()).isEqualTo("MAX 3 Foo")
    assertThat(Max(sat(scalar = 3)).toString()).isEqualTo("MAX 3 Megacredit")
  }

  private fun testRoundTrip(start: String, end: String = start) =
      testRoundTrip<Requirement>(start, end)

  @Test
  fun roundTrips() {
    testRoundTrip("1", "1")
    testRoundTrip("Megacredit", "1")
    testRoundTrip("1 Megacredit", "1")
    testRoundTrip("Plant")
    testRoundTrip("1 Plant", "Plant")
    testRoundTrip("3 Plant")
    testRoundTrip("MAX 0 Plant")
    testRoundTrip("MAX 1 Plant")
    testRoundTrip("MAX 3 Plant")
    testRoundTrip("CityTile<LandArea>, GreeneryTile<WaterArea>")
    testRoundTrip("PlantTag, MicrobeTag OR AnimalTag")
    testRoundTrip("(PlantTag, MicrobeTag) OR AnimalTag")
  }

  @Test
  fun testProd() {
    testRoundTrip("PROD[2]")
    testRoundTrip("Steel, PROD[1]")
    testRoundTrip("PROD[Steel, 1]")
    testRoundTrip("PROD[Steel OR 1]")
  }

  @Test
  fun hairy() {
    val parsed =
        requirement(
            "Adjacency<CityTile<Anyone>, OceanTile> OR 1 Adjacency<OceanTile, CityTile<Anyone>>")
    assertThat(parsed)
        .isEqualTo(
            Requirement.Or(
                setOf(
                    Min(
                        sat(
                            typeExpr =
                                cn("Adjacency")
                                    .addArgs(
                                        cn("CityTile").addArgs(cn("Anyone").ptype),
                                        cn("OceanTile").ptype))),
                    Min(
                        sat(
                            typeExpr =
                                cn("Adjacency")
                                    .addArgs(
                                        cn("OceanTile").ptype,
                                        cn("CityTile").addArgs(cn("Anyone").ptype)))))))
  }
}
