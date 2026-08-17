package com.vivaedemo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@CamelSpringBootTest
@SpringBootTest
class FileProcessingRouteTest {

	@Autowired
	private CamelContext camelContext;

	@Autowired
	private ProducerTemplate producerTemplate;

	private final Path inputDir = Paths.get("camelDirectory/input");

	private final Path approvedDir = Paths.get("camelDirectory/approved");

	private final Path declinedDir = Paths.get("camelDirectory/declined");

	private final Path errorDir = Paths.get("camelDirectory/error");

	private final Path archiveDir = Paths.get("camelDirectory/archive");

	@BeforeEach
	void setUp() throws Exception {
		createDirectories();
		cleanDirectory(inputDir);
		cleanDirectory(approvedDir);
		cleanDirectory(declinedDir);
		cleanDirectory(errorDir);
		cleanDirectory(archiveDir);
	}

	@AfterEach
	void tearDown() throws Exception {
		cleanDirectory(inputDir);
		cleanDirectory(approvedDir);
		cleanDirectory(declinedDir);
		cleanDirectory(errorDir);
		cleanDirectory(archiveDir);
	}

	private void createDirectories() throws Exception {
		Files.createDirectories(inputDir);
		Files.createDirectories(approvedDir);
		Files.createDirectories(declinedDir);
		Files.createDirectories(errorDir);
		Files.createDirectories(archiveDir);
	}

