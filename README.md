# Jar Injector

A jar string-injection tool

## Usage

#### Clone and build

`git clone https://github.com/Yamakaja/jar-injector.git`

`./gradlew build`

You can now get the `injector.jar` from `build/libs/injector.jar`

#### Inject into a jar

`java -jar jarinjector.jar <jar-to-inject> <replacement>+`

where

`<replacement>` = `<replace> <with>`

#### Example

For example, to insert the download time into a jar, you could have a shell exec modifying it like this before the actual download:

    java -jar injector.jar jar-to-edit.jar "%%__DOWNLOADTIME__%%" "`date`"
