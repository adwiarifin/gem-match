# Mirrors CONSTRAINTS.md. If this file and CONSTRAINTS.md disagree, CONSTRAINTS.md wins.

.PHONY: check-fast check-task check-full release-check sign

# Edit loop: seconds.
check-fast:
	./gradlew :core:compileKotlin ktlintCheck
	scripts/check-core-boundary.sh
	scripts/floor-guard.py
	scripts/test-check-permissions.sh
	gitleaks detect --redact --no-banner

# Task end: aim under 2 minutes, ceiling 5.
check-task: check-fast
	./gradlew :core:test :core:koverVerify :app:testDebugUnitTest

# Review and CI: no time limit.
check-full: check-task
	./gradlew detekt :app:lint :app:assembleDebug :app:assembleRelease
	scripts/check-apk-size.sh all
	scripts/check-permissions.sh
	osv-scanner scan source -r .

# Builds and signs the release APK. Run from your own terminal: it prompts for the key password.
sign:
	scripts/sign-release.sh

# Before calling a version released. check-full, plus the floor guard against everything since the last reviewed
# commit or tag (the guard on its own only sees uncommitted changes). Usage: make release-check BASE=v0.1.0
release-check:
	@test -n "$(BASE)" || { echo "usage: make release-check BASE=<last reviewed commit or tag>"; exit 2; }
	scripts/floor-guard.py --base "$(BASE)"
	$(MAKE) check-full
