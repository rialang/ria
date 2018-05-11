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

If you want to build an executable jar, you will need to use the Maven assembly plugin or the shade plugin.
Assuming that your main program is called `main` you can use the following in your `pom.xml` for the assembly plugin.

```xml
            <plugin>
                <artifactId>maven-assembly-plugin</artifactId>
                <configuration>
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                    <archive>
                        <manifest>
                            <mainClass>main</mainClass>
                        </manifest>
                    </archive>
                </configuration>
                <executions>
                    <execution>
                        <id>make-assembly</id>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
```

Similarly for the Shade plugin, you can use the following:

```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.1.0</version>
                <executions>
                    <execution>
                        <id>main-jar</id>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <createDependencyReducedPom>false</createDependencyReducedPom>
                            <transformers>
                                <transformer
                                        implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <manifestEntries>
                                        <Main-Class>main</Main-Class>
                                    </manifestEntries>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
```

Either of the two plugins above will build a 'fat' jar containing all the dependencies that you need to run your
Ria programs.

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
        classpath 'com.github.rialang:ria-gradle-plugin:0.7.0'
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

For building a 'fat' jar, you can use the following in your `build.gradle`.

Basically, you will need to add the line below to your buildscript repositories section
```groovy
        jcenter()
```

and the following to your dependencies in the buildscript

```groovy
        classpath 'com.github.jengelman.gradle.plugins:shadow:2.0.3'
```

Finally, applying the shadow plugin as below.

```groovy
// Shadow plugin
apply plugin: 'com.github.johnrengelman.shadow'
```

The full script is below.

```groovy
buildscript {
    repositories {
        mavenCentral()
        jcenter()
    }
    dependencies {
        classpath 'com.github.rialang:ria-gradle-plugin:0.7.0'
        classpath 'com.github.jengelman.gradle.plugins:shadow:2.0.3'
    }
}

// Shadow plugin
apply plugin: 'com.github.johnrengelman.shadow'

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
