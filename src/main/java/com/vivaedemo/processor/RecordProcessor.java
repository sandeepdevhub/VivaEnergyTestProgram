package com.vivaedemo.processor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component
public class RecordProcessor implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		Path path = Paths.get("camelDirectory/input/" + exchange.getMessage().getHeader("fileName"));
		BufferedReader reader = Files.newBufferedReader(path);
		reader.readLine(); // We need to skip the header
		exchange.getMessage().setBody(reader.lines().iterator());
	}

}
