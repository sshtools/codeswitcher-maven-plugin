# codeswitcher-maven-plugin

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.sshtoosl/codeswitcher-maven-plugin/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/com.sshtools/codeswitcher-maven-plugin/)

A simple pre-processor plugin for Maven that can adjust Java source, removing or inserting blocks of code depending on simple boolean conditions, or replacing any strings (not just variable patterns). 

It is based on HSQLDB's `CodeSwitcher` class, turned into a Maven plugin and a couple of extra features added.

PS. Do not use this plugin, there are probably other ways to achieve your goal. 

## Requirements

 * Maven 3.6.3 or above
 * Java 1.8 or above
 
## Installation
 
Add to the `<plugins>` section.

```xml
	<pluginManagement>
		<plugin>
	        <groupId>com.sshtools</groupId>
	        <artifactId>codeswitcher-maven-plugin</artifactId>
	        <!-- Use the latest released version: https://repo1.maven.org/maven2/com/sshttols/codeswitcher-maven-plugin/ -->
	        <version>LATEST_VERSION</version>
	    </plugin>
	</pluginManagement>
```

## Usage

To make use of this plugin you will :-

 * Edit your Java source to add the required directives and tokens.
 * Edit your POM, configuring the plugin with the used directives and tokens.

### Source

#### Conditional Blocks

You can include (enable) or exclude (disable) blocks using the `ifdef` directive, terminated by an `endif`. An optional `else` directive also may be used.

Directives are introduced as single line comments where the first character is a `#`. 

```java
	//#ifdef LICENSE_CHECKS
	/*
		File f = new File("myapp.lic");
		License l = new License(f);
		l.verify();
		System.out.println("Valid license.");
	*/
	//#else
		System.out.println("This build does not require a license.");
	//#endif
```

The `ifdef` directive works by adding or removing comment characters in the final source. 3 different styles of commenting are recognised. In addition to the style above, you can also use :-

```java
	//#ifdef LICENSE_CHECKS
	//
	// your code 
	//
	//#else
	.. your other code
	//#endif
```

.. or ..

```java
	//#ifdef LICENSE_CHECKS
	/*
	 * your code 
	 */
	//#else
	.. your other code
	//#endif
```

The above example requires that you add an `<enable>` with a value of `LICENSE_CHECKS` to the plugin `<configuration>` in your POM. 

*Single Line Directives* may be used. These are introduced using the following.

```java
	//#[LICENSE_CHECKS] System.out.println("This app will require a license");
```

A final special directive, `del` may be added to entirely remove blocks of code using the same format. 

#### Replacement

Simple string replacement just replaces any text with another piece of text determined at build time. In the example below, the `<configuration>` tag in your POM will have a `<token>` that specifies a `<key>` of `SOFTWARE_VERSION` with a value of `${project.version}` which will expand to the project version.

```java
	String version = "SOFTWARE_VERSION"
```

#### Timestamp

A special token may be specified that is set to the current timestamp. In the example below, the `<configuration>` tag in your POM will have `<timestampToken>` set to `/* RELEASE_DATE */`.

```java
	Data r = new Date(/* RELEASE_DATE */);
```

### Running Automatically On Every Build

Add to `<plugins>` section, bind to the `pre-process` goal on the `generate-sources` phase and add required configuration. 

```xml
	<plugin>
        <groupId>com.sshtools</groupId>
        <artifactId>codeswitcher-maven-plugin</artifactId>
        <executions>
			<execution>
				<id>enterprise-build</id>
				<phase>generate-sources</phase>
				<goals>
					<goal>pre-process</goal>
				</goals>
				<configuration>
					<!-- When set, the new source code will be set as the main source directory for the built. workOnCopy must also be true -->
					<changeBuildSourceDirectory>true</changeBuildSourceDirectory>
					
					<!--  The temporary directory to write the pre-processed files to when workOnCopy is set to true (the default). -->
					<temporaryDirectory>target/preprocessed</temporaryDirectory>
					
					<!-- When set, code will be commented out rather than stripped out -->
					<comment>false</comment>
					
					<!-- When set, the goal will run on a copy of the source. -->
					<workOnCopy>true</workOnCopy>
					
					<!-- Special token that gets replaced with the current timestamp -->
					<timestampToken>/* RELEASE_DATE */</timestampToken>
					
					<!-- List of 'token's consisting of a 'key' and 'value'. Every
					     occurrence of 'key' is replaced with 'value'. key is a
					     simple string search. Patterns, regexp's or otherwise are
					     not supported -->
					<tokens>
						<token>
							<key>SOFTWARE_VERSION</key>
							<value>MyApp_${project.version}</value>
						</token>
					</tokens>
					
					<!-- Enable symbols. Any code associated with this symbol
					     will be uncommented in the pre-processed result -->
					<enables>
						<enable>LICENSE_CHECKS</enable>
					</enables>
					
					<!-- Disable symbols. Any code associated with this symbol
					     will be commented in the pre-processed result -->
					<disables>
						<disable>PRODUCTION</disable>
					</disables>
				</configuration>
			</execution>
		</executions>
    </plugin>
```

Then any Maven compilation will pre-process any source in `src/main/java` into `target/preprocessed`, which will then be used to compile.

```
mvn clean compile
```
