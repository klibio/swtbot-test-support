# Installation

## Eclipse p2 Target Platform

Use this exact release repository:

```text
https://klibio.github.io/swtbot-test-support/p2/@VERSION@/
```

For the highest stable release:

```text
https://klibio.github.io/swtbot-test-support/p2/latest/
```

In your target definition, add a Software Site with that URL. Select **SWTBot Screenshot Support**, then optionally **SWTBot JUnit 4 Adapter** or **SWTBot Jupiter Adapter**. Your other target locations must supply SWT and the matching JUnit dependencies. Do not mix incompatible Jupiter/Platform versions. Prereleases have exact version URLs and never replace `latest`.

Feature IDs are `io.klib.swtbot.support.feature.feature.group`, `io.klib.swtbot.junit4.feature.feature.group`, and `io.klib.swtbot.jupiter.feature.feature.group`.

## Maven

Artifacts are released to Maven Central with group `io.klib.tools`. Central availability follows approval of the release deployment and may lag GitHub publication.

```xml
<dependency>
  <groupId>io.klib.tools</groupId>
  <artifactId>io.klib.swtbot.support</artifactId>
  <version>@VERSION@</version>
</dependency>
```

Add `io.klib.swtbot.junit4` or `io.klib.swtbot.jupiter` at the same version when needed. Supply platform-specific SWT separately. Maven dependencies do not replace an OSGi target/runtime configuration.

## bnd

Add the versioned p2 repository to your workspace:

```bnd
-plugin.swtbot.support: aQute.bnd.repository.p2.provider.P2Repository; \
    name='SWTBot Test Support'; \
    url='https://klibio.github.io/swtbot-test-support/p2/@VERSION@/'; \
    location='${build}/cache/swtbot-support'
```

Use the bundle symbolic name `io.klib.swtbot.support` and your chosen adapter on `-buildpath`. Use OSGi identity requirements for runtime resolution. Preserve your application's framework, native SWT and platform configuration.