package fr.soe.a3s.constant;

public enum GameVersions {

	ARMA3("Arma 3"),ARMA3_AIA("Arma 3 - AiA");

	private String description;
	
	private GameVersions(String description){
		this.description = description;
	}
	
	public String getDescription(){
		return this.description;
	}
	
}
