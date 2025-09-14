package fr.soe.a3s.dao.connection;

public class RemoteFile {

	private String filename;
	private String parentDirectoryRelativePath;
	private final boolean isDirectory;

	public RemoteFile(String filename, String parentDirectoryRelativePath, boolean isDirectory) {
		this.filename = filename;
		this.parentDirectoryRelativePath = parentDirectoryRelativePath;
		this.isDirectory = isDirectory;
		assert (this.filename != null);
		assert (this.parentDirectoryRelativePath != null);
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

		String relativePath = "";

		if (parentDirectoryRelativePath.isEmpty()) {
			relativePath = "/" + filename;
		} else {
			relativePath = "/" + parentDirectoryRelativePath + "/" + filename;
		}

		// http server require a slash at the end of the directory name. Otherwise they
		// will respond with code 301 (permanently moved),
		if (isDirectory) {
			relativePath += "/";
		}

		return relativePath;
	}
}
