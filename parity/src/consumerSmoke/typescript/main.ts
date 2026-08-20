import { dev } from "solarnet-parity";

declare const require: {
  (id: string): unknown;
  resolve(id: string): string;
};

type EventBatch = {
  nextCursor: number;
  lines: string[];
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
  ) as { phase: string };
  check(printNewEvents().lines.length > 0, "the second move produced no events");
  check(snapshot.phase === "ActionPhase", "the session did not reach the action phase");
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
  ) as { phase: string };
  const projectEvents = printNewEvents();
  check(projectSnapshot.phase === "ActionPhase", "the project ended the action phase");
  check(projectEvents.lines.some((line) => line.includes("EarthOffice")), "project not logged");
  check(
    projectEvents.lines.some((line) =>
      line.includes("Pay<Player1, Class<Megacredit>> FROM Megacredit<Player1>"),
    ),
    "project payment not logged",
  );
} finally {
  session.close();
}
