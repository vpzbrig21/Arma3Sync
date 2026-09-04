package fr.soe.a3s.domain.configration;

import java.io.Serializable;

public class ExternalApplication implements Comparable<ExternalApplication>, Serializable {

	private static final long serialVersionUID = 6695449091678057038L;
	private String name;
	private String executablePath;
	private String parameters;
	private boolean enable = false;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getExecutablePath() {
		return executablePath;
	}
	public void setExecutablePath(String executablePath) {
		this.executablePath = executablePath;
	}
	public String getParameters() {
		return parameters;
	}
	public void setParameters(String parameters) {
		this.parameters = parameters;
	}
	public boolean isEnable() {
		return enable;
	}
	public void setEnable(boolean enable) {
		this.enable = enable;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	@Override
	public int compareTo(ExternalApplication other) {
		String otherName = other != null && other.getName() != null ? other.getName() : "";
		String thisName = this.name != null ? this.name : "";
		return thisName.compareToIgnoreCase(otherName);
	}
}
