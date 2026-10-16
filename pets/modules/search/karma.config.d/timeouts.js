// Canon loading and elaboration run inside the browser on both JS and Wasm.
config.client.mocha = Object.assign({}, config.client.mocha, { timeout: 60000 });
config.browserNoActivityTimeout = 90000;
