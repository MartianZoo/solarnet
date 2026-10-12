// Development-server endpoint for the mockup layout file. The static distribution has none.
//   GET  /layout.json         current file contents, or {} before the first save
//   PUT  /layout.json         replaces the file
//   GET  /layout-events       server-sent event whenever the file changes on disk
if (process.env.WEBPACK_SERVE) {
  const fs = require("fs");
  const path = require("path");
  const argument = process.argv.find((arg) => arg.startsWith("playgroundLayout="));
  const layoutFile = argument && argument.substring("playgroundLayout=".length);
  if (!layoutFile) throw new Error("playgroundLayout argument missing from webpack command line");

  config.devServer = config.devServer || {};
  config.devServer.open = false;
  // Its own port, so it can run beside the viewer's development server on 8080.
  config.devServer.port = 8086;
  config.devServer.setupMiddlewares = (middlewares, devServer) => {
    const app = devServer.app;
    const listeners = new Set();
    let lastWritten = null;

    fs.mkdirSync(path.dirname(layoutFile), { recursive: true });
    fs.watch(path.dirname(layoutFile), (event, name) => {
      if (name !== path.basename(layoutFile) || !fs.existsSync(layoutFile)) return;
      const text = fs.readFileSync(layoutFile, "utf8");
      if (text === lastWritten) return;
      lastWritten = text;
      listeners.forEach((response) => response.write("data: changed\n\n"));
    });

    app.get("/layout.json", (request, response) => {
      const text = fs.existsSync(layoutFile) ? fs.readFileSync(layoutFile, "utf8") : "{}";
      response.type("application/json").send(text);
    });
    app.put("/layout.json", (request, response) => {
      let body = "";
      request.setEncoding("utf8");
      request.on("data", (chunk) => (body += chunk));
      request.on("end", () => {
        lastWritten = body;
        fs.writeFileSync(layoutFile, body);
        response.status(204).end();
      });
    });
    app.get("/layout-events", (request, response) => {
      response.writeHead(200, {
        "Content-Type": "text/event-stream",
        "Cache-Control": "no-cache",
        Connection: "keep-alive",
      });
      response.write(": connected\n\n");
      listeners.add(response);
      request.on("close", () => listeners.delete(response));
    });
    return middlewares;
  };
}
