package com.vivaedemo.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component
public class ValidationProcessor implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		
		String row = exchange.getIn().getBody(String.class);
		String[] fields = row.split(",");

		exchange.getMessage().setBody(row + System.lineSeparator()); //Adding a next post validation to take it to appropriate files.
		if (fields.length != 5) {
			throw new IllegalArgumentException("Invalid row: expected 5 fields");
		}
		if (fields[0].isBlank()) {
			throw new IllegalArgumentException("TransactionId is missing");
		}
		if (fields[1].isBlank()) {
			throw new IllegalArgumentException("CustomerId is missing");
		}
		if (!fields[2].matches("\\d+(\\.\\d+)?")) {
			throw new IllegalArgumentException("Amount must be numeric");
		}
		if (fields[3].isBlank()) {
			throw new IllegalArgumentException("Currency is missing");
		}
		if (fields[4].isBlank()) {
			throw new IllegalArgumentException("Status is missing");
		}
	}
}
