# compose ui-tooling ships in release APKs

**Priority:** low
**Area:** `app/build.gradle`

## Problem

```groovy
implementation "androidx.compose.ui:ui-tooling"
```

`ui-tooling` is the inspector/preview runtime. As `implementation` it is packaged into
release builds, adding size and keeping tooling classes around in a shipped app. The
`@Preview` annotations themselves only need `ui-tooling-preview`.

## Proposed fix

```groovy
implementation "androidx.compose.ui:ui-tooling-preview"
debugImplementation "androidx.compose.ui:ui-tooling"
```

## Acceptance criteria

- [ ] All `@Preview` composables still resolve and render in the IDE.
- [ ] `ui-tooling` is absent from the release APK.
