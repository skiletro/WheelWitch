# TODO

## ChangelogDetailScreen / VersionHistoryScreen dedup

`ChangelogDetailScreen.kt` and `VersionHistoryScreen.kt` render
`VersionHistoryState` (Loading/Error/Success) with a `ChangelogCard`
list using near-duplicate code. `VersionHistoryScreen.kt` extracts
`LoadingContent`/`ErrorContent`/`ChangelogList` private helpers;
`ChangelogDetailScreen.kt` inlines them and adds `ScreenHeader` +
`dpadScroll`.

Action: extract the shared body into a `VersionHistoryContent`
composable in `ui/components/` and have both screens delegate to it.
Or, if one screen is unreachable, delete it.
