# Steps to Build and Run the Viva Energy Test Program

Follow the steps below to build, start, and execute the Spring Camel application.

## 1. Build the Application

1. Check out the source code from the repository.
2. Open a terminal at the project root directory.
3. Run the following command:

```bash
.\gradlew build
```

4. Wait for the build to complete successfully.

## 2. Locate the Generated JAR File

Once the build is complete, navigate to the following directory from the project root:

```text
build\libs
```

The generated JAR file should be available in this directory.

## 3. Start the Spring Camel Application

From the `build\libs` directory, execute the following command:

```bash
java -jar VivaEnergyTestProgram-0.0.1-SNAPSHOT.jar
```

This will start the Spring Camel application.

Verify that the application has started successfully before proceeding to the next step.

## 4. Create the Required Directory Structure

Once the application has started, create a directory named `camelDirectory` at the same level as the JAR file.

The directory structure should be:

```text
camelDirectory
├── input
├── error
├── approved
├── declined
└── archived
```

## 5. Add the Input File

The test file `vivaTest.txt` is available in the project root directory.

Copy this file into:

```text
camelDirectory\input
```

Ensure that the file name remains:

```text
vivaTest.txt
```

## 6. Trigger File Processing

Ensure that the Spring Camel application is still running.

Open a browser and access the following URL:

```text
http://localhost:8081/start-file-processing?fileName=vivaTest.txt
```

This will trigger the file-processing workflow for `vivaTest.txt`.

## 7. Verify File Processing

Once processing is completed, the original input file will be moved from the `input` directory to the `archived` directory.

The expected location of the processed original file is:

```text
camelDirectory\archived
```

## 8. Verify Error Records

The `vivaTest.txt` test file contains a few intentionally invalid records, including:

* Empty lines.
* Rows with missing values.

These records are expected to be identified as invalid during processing and moved to the `error` directory.

The expected location of error records is:

```text
camelDirectory\error
```

### Expected Outcome

After successful execution:

* Valid records are processed according to the application logic.
* Invalid records are moved to the `error` directory.
* The original input file is moved from `input` to `archived`.
* The Spring Camel application remains running and available for subsequent processing requests.
