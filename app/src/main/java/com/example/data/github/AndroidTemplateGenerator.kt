package com.example.data.github

import com.example.data.ai.GeminiEngine
import com.example.data.local.ProjectFileEntity
import com.example.model.EngineMode
import com.google.firebase.ai.type.Schema
import org.json.JSONObject

/**
 * Generates complete, compilable Kotlin + Jetpack Compose Android project scaffolds
 * bundled with a zero-error GitHub Actions CI/CD workflow that automatically builds
 * Debug + Release APKs and publishes them directly to the repository's GitHub Releases page.
 */
class AndroidTemplateGenerator(
    private val geminiEngine: GeminiEngine
) {
    private val projectFilesSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "appName" to Schema.string(description = "Clean short Android app title <= 24 chars"),
                "summary" to Schema.string(description = "Brief summary of generated architecture"),
                "files" to Schema.array(
                    description = "Complete Kotlin source files inside app/src/main/java/... to add or replace",
                    items = Schema.obj(
                        properties = mapOf(
                            "path" to Schema.string(description = "Relative file path e.g. app/src/main/java/com/gendev/sampleapp/MainActivity.kt"),
                            "language" to Schema.string(description = "kotlin, xml, or yaml"),
                            "content" to Schema.string(description = "Full complete compilable file content with no placeholders")
                        )
                    )
                )
            )
        )
    }

    fun buildDefaultProjectFiles(
        appName: String,
        packageName: String
    ): List<ProjectFileEntity> {
        val safeAppName = appName.trim().ifEmpty { "GenDevSampleApp" }
        val safePkg = packageName.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9.]"), "")
            .ifEmpty { "com.gendev.sampleapp" }
        val pkgPath = safePkg.replace('.', '/')

        return listOf(
            ProjectFileEntity(
                path = ".github/workflows/android-apk-release.yml",
                language = "yaml",
                content = buildZeroErrorWorkflowYaml(safeAppName)
            ),
            ProjectFileEntity(
                path = "settings.gradle.kts",
                language = "kotlin",
                content = """
                    pluginManagement {
                        repositories {
                            google()
                            mavenCentral()
                            gradlePluginPortal()
                        }
                    }
                    dependencyResolutionManagement {
                        repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                        repositories {
                            google()
                            mavenCentral()
                        }
                    }
                    rootProject.name = "$safeAppName"
                    include(":app")
                """.trimIndent()
            ),
            ProjectFileEntity(
                path = "gradle.properties",
                language = "properties",
                content = """
                    org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
                    android.useAndroidX=true
                    kotlin.code.style=official
                    android.nonTransitiveRClass=true
                """.trimIndent()
            ),
            ProjectFileEntity(
                path = "build.gradle.kts",
                language = "kotlin",
                content = """
                    plugins {
                        id("com.android.application") version "8.7.3" apply false
                        id("org.jetbrains.kotlin.android") version "2.0.21" apply false
                        id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
                    }
                """.trimIndent()
            ),
            ProjectFileEntity(
                path = "app/build.gradle.kts",
                language = "kotlin",
                content = """
                    plugins {
                        id("com.android.application")
                        id("org.jetbrains.kotlin.android")
                        id("org.jetbrains.kotlin.plugin.compose")
                    }

                    android {
                        namespace = "$safePkg"
                        compileSdk = 35

                        defaultConfig {
                            applicationId = "$safePkg"
                            minSdk = 26
                            targetSdk = 35
                            versionCode = 1
                            versionName = "1.0.0"
                        }

                        signingConfigs {
                            create("release") {
                                val ksPath = System.getenv("KEYSTORE_PATH")
                                if (!ksPath.isNullOrBlank() && file(ksPath).exists()) {
                                    storeFile = file(ksPath)
                                    storePassword = System.getenv("KEYSTORE_PASSWORD")
                                    keyAlias = System.getenv("KEY_ALIAS")
                                    keyPassword = System.getenv("KEY_PASSWORD")
                                }
                            }
                        }

                        buildTypes {
                            release {
                                isMinifyEnabled = false
                                proguardFiles(
                                    getDefaultProguardFile("proguard-android-optimize.txt"),
                                    "proguard-rules.pro"
                                )
                                val ksPath = System.getenv("KEYSTORE_PATH")
                                signingConfig = if (!ksPath.isNullOrBlank() && file(ksPath).exists()) {
                                    signingConfigs.getByName("release")
                                } else {
                                    signingConfigs.getByName("debug")
                                }
                            }
                        }

                        compileOptions {
                            sourceCompatibility = JavaVersion.VERSION_17
                            targetCompatibility = JavaVersion.VERSION_17
                        }
                        kotlinOptions {
                            jvmTarget = "17"
                        }
                        buildFeatures {
                            compose = true
                        }
                    }

                    dependencies {
                        implementation(platform("androidx.compose:compose-bom:2024.12.01"))
                        implementation("androidx.core:core-ktx:1.15.0")
                        implementation("androidx.activity:activity-compose:1.9.3")
                        implementation("androidx.compose.ui:ui")
                        implementation("androidx.compose.ui:ui-graphics")
                        implementation("androidx.compose.material3:material3")
                        implementation("androidx.compose.material:material-icons-extended")
                    }
                """.trimIndent()
            ),
            ProjectFileEntity(
                path = "app/proguard-rules.pro",
                language = "text",
                content = "# Keep Compose and app entrypoints\n-keep class $safePkg.** { *; }\n"
            ),
            ProjectFileEntity(
                path = "app/src/main/AndroidManifest.xml",
                language = "xml",
                content = """
                    <?xml version="1.0" encoding="utf-8"?>
                    <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                        <uses-permission android:name="android.permission.INTERNET" />
                        <application
                            android:allowBackup="true"
                            android:label="$safeAppName"
                            android:supportsRtl="true"
                            android:theme="@android:style/Theme.Material.NoActionBar">
                            <activity
                                android:name=".MainActivity"
                                android:exported="true">
                                <intent-filter>
                                    <action android:name="android.intent.action.MAIN" />
                                    <category android:name="android.intent.category.LAUNCHER" />
                                </intent-filter>
                            </activity>
                        </application>
                    </manifest>
                """.trimIndent()
            ),
            ProjectFileEntity(
                path = "app/src/main/java/$pkgPath/MainActivity.kt",
                language = "kotlin",
                content = """
                    package $safePkg

                    import android.os.Bundle
                    import androidx.activity.ComponentActivity
                    import androidx.activity.compose.setContent
                    import androidx.activity.enableEdgeToEdge
                    import androidx.compose.foundation.background
                    import androidx.compose.foundation.layout.*
                    import androidx.compose.foundation.shape.RoundedCornerShape
                    import androidx.compose.material3.*
                    import androidx.compose.runtime.*
                    import androidx.compose.ui.Alignment
                    import androidx.compose.ui.Modifier
                    import androidx.compose.ui.graphics.Brush
                    import androidx.compose.ui.graphics.Color
                    import androidx.compose.ui.text.font.FontWeight
                    import androidx.compose.ui.unit.dp

                    class MainActivity : ComponentActivity() {
                        override fun onCreate(savedInstanceState: Bundle?) {
                            super.onCreate(savedInstanceState)
                            enableEdgeToEdge()
                            setContent {
                                MaterialTheme(
                                    colorScheme = darkColorScheme(
                                        primary = Color(0xFFA855F7),
                                        secondary = Color(0xFF22D3EE),
                                        background = Color(0xFF090714),
                                        surface = Color(0xFF141026)
                                    )
                                ) {
                                    AppHomeScreen()
                                }
                            }
                        }
                    }

                    @Composable
                    fun AppHomeScreen() {
                        var counter by remember { mutableIntStateOf(0) }
                        Scaffold(
                            containerColor = MaterialTheme.colorScheme.background
                        ) { padding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF120C29), Color(0xFF090714))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "$safeAppName",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Generated & synced via GenDev AI • Auto-built on GitHub Releases",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFB8B2D4)
                                        )
                                        Button(
                                            onClick = { counter++ },
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Text("Interactive Test: ${'$'}counter")
                                        }
                                    }
                                }
                            }
                        }
                    }
                """.trimIndent()
            ),
            ProjectFileEntity(
                path = "README.md",
                language = "markdown",
                content = """
                    # $safeAppName

                    Android Jetpack Compose project generated and synchronized from **GenDev AI**.

                    ## Automated GitHub APK Release Pipeline
                    Every push to `main` (or manual workflow trigger) runs `.github/workflows/android-apk-release.yml` to build both **Debug APK** and **Release APK** using JDK 17 + Gradle 8.11.1 and publishes them automatically to the **Releases** menu of this repository.
                """.trimIndent()
            )
        )
    }

    suspend fun generateAiAndroidProject(
        engineMode: EngineMode,
        modelId: String,
        appIdea: String,
        appName: String,
        packageName: String
    ): Pair<List<ProjectFileEntity>, String> {
        val baseFiles = buildDefaultProjectFiles(appName, packageName).associateBy { it.path }.toMutableMap()
        val safePkg = packageName.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9.]"), "")
            .ifEmpty { "com.gendev.sampleapp" }
        val pkgPath = safePkg.replace('.', '/')

        val system = """
            You are a Senior Android Jetpack Compose Engineer inside GenDev AI.
            Generate a complete, compilable, zero-error Android Jetpack Compose application based on the user's app idea.
            STRICT RULES to guarantee zero GitHub Actions build errors:
            1. Package MUST be `$safePkg`.
            2. Place all Kotlin source files under `app/src/main/java/$pkgPath/`.
            3. Primary entry point MUST be `app/src/main/java/$pkgPath/MainActivity.kt` extending `ComponentActivity`.
            4. ONLY use dependencies already declared in app/build.gradle.kts:
               - androidx.core:core-ktx
               - androidx.activity:activity-compose
               - androidx.compose.ui:*
               - androidx.compose.material3:*
               - androidx.compose.material:material-icons-extended
               - Standard Kotlin stdlib & kotlinx.coroutines (available via Compose runtime).
            5. DO NOT reference non-existent R.drawable or R.font resources. Use Material Icons (Icons.Filled / Icons.Outlined) and dark Material 3 styling.
            6. DO NOT modify .github/workflows/android-apk-release.yml or Gradle files unless strictly necessary; focus on complete, working Kotlin Compose screens.
        """.trimIndent()

        val userPrompt = """
            App Name: $appName
            Package Name: $safePkg
            Feature Specification:
            $appIdea

            Generate the complete MainActivity.kt (and optional supporting Kotlin files in the same package) implementing all requested UI and interactive logic cleanly with zero compilation errors.
        """.trimIndent()

        val result = geminiEngine.generateStructuredJson(
            engineMode = engineMode,
            modelId = modelId,
            systemPrompt = system,
            userPrompt = userPrompt,
            schema = projectFilesSchema,
            jsonSchemaHint = """{"appName": "...", "summary": "...", "files": [{"path": "app/src/main/java/$pkgPath/MainActivity.kt", "language": "kotlin", "content": "..."}]}"""
        )

        val json = JSONObject(result.jsonText)
        val summary = json.optString("summary", "Generated Android Compose source files with Gemini.")
        val filesArr = json.optJSONArray("files")
        if (filesArr != null) {
            for (i in 0 until filesArr.length()) {
                val item = filesArr.getJSONObject(i)
                val path = item.optString("path").trim().trimStart('/')
                val content = item.optString("content")
                val language = item.optString("language", "kotlin")
                // Never overwrite the battle-tested CI/CD workflow file from AI output
                if (path.isNotBlank() &&
                    !path.contains("..") &&
                    !path.startsWith(".github/") &&
                    content.isNotBlank()
                ) {
                    baseFiles[path] = ProjectFileEntity(
                        path = path,
                        content = content,
                        language = language
                    )
                }
            }
        }
        return baseFiles.values.sortedBy { it.path } to summary
    }

    fun buildZeroErrorWorkflowYaml(appName: String): String {
        val safeArtifact = appName.trim()
            .replace(Regex("[^a-zA-Z0-9_-]"), "-")
            .ifEmpty { "GenDevApp" }
        return """
            name: Android APK Build & Auto-Release

            on:
              push:
                branches: [ "main", "master" ]
              workflow_dispatch:

            permissions:
              contents: write

            jobs:
              build-and-release:
                name: Build Debug & Release APK
                runs-on: ubuntu-latest

                steps:
                  - name: Checkout source code
                    uses: actions/checkout@v4

                  - name: Set up JDK 17
                    uses: actions/setup-java@v4
                    with:
                      distribution: temurin
                      java-version: 17

                  - name: Setup Gradle (Zero-Wrapper Safe)
                    uses: gradle/actions/setup-gradle@v4
                    with:
                      gradle-version: '8.11.1'

                  - name: Decode optional Release Keystore
                    if: ${'$'}{{ secrets.RELEASE_KEYSTORE_BASE64 != '' }}
                    run: |
                      echo "${'$'}{{ secrets.RELEASE_KEYSTORE_BASE64 }}" | base64 --decode > release-keystore.jks
                      echo "KEYSTORE_PATH=${'$'}{{ github.workspace }}/release-keystore.jks" >> ${'$'}GITHUB_ENV

                  - name: Build Debug & Release APKs
                    env:
                      KEYSTORE_PASSWORD: ${'$'}{{ secrets.KEYSTORE_PASSWORD }}
                      KEY_ALIAS: ${'$'}{{ secrets.KEY_ALIAS }}
                      KEY_PASSWORD: ${'$'}{{ secrets.KEY_PASSWORD }}
                    run: |
                      gradle assembleDebug assembleRelease --no-daemon --stacktrace

                  - name: Collect APK Outputs
                    run: |
                      mkdir -p release-apks
                      DEBUG_APK=${'$'}(find app/build/outputs/apk/debug -name "*.apk" | head -n 1)
                      RELEASE_APK=${'$'}(find app/build/outputs/apk/release -name "*.apk" | head -n 1)
                      if [ -n "${'$'}DEBUG_APK" ]; then cp "${'$'}DEBUG_APK" "release-apks/${safeArtifact}-debug.apk"; fi
                      if [ -n "${'$'}RELEASE_APK" ]; then cp "${'$'}RELEASE_APK" "release-apks/${safeArtifact}-release.apk"; fi
                      ls -lh release-apks/

                  - name: Upload APK Build Artifacts
                    uses: actions/upload-artifact@v4
                    with:
                      name: ${safeArtifact}-APKs-build-${'$'}{{ github.run_number }}
                      path: release-apks/*.apk

                  - name: Publish APKs to GitHub Releases
                    uses: softprops/action-gh-release@v2
                    with:
                      tag_name: v1.0.${'$'}{{ github.run_number }}
                      name: "$appName Build #${'$'}{{ github.run_number }}"
                      body: |
                        Automated Debug & Release APK build triggered by **GenDev AI**.
                        - Commit: `${'$'}{{ github.sha }}`
                        - Gradle: 8.11.1 | JDK: 17
                      files: release-apks/*.apk
                      make_latest: true
        """.trimIndent()
    }
}
