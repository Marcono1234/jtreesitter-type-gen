# node-types-data

`node-types.json` files which are used for code generation for the integration test.

These files come from the official Tree-sitter repositories:

- `node-types-java.json`\
  <https://github.com/tree-sitter/tree-sitter-java/blob/v0.23.4/src/node-types.json>
- `node-types-json.json`\
  <https://github.com/tree-sitter/tree-sitter-json/blob/v0.24.8/src/node-types.json>
- `node-types-python.json`\
  <https://github.com/tree-sitter/tree-sitter-python/blob/v0.23.4/src/node-types.json>

To use them in the integration test, adjust the `build.gradle.kts` `generateSourcesTask` to generate code for them,
then add a corresponding test class in the test sources.
