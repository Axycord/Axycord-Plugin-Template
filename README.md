# Axycord Plugin Template

An installed-APK plugin starter for [Axycord](https://github.com/Axycord/Axycord),
maintained by **Maarij**. The example plugin demonstrates settings, themed dialogs,
header and composer actions, message components and interceptors, context-menu
actions, a bottom tab, profile sections/tabs, a local `/echo` command and namespaced
storage. It also includes a local-cache View Raw message page.

## Start a plugin

1. Click **Use this template → Create a new repository**.
2. Install JDK 21 and Android SDK platform 37 with build-tools 36.0.0.
3. Change the plugin ID, description and entrypoint in `build.gradle.kts`.
4. Change `namespace` and `applicationId`, move/rename the Kotlin package, and update
   its binding imports. The manifest's relative discovery-service name follows the namespace.
5. Set your plugin name in `src/main/res/values/strings.xml` and replace the examples
   with your feature. Increase `versionCode` for each installable update.

Build and check Android API compatibility:

```bash
./gradlew lintDebug assembleDebug
```

On Windows:

```powershell
.\gradlew.bat lintDebug assembleDebug
```

The APK is `build/outputs/apk/debug/axycord-plugin-template-debug.apk`. GitHub Actions
runs the same checks on pushes and pull requests and saves the APK as a workflow
artifact. It does not publish releases.

Install the APK, open **Axycord Settings → Plugins**, approve its signing certificate
and enable **Axycord Example Plugin**. There is no launcher icon. Contributed bottom
tabs and profile elements can be arranged through Axycord's customization settings.

## SDK and compatibility

The template resolves these compile-time dependencies without a GitHub login:

- Gradle plugin: `mmaarij.axycord.plugin:1.0.0`.
- Host SDK: `mmaarij.axycord:plugin-api:1.0.6`.
- Maven repository: `https://raw.githubusercontent.com/Axycord/Axycord/maven`.

The SDK is `compileOnly`: Axycord supplies the API, app classes and RxJava at runtime.
Keep those dependencies out of the plugin APK. Axycord 1.0 implements **plugin API
level 3**. The DSL's `axycordVersionMinimum` is an API-level requirement, separate
from both the app version and the SDK artifact version. Its maximum field is
retained for metadata compatibility; current Axycord ignores that upper bound.

The discovery action and metadata use `mmaarij.axycord`, while the host's internal
Android classes retain their `com.discord` namespaces. Plugins execute inside the
host process; install plugins from developers you trust.

See the [plugin API documentation](https://github.com/Axycord/Axycord/blob/main/docs/plugin-api.md)
for lifecycle, UI contributions, storage and signer approval.

### Develop against a local Axycord checkout

Generate the SDK from the host checkout first:

```bash
bash scripts/build.sh pluginSdkJar
```

Then, from this template, explicitly select that checkout:

```bash
./gradlew -PaxycordCheckout=../microcord lintDebug assembleDebug
```

This uses that checkout's `plugin-gradle` build and
`build/plugin-sdk/axycord-plugin-api-1.0.6.jar`. Normal builds use the public Maven
repository and do not depend on a sibling checkout.

## Release signing

Create a dedicated plugin signing key and copy the example properties:

```powershell
keytool -genkeypair -keystore release-key.jks -alias plugin -keyalg RSA -keysize 2048 -validity 10000
Copy-Item keystore.properties.example keystore.properties
```

Fill in `keystore.properties`, then run `./gradlew assembleRelease` (or
`.\gradlew.bat assembleRelease`). The key and properties are ignored by Git. Keep
the key safe and reuse it for updates: Axycord's approval is tied to its SHA-256
signing-certificate fingerprint. Debug builds use the local Android debug key;
different machines can produce different debug signing identities.

## Source provenance

Adapted from [microcord/plugin-template](https://github.com/microcord/plugin-template)
at `13af556`. See [NOTICE.md](NOTICE.md) for the upstream snapshot and license status.
The Axycord host and its SDK are distributed under AGPL-3.0-only.
