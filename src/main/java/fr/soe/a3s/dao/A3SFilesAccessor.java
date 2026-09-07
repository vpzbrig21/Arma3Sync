package fr.soe.a3s.dao;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.ObjectInputFilter;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipException;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SealedObject;

public class A3SFilesAccessor implements DataAccessConstants {

	protected static final String FILE_CORRUPTED = "The file seems to be corrupted.";
	private static final ObjectInputFilter LEGACY_OBJECT_FILTER = info -> {
		if (info.depth() > 100 || info.references() > 250_000 || info.streamBytes() > 256L * 1024L * 1024L) {
			return ObjectInputFilter.Status.REJECTED;
		}

		Class<?> serialClass = info.serialClass();
		if (serialClass == null) {
			return ObjectInputFilter.Status.UNDECIDED;
		}
		if (serialClass.isArray()) {
			/*
			 * ObjectOutputStream uses Object[] as an internal container for several
			 * legacy collection implementations. Check the component type without
			 * treating the array itself as an ordinary serialized class. Reject arrays
			 * whose component type is not part of the allowlist.
			 */
			Class<?> componentType = serialClass.getComponentType();
			if (componentType.isPrimitive() || componentType == Object.class || isAllowedClass(componentType)) {
				return ObjectInputFilter.Status.ALLOWED;
			}
			return ObjectInputFilter.Status.REJECTED;
		}
		if (serialClass.isPrimitive() || serialClass == Object.class || isAllowedClass(serialClass)) {
			return ObjectInputFilter.Status.ALLOWED;
		}
		return ObjectInputFilter.Status.REJECTED;
	};

	public static Object read(File file) throws IOException {

		if (file == null) throw new IllegalArgumentException("file must not be null");

		Object object = null;
		ObjectInputStream fRo = null;
		try {
			fRo = new ObjectInputStream(new GZIPInputStream(new FileInputStream(file)));
			fRo.setObjectInputFilter(LEGACY_OBJECT_FILTER);
			object = fRo.readObject();
		} catch (IOException e) {
			String detail = e.getMessage();
			if ((detail == null || detail.isBlank()) && e.getCause() != null) {
				detail = e.getCause().toString();
			}
			String message = "Failed to read file: " + file.getName() + (detail == null ? "" : "\n" + detail);
			if (e instanceof ZipException || e instanceof EOFException) {
				message = message + "\n" + FILE_CORRUPTED;
			}
			throw new IOException(message);
		} catch (ClassNotFoundException e) {
			throw new IOException("Failed to deserialize file: " + file.getName(), e);
		} finally {
			if (fRo != null) {
				fRo.close();
			}
		}
		return object;
	}

	public static Object read(Cipher cipher, File file) throws IOException {

		if (file == null) throw new IllegalArgumentException("file must not be null");

		Object object = null;
		ObjectInputStream fRo = null;
		try {
			fRo = new ObjectInputStream(new GZIPInputStream(new FileInputStream(file)));
			fRo.setObjectInputFilter(LEGACY_OBJECT_FILTER);
			SealedObject sealedObject = (SealedObject) fRo.readObject();
			object = sealedObject.getObject(cipher);
		} catch (IOException e) {
			String detail = e.getMessage();
			if ((detail == null || detail.isBlank()) && e.getCause() != null) {
				detail = e.getCause().toString();
			}
			String message = "Failed to read file: " + file.getName() + (detail == null ? "" : "\n" + detail);
			if (e instanceof ZipException || e instanceof EOFException) {
				message = message + "\n" + FILE_CORRUPTED;
			}
			throw new IOException(message, e);
		} catch (Exception e) {
			throw new IOException("Failed to decrypt serialized file: " + file.getName(), e);
		} finally {
			if (fRo != null) {
				fRo.close();
			}
		}
		return object;
	}

	private static boolean isAllowedClass(Class<?> serialClass) {
		String name = serialClass.getName();
		if (name.startsWith("fr.soe.a3s.")) {
			return true;
		}
		return name.equals("java.lang.String")
				|| name.equals("java.lang.Enum")
				|| name.equals("java.lang.Boolean")
				|| name.equals("java.lang.Byte")
				|| name.equals("java.lang.Character")
				|| name.equals("java.lang.Double")
				|| name.equals("java.lang.Float")
				|| name.equals("java.lang.Integer")
				|| name.equals("java.lang.Long")
				|| name.equals("java.lang.Short")
				|| name.equals("java.util.ArrayList")
				|| name.equals("java.util.HashMap")
				|| name.equals("java.util.HashSet")
				|| name.equals("java.util.LinkedHashMap")
				|| name.equals("java.util.LinkedHashSet")
				|| name.equals("java.util.Properties")
				|| name.equals("java.util.TreeMap")
				|| name.equals("java.util.TreeSet")
				|| name.equals("java.util.Date")
				|| name.equals("java.util.Map$Entry")
				|| name.equals("javax.crypto.SealedObject");
	}

	public static void write(Serializable object, File file) throws IOException {

		if (object == null) throw new IllegalArgumentException("object must not be null");

		Path temporary = createTemporaryFile(file);
		ObjectOutputStream fWo = null;
		try {
			fWo = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(temporary.toFile())));
			fWo.writeObject(object);
			fWo.close();
			fWo = null;
			replaceFile(temporary, file.toPath());
		} finally {
			if (fWo != null) {
				fWo.close();
			}
			Files.deleteIfExists(temporary);
		}
	}

	public static void write(Serializable object, Cipher cipher, File file)
			throws IllegalBlockSizeException, IOException {

		if (object == null) throw new IllegalArgumentException("object must not be null");

		Path temporary = createTemporaryFile(file);
		ObjectOutputStream fWo = null;
		try {
			SealedObject sealedObject = new SealedObject(object, cipher);
			fWo = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(temporary.toFile())));
			if (sealedObject != null) {
				fWo.writeObject(sealedObject);
			}
			fWo.close();
			fWo = null;
			replaceFile(temporary, file.toPath());
		} finally {
			if (fWo != null) {
				fWo.close();
			}
			Files.deleteIfExists(temporary);
		}
	}

	private static Path createTemporaryFile(File file) throws IOException {
		Path target = file.toPath().toAbsolutePath();
		Path parent = target.getParent();
		if (parent == null) throw new IOException("Target file has no parent directory: " + file);
		return Files.createTempFile(parent, target.getFileName().toString() + ".", ".tmp");
	}

	private static void replaceFile(Path temporary, Path target) throws IOException {
		try {
			Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (AtomicMoveNotSupportedException exception) {
			Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
