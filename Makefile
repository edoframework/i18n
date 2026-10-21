SHELL = /bin/bash

# Maven Central coordinates of one published artifact, used to see whether a version is already
# released. Central is immutable: a version that is there cannot be published again.
CENTRAL_PATH = com/edoframework/edo-i18n_3

# The release credentials (MILL_SONATYPE_USERNAME / MILL_SONATYPE_PASSWORD for the Central Portal
# user token, MILL_PGP_SECRET_BASE64 / MILL_PGP_PASSPHRASE for the release key). Read from this
# file when it exists, otherwise from the environment (CI). Never passed as arguments: a
# command line is readable by every local user in /proc.
RELEASE_ENV ?= $(HOME)/.edo-release.env

RELEASE_STATE   = /tmp/release-state-$(notdir $(CURDIR)).json
RELEASE_CONTEXT = /tmp/release-context-$(notdir $(CURDIR)).md

.PHONY: clean fix fmt compile prepare build test validate deps publishLocal publish \
        publish-dry-run release-preflight release-prepare release-finalize release-abort \
        release-status

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
# Publishing to Maven Central (namespace com.edoframework)
# ---------------------------------------------------------------------------

# A release number in ~/.ivy2/local shadows the published artifact for every build on the machine
# (Coursier reads it first). So a publishLocal at a release tag on a clean tree is refused; a
# SNAPSHOT publishLocal stays.
publishLocal:
	@if [ -z "$$(git status --porcelain | grep -v '^??')" ] && git describe --exact-match --tags --match 'v[0-9]*' HEAD >/dev/null 2>&1; then \
	  echo "[publishLocal] refused: HEAD is release tag $$(git describe --exact-match --tags HEAD); publish it instead"; \
	  exit 1; \
	fi
	./mill __.publishLocal

# Signs every artifact and uploads one bundle to the Central Portal, which validates it and
# publishes it (Mill's SonatypeCentralPublishModule, AUTOMATIC). Only a release version: the
# Central snapshot repository is not enabled for the namespace.
publish:
	@set -e; \
	if [ -f "$(RELEASE_ENV)" ]; then . "$(RELEASE_ENV)"; fi; \
	for V in MILL_SONATYPE_USERNAME MILL_SONATYPE_PASSWORD MILL_PGP_SECRET_BASE64 MILL_PGP_PASSPHRASE; do \
	  if [ -z "$${!V}" ]; then echo "[publish] $$V is not set ($(RELEASE_ENV) or the environment)"; exit 1; fi; \
	done; \
	VERSION=$$(./mill show release.gitVersion | tr -d '"'); \
	case "$$VERSION" in *-SNAPSHOT) echo "[publish] refused: $$VERSION is not a release (an exact v-tag on a clean tree)"; exit 1;; esac; \
	echo "[publish] $$VERSION to Maven Central"; \
	./mill mill.javalib.SonatypeCentralPublishModule/

# The same bundle, signed, written to out/ instead of uploaded: what Central would receive.
publish-dry-run:
	@set -e; \
	if [ -f "$(RELEASE_ENV)" ]; then . "$(RELEASE_ENV)"; fi; \
	MILL_TESTS_PUBLISH_DRY_RUN=1 ./mill mill.javalib.SonatypeCentralPublishModule/; \
	echo "[publish-dry-run] bundle in out/mill.javalib.SonatypeCentralPublishModule/publishAll.dest/repository"

# Refuses when the version at HEAD is already on Central (immutable) or is not a release.
release-preflight:
	@set -e; \
	VERSION=$$(./mill show release.gitVersion | tr -d '"'); \
	case "$$VERSION" in *-SNAPSHOT) echo "[preflight] $$VERSION is not a release"; exit 1;; esac; \
	CODE=$$(curl -s -o /dev/null -w '%{http_code}' "https://repo1.maven.org/maven2/$(CENTRAL_PATH)/$$VERSION/"); \
	if [ "$$CODE" = "200" ]; then echo "[preflight] $$VERSION is already on Maven Central"; exit 1; fi; \
	echo "[preflight] $$VERSION is not on Maven Central yet"

