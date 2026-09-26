# English wording comparisons

Printed wording, current generated English, and authored Pets, grouped by category and source expansion.

| Category | Entries | Missing printed transcription |
| --- | ---: | ---: |
| [Corporations](corporations.md) | 53 | 5 |
| [Projects](projects.md) | 426 | 0 |
| [Preludes](preludes.md) | 71 | 0 |
| [Milestones](milestones.md) | 51 | 16 |
| [Awards](awards.md) | 40 | 5 |
| [Global events](global-events.md) | 36 | 0 |

## Reading the comparisons

**Not transcribed** means there is no row for that class in the published evidence corpus. An em dash means an existing row or generated region has no text. Square-bracketed Pets in generated text are unresolved renderer fragments, not printed wording.

Coverage includes every canonical card, milestone, award, and global event, plus the abstract Beginner Corporation, replay-only card models named in the published evidence, and replay-only goals. Thawer’s printed evidence is paired with `FakeThawer`, its only modeled variant. Replay-only models are listed separately; they may deliberately differ from the physical cards. Classes distinguish names shared by different milestone or award variants.

Card tables retain the bottom/top text regions. Costs, tags, fixed victory-point icons, and other icon-only information are outside those text regions. Global events compare resolution text only. Pets blocks serialize each authored class declaration with normalized formatting; they are not transcriptions of the physical component. Inherited behavior is declared on the named superclass (including the five Beginner Corporation copies).

These are all categories with published wording corpora and dedicated renderers currently available. Colony tiles, political parties, standard projects, and map bonuses do not yet have comparison corpora here.

## Regenerate

From the repository root:

```sh
./gradlew :tfm-text:writeEnglishTextComparisons
```

This renders directly from the current model, without relying on saved generated-text snapshots. Printed wording is read only from the published evidence TSVs.
