# Branch merging and worktree isolation

> **Read when:** the user asks to merge a branch, run `merge-all-work`, or perform the separate
> post-merge worktree advance.
>
> **Skip when:** doing routine Git inspection, commits, or work confined to this working copy.
>
> **Status:** mandatory repository procedure.

## Isolation model

Other linked or sibling working copies are out of scope. Never discover, enumerate, resolve,
inspect, read, write, or otherwise access them. Do not run `git worktree list`, use `git -C` against
another working copy, search the filesystem for one, or read its path metadata from Git internals.

Names such as `work1` are local branch refs. Their committed histories and tips may be inspected and
merged through this repository without locating any associated working copy. Never advance, reset,
or otherwise update a source branch ref after merging it; another inaccessible working copy may
have that branch checked out.

## Merge requests

When asked to “merge from work1,” run `./_local/scripts/merge-from-work work1` and follow its instructions.
If that helper does not exist, tell the user to perform the request from `main`.

All merge and synchronization work must operate only in this project root or temporary storage
under `$TMPDIR`. It must merge committed branch tips into `main` without inspecting or changing
uncommitted work elsewhere. It must never stage, commit, stash, discard, format, or otherwise alter
another working copy.

A request to run `merge-all-work` authorizes only that branch-merging purpose. Its helper must use a
branch-only workflow from this project root and leave every source ref unchanged.

## Explicit post-merge exception

The ignored `_local/scripts/advance-worktrees-to-main` helper is the sole exception to the isolation rule.
Run it only when the user explicitly asks for that separate post-merge step. It may inspect
registered `workN` working copies and fast-forward only those that are clean and whose branch tips
are already contained in verified `origin/main`. It must leave dirty, missing, or divergent working
copies unchanged.