# ---------------------------------------------------------------------------
# Release
# Usage: make release-prepare TYPE=patch|minor|major
#        (review RELEASE_x_y_z.md)
#        make release-finalize   (notes commit, local tag, publish; the push is yours)
#
# Publishing is atomic with the tag: the tag is created locally, the artifacts are published with
# the clean version it gives, and only then do you push main and the tag. A failed publish leaves
# nothing public: make release-abort, fix the cause, retry the same version.
# ---------------------------------------------------------------------------

# Nothing is staged on the release path: the clean-tree check counts staged changes as dirty, and
# whatever validate rewrote is for a human to review and commit, never to fold into the release.
release-prepare:
	@set -e; \
	if [ -z "$(TYPE)" ]; then \
		echo "[ERROR] Usage: make release-prepare TYPE=patch|minor|major"; exit 1; \
	fi; \
	if [ -n "$$(git status --porcelain | grep -v '^??')" ]; then \
		echo "[ERROR] Uncommitted changes:"; git status --short | grep -v '^??'; exit 1; \
	fi; \
	BRANCH=$$(git branch --show-current); \
	if [ "$$BRANCH" != "main" ]; then echo "[WARN] Not on main branch ($$BRANCH)"; fi; \
	git fetch --tags; \
	CURRENT=$$(git describe --tags --abbrev=0 --match 'v[0-9]*' 2>/dev/null || echo "v0.0.0"); \
	CURRENT=$${CURRENT#v}; \
	echo "[OK] Current version: $$CURRENT"; \
	MAJOR=$${CURRENT%%.*}; REST=$${CURRENT#*.}; MINOR=$${REST%%.*}; PATCH=$${REST#*.}; \
	case "$(TYPE)" in \
		patch) NEW="$$MAJOR.$$MINOR.$$((PATCH + 1))";; \
		minor) NEW="$$MAJOR.$$((MINOR + 1)).0";; \
		major) NEW="$$((MAJOR + 1)).0.0";; \
		*) echo "[ERROR] Invalid TYPE: $(TYPE)"; exit 1;; \
	esac; \
	echo "[OK] New version: $$NEW"; \
	echo "[INFO] Cleaning build cache..."; \
	./mill clean; \
	echo "[INFO] Running validation..."; \
	$(MAKE) validate; \
	if [ -n "$$(git status --porcelain | grep -v '^??')" ]; then \
		echo "[ERROR] validate changed files: review and commit the reformatting first, then run release-prepare again."; \
		git status --short | grep -v '^??'; exit 1; \
	fi; \
	COMMITS=$$(git log "v$$CURRENT"..HEAD --pretty=format:"%h %s" --no-merges 2>/dev/null || true); \
	CHANGED=$$(git diff --name-only "v$$CURRENT"..HEAD 2>/dev/null || true); \
	COUNT=$$(echo "$$COMMITS" | grep -c . || echo "0"); \
	printf '{"currentVersion":"%s","newVersion":"%s","releaseType":"%s","branch":"%s"}\n' \
		"$$CURRENT" "$$NEW" "$(TYPE)" "$$BRANCH" > $(RELEASE_STATE); \
	NOTES="RELEASE_$${NEW//./_}.md"; \
	if command -v claude >/dev/null 2>&1; then \
		echo "[INFO] Generating release notes with Claude Code..."; \
		printf '%s\n' \
			"Generate release notes for version $$NEW of this project." \
			"" \
			"Previous version: v$$CURRENT" \
			"New version: v$$NEW" \
			"Release date: $$(date +%Y-%m-%d)" \
			"" \
			"Commits since v$$CURRENT:" \
			"$$COMMITS" \
			"" \
			"Changed files:" \
			"$$CHANGED" \
			"" \
			"Instructions:" \
			"- Read build.mill to determine the project name, Scala version, and key dependencies" \
			"- Read the changed source files to understand what was actually modified" \
			"- Do NOT rely solely on commit messages — look at the actual code changes" \
			"- Output ONLY the raw markdown, no code fences, no preamble, no explanation" \
			"- Omit sections that do not apply (e.g. skip Breaking Changes if there are none)" \
			"" \
			"Follow this template:" \
			"" \
			"# Release {version}" \
			"" \
			"**Release Date:** {YYYY-MM-DD}" \
			"" \
			"{1-2 paragraph summary of the most important changes}" \
			"" \
			"## Highlights" \
			"" \
			"- **Highlight 1** - Brief description" \
			"" \
			"## New Features" \
			"" \
			"### {Feature Name}" \
			"" \
			"{Description with code examples}" \
			"" \
			"## Changelog" \
			"" \
			"### Added" \
			"- Item" \
			"" \
			"### Changed" \
			"- Item" \
			"" \
			"### Fixed" \
			"- Item" \
			"" \
			"## Module Changes" \
			"" \
			"{List affected modules}" \
			"" \
			"## Compatibility" \
			"" \
			"{Scala version, key dependency versions from build.mill}" \
		> $(RELEASE_CONTEXT); \
		claude -p --allowedTools 'Read' --allowedTools 'Glob' --allowedTools 'Grep' < $(RELEASE_CONTEXT) > "$$NOTES"; \
		rm -f $(RELEASE_CONTEXT); \
	else \
		echo "[INFO] claude not found: writing a skeleton to fill in by hand"; \
		printf '# Release %s\n\n**Release Date:** %s\n\n## Changelog\n\n%s\n' \
			"$$NEW" "$$(date +%Y-%m-%d)" "$$(echo "$$COMMITS" | sed 's/^/- /')" > "$$NOTES"; \
	fi; \
	echo "[OK] Generated $$NOTES"; \
	echo ""; \
	echo "=============================================="; \
	echo "RELEASE PREPARATION COMPLETE"; \
	echo "=============================================="; \
	echo "Version: v$$CURRENT -> v$$NEW"; \
	echo "Commits: $$COUNT"; \
	echo "Notes:   $$NOTES"; \
	echo ""; \
	echo "Review the release notes, then run:"; \
	echo "  make release-finalize"

