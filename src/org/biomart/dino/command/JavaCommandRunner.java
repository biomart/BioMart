package org.biomart.dino.command;

public abstract class JavaCommandRunner implements CommandRunner {

	protected JavaCommand cmd;
	
	public JavaCommandRunner setCmd(JavaCommand cmd) {
		this.cmd = cmd;
		return this;
	}
	
	@Override
	public CommandRunner run() {
		this.cmd.exec();
		return this;
	}
}
