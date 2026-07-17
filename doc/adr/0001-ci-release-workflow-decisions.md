# CI Release Workflow Decisions

## Status

Accepted

## Context

The repository now uses GitHub Actions for three different paths:

- pull request verification
- snapshot publishing from `main` or `master`
- manual release publishing

Several parts are easy to forget because they are spread across workflow files and Maven configuration:

- where versions come from
- what `dry_run` really skips
- why the publish jobs restore a workspace artifact
- why release jobs need specific permissions
- how reproducible timestamps are kept aligned

Without one short decision record, these details drift and somebody later "simplifies" the wrong thing.

## Decisions

### 1. Workflow versioning comes from git tags, not from `pom.xml`

Workflow version resolution uses the latest reachable git tag plus the selected release strategy.

- `none` and `snapshot` resolve the next snapshot line
- `rc` resolves the next release candidate on the same line
- `patch`, `minor`, and `major` resolve the next numbered release

If no reachable tag exists, the workflow bootstraps from `0.0.0`.

The checked-in Maven version is not treated as release truth.

### 2. The build job is the single source of truth for resolved version and timestamp

`build-common.yml` resolves:

- `resolved_version`
- `build_output_timestamp`
- `commit_sha`

Later jobs consume those outputs instead of recomputing them.

This keeps publish jobs aligned with what the build job verified.

### 3. `dry_run` skips Maven Central upload only

`dry_run=true` is intentionally close to the live path.

- Central publish runs the publish profile without sending artifacts to Maven Central
- GitHub Packages still publishes
- release dry runs still create the GitHub tag and GitHub release

That behavior is deliberate because it tests the real GitHub-side path.

### 4. Release jobs publish from the build artifact handoff

Publish and release jobs restore the `build-workspace` artifact instead of rebuilding from scratch.

Why:

- the build job already resolved the version
- the build job already rewrote the Maven version
- the build job already verified the project in that exact state

The artifact includes hidden files so the Maven wrapper and `.mvn` directory survive the handoff.

Artifact retention is one day because the artifact is only needed for immediate publish jobs and short-lived rehearsals.

### 5. Reproducible timestamps use the commit timestamp

The build uses:

- `project.build.outputTimestamp=${git.commit.time}`
- `git-commit-id-maven-plugin`

The workflow also passes `-Dproject.build.outputTimestamp` from the checked-out commit timestamp into the Maven commands used in build and publish jobs.

This keeps local reproducible-build checks and workflow publishing aligned.

### 6. Job permissions stay minimal and explicit

Important examples:

- publish jobs need `deployments: write` to create GitHub deployment records
- GitHub Packages publish needs `packages: write`
- `create-release` needs:
  - `contents: write` to create the tag and release
  - `actions: read` to download the build artifact from the same run

That last point was proven by a failing rehearsal: `contents: write` alone was not enough once the release job started downloading artifacts before attaching assets.

### 7. Release pages should carry the release artifacts

The GitHub release page attaches:

- parent POM
- starter POM
- core JAR, sources JAR, javadoc JAR, POM
- binder JAR, sources JAR, javadoc JAR, POM

This makes the release page useful as a human-readable distribution record instead of only a tag with notes.

## Consequences

Good:

- versioning is deterministic
- no silent fallback to stale `pom.xml` versions
- snapshot rehearsals exercise the real GitHub publishing path
- reproducible-build settings stay aligned between local builds and workflow runs
- deployment records show publish activity in GitHub

Trade-offs:

- `dry_run` is not a no-op rehearsal; it still writes to GitHub
- release assets add some noise to the release page
- action versions are currently pinned only to major tags, not immutable commit SHAs

## Follow-up

Open items that are intentionally not solved by this ADR:

- whether to pin third-party actions to immutable commit SHAs
- whether to slim GitHub release assets by dropping less-useful module POM files
