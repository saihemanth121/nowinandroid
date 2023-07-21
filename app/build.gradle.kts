/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.google.samples.apps.nowinandroid.NiaBuildType

plugins {
    id("nowinandroid.android.application")
    id("nowinandroid.android.application.compose")
    id("nowinandroid.android.application.flavors")
    id("nowinandroid.android.application.jacoco")
    id("nowinandroid.android.hilt")
    id("jacoco")
    id("nowinandroid.android.application.firebase")
}

android {
    defaultConfig {
        applicationId = "com.google.samples.apps.nowinandroid"
        versionCode = 5
        versionName = "0.0.5" // X.Y.Z; X = Major, Y = minor, Z = Patch level

        // Custom test runner to set up Hilt dependency graph
        testInstrumentationRunner = "com.google.samples.apps.nowinandroid.core.testing.NiaTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        val debug by getting {
            applicationIdSuffix = NiaBuildType.DEBUG.applicationIdSuffix
        }
        val release by getting {
            isMinifyEnabled = true
            applicationIdSuffix = NiaBuildType.RELEASE.applicationIdSuffix
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            // To publish on the Play store a private signing key is required, but to allow anyone
            // who clones the code to sign and run the release variant, use the debug signing key.
            // TODO: Abstract the signing configuration to a separate file to avoid hardcoding this.
            signingConfig = signingConfigs.getByName("debug")
        }
        val benchmark by creating {
            // Enable all the optimizations from release build through initWith(release).
            initWith(release)
            matchingFallbacks.add("release")
            // Debug key signing is available to everyone.
            signingConfig = signingConfigs.getByName("debug")
            // Only use benchmark proguard rules
            proguardFiles("benchmark-rules.pro")
            isMinifyEnabled = true
            applicationIdSuffix = NiaBuildType.BENCHMARK.applicationIdSuffix
        }
    }

    packagingOptions {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    namespace = "com.google.samples.apps.nowinandroid"
}

dependencies {
    implementation(project(":feature:interests"))
    implementation(project(":feature:foryou"))
    implementation(project(":feature:bookmarks"))
    implementation(project(":feature:topic"))
    implementation(project(":feature:settings"))

    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:model"))
    implementation(project(":core:analytics"))

    implementation(project(":sync:work"))

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(project(":core:datastore-test"))
    androidTestImplementation(project(":core:data-test"))
    androidTestImplementation(project(":core:network"))
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.accompanist.testharness)
    androidTestImplementation(kotlin("test"))
    debugImplementation(libs.androidx.compose.ui.testManifest)
    debugImplementation(project(":ui-test-hilt-manifest"))

    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.compose.runtime.tracing)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.window.manager)
    implementation(libs.androidx.profileinstaller)

    implementation(libs.coil.kt)
}

// androidx.test is forcing JUnit, 4.12. This forces it to use 4.13
configurations.configureEach {
    resolutionStrategy {
        force(libs.junit4)
        // Temporary workaround for https://issuetracker.google.com/174733673
        force("org.objenesis:objenesis:2.6")
    }
}

tasks.register<JacocoReport>("jacocoUiTestFullReport") {
    group = "Reports"
    description = "Generate Jacoco Instrumented Tests coverage reports for all modules"

    // Run android tests in all modules
    dependsOn(rootProject.getTasksByName("createDemoDebugAndroidTestCoverageReport", true))

    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(file("${rootProject.buildDir}/coverage-report"))
    }

    val fileFilter = listOf(
        // data binding
        "android/databinding/**/*.class",
        "**/android/databinding/*Binding.class",
        "**/android/databinding/*",
        "**/androidx/databinding/*",
        "**/BR.*",
        // android
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        // butterKnife
        "**/*\$ViewInjector*.*",
        "**/*\$ViewBinder*.*",
        // dagger
        "**/*_MembersInjector.class",
        "**/Dagger*Component.class",
        "**/Dagger*Component\$Builder.class",
        "**/*Module_*Factory.class",
        "**/di/module/*",
        "**/*_Factory*.*",
        "**/*Module*.*",
        "**/*Dagger*.*",
        "**/*Hilt*.*",
        // kotlin
        "**/*MapperImpl*.*",
        "**/*\$ViewInjector*.*",
        "**/*\$ViewBinder*.*",
        "**/*Component*.*",
        "**/*BR*.*",
        "**/*\$Lambda$*.*",
        "**/*Companion*.*",
        "**/*Module*.*",
        "**/*Dagger*.*",
        "**/*Hilt*.*",
        "**/*MembersInjector*.*",
        "**/*_MembersInjector.class",
        "**/*_GeneratedInjector.class",
        "**/*_Factory*.*",
        "**/*_Provide*Factory*.*",
        "**/*Extensions*.*",
        // sealed and data classes
        "**/*$Result.*",
        "**/*$Result$*.*"
    )

    val javaClasses = mutableListOf<FileCollection>()
    val kotlinClasses = mutableListOf<FileCollection>()
    val javaSrc = mutableListOf<String>()
    val kotlinSrc = mutableListOf<String>()
    val execution = mutableListOf<FileCollection>()

    rootProject.subprojects.forEach { proj ->
        if (proj.tasks.findByName("createDemoDebugAndroidTestCoverageReport") == null) {
            // Skip projects which dont have androidTest
            println("jacocoUiTestReportAllModules: Skipping ${proj.name}")
            return@forEach
        }
        javaClasses.add(fileTree("${proj.buildDir}/intermediates/javac/demoDebug") {
            exclude(fileFilter)
        })
        kotlinClasses.add(fileTree("${proj.buildDir}/tmp/kotlin-classes/demoDebug") {
            exclude(fileFilter)
        })
        javaSrc.add("${proj.projectDir}/src/main/java")
        kotlinSrc.add("${proj.projectDir}/src/main/kotlin")
        execution.add(fileTree(proj.buildDir) {
            include("outputs/code_coverage/demoDebugAndroidTest/connected/**/*.ec")
        })
    }

    sourceDirectories.setFrom(files(javaSrc, kotlinSrc))
    classDirectories.setFrom(files(javaClasses, kotlinClasses))
    executionData.setFrom(execution)

    doLast {
        println("file://${reports.html.outputLocation}/index.html")
    }
}