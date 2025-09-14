package fr.soe.a3s.service.synchronization;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fr.soe.a3s.service.RepositoryService;
import fr.soe.a3s.utils.UnitConverter;

public class FilesSynchronizationReportManager {

	/* Data */
	private String repositoryName;

	/* Manager */
	private FilesSynchronizationManager filesManager;

	/* Services */
	private final RepositoryService repositoryService = new RepositoryService();

	public FilesSynchronizationReportManager(String repositoryName, FilesSynchronizationManager filesManager) {
		this.repositoryName = repositoryName;
		this.filesManager = filesManager;
	}

	/* Generate Report */

	public String generateReport(String message) {

		String header = "--- Download report ---";
		String repositoryInfo = "Repository name: " + repositoryName;
		String repositoryUrl = "Repository url: " + repositoryService.getRepositoryUrl(repositoryName);
		String endDate = "Download finished on: " + new Date().toLocaleString();

		// Server Connection
		String avgDlSpeed = "unavailable";
		if (filesManager.getAverageDownloadSpeed() > 0) {
			avgDlSpeed = UnitConverter.convertSpeed(filesManager.getAverageDownloadSpeed());
		}
		String serverConnectionInfo = "Server connection:" + "\n" + "- Average download speed: " + avgDlSpeed + "\n"
				+ "- Number of active connections used: " + filesManager.getMaxActiveconnections();

		filesManager.report();

		// Global File transfer:
		String savedSizeFileTransfer = UnitConverter
				.convertSize(filesManager.getTotalDiskFilesSize() - filesManager.getResumedFilesSize());
		int savedSizeFileTransferFraction = 0;
		if (filesManager.getTotalDiskFilesSize() != 0) {
			savedSizeFileTransferFraction = (int) (((filesManager.getTotalDiskFilesSize()
					- filesManager.getResumedFilesSize()) * 100) / filesManager.getTotalDiskFilesSize());
		}

		String fileTransfer = "Global file transfer:" + "\n" + "- Number of files updated: "
				+ filesManager.getListFilesToUpdate().size() + "\n" + "- Total files size on disk: "
				+ UnitConverter.convertSize(filesManager.getTotalDiskFilesSize()) + "\n" + "- Downloaded data: "
				+ UnitConverter.convertSize(filesManager.getResumedFilesSize()) + "\n" + "- Saved: "
				+ savedSizeFileTransfer + " (" + savedSizeFileTransferFraction + "%)";

		// Partial file transfer
		String savedPartialSizeFileTransfer = UnitConverter.convertSize(
				filesManager.getTotalUncompleteDiskFileSize() - filesManager.getTotalUncompleteExpectedFileSize());
		int savedPartialSizeFileTransferFraction = 0;
		if (filesManager.getTotalUncompleteDiskFileSize() != 0) {
			savedPartialSizeFileTransferFraction = (int) (((filesManager.getTotalUncompleteDiskFileSize()
					- filesManager.getTotalUncompleteExpectedFileSize()) * 100)
					/ filesManager.getTotalUncompleteDiskFileSize());
		}

		String partialFileTransferInfo = "Partial file transfer:" + "\n" + "- Number of files updated: "
				+ filesManager.getTotalNumberUnCompleteFiles() + "\n" + "- Total files size on disk: "
				+ UnitConverter.convertSize(filesManager.getTotalUncompleteDiskFileSize()) + "\n"
				+ "- Downloaded data: " + UnitConverter.convertSize(filesManager.getTotalUncompleteExpectedFileSize())
				+ "\n" + "- Saved: " + savedPartialSizeFileTransfer + " (" + savedPartialSizeFileTransferFraction
				+ "%)";

		// Compressed file transfer
		String savedCompressedSizeFileTransfer = UnitConverter
				.convertSize(filesManager.getTotalUncompressedFilesSize() - filesManager.getTotalCompressedFilesSize());
		int savedCompressedSizeFileTransferFraction = 0;
		if (filesManager.getTotalUncompressedFilesSize() != 0) {
			savedCompressedSizeFileTransferFraction = (int) (((filesManager.getTotalUncompressedFilesSize()
					- filesManager.getTotalCompressedFilesSize()) * 100)
					/ filesManager.getTotalUncompressedFilesSize());
		}

		String compressionFileTransferInfo = "Compressed file transfer:" + "\n" + "- Number of files updated: "
				+ filesManager.getTotalNumberCompressedFiles() + "\n" + "- Total files size on disk: "
				+ UnitConverter.convertSize(filesManager.getTotalUncompressedFilesSize()) + "\n" + "- Downloaded data: "
				+ UnitConverter.convertSize(filesManager.getTotalCompressedFilesSize()) + "\n" + "- Saved: "
				+ savedCompressedSizeFileTransfer + " (" + savedCompressedSizeFileTransferFraction + "%)";

		String report = header + "\n" + repositoryInfo + "\n" + repositoryUrl + "\n" + endDate + "\n\n" + message
				+ "\n\n" + serverConnectionInfo + "\n\n" + fileTransfer + "\n\n" + partialFileTransferInfo + "\n\n"
				+ compressionFileTransferInfo;

		return report;
	}

	public String generateReport(String message, List<Exception> errors) {

		String header = "--- Download report ---";
		String repositoryInfo = "Repository name: " + repositoryName;
		String repositoryUrl = "Repository url: " + repositoryService.getRepositoryUrl(repositoryName);
		String endDate = "Download finished on: " + new Date().toLocaleString();

		List<String> messages = new ArrayList<String>();
		for (Exception e : errors) {
			if (e instanceof IOException) {
				messages.add("- " + e.getMessage());
			} else {
				String coreMessage = "- An unexpected error has occured.";
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				String stacktrace = sw.toString(); // stack trace as a string
				coreMessage = coreMessage + "\n" + "StackTrace:" + "\n" + stacktrace;
				messages.add(coreMessage);
			}
		}

		String report = header + "\n" + repositoryInfo + "\n" + repositoryUrl + "\n" + endDate + "\n\n" + message;
		for (String m : messages) {
			report = report + "\n" + m;
		}
		return report;
	}
}
