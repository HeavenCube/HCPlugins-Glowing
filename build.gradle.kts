plugins { java }

group = "fr.noltox.hcplugins"
version = providers.gradleProperty("version").get()

java { toolchain.languageVersion = JavaLanguageVersion.of(25) }

dependencies {
    compileOnly("fr.noltox.hcplugins:core-api")
    compileOnly("fr.noltox.hcplugins:placeholders-api")
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")

    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testImplementation("fr.noltox.hcplugins:core-api")
    testImplementation("io.papermc.paper:paper-api:26.3.build.+")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:all")
}

tasks.processResources {
    val pluginVersion = project.version.toString()
    inputs.property("version", pluginVersion)
    filesMatching("paper-plugin.yml") { expand("version" to pluginVersion) }
}

tasks.jar {
    archiveFileName.set("HCGlowing-${project.version}.jar")
}
tasks.test { useJUnitPlatform() }
