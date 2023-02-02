//[pets](../../../index.md)/[dev.martianzoo.tfm.pets](../index.md)/[ClassDeclarationParsers](index.md)

# ClassDeclarationParsers

[jvm]\
object [ClassDeclarationParsers](index.md) : [PetParser](../-pet-parser/index.md)

Parses the PETS language.

## Properties

| Name | Summary |
|---|---|
| [oneLineDeclaration](one-line-declaration.md) | [jvm]<br>val [oneLineDeclaration](one-line-declaration.md): Parser&lt;[ClassDeclaration](../../dev.martianzoo.tfm.data/-class-declaration/index.md)&gt;<br>Parses a one-line declaration such as found in `cards.json5` for cards like B10 (UNMI). |
| [scalar](../-pet-parser/scalar.md) | [jvm]<br>val [scalar](../-pet-parser/scalar.md): Parser&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)&gt; |
| [topLevelDeclarationGroup](top-level-declaration-group.md) | [jvm]<br>val [topLevelDeclarationGroup](top-level-declaration-group.md): Parser&lt;[List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ClassDeclaration](../../dev.martianzoo.tfm.data/-class-declaration/index.md)&gt;&gt;<br>Parses a section of `components.pets` etc. |
