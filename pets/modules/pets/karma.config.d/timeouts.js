// Generated parser round trips can exceed Mocha's two-second default in Wasm.
config.client.mocha = Object.assign({}, config.client.mocha, { timeout: 60000 });
