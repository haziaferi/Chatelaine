# Chatelaine

A home inventory for Android that keeps stored things in view, so what is in a closed box stays present in mind.

Status: stage 1 (data model, sync and backup) is built, with no screens yet; stage 2 is next. See [SPEC.md](SPEC.md) (R100) for the stages.

How changes land: through a pull request whose checks (`publish-check`, `build` and CodeQL) pass, merged on the owner's go-ahead; nothing is pushed straight to `main`; no version tags. The merge method is picked per pull request and named in its description:

- squash, by default, for feature and fix pull requests;
- rebase-merge when each commit was deliberately kept clean and self-contained;
- a merge commit only to sync branches, never for feature work.

## Licence

GPL-3.0. See [LICENSE](LICENSE).
