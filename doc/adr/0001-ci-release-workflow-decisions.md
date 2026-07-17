# CI Release Workflow Decisions

Status: Accepted

## Rules

1. Version truth is the latest reachable git tag, not `pom.xml`.
2. No reachable tag bootstraps from `0.0.0`.
3. `build-common.yml` resolves `resolved_version`, `commit_sha`, and `build_output_timestamp` once.
4. Publish and release jobs restore the `build-workspace` artifact instead of rebuilding.
5. Build artifact retention is one day.
6. `dry_run=true` skips Maven Central upload only.
7. Snapshot dry runs still publish to GitHub Packages.
8. Release dry runs still create the GitHub tag and GitHub release.
9. `project.build.outputTimestamp` is the checked-out commit timestamp and must stay aligned with `git.commit.time`.
10. Release assets are:
    - parent POM
    - starter POM
    - core JAR, sources JAR, javadoc JAR
    - binder JAR, sources JAR, javadoc JAR

## Required Permissions

- publish jobs:
  - `actions: read`
  - `contents: read`
  - `deployments: write`
- GitHub Packages publish:
  - `packages: write`
- create-release:
  - `actions: read`
  - `contents: write`

## Notes

- Hidden files must stay in the build artifact so `.mvn` survives restore.
- Publish jobs must `chmod +x mvnw` after artifact restore.
- External actions are pinned to immutable SHAs.