release-finalize:
	@set -e; \
	if [ ! -f $(RELEASE_STATE) ]; then \
		echo "[ERROR] No release in progress. Run 'make release-prepare TYPE=...' first."; exit 1; \
	fi; \
	NEW=$$(python3 -c "import json; print(json.load(open('$(RELEASE_STATE)'))['newVersion'])"); \
	NOTES="RELEASE_$${NEW//./_}.md"; \
	if [ ! -f "$$NOTES" ]; then \
		echo "[ERROR] Release notes not found: $$NOTES"; exit 1; \
	fi; \
	echo "[OK] Found release notes: $$NOTES"; \
	git add "$$NOTES"; \
	if ! git diff --cached --quiet; then \
		git commit -m "Add release notes for version $$NEW"; \
		echo "[OK] Committed release notes"; \
	else \
		echo "[WARN] Nothing to commit - release notes may already be committed"; \
	fi; \
	if [ -n "$$(git status --porcelain | grep -v '^??')" ]; then \
		echo "[ERROR] Uncommitted changes besides the notes: v$$NEW would publish as a SNAPSHOT"; \
		git status --short | grep -v '^??'; exit 1; \
	fi; \
	git tag -a "v$$NEW" -m "Release $$NEW"; \
	echo "[OK] Created local tag v$$NEW (not pushed)"; \
	$(MAKE) release-preflight; \
	$(MAKE) publish; \
	rm -f $(RELEASE_STATE); \
	echo ""; \
	echo "=============================================="; \
	echo "RELEASE v$$NEW PUBLISHED TO MAVEN CENTRAL (tag local)"; \
	echo "=============================================="; \
	echo "Push main and the tag:"; \
	echo "  git push origin main && git push origin v$$NEW"

release-abort:
	@set -e; \
	if [ -f $(RELEASE_STATE) ]; then \
		NEW=$$(python3 -c "import json; print(json.load(open('$(RELEASE_STATE)'))['newVersion'])"); \
		git tag -d "v$$NEW" 2>/dev/null || true; \
		if [ "$$(git log -1 --pretty=%s)" = "Add release notes for version $$NEW" ]; then \
			git reset --soft HEAD~1; \
			echo "[OK] Reverted local release-notes commit"; \
		fi; \
		rm -f $(RELEASE_STATE); \
		echo "[OK] Release aborted, tag v$$NEW removed"; \
	else \
		echo "No release in progress"; \
	fi

release-status:
	@if [ -f $(RELEASE_STATE) ]; then \
		echo "Release in progress:"; \
		python3 -c "import json; d=json.load(open('$(RELEASE_STATE)')); [print(f'  {k}: {v}') for k,v in d.items()]"; \
	else \
		echo "No release in progress"; \
	fi
