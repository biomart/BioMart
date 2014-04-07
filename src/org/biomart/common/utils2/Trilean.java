package org.biomart.common.utils2;

public enum Trilean {
	PLUS, ZERO, MINUS;
	public int toInteger () {
		if (this==PLUS) {
			return 1;
		} else if (this==MINUS) {
			return -1;
		} else {
			return 0;
		}
	}
	public static Trilean toTrilean (int integer) {
		if (integer==1) {
			return PLUS;
		} else if (integer==-1) {
			return MINUS;
		} else if (integer==0) {
			return ZERO;
		} else {
			return null;
		}
	}
	public boolean isPlus() {
		return this.equals(PLUS);
	}
	public boolean isMinus() {
		return this.equals(MINUS);
	}
	public boolean isZero() {
		return this.equals(ZERO);
	}
	public boolean isNotMinus() {
		return this.equals(ZERO) || this.equals(PLUS);
	}
	public boolean isNotPlus() {
		return this.equals(ZERO) || this.equals(MINUS);
	}
}
