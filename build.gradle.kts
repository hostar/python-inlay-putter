import org.jetbrains.changelog.closure

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    java
    id("org.jetbrains.intellij") version "1.1.6"
    id("org.jetbrains.changelog") version "1.1.2"
}

version = properties("kpxPluginVersion")

repositories {
    mavenCentral()
}

sourceSets {
    main {
        java.srcDir("src")
        resources.srcDir("resources")
    }
}

intellij {
    pluginName.set(properties("kpxPluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))
    downloadSources.set(properties("platformDownloadSources").toBoolean())
    updateSinceUntilBuild.set(true)
    plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
}

changelog {
    version = properties("kpxPluginVersion")
    path = "${project.projectDir}/CHANGELOG.md"
    header = closure { "[${properties("kpxPluginVersion")}]" }
    // 2019, 2019.2, 2020.1.2
    headerParserRegex = """\d+(\.\d+)+""".toRegex()
    itemPrefix = "-"
    keepUnreleasedSection = true
    unreleasedTerm = "[Unreleased]"
    groups = listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security")
}

/**
 * Simple function to load HTML files and remove the surrounding `<html>` tags. This is useful for maintaining changes-notes
 * and the description of plugins in separate HTML files which makes them much more readable.
 */
fun htmlFixer(filename: String): String {
    if (!File(filename).exists()) {
        logger.error("File $filename not found.")
    } else {
        return File(filename).readText().replace("<html>", "").replace("</html>", "")
    }
    return ""
}

tasks {

    buildSearchableOptions {
        enabled = false
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = "11"
        targetCompatibility = "11"
        options.compilerArgs.add("-Xlint:all")
    }

    patchPluginXml {
        sinceBuild.set(properties("kpxPluginSinceBuild"))
        untilBuild.set(properties("kpxPluginUntilBuild"))
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(System.getenv("PUBLISH_TOKEN"))
        // Use beta versions like 2020.3-beta-1
        channels.set(
                listOf(
                        properties("kpxPluginVersion")
                                .split('-')
                                .getOrElse(1) { "default" }
                                .split('.')
                                .first()
                )
        )
    }
}

