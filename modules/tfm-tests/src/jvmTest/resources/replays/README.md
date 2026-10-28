# Playground game, 2026-10-25

`PlaygroundGame20261025.short.json.gz` is the existing short game-file JSON format,
minified and gzip-compressed. It retains the original premise, fingerprints, game start,
and all 1077 Green commands in order. It contains no events, positions, or Admin commands.
The replay supplies the four neutral setup placements separately; all subsequent Admin
work is automatic.

Source: the user's completed Tharsis Republic / Hellas solo game, saved as
`_local/saved-game-2026-10-25_234541-tharsis-republic-hellas.json`.
Source SHA-256: `38fc0a47125f75aae6bd8569060ea74bb35674b7f678e67dd5c227d622bd1170`.

Conversion: parse the original JSON, remove `events` and `positions`, keep `start` and
commands whose actor is `Green`, serialize without whitespace, and gzip with timestamp zero.
No premise fields, fingerprints, command instructions, or task selectors were rewritten.

Expected dashboard and map values are transcribed in the test from the user's screenshots
`Screenshot 2026-10-25 at 11.50.27 PM.png` and
`Screenshot 2026-10-25 at 11.51.46 PM.png`; the user specified 151 VP. The map distinguishes
Green's tiles from neutral tiles. The source log identifies the southern special tile as
`DeimosDownPromo_SpecialTile` and the northeastern special tile as `GreatDamPromo_SpecialTile`.
