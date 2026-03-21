package fr.soe.a3s.exception.repository;

public class RepositoryDefaultDownloadLocationNotFoundException extends RepositoryException {

	private static String message = "Default destination folder is empty!";

	public RepositoryDefaultDownloadLocationNotFoundException(String repositoryName) {
		super(message);
	}
}
