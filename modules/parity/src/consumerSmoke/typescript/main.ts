import { dev } from "solarnet-parity";

declare const require: {
  (id: string): unknown;
  resolve(id: string): string;
};

type EventBatch = {
  nextCursor: number;
  lines: string[];
};

type Resources = {
  megacredits: number;
  steel: number;
  titanium: number;
  plants: number;
  energy: number;
  heat: number;
};

type Snapshot = {
  generation: number;
  phase: string;
  firstPlayer: number;
  passedPlayers: number[];
  players: Array<{
    seat: number;
    terraformRating: number;
    resources: Resources;
    production: Resources;
    handCount: number;
    playedCardIds: string[];
  }>;
  globalParameters: { temperature: number; oxygen: number; oceans: number };
  tiles: Array<{ row: number; column: number; kind: "ocean"; owner: null }>;
};

type FileSystem = {
  readFileSync(path: string, encoding: "utf8"): string;
};

type Paths = {
  dirname(path: string): string;
  join(...paths: string[]): string;
};

function check(condition: boolean, message: string): asserts condition {
  if (!condition) {
    throw new Error(message);
  }
}

function eventBatch(json: string): EventBatch {
  const batch = JSON.parse(json) as EventBatch;
  check(Number.isInteger(batch.nextCursor), "event cursor is not an integer");
  check(Array.isArray(batch.lines), "event lines are not an array");
  check(batch.lines.every((line) => typeof line === "string"), "event line is not text");
  return batch;
}

const fs = require("node:fs") as FileSystem;
const paths = require("node:path") as Paths;
const parityKotlinDirectory = paths.dirname(require.resolve("solarnet-parity"));
const readResource = (resourcePath: string): string =>
  fs.readFileSync(paths.join(parityKotlinDirectory, resourcePath), "utf8");

const session = new dev.martianzoo.parity.SolarnetSession(
  "CorporateEraExpansion",
  2,
  readResource,
);

try {
  let cursor = eventBatch(session.eventsSince(0)).nextCursor;

  const printNewEvents = (): EventBatch => {
    const batch = eventBatch(session.eventsSince(cursor));
    check(batch.nextCursor >= cursor, "event cursor moved backward");
    batch.lines.forEach((line) => console.log(line));
    cursor = batch.nextCursor;
    return batch;
  };

  session.apply(
    JSON.stringify({
      operation: "selectCorporation",
      player: 1,
      corporation: "InterplanetaryCinematics",
      projectCards: 1,
    }),
  );
  check(printNewEvents().lines.length > 0, "the first move produced no events");

  const snapshot = JSON.parse(
    session.apply(
      JSON.stringify({
        operation: "selectCorporation",
        player: 2,
        corporation: "CrediCor",
        projectCards: 0,
      }),
    ),
  ) as Snapshot;
  check(printNewEvents().lines.length > 0, "the second move produced no events");
  check(snapshot.phase === "action", "the session did not reach the action phase");
  check(printNewEvents().lines.length === 0, "an unchanged cursor replayed events");

  const projectSnapshot = JSON.parse(
    session.apply(
      JSON.stringify({
        operation: "playProject",
        player: 1,
        cardId: "105",
        payment: { megacredits: 1, steel: 0, titanium: 0 },
      }),
    ),
  ) as Snapshot;
  const projectEvents = printNewEvents();
  check(projectSnapshot.phase === "action", "the project ended the action phase");
  check(projectEvents.lines.some((line) => line.includes("EarthOffice")), "project not logged");
  check(
    projectEvents.lines.some((line) =>
      line.includes("Pay<Player1, Class<Megacredit>> FROM Megacredit<Player1>"),
    ),
    "project payment not logged",
  );

  session.apply(
    JSON.stringify({
      operation: "standardProject",
      player: 1,
      project: "aquifer",
    }),
  );
  const standardProjectEvents = printNewEvents();
  check(
    standardProjectEvents.lines.some((line) => line.includes("-18 Megacredit<Player1>")),
    "Aquifer did not charge Player 1",
  );
  check(
    !standardProjectEvents.lines.some((line) => line.includes(": +OceanTile<")),
    "Aquifer placed its ocean before the app selected a space",
  );

  const tileSnapshot = JSON.parse(
    session.apply(
      JSON.stringify({ operation: "placeTile", player: 1, tile: "ocean", spaceId: "04" }),
    ),
  ) as Snapshot;
  const tileEvents = printNewEvents();
  check(
    tileEvents.lines.some((line) => line.includes("+OceanTile<Tharsis_1_2")),
    "Aquifer did not place its ocean at app space 04",
  );
  check(
    tileEvents.lines.some((line) => line.includes("+TerraformRating<Player1>")),
    "Aquifer did not raise Player 1's TR",
  );
  check(
    tileEvents.lines.some((line) => line.includes("+2 Steel<Player1>")),
    "app space 04 did not grant its steel bonus",
  );
  check(
    tileEvents.lines.some((line) => line.includes("NewTurn<Player2>")),
    "Player 1's second action did not rotate to Player 2",
  );
  const player1 = tileSnapshot.players.find((player) => player.seat === 1);
  check(player1 !== undefined, "the snapshot omitted Player 1");
  check(player1.resources.megacredits === 8, "the snapshot has the wrong Player 1 M€");
  check(player1.resources.steel === 22, "the snapshot omitted the space bonus");
  check(player1.terraformRating === 21, "the snapshot omitted the ocean TR");
  check(
    tileSnapshot.tiles.some(
      (tile) => tile.row === 1 && tile.column === 2 && tile.kind === "ocean" && tile.owner === null,
    ),
    "the snapshot omitted the normalized ocean tile",
  );

  const finalSnapshot = JSON.parse(
    session.apply(JSON.stringify({ operation: "pass", player: 2 })),
  ) as Snapshot;
  const passEvents = printNewEvents();
  check(passEvents.lines.some((line) => line.includes("+Pass<Player2>")), "pass not logged");
  check(
    passEvents.lines.some((line) => line.includes("NewTurn<Player1>")),
    "Player 2's pass did not rotate to Player 1",
  );
  check(finalSnapshot.passedPlayers.join() === "2", "the snapshot omitted Player 2's pass");
} finally {
  session.close();
}