	private void cleanDirectory(Path directory) throws Exception {
		if (!Files.exists(directory)) {
			return;
		}
		try (var files = Files.list(directory)) {
			files.forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	private void createInputFile(String content) throws Exception {
		Files.writeString(inputDir.resolve("input.txt"), content);
	}

	private void startRouteForTest() throws Exception {
		AdviceWith.adviceWith(camelContext, "file-processing-trigger", route -> route.replaceFromWith("direct:test"));
		camelContext.start();
	}

	private void triggerProcessing() {
		producerTemplate.requestBodyAndHeaders("direct:test", null, Map.of("fileName", "input.txt"));
	}

	// ============================================================
	// TEST 1
	// Valid APPROVED and DECLINED transactions
	// ============================================================

	@Test
	void shouldProcessApprovedAndDeclinedTransactions() throws Exception {
		startRouteForTest();
		String input = """
				transactionId,customerId,amount,currency,status
				10001,C001,125.50,AUD,APPROVED
				10002,C002,250.00,AUD,DECLINED
				10003,C003,75.25,AUD,APPROVED
				""";

		createInputFile(input);
		triggerProcessing();
		Path approvedFile = approvedDir.resolve("input.txt");
		Path declinedFile = declinedDir.resolve("input.txt");
		assertTrue(Files.exists(approvedFile), "Approved file should be created");
		assertTrue(Files.exists(declinedFile), "Declined file should be created");
		String approvedContent = Files.readString(approvedFile);
		String declinedContent = Files.readString(declinedFile);
		assertTrue(approvedContent.contains("10001,C001,125.50,AUD,APPROVED"));
		assertTrue(approvedContent.contains("10003,C003,75.25,AUD,APPROVED"));
		assertTrue(declinedContent.contains("10002,C002,250.00,AUD,DECLINED"));
	}

	// ============================================================
	// TEST 2
	// Header should be skipped
	// ============================================================

	@Test
	void shouldSkipHeader() throws Exception {
		startRouteForTest();
		String input = """
				transactionId,customerId,amount,currency,status
				10001,C001,125.50,AUD,APPROVED
				""";
		createInputFile(input);
		triggerProcessing();
		Path approvedFile = approvedDir.resolve("input.txt");
		assertTrue(Files.exists(approvedFile), "Approved file should exist");
		String approvedContent = Files.readString(approvedFile);
		assertFalse(approvedContent.contains("transactionId,customerId,amount,currency,status"),
				"Header must not be processed");
		assertTrue(approvedContent.contains("10001,C001,125.50,AUD,APPROVED"), "Transaction should be processed");
	}

	// ============================================================
	// TEST 3
	// Invalid line should go to ERROR
	// and processing should continue
	// ============================================================

	@Test
	void shouldSkipInvalidLineAndContinueProcessing() throws Exception {
		startRouteForTest();
		String input = """
				transactionId,customerId,amount,currency,status
				10001,C001,125.50,AUD,APPROVED
				10002,C002,250.00,AUD,
				10003,C003,75.25,AUD,APPROVED
				""";
		createInputFile(input);
		triggerProcessing();
		Path errorFile = errorDir.resolve("input.txt");
		Path approvedFile = approvedDir.resolve("input.txt");
		assertTrue(Files.exists(errorFile), "Error file should be created");
		assertTrue(Files.exists(approvedFile), "Approved file should be created");
		String errorContent = Files.readString(errorFile);
		String approvedContent = Files.readString(approvedFile);
		// Invalid transaction should be written to error
		assertTrue(errorContent.contains("10002,C002,250.00,AUD,"), "Invalid transaction should be written to error");
		// First valid transaction should be processed
		assertTrue(approvedContent.contains("10001,C001,125.50,AUD,APPROVED"));
		// IMPORTANT:
		// This proves processing continued after invalid line
		assertTrue(approvedContent.contains("10003,C003,75.25,AUD,APPROVED"),
				"Processing should continue after invalid line");
	}

	// ============================================================
	// TEST 4
	// File should be archived after all processing completes
	// ============================================================

	@Test
	void shouldArchiveFileAfterProcessing() throws Exception {
		startRouteForTest();
		String input = """
				transactionId,customerId,amount,currency,status
				10001,C001,125.50,AUD,APPROVED
				10002,C002,250.00,AUD,DECLINED
				10003,C003,75.25,AUD,APPROVED
				""";
		createInputFile(input);
		Path originalFile = inputDir.resolve("input.txt");
		Path archivedFile = archiveDir.resolve("input.txt");
		assertTrue(Files.exists(originalFile), "Input file should initially exist");
		triggerProcessing();
		assertTrue(Files.exists(archivedFile), "File should exist in archive after processing");
		assertFalse(Files.exists(originalFile), "Original input file should be moved");
	}

	// ============================================================
	// TEST 5
	// Missing file should be handled
	// ============================================================

	@Test
	void shouldHandleMissingFile() throws Exception {
		startRouteForTest();
		// Do NOT create input.txt
		Path inputFile = inputDir.resolve("input.txt");
		Path archivedFile = archiveDir.resolve("input.txt");
		assertFalse(Files.exists(inputFile));
		triggerProcessing();
		assertFalse(Files.exists(archivedFile), "Missing file must not be archived");
	}

	// ============================================================
	// TEST 6
	// Process the uploaded/large input structure
	// ============================================================

	@Test
	void shouldProcessLargeInputFile() throws Exception {
		startRouteForTest();
		StringBuilder input = new StringBuilder();
		input.append("transactionId,customerId,amount,currency,status\n");
		for (int i = 0; i < 1000; i++) {
			input.append("10001,C001,125.50,AUD,APPROVED\n");
			input.append("10002,C002,250.00,AUD,DECLINED\n");
			input.append("10003,C003,75.25,AUD,APPROVED\n");
		}
		createInputFile(input.toString());
		triggerProcessing();
		Path approvedFile = approvedDir.resolve("input.txt");
		Path declinedFile = declinedDir.resolve("input.txt");
		Path archivedFile = archiveDir.resolve("input.txt");
		assertTrue(Files.exists(approvedFile), "Approved output should exist");
		assertTrue(Files.exists(declinedFile), "Declined output should exist");
		assertTrue(Files.exists(archivedFile), "Input should be archived after processing");
		assertFalse(Files.exists(inputDir.resolve("input.txt")), "Input should no longer exist in input directory");
		String approvedContent = Files.readString(approvedFile);
		String declinedContent = Files.readString(declinedFile);
		long approvedCount = approvedContent.lines().count();
		long declinedCount = declinedContent.lines().count();
		Assertions.assertEquals(2000L, approvedCount, "Expected 2000 approved transactions");
		Assertions.assertEquals(1000L, declinedCount, "Expected 1000 declined transactions");
	}
}