package fr.soe.a3s.exception.repository;

public class RepositoryServerInfoReadException extends RepositoryException {

	private static final long serialVersionUID = 1L;

	public RepositoryServerInfoReadException(String repositoryName, String details) {
		super(buildMessage(repositoryName, details));
	}

	private static String buildMessage(String repositoryName, String details) {
		String base = "Failed to reload server info for repository \"" + repositoryName + "\"";
		if (details != null && !details.isEmpty()) {
			return base + ": " + details;
		}
		return base;
	}
}
