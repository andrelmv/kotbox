<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Kotlin toolbox Changelog

## [Unreleased]

## [2.0.5] - 2026-07-12

- Changelog update - `2.0.4` by @github-actions[bot] in https://github.com/andrelmv/kotbox/pull/54
- chore(deps): bump JetBrains/qodana-action from 2026.1.0 to 2026.1.3 in the dependencies group by @dependabot[bot] in https://github.com/andrelmv/kotbox/pull/56
- chore(deps): bump org.jetbrains.qodana from 2026.1.0 to 2026.1.3 in the dependencies group by @dependabot[bot] in https://github.com/andrelmv/kotbox/pull/55
- chore(deps): bump the dependencies group with 2 updates by @dependabot[bot] in https://github.com/andrelmv/kotbox/pull/59
- chore(deps): bump the dependencies group across 1 directory with 2 updates by @dependabot[bot] in https://github.com/andrelmv/kotbox/pull/60
- chore: add support for collections of enums by @affelix in https://github.com/andrelmv/kotbox/pull/57
- chore: update plugin version to 2.0.5 by @andrelmv in https://github.com/andrelmv/kotbox/pull/61

## [2.0.4] - 2026-06-16

- @affelix made their first contribution in https://github.com/andrelmv/kotbox/pull/49

## [2.0.2] - 2026-04-21

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

[Unreleased]: https://github.com/andrelmv/kotbox/compare/2.0.5...HEAD
[2.0.5]: https://github.com/andrelmv/kotbox/compare/2.0.4...2.0.5
[2.0.4]: https://github.com/andrelmv/kotbox/compare/2.0.2...2.0.4
[2.0.2]: https://github.com/andrelmv/kotbox/compare/2.0.1...2.0.2
[2.0.1]: https://github.com/andrelmv/kotbox/compare/2.0.0...2.0.1
[2.0.0]: https://github.com/andrelmv/kotbox/compare/1.0.0...2.0.0
[1.0.0]: https://github.com/andrelmv/kotbox/commits/1.0.0
