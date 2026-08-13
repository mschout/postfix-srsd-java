// Gradle build file, set up to build a native executable via graalvm
plugins {
  application

  // auto version from git tag
  alias(libs.plugins.jgitver)

  // Delombok, use delomboked sources for javadoc
  alias(libs.plugins.lombok)

  // Format java code with spotless via prettier-java
  alias(libs.plugins.spotless)
}

repositories {
  mavenLocal()
  mavenCentral()
}

dependencies {
  implementation(libs.logback.classic)
  implementation(libs.logback.core)
  implementation(libs.guava)
  implementation(libs.picocli)
  implementation(libs.mail.srs)
  implementation(libs.netty.codec.netstring)
  implementation(libs.netty.all)
  implementation(libs.jetbrains.annotations)
  implementation(libs.slf4j.api)

  testImplementation(libs.junit.jupiter)
  testRuntimeOnly(libs.junit.jupiter.engine)
}

application {
  mainClass = "io.github.mschout.srsd.postfix.App"
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(11)
  }
}

// Include all dependendencies in the jar
tasks.jar {
  manifest {
    attributes("Main-Class" to "io.github.mschout.srsd.postfix.App")
  }

  duplicatesStrategy = DuplicatesStrategy.EXCLUDE

  from({
    configurations.runtimeClasspath.get().map {
      if (it.isDirectory) it else zipTree(it)
    }
  })
}

// Format java code with spotlessApply task
//spotless {
//  java {
//    prettier(mapOf("prettier" to "2.0.5", "prettier-plugin-java" to "0.8.0")).config(
//      mapOf(
//        "parser" to "java",
//        "tabWidth" to 2,
//        "printWidth" to 140,
//        "trailingComma" to "none",
//        "useTabs" to false
//      )
//    )
//  }
//}

tasks.withType<JavaCompile>().configureEach {
  options.encoding = "UTF-8"
}

jgitver {
  autoIncrementPatch = false
  nonQualifierBranches = "main,master"
}

// vim: ts=2 sw=2
