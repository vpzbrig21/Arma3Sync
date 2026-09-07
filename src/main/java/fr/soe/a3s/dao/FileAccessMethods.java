package fr.soe.a3s.dao;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileAccessMethods implements DataAccessConstants {

	private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

	public static void copyDirectory(File sourceLocation, File targetLocation) throws IOException {

		if (sourceLocation.isDirectory()) {
			if (!targetLocation.exists()) {
				targetLocation.mkdir();
			}

			String[] children = sourceLocation.list();
			if (children != null) {
				for (String child : children) {
					copyDirectory(new File(sourceLocation, child), new File(targetLocation, child));
				}
			}
		} else {
			try (InputStream in = new BufferedInputStream(new FileInputStream(sourceLocation));
					OutputStream out = new BufferedOutputStream(new FileOutputStream(targetLocation))) {
				in.transferTo(out);
			}
		}
	}

	public static boolean deleteDirectory(File file) {

		if (file.exists()) {
			File[] subFiles = file.listFiles();
			if (subFiles != null) {
				for (int i = 0; i < subFiles.length; i++) {
					if (subFiles[i].isDirectory()) {
						deleteDirectory(subFiles[i]);
					} else {
						subFiles[i].delete();
					}
				}
			}
		}
		return (file.delete());
	}

	public static void copyFile(File sourceLocation, File targetLocation) throws IOException {

		try (FileInputStream fis = new FileInputStream(sourceLocation);
				FileOutputStream fos = new FileOutputStream(targetLocation);
				FileChannel source = fis.getChannel();
				FileChannel destination = fos.getChannel()) {
			destination.transferFrom(source, 0, source.size());
		}
	}

	public static boolean deleteFile(File file) {
		boolean response = false;
		if (file.exists()) {
			response = file.delete();
		}
		return response;
	}

	public static void setWritePermissions(File folder) {

		if (folder.exists()) {
			File[] files = folder.listFiles();
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					File f = files[i];
					if (f.isDirectory()) {
						boolean succeed = f.setWritable(true, false);
						setWritePermissions(f);
					}
				}
			}
		}
	}

	public static String getCanonicalPath(File file) {

		String filePath = null;
		try {
			filePath = file.getCanonicalPath();
		} catch (IOException e) {
			filePath = file.getAbsolutePath();
		}
		return filePath;
	}

	public static void extractToFolder(File zipFile, File folder) throws IOException {

		final File canonicalFolder = folder.getCanonicalFile();
		if (!canonicalFolder.exists() && !canonicalFolder.mkdirs()) {
			throw new IOException("Unable to create extraction folder: " + canonicalFolder);
		}
		if (!canonicalFolder.isDirectory()) {
			throw new IOException("Extraction target is not a directory: " + canonicalFolder);
		}

		try (ZipInputStream zis = new ZipInputStream(
				new BufferedInputStream(new FileInputStream(zipFile.getCanonicalFile())))) {
			ZipEntry ze;
			while ((ze = zis.getNextEntry()) != null) {
				File f = resolveZipEntry(canonicalFolder, ze.getName());
				if (ze.isDirectory()) {
					if (!f.exists() && !f.mkdirs()) {
						throw new IOException("Unable to create extracted directory: " + f);
					}
					continue;
				}

				File parent = f.getParentFile();
				if (parent != null && !parent.exists() && !parent.mkdirs()) {
					throw new IOException("Unable to create extracted file parent: " + parent);
				}
				try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(f))) {
					final byte[] buf = new byte[8192];
					int bytesRead;
					while ((bytesRead = zis.read(buf)) != -1) {
						fos.write(buf, 0, bytesRead);
					}
				} catch (IOException ioe) {
					f.delete();
					throw ioe;
				}
			}
		}
	}

	private static File resolveZipEntry(File extractionFolder, String entryName) throws IOException {
		if (entryName == null || entryName.isEmpty()) {
			throw new IOException("ZIP entry has no name.");
		}
		File destination = new File(extractionFolder, entryName).getCanonicalFile();
		String root = extractionFolder.getCanonicalPath();
		String destinationPath = destination.getCanonicalPath();
		if (!destinationPath.equals(root)
				&& !destinationPath.startsWith(root + File.separator)) {
			throw new IOException("Unsafe ZIP entry: " + entryName);
		}
		return destination;
	}

	public static boolean zip(File zipFile, File folder) throws IOException {

		boolean result = false;
		try {
			System.out.println("Program Start zipping the given files");
			/*
			 * send to the zip procedure
			 */
			zipFolder(folder.getAbsolutePath(), zipFile.getAbsolutePath());
			result = true;
			System.out.println("Given files are successfully zipped");
		} catch (Exception e) {
			System.out.println("Some Errors happned during the zip process");
		}
		return result;
	}

	private static void zipFolder(String srcFolder, String destZipFile) throws Exception {
		try (FileOutputStream fileWriter = new FileOutputStream(destZipFile);
				ZipOutputStream zip = new ZipOutputStream(fileWriter)) {
			addFolderToZip("", srcFolder, zip);
		}
	}

	/*
	 * recursively add files to the zip files
	 */
	private static void addFileToZip(String path, String srcFile, ZipOutputStream zip, boolean flag) throws Exception {
		/*
		 * create the file object for inputs
		 */
		File folder = new File(srcFile);

		/*
		 * if the folder is empty add empty folder to the Zip file
		 */
		if (flag) {
			zip.putNextEntry(new ZipEntry(path + "/" + folder.getName() + "/"));
		} else if (folder.isDirectory()) {
			addFolderToZip(path, srcFile, zip);
		} else {
			byte[] buf = new byte[1024];
			int len;
			try (FileInputStream in = new FileInputStream(srcFile)) {
				zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
				while ((len = in.read(buf)) > 0) {
					zip.write(buf, 0, len);
				}
			}
		}
	}

	/*
	 * add folder to the zip file
	 */
	private static void addFolderToZip(String path, String srcFolder, ZipOutputStream zip) throws Exception {
		File folder = new File(srcFolder);

		/*
		 * check the empty folder
		 */
		String[] children = folder.list();
		if (children == null || children.length == 0) {
			addFileToZip(path, srcFolder, zip, true);
			return;
		}

		for (String fileName : children) {
			String prefix = path.isEmpty() ? folder.getName() : path + "/" + folder.getName();
			addFileToZip(prefix, srcFolder + "/" + fileName, zip, false);
		}
	}

	public static String computeSHA1(File file) throws IOException {

		if (file.length() == 0) {
			return "0";
		}

		/*
		 * char[] chars = null; MessageDigest md = MessageDigest.getInstance("SHA1");
		 * FileInputStream fis = new FileInputStream(file); FileChannel ch =
		 * fis.getChannel(); MappedByteBuffer mb = ch.map(FileChannel.MapMode.READ_ONLY,
		 * 0L, ch.size()); int buffsize = (int) Math.min(file.length(), 4 * 1024 *
		 * 1024); byte[] dataBytes = new byte[buffsize]; long checkSum = 0L; int nread;
		 * while (mb.hasRemaining()) { nread = Math.min(mb.remaining(), buffsize);
		 * mb.get(dataBytes, 0, nread); md.update(dataBytes, 0, nread); } fis.close();
		 * byte[] mdbytes = md.digest(); chars = new char[2 *
		 * mdbytes.length]; for (int i = 0; i < mdbytes.length; ++i) { chars[2 * i] =
		 * HEX_CHARS[(mdbytes[i] & 0xF0) >>> 4]; chars[2 * i + 1] = HEX_CHARS[mdbytes[i]
		 * & 0x0F]; }
		 */

		try (FileInputStream fis = new FileInputStream(file); ReadableByteChannel inChannel = Channels.newChannel(fis)) {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			int buffsize = (int) Math.min(file.length(), 4 * 1024 * 1024);
			ByteBuffer buffer = ByteBuffer.allocate(buffsize);
			int nread = 0;
			while ((nread = inChannel.read(buffer)) != -1) {
				md.update(buffer.array(), 0, nread);
				((Buffer) buffer).clear();
			}
			byte[] mdbytes = md.digest();
			char[] chars = new char[2 * mdbytes.length];
			for (int i = 0; i < mdbytes.length; ++i) {
				chars[2 * i] = HEX_CHARS[(mdbytes[i] & 0xF0) >>> 4];
				chars[2 * i + 1] = HEX_CHARS[mdbytes[i] & 0x0F];
			}
			return new String(chars);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
