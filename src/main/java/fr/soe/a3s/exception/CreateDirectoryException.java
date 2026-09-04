package fr.soe.a3s.exception;

import java.io.File;
import java.io.IOException;

import fr.soe.a3s.dao.FileAccessMethods;

public class CreateDirectoryException extends IOException {

	private String filePath;

	public CreateDirectoryException(File file) {
		this.filePath = FileAccessMethods.getCanonicalPath(file);
	}

	@Override
	public String getMessage() {
		String message = "Cannot create directory " + filePath + "\n"
				+ "Please checkout file access permissions.";
		return message;
	}
}
