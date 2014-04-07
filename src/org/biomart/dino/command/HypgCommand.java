package org.biomart.dino.command;

import java.io.File;

import org.apache.commons.lang.StringUtils;

public class HypgCommand implements ShellCommand {

	File background, sets, annotations, bin;
	String cutoff;
	boolean bonferroni = false;

	@Override
	public String build() {
		return StringUtils.join(new String[] {
			bin.getPath(),
			bonferroni ? "-B": "",
			"-g", getBackground().getPath(),
			"-s", getSets().getPath(),
			"-a", getAnnotations().getPath(),
			"-c", getCutoff()
		}, " ");
	}
	
	

	public boolean isBonferroni() {
        return bonferroni;
    }


    public void setBonferroni(boolean bonferroni) {
        this.bonferroni = bonferroni;
    }


    public File getCmdBinPath() {
		return bin;
	}

	public HypgCommand setCmdBinPath(File bin) {
		this.bin = bin;
		return this;
	}

	public File getBackground() {
		return background;
	}

	public HypgCommand setBackground(File background) {
		this.background = background;
		return this;
	}

	public File getSets() {
		return sets;
	}

	public HypgCommand setSets(File sets) {
		this.sets = sets;
		return this;
	}

	public File getAnnotations() {
		return annotations;
	}

	public HypgCommand setAnnotations(File annotations) {
		this.annotations = annotations;
		return this;
	}

	public String getCutoff() {
		return cutoff;
	}

	public HypgCommand setCutoff(String cutoff) {
		this.cutoff = cutoff;
		return this;
	}



}
