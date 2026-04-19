<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Kotlin toolbox Changelog

## [Unreleased]

### Added

- Copy Interpolated String value intention
- DSL builder generator
- Update shortcuts for wrap with coroutine builders

## [2.0.1] - 2026-04-04

### Added

- Password Generator tool with Random, Memorable, and PIN modes
- Customizable password length, Numbers and Symbols options
- Bulk password generation with configurable quantity (1–1000)
- Wrap with coroutine build feature

## [2.0.0] - 2026-03-25

### Fixed

- Compatibility with Kotlin K2 compiler: replaced deprecated `KotlinRecursiveElementVisitor` with `KtTreeVisitorVoid`
- Replaced `runReadAction` with `runReadActionBlocking` in inlay hint collector for correct coroutine context handling

### Changed

- Updated Gradle wrapper and plugin dependencies

## [1.0.0] - 2026-03-15

### Added

- JWT Encoder/Decoder tool 
- String interpolation inlay hints for Kotlin code
- String constant inlay hints showing evaluated values

### Changed

- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)

[Unreleased]: https://github.com/andrelmv/kotbox/compare/2.0.1...HEAD
[2.0.1]: https://github.com/andrelmv/kotbox/compare/2.0.0...2.0.1
[2.0.0]: https://github.com/andrelmv/kotbox/compare/1.0.0...2.0.0
[1.0.0]: https://github.com/andrelmv/kotbox/commits/1.0.0
