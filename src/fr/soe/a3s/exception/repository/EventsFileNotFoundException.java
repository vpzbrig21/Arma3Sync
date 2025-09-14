package fr.soe.a3s.exception.repository;

public class EventsFileNotFoundException extends RepositoryException {

	private static String message = "File /.a3s/events not found on repository: ";

	public EventsFileNotFoundException(String repositoryName) {
		super(message + repositoryName);
	}
}
