# Security Policy

Please report vulnerabilities privately through GitHub's [vulnerability reporting](https://github.com/Marcono1234/jtreesitter-type-gen/security/advisories/new).
If you are unsure whether an issue represents a vulnerability, report it privately nonetheless. We can then discuss it and decide whether to create a public bug report instead.

Examples for what is considered a security vulnerability:
- Code generator reads files other than those specified as CLI options
- Code generator writes files to locations other than those specified as CLI options
- Specifically crafted `node-types.json` injects arbitrary Java code in the generated code
