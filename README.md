# Ria Programming Language

## Getting Started

Clone the repository

Run `mvn package`

Run `./ria.sh` to open a interactive interpreter

You should see the following prompt...
```
Ria 0.7.0 REPL.

>
```

You can then start trying out the language.

```
> 2 + 2
4 is number
> 
```

## Using Maven

There is a Maven plugin that can be used to compile Ria code.

Add the following to the `pom.xml` file build section

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>com.github.rialang</groupId>
                <artifactId>ria-maven-plugin</artifactId>
                <version>0.7.0</version>
                <executions>
                    <execution>
                        <id>ria-compile</id>
                        <phase>compile</phase>
                        <goals>
                            <goal>compile</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            ...
        </plugins>
    </build>
```

In order to actually run your Ria code, you will also need to add in the dependency on the Ria runtime.
Currently this has not been split out from the main compiler, so you will need to add a dependency on
this project.

```xml
        <dependency>
            <groupId>com.github.rialang</groupId>
            <artifactId>ria</artifactId>
            <version>0.7.0</version>
        </dependency>
```

By default the Maven plugin will use the java roots for locating the source.
If you wish to change this, you can do so by adding a configuration section to the execution tag.

```xml
    <configuration>
         <compileSourceRoots>
              <compileSourceRoot>${project.basedir}/src/main/ria</compileSourceRoot>
         </compileSourceRoots>
    </configuration>
```

Source for the Maven plugin is available at [https://github.com/rialang/ria-maven-plugin/](https://github.com/rialang/ria-maven-plugin/)

## Using Gradle

If your preferred choice of build is Gradle, you are also in luck.

To build using Gradle, you will need to add the following to your `build.gradle` file.

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'com.github.rialang:ria-gradle-plugin:1.0'
    }
}

//...

apply plugin: 'ria-gradle-plugin'

dependencies {
    compile 'com.github.rialang:ria:0.7.0'
}

riaCompile {
    sourceDirs = [
            "src/main/ria",
            "src/main/java"]
}

```

Source for the Gradle plugin is available at [https://github.com/rialang/ria-gradle-plugin/](https://github.com/rialang/ria-gradle-plugin/)

A short language tutorial will be available in due course. In the meantime,
please look at the examples and core libraries code.

Please report any issues you find, and feel free to submit pull requests.