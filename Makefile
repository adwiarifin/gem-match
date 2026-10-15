# Mirrors CONSTRAINTS.md. If this file and CONSTRAINTS.md disagree, CONSTRAINTS.md wins.
# osv-scanner joins check-full once Gradle dependency lockfiles exist (planned for T18).

.PHONY: check-fast check-task check-full sign

# Edit loop: seconds.
check-fast:
	./gradlew :core:compileKotlin ktlintCheck
	scripts/check-core-boundary.sh
	gitleaks detect --redact --no-banner

# Task end: aim under 2 minutes, ceiling 5.
check-task: check-fast
	./gradlew :core:test :core:koverVerify

# Review and CI: no time limit.
check-full: check-task
	./gradlew detekt :app:lint :app:assembleDebug :app:assembleRelease
	scripts/check-apk-size.sh all

# Builds and signs the release APK. Run from your own terminal: it prompts for the key password.
sign:
	scripts/sign-release.sh
