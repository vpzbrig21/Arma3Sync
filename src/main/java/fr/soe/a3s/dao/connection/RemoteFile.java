package fr.soe.a3s.dao.connection;

public class RemoteFile {

	private String filename;
	private String parentDirectoryRelativePath;
	private final boolean isDirectory;

	public RemoteFile(String filename, String parentDirectoryRelativePath, boolean isDirectory) {
		this.filename = filename;
		this.parentDirectoryRelativePath = parentDirectoryRelativePath;
		this.isDirectory = isDirectory;
		if (this.filename == null || this.parentDirectoryRelativePath == null) {
			throw new IllegalArgumentException("Remote file name and parent path must not be null.");
		}
	}

	public String getFilename() {
		return filename;
	}
	
	public void setFilename(String filenameIn) {
		filename = filenameIn;
	}

	public String getParentDirectoryRelativePath() {
		return parentDirectoryRelativePath;
	}
	
	public void setParentDirectoryRelativePath(String parentDirectoryRelativePathIn) {
		parentDirectoryRelativePath = parentDirectoryRelativePathIn;
	}

	public boolean isDirectory() {
		return isDirectory;
	}

	public String getRelativeFilePath() {

		String relativePath;
		if (parentDirectoryRelativePath == null || parentDirectoryRelativePath.isEmpty()) {
			relativePath = "/" + filename;
		} else {
			String normalizedParent = parentDirectoryRelativePath;
			if (normalizedParent.startsWith("/")) {
				normalizedParent = normalizedParent.substring(1);
			}
			if (normalizedParent.endsWith("/")) {
				normalizedParent = normalizedParent.substring(0, normalizedParent.length() - 1);
			}
			if (normalizedParent.isEmpty()) {
				relativePath = "/" + filename;
			} else {
				relativePath = "/" + normalizedParent + "/" + filename;
			}
		}

		// http server require a slash at the end of the directory name. Otherwise they
		// will respond with code 301 (permanently moved),
		if (isDirectory) {
			relativePath += "/";
		}

		return relativePath;
	}
}
