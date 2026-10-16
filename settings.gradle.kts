fun loadDotEnv(file: java.io.File): Map<String, String> {
    if (!file.isFile) {
        return emptyMap()
    }
    return file.readLines()
        .asSequence()
        .map { it.trim() }
        .filter { line -> line.isNotEmpty() && !line.startsWith("#") }
        .mapNotNull { line ->
            val separator = line.indexOf('=')
            if (separator <= 0) {
                return@mapNotNull null
            }
            val key = line.substring(0, separator).trim()
            val rawValue = line.substring(separator + 1).trim()
            val value = rawValue.removeSurrounding("\"").removeSurrounding("'")
            key to value
        }
        .toMap()
}

fun githubCredential(name: String, dotenv: Map<String, String>): String {
    val fromEnvironment = System.getenv(name)
    if (!fromEnvironment.isNullOrBlank()) {
        return fromEnvironment
    }
    return dotenv[name].orEmpty()
}

val dotenv = loadDotEnv(settingsDir.resolve(".env"))

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // karoo-ext from GitHub Packages (requires GitHub authentication).
        // Copy .env.example to .env, or export GITHUB_ACTOR and GITHUB_TOKEN.
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/hammerheadnav/karoo-ext")
            credentials {
                username = githubCredential("GITHUB_ACTOR", dotenv)
                password = githubCredential("GITHUB_TOKEN", dotenv)
            }
        }
    }
}

rootProject.name = "FuelTheRide"
include(":app")
