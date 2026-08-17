package com.vivaedemo.processor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component
public class ArchieveFile implements Processor {
	@Override
	public void process(Exchange exchange) throws Exception {
		String fileName = exchange.getMessage().getHeader("fileName", String.class);
		Path source = Paths.get("camelDirectory/input", fileName);
		Path target = Paths.get("camelDirectory/archive", fileName);
		Files.move(source, target);
	}
}
