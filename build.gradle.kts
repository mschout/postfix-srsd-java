plugins {
  application

  // De-lombok, use de-lomboked sources for Javadoc
  alias(libs.plugins.lombok)

  // Format Java code with spotless via prettier-java
  alias(libs.plugins.mschout.all.conventions)
}

repositories {
  mavenLocal()
  mavenCentral()
}

dependencies {
  implementation(libs.logback.classic)
  implementation(libs.logback.core)
  implementation(libs.picocli)
  implementation(libs.mail.srs)
  implementation(libs.netty.codec.netstring)
  implementation(libs.netty.all)
  implementation(libs.jetbrains.annotations)
  implementation(libs.slf4j.api)

  testImplementation(libs.junit.jupiter)
  testImplementation(libs.assertj.core)
  testImplementation(libs.mockito.core)
  testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.test {
  useJUnitPlatform()
}

application {
  mainClass = "io.github.mschout.srsd.postfix.App"
}

// Include all dependencies in the jar
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

spotless {
  java {
    importOrder()
    removeUnusedImports()
    googleJavaFormat()
  }
}
