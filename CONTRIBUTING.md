# Contributing to Kotlin Developer Toolbox

Thank you for your interest in contributing to Kotlin Developer Toolbox! 🎉

## Development Setup

### Prerequisites
- JDK 21 or later
- IntelliJ IDEA 2025.3.2 or later

### Building the Project

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

### Running the Plugin in Development

```bash
./gradlew runIde
```

Or use the "Run Plugin" run configuration in IntelliJ IDEA.

## Code Style

- Follow Kotlin coding conventions
- Use 4 spaces for indentation
- Maximum line length: 140 characters
- Add KDoc comments for public APIs
- Write unit tests for new features

## Pull Request Process

1. **Fork and Clone**: Fork the repository and clone your fork
2. **Create Branch**: Create a feature branch from `main`
   ```bash
   git checkout -b feature/my-new-feature
   ```
3. **Make Changes**: Implement your feature or fix
4. **Add Tests**: Write tests for your changes
5. **Run Tests**: Ensure all tests pass
   ```bash
   ./gradlew test
   ```
6. **Run Verifications**: Run plugin verifications
   ```bash
   ./gradlew runPluginVerifier
   ```
7. **Update CHANGELOG**: Add your changes to CHANGELOG.md under `[Unreleased]`
8. **Commit**: Follow conventional commit format
   ```
   feat: add new JWT algorithm support
   fix: resolve inlay hint EDT freeze
   docs: update README with examples
   test: add tests for JsonUtils
   ```
9. **Push and PR**: Push to your fork and create a Pull Request

## Testing Guidelines

- Write unit tests for business logic
- Add integration tests for UI components when applicable
- Aim for >80% code coverage for new code
- Test edge cases and error conditions

## Code Review

All submissions require review. We use GitHub pull requests for this purpose.

## Questions?

Feel free to open an issue for questions or discussions!
