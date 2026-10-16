#!/usr/bin/env bash
# Publish the three library bundles for a validated Git tag.
# Build steps do not receive signing secrets; only this script imports the key.
set -euo pipefail
IFS=$'\n\t'

cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.."

err() { printf '%s: %s\n' "${0##*/}" "$*" >&2; }

require_signing_credentials() {
	if [[ -z "${GPG_KEY_ID:-}" || -z "${GPG_PASSPHRASE:-}" || -z "${GPG_PRIVATE_KEY:-}" ]]; then
		err "Remote publication requires GPG_KEY_ID, GPG_PASSPHRASE, and GPG_PRIVATE_KEY"
		exit 2
	fi
}

if [[ -n "${GPG_PRIVATE_KEY:-}" && -n "${GPG_PASSPHRASE:-}" ]]; then
	err "Importing GPG private key"
	printf '%s\n' "${GPG_PRIVATE_KEY}" | \
		gpg --batch \
			--yes \
			--pinentry-mode loopback \
			--passphrase "${GPG_PASSPHRASE}" \
			--import
fi
release_tasks=(
	:io.klib.swtbot.support:release
	:io.klib.swtbot.junit4:release
	:io.klib.swtbot.jupiter:release
)

if [[ -z "${RELEASE_BUNDLE_VERSION:-}" && -z "${RELEASE_MAVEN_VERSION:-}" && -z "${RELEASE_TAG:-}" ]]; then
	if [[ -z "${SONATYPE_BEARER:-}" ]]; then
		err "Snapshot build without SONATYPE_BEARER: releasing to dist/bundles only"
		CANONICAL= ./gradlew --no-daemon --console=plain --rerun-tasks "${release_tasks[@]}" "$@"
		exit 0
	fi
	require_signing_credentials
	err "Publishing snapshot build to Sonatype Central snapshots"
	CANONICAL=true ./gradlew --no-daemon --console=plain --rerun-tasks "${release_tasks[@]}" "$@"
	exit 0
fi

if [[ -z "${RELEASE_BUNDLE_VERSION:-}" || -z "${RELEASE_MAVEN_VERSION:-}" || -z "${RELEASE_TAG:-}" ]]; then
	err "Release requires RELEASE_BUNDLE_VERSION, RELEASE_MAVEN_VERSION, and RELEASE_TAG from the tag guard"
	exit 2
fi

if [[ -z "${SONATYPE_BEARER:-}" ]]; then
	err "SONATYPE_BEARER is required for tag releases"
	exit 2
fi
require_signing_credentials

err "Releasing ${RELEASE_TAG} (${RELEASE_BUNDLE_VERSION}; Maven ${RELEASE_MAVEN_VERSION}) to dist/bundles"
CANONICAL= ./gradlew --no-daemon --console=plain --rerun-tasks "${release_tasks[@]}" "$@"
./.github/scripts/sonatype-upload.sh \
	--publishing-type "${SONATYPE_PUBLISHING_TYPE:-USER_MANAGED}" \
	dist/bundles
