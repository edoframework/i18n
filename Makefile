SHELL = /bin/bash

.PHONY: clean fix fmt compile prepare build test validate deps publishLocal publish \
        release-prepare release-finalize release-abort release-status

# ---------------------------------------------------------------------------
# Build (cross-platform: __ covers the JVM and Scala.js modules)
# ---------------------------------------------------------------------------

clean:
	./mill clean

fix:
	./mill __.fix

fmt:
	./mill __.reformat

compile:
	./mill __.compile

prepare: fix fmt compile

build: prepare test

# __.test.testForked resolves to both i18n.jvm.test and i18n.js.test
# (JS tests run under Node).
test:
	./mill __.test.testForked

validate: fix fmt compile test

deps:
	./mill mill.scalalib.Dependency/showUpdates

# ---------------------------------------------------------------------------
# Publishing
# ---------------------------------------------------------------------------

publishLocal:
	./mill __.publishLocal

publish:
	./mill mill.javalib.MavenPublishModule/ \
		--username "$$REPO_USERNAME" \
		--password "$$REPO_TOKEN" \
		--releaseUri https://repo.genius.fish/releases \
		--snapshotUri https://repo.genius.fish/snapshots

# ---------------------------------------------------------------------------
# Release (delegated to mill-release plugin)
# Validation and publishing run as separate Mill processes (cannot nest).
# Usage: make release-prepare TYPE=patch|minor|major
#        (review RELEASE_x_y_z.md)
#        make release-finalize
# ---------------------------------------------------------------------------

# Nothing is staged on the release path (GFSDK-2): mill-release's clean-tree check counts staged
# changes as dirty, and whatever validate rewrote is for a human to review and commit, never for
# `git add -u` to fold into the release. A dirty tree before or after validate stops here.
release-prepare:
	@if [ -z "$(TYPE)" ]; then \
		echo "[ERROR] Usage: make release-prepare TYPE=patch|minor|major"; exit 1; \
	fi
	@if [ -n "$$(git status --porcelain | grep -v '^??')" ]; then \
		echo "[ERROR] Uncommitted changes. Commit or stash them before release-prepare:"; \
		git status --short | grep -v '^??'; exit 1; \
	fi
	./mill clean
	$(MAKE) validate
	@if [ -n "$$(git status --porcelain | grep -v '^??')" ]; then \
		echo "[ERROR] validate changed files: review and commit the reformatting first, then run release-prepare again."; \
		echo "[ERROR] Nothing was staged:"; \
		git status --short | grep -v '^??'; exit 1; \
	fi
	./mill release.releasePrepare --releaseType $(TYPE)

release-finalize:
	./mill release.releaseFinalize
	$(MAKE) publishLocal

release-abort:
	./mill release.releaseAbort

release-status:
	./mill release.releaseStatus
