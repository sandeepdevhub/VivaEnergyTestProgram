package com.vivaedemo.route;

import java.nio.file.NoSuchFileException;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.vivaedemo.processor.ArchieveFile;
import com.vivaedemo.processor.RecordProcessor;
import com.vivaedemo.processor.ValidationProcessor;

@Component
public class RouteDefinition extends RouteBuilder {

	private RecordProcessor processor;
	private ValidationProcessor validationProcessor;
	private ArchieveFile archieveFile;

	public RouteDefinition(RecordProcessor processor, ValidationProcessor validationProcessor,
			ArchieveFile archieveFile) {
		this.processor = processor;
		this.validationProcessor = validationProcessor;
		this.archieveFile = archieveFile;
	}

	@Override
	public void configure() throws Exception {
		from("platform-http:/start-file-processing")
			.routeId("file-processing-trigger")
			.to("direct:process-file")
			.setBody(constant("File processing completed"));

		from("direct:process-file")
		.log("Processing file...")
			.doTry()
				.process(processor)
			.doCatch(NoSuchFileException.class)
				.log("File not found: ${exception.message}").stop(
						).end()
			.split(body()).streaming()
				.doTry()
					.process(new ValidationProcessor())
				.doCatch(Exception.class)
					.log("Skipping invalid line: ${body} - ${exception.message}")
					.to("file:camelDirectory/error?fileName=${header.fileName}&fileExist=Append")
					.stop()
				.end() //writting the msg in error file.
			.choice()
				.when(simple("${body} contains 'APPROVED'"))
					.to("log: Approved Transactions")
					.to("file:camelDirectory/approved?fileName=${header.fileName}&fileExist=Append")
				.when(simple("${body} contains 'DECLINED'"))
					.to("log: DECLINED Transactions ")
					.to("file:camelDirectory/declined?fileName=${header.fileName}&fileExist=Append").end()
				.end()
				.end().
				log("All lines processed. Moving file to archive.")
		    .process(archieveFile).end();
		//We can optemize it later

	}

}
