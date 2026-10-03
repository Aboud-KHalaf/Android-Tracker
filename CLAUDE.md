# Project Development Guidelines

## 1. Git Workflow

- Create a separate branch for each independent task or feature.
- Use descriptive branch names, such as `feature/workout-tracking`, `fix/set-validation`, and `refactor/workout-viewmodel`.
- Make a commit after every meaningful development step.
- Keep commits small, focused, and descriptive using conventional commit messages (e.g., `feat: add workout tracking`).
- Never mix unrelated changes in one branch or commit.
- Test and review changes before merging into the target branch.

## 2. Architecture — MVVM

- Follow MVVM (Model-View-ViewModel) with clear separation of concerns.
- **View:** Render UI, observe state, and forward user actions.
- **ViewModel:** Manage UI state, handle user actions, and coordinate business operations.
- **Model/Data Layer:** Manage domain models, repositories, and data sources.
- Keep business logic out of Composables and database operations out of ViewModels.
- Use repositories and use cases when they provide meaningful separation; avoid unnecessary abstractions and over-engineering.

## 3. Kotlin & Jetpack Compose

- Use Kotlin and Jetpack Compose idiomatically.
- Prefer unidirectional data flow, state hoisting, and stateless Composables where practical.
- Expose observable UI state from ViewModels using appropriate state holders, such as `StateFlow`.
- Handle loading, success, empty, and error states explicitly.
- Keep functions and classes focused, readable, and maintainable.

## 4. Material Design 3

- Follow Material Design 3 consistently; use its components and established patterns instead of reinventing them.
- Use blue as the primary brand color and define colors, typography, and shapes through a centralized Material theme.
- Support light and dark themes.
- Avoid hardcoded styling values and inconsistent UI patterns.
- Prioritize accessibility, touch targets, and layouts that adapt to different screen sizes.

## 5. Code Quality & Testing

- Prioritize readability, maintainability, low coupling, and clear naming.
- Avoid God classes, duplicated logic, premature abstractions, and unnecessary dependencies.
- Handle exceptions explicitly and provide meaningful user-facing error states.
- Write unit tests for business logic and ViewModels, and UI tests for critical user flows where appropriate.
- Run relevant tests and static analysis after meaningful changes.

## 6. Development Process

For every task:

1. Understand the requirements and inspect the existing code.
2. Create a dedicated branch.
3. Implement changes incrementally.
4. Test and commit every meaningful step.
5. Review the final changes and merge only after verification.

## Core Principle

Prefer the simplest maintainable solution that meets the requirements. Preserve existing functionality unless a change is explicitly required, and avoid unnecessary complexity, dependencies, and architectural layers.
