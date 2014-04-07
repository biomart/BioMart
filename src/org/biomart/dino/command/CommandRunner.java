package org.biomart.dino.command;

import java.io.IOException;

public interface CommandRunner {

	public CommandRunner run() throws Exception;
	public Object getResults() throws IOException;
}
