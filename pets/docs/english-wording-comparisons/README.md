# English wording comparisons

Printed wording, current generated English, and authored Pets, grouped by category and source expansion.

| Category | Entries | Missing printed transcription |
| --- | ---: | ---: |
| [Corporations](corporations.md) | 47 | 0 |
| [Projects](projects.md) | 427 | 0 |
| [Preludes](preludes.md) | 71 | 0 |
| [Milestones](milestones.md) | 51 | 16 |
| [Awards](awards.md) | 40 | 5 |
| [Other cards](other-cards.md) | 6 | 5 |

Global Event comparisons are paused while their renderer is updated for player-owned effects. The published wording evidence remains in the source corpus.

## Reading the comparisons

**Not transcribed** means there is no row for that class in the published evidence corpus. An em dash means an existing row or generated region has no text. Square-bracketed Pets in generated text are unresolved renderer fragments, not printed wording.

Coverage includes every canonical card, milestone, and award, plus the abstract Beginner Corporation and replay-only card models named in the published evidence. Replay-only models are listed separately; they may deliberately differ from the physical cards. Classes distinguish names shared by different milestone or award variants.

Card tables retain the bottom/top text regions. Costs, tags, fixed victory-point icons, and other icon-only information are outside those text regions. Pets blocks serialize each authored class declaration with normalized formatting; they are not transcriptions of the physical component. Inherited behavior is declared on the named superclass (including the five Beginner Corporation copies).

These are all categories with published wording corpora and dedicated renderers currently available. Colony tiles, political parties, standard projects, and map bonuses do not yet have comparison corpora here.

## Regenerate

From the repository root:

```sh
./gradlew :text:writeEnglishTextComparisons
```

This renders from the current model, without relying on saved generated-text snapshots. Printed wording is read only from the published evidence TSVs.
