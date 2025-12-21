# jtreesitter-type-gen 

Generates a type-safe API for your Tree-sitter grammar to use with [Java Tree-sitter (jtreesitter)](https://github.com/tree-sitter/java-tree-sitter/). This tool works based on the `node-types.json` file which Tree-sitter generates for grammars.

Here is an example for using the generated API for [tree-sitter-json](https://github.com/tree-sitter/tree-sitter-json) to process a JSON document:

```java
public static void main(String[] args) {
    // create parse tree using jtreesitter
    var tree = ...;

    try (var typedTree = TypedTree.fromTree(tree)) {
        typedTree.getRootNode().getChildren().forEach(c -> printJson(c, 0));
    }
}

private static void printJson(NodeValue jsonValue, int indentation) {
    switch (jsonValue) {
        case NodeArray jsonArray -> {
            System.out.println("array:");
            jsonArray.getChildren().forEach(child -> {
                System.out.print("  ".repeat(indentation) + "- ");
                printJson(child, indentation + 1);
            });
        }
        case NodeObject jsonObject -> {
            System.out.println("object:");
            jsonObject.getChildren().forEach(pair -> {
                System.out.print("  ".repeat(indentation) + "- ");
                // Print the node text, as it appeared in the source
                System.out.print(pair.getFieldKey().getText() + ": ");
                printJson(pair.getFieldValue(), indentation + 1);
            });
        }
        case NodeFalse jsonFalse -> {
            System.out.println("false");
        }
        case NodeTrue jsonTrue -> {
            System.out.println("true");
        }
        case NodeNull jsonNull -> {
            System.out.println("null");
        }
        case NodeNumber jsonNumber -> {
            // Print the node text, as it appeared in the source
            System.out.println(jsonNumber.getText());
        }
        case NodeString jsonString -> {
            // Print the node text, as it appeared in the source
            System.out.println(jsonString.getText());
        }
    }
}
```

As you can see, the generated API includes:
- Separate classes for all node types\
  They have a _sealed_ Java interface as superinterface, and are therefore well suited for usage in Java `switch` expressions and statements.
- Type-safe getters for node children
- Type-safe getters for node fields
- Type-safe query builder (opt-in, see [Typed Query section](#typed-query) below)

See also the [Usage section](#usage) below for details.

## Usage

> [!CAUTION]\
> Only use this tool with `node-types.json` files form a trusted origin, such as from a grammar from the https://github.com/tree-sitter organization or a grammar you have created yourself.
> 
> This tool includes some validation to protect against invalid type names. However, using a `node-types.json` file from an untrusted source could in the worst case lead to arbitrary code execution or creation of files in arbitrary locations, possibly even during generation of the code.

### Requirements

- Code generation: JDK 21 or newer
- Usage of the generated code:\
  same requirements as jtreesitter, namely:
  - JDK 22 or newer
  - Tree-sitter native library (e.g. `tree-sitter.dll`), see also the [jtreesitter documentation](https://tree-sitter.github.io/java-tree-sitter/io/github/treesitter/jtreesitter/package-summary.html#libraries)
  - native library for your Tree-sitter parser
- `node-types.json` for your parser\
  This is generated as `src/node-types.json` when you run `tree-sitter generate` for your grammar, see also the [Tree-sitter documentation](https://tree-sitter.github.io/tree-sitter/creating-parsers/1-getting-started.html#generate).
  For the official Tree-sitter grammars you usually find this in the Git repository, for example <https://github.com/tree-sitter/tree-sitter-json/blob/v0.21.0/src/node-types.json>.
- (optional) name of the root node\
  This is normally the name of the first `rules` entry in the `grammar.js` file, for example for tree-sitter-json [it would be `document`](https://github.com/tree-sitter/tree-sitter-json/blob/80e623c2165887f9829357acfa9c0a0bab34a3cd/grammar.js#L24). Recent Tree-sitter versions [include this information](https://github.com/tree-sitter/tree-sitter/pull/3615) in the `node-types.json` file. 

### Generating the code

First obtain the standalone jtreesitter-type-gen CLI JAR, either from the [GitHub Releases](https://github.com/Marcono1234/jtreesitter-type-gen/releases) or by [building it yourself](#building). In the following that JAR will be referred to as `jtreesitter-type-gen.jar`.

The following options are required:
- `--node-types=<nodeTypesFile>`\
  Path to the `node-types.json` file for which code should be generated
- `--package=<packageName>`\
  Java package name to use for the generated code
- `--output-dir=<outputDir>`
  Output directory where the generated source code files should be placed in (should not include the package name)

To see all available options, run:
```sh
java -jar jtreesitter-type-gen.jar --help
```

To see the version information of the CLI, and to see the minimum required jtreesitter version, run:
```sh
java -jar jtreesitter-type-gen.jar --version
```

> [!TIP]\
> Specify these additional options to get the best experience:
> - `--root-node`\
>   If the `node-types.json` file does not contain an entry with `"root": true`, specify the root type manually to enable generation of the `TypedTree` class, making it easier to obtain the typed root node from a jtreesitter `Tree`.
> - `--language-provider`\
>   Specify how the generated code can access a jtreesitter `Language` instance of your language. This enables additional validation to ensure that the generated code is compatible with the loaded language and can increase performance.
> - `--expected-language-version`\
>   Specify the expected language / grammar version. This adds additional checks to ensure that the generated code is compatible with the loaded language, to avoid difficult to troubleshoot issues when an incompatible language version is used by accident.
> - `--generated-time`\
>   Set a custom 'generated time' to make the generated code reproducible. By default, the current time is included as value for `@Generated` annotations in the generated code.
> - `--generate-typed-query`\
>   Generates 'typed query' code which allows building a Tree-sitter query and consuming captured nodes, both in a type-safe way.
>   See the [Typed Query section](#typed-query) below for details.

### Using the generated code

A few 'base' interfaces and classes are generated regardless of which `node-types.json` is used:
- `TypedTree`:
  - Only generated if the root node name is supplied
  - Instances can be obtained through `TypedTree#fromTree(Tree)`
  - Provides access to the typed root node through `#getRootNode()`
- `TypedNode`:
  - Base interface for all generated node type classes
  - Instances can be obtained through `TypedNode#fromNode(Node)`, from a `TypedTree` or as children of other typed nodes
  - The underlying jtreesitter `Node` can be obtained again through `#getNode()`
  - _sealed_ Java interface, meaning an exhaustive Java `switch` expression or statement can be used

For all named node types a dedicated typed node class is generated:
- Its `fromNode(Node)` method can be used to obtain an instance from a jtreesitter `Node`
- Its `findNodes(TypedNode)` method can be used to find all instances of this node type
- Methods for accessing typed children and fields are provided

Switching between `TypedNode` and jtreesitter's `Node` (using `getNode` and `fromNode`) can be useful when functionality is needed which is only available through jtreesitter's `Node`. For convenience `TypedNode` directly exposes some of `Node`'s method as well (which simply delegate to the same method of the underlying node).

### Example

Using [tree-sitter-json v0.24.8 `node-types.json`](https://github.com/tree-sitter/tree-sitter-json/blob/v0.24.8/src/node-types.json)

Generate the code:
```sh
java -jar jtreesitter-type-gen.jar --node-types=node-types.json --package=com.example --output-dir=generated-src
```

Use the generated source:
```java
Language language = ...;  // obtain jtreesitter Language instance
String source = """
    {
        "name": 1.0
    }
    """;

try (
    var parser = new Parser(language);
    var typedTree = TypedTree.fromTree(parser.parse(source).orElseThrow());
) {
    var jsonObject = (NodeObject) typedTree.getRootNode().getChildren().getFirst();
    var jsonMember = jsonObject.getChildren().getFirst();
    System.out.println(jsonMember.getFieldKey());
    System.out.println(jsonMember.getFieldValue());
}
```

### Typed Query

When specifying the CLI option `--generate-typed-query`, additional 'typed query' code is generated which allows building a Tree-sitter query and consuming captured nodes, both in a type-safe way.
The Javadoc of the generated classes, espcially of the generated class `TypedQuery`, explains the usage in detail and provides examples.

The general usage looks like this:
1. Build the query using `TypedQuery.Builder`
2. Create a `TypedQuery` from the query builder objects
3. Execute the `TypedQuery`

The following example shows how to capture certain nodes from a JSON document:
```java
var q = new TypedQuery.Builder<List<NodePair>>();
var query = q.nodePair()
    .withFieldValue(q.nodeNumber().textEq("12"))
    .captured(List::add)
    .buildQuery(language);

String source = """
    {
      "a": 1,
      "b": 12,
      "c": 123
    }
    """;

try (
    var parser = new Parser(language);
    var tree = parser.parse(source).orElseThrow();
    var matches = query.findMatches(tree.getRootNode())
) {
    var nodes = new ArrayList<NodePair>();
    matches.forEach(m -> m.collectCaptures(nodes));
    assertEquals(List.of("\"b\": 12"), nodes.stream().map(NodePair::getText).toList());
}

query.close();
```

Note: If you specified the CLI option `--language-provider`, then the `language` object does not have to be passed to
the generated `buildQuery()` method.

## Project structure

- [`lib/`](./lib)\
  Underlying library for code generation. Users normally don't have to use this directly unless they want to integrate code generation in an application (e.g. a custom Maven or Gradle plugin).
- [`cli/`](./cli)\
  Standalone command line program for performing code generation. Uses the library code from `lib/` internally.

## Building

Requires JDK 21 or newer

This project uses Gradle for building. To only create the final artifacts, run:
```sh
./gradlew clean assemble
```

This produces the CLI standalone JAR as `cli/build/libs/jtreesitter-type-gen-<version>.jar`.

To perform a full build (except for integration tests, see [section below](#running-integration-test)), run:
```shell
./gradlew clean build
```

### Running integration test

This project includes integration tests which run the generated code against the Tree-sitter and grammar native libraries. Because those native libraries have to be set up manually, the integration tests are not run as part of a regular build (`gradlew build`).

The [GitHub CI workflow](./.github/workflows/build.yml) can be used as reference for which native libraries are needed and how to build them.\
Those libraries have to be placed within the [`cli/`](./cli) directory. On Linux the directory must also be included in the `LD_LIBRARY_PATH`:
```sh
export LD_LIBRARY_PATH="cli:$LD_LIBRARY_PATH"
```

Afterwards the integration tests can be executed using:


```sh
./gradlew clean cli:integrationTest
```

### Updating expected test output

Some of the tests are implemented as "snapshot tests" which contain the expected test output as test resources. When making changes to the code generation, it will be necessary to update this expected test output, otherwise the tests will fail.

To update the expected test output, run:
```sh
./gradlew clean test cli:integrationTest --continue -Dtest-update-expected
```
(note that this includes integration tests, which [require manual setup](#running-integration-test))

When running the above command, test execution will intentionally fail to indicate that the expected output was adjusted; nonetheless the expected test output was updated. When running without `-Dtest-update-expected` again it will run the tests as usual.

If any expected output changed, verify if the changes are reasonable and then commit them with Git.

## Similar projects

- (Python) <https://github.com/JoranHonig/python-tree-sitter-types>
  > Generate python types for tree-sitter parsers
- (Rust) <https://github.com/Jakobeha/type-sitter>
  > Generate typed wrappers for tree-sitter grammars from node-types.json and queries
- (Rust) <https://crates.io/crates/ts-typed-ast>
  > Typed AST generator for tree-sitter
- (Rust) <https://github.com/tibordp/tree-sitter-visitor>
  > Procedural macro for generating a visitor trait for Tree Sitter Rust bindings
- (TypeScript) <https://github.com/asgerf/dts-tree-sitter>
  > Generate TypeScript .d.ts files for using tree-sitter grammars
