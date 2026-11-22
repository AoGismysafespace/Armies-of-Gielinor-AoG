# Contributing Guidelines

Thank you for contributing to the historical preservation and maintenance of **Armies of Gielinor Standalone Edition**!

To ensure code quality, long-term maintainability, and seamless runtime stability, all contributions must strictly adhere to the standards outlined below.

---

## 1. Bytecode Compatibility Rules (Java 18 Strict)

The standalone distribution bundles an isolated OpenJDK 18 runtime (`18.0.2.1`).

- **Target Bytecode Version**: Class file major version must strictly be $\le 62.0$ (Java 18).
- **Compilation Requirement**: When compiling any Java source code with JDK 21+, you **MUST** pass `--release 18`:
  ```powershell
  javac --release 18 -cp "extracted/app/ArmiesOfGielinor.jar" -d bin src/MyClass.java
  ```
- Any class compiled without `--release 18` on newer JDKs will trigger a fatal `UnsupportedClassVersionError` on the bundled runtime.
- Always run `RestoreAndVerify` before committing to verify zero bytecode violations across all classes in `ArmiesOfGielinor.jar`.

---

## 2. Mandatory Code Commenting Rules

Whenever writing, modifying, patching, or refactoring code, scripts, or configuration files, you **MUST always comment the code thoroughly** in a way that works for both humans and AI to read, understand, and collaborate with:

1. **File & Class Header**:
   - State the component's purpose, architectural context, and rationale for technical decisions (e.g. why bytecode instrumentation or server event pumping is chosen).
2. **Rosetta Stone / Symbol Mappings**:
   - When touching obfuscated, reverse-engineered, or decompiled code (e.g. Jagex/FunOrb classes), always provide clear mapping tables detailing what every obfuscated class, field, and method represents in reality, its valid values, and its side effects.
3. **Inline Explanations**:
   - Provide inline commentary explaining *why* a change or check is being made, what failure mode or edge case it addresses (e.g. disconnection veil, input interception, race conditions), and how it coordinates with the main loop.
4. **Preserve Existing Documentation**:
   - Never remove or degrade existing docstrings or explanatory comments unless explicitly asked or replacing them with strictly more accurate documentation.

---

## 3. Privacy & Sanitization Guidelines

- Never commit personal Steam IDs, player usernames, local machine directory paths, or account metadata to source code, scripts, or documentation.
- Use generic placeholders (`Commander 1 [Host]`, `Commander 2 [Ready]`) for interface schematics and documentation.
- Use dynamic path variables (e.g. `$PSScriptRoot`, `$PWD`) rather than hardcoded absolute file system paths.

---

## 4. Submitting Pull Requests

1. Fork the repository and create a feature branch (`feature/your-feature-name`).
2. Implement your changes following the commenting and bytecode guidelines.
3. Run the automated verification tools:
   ```powershell
   powershell -ExecutionPolicy Bypass -File tools/build_release.ps1
   ```
4. Verify that all tests pass and that `ArmiesOfGielinor_v0.6.zip` builds cleanly with 0 bytecode violations.
5. Submit a concise Pull Request detailing your changes and test results.
