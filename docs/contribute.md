# Contribute

## Introduction

We are delighted and honored that you're not only excited by `bach`, but you've also shown an interest in contributing towards its development.

`bach` is written in [Clojure](https://clojure.org/guides/learn/syntax), so in order to make changes to the core `bach` library you will need to become familiar with it.

You should also read the [Guide](guide), [Development](dev) and [Syntax](syntax) pages before you start diving into any code.

`bach` is also open-source and is licensed under the MIT license. It is actively seeking developers to help not only improve the core library but to also create useful high-level tools in order to increase adoption.

## Source

![Github Logo](_media/github.svg) https://github.com/slurmulon/bach

## Issues

You can contribute a enormously by simply reporting any issues you find along the way.

Please open up a ticket for any issues you find on [GitHub](https://github.com/slurmulon/bach/issues).

Be sure to always provide a thorough description and all of the contextual info and steps required for reproduction.

## Changes

Before making changes to the `bach` core library or any of its related tools, please be sure to first create a ticket describing the change.

### Branches

Then create a branch adhering to the following name convention:

`<kind-of-work>/<ticket-number>-<short-description-of-specific-task>`.

Examples of branch names:

- `feat/123-add-clef-support`
- `fix/456-grammar-bug`
- `refact/789-reorganize-modules`
- `upgrade/321-build-dependencies`
- `test/654-grammar-edge-case-tests`

### Commits

Nearly all of the commit messages use [`tasty-commits`](https://github.com/slurmulon/tasty-commits) in order to make them more semantic and groupable. You don't have to use this convention but it's greatly appreciated if you decide to.

When there is a ticket associated with your work (and, ideally, there always should be), the commit message is prefixed like so:

`[ref #123] :art: Adding new feature`

This allows focused commit timelines to appear in Github issues, which I've found makes navigating the code significantly easier later on.

Its prefered that you refrain from abusing `git rebase` on your local branch as it makes `git bisect` much more limited, making it difficult or impossible to track down the root cause of a bug. It also gets hairy when multiple people need or want to work in the same branch.

### Pull Request

If you have already made some proposed changes then please ensure that any required tests are written and that any development or debugging cruft is removed.

From there you can simply open a pull request in Github.

Here is a suggested template you can use for your PR, which I've found to be useful in improving organization and archival while avoiding being overbearing.

```md
**Changes**
 - Itemized list of changes included in the PR, one bullet point for each
  * Sub-points and details of the changes belong in a nested bullet point

**Issues**
 - Itemized list of known issues and bugs that will be introduced

**Future**
 - Itemized list of outstanding units of work that should be handled soon

**Tickets**
 - Itemized list of relevant ticket numbers and their status
 - #123 (done)
```

