package sk.tomsik68.mclauncher.util;

import sk.tomsik68.mclauncher.api.ui.IProgressMonitor;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;

import org.tukaani.xz.XZInputStream;

public final class FileUtils {
	public static void createFileSafely(File file) throws Exception {
		File parentFile = new File(file.getParent());
		if (!parentFile.exists()) {
			if (!parentFile.mkdirs()) {
				throw new IOException("Unable to create parent file: " + file.getParent());
			}
		}
		if (file.exists())
			if (!file.delete())
				throw new IOException("Couldn't delete '".concat(file.getAbsolutePath()).concat("'"));
		if (!file.createNewFile())
			throw new IOException("Couldn't create '".concat(file.getAbsolutePath()).concat("'"));
	}

	public static void deleteDirectory(File file) {
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				deleteDirectory(f);
			}
			file.delete();
		} else {
			file.delete();
		}
	}

	public static InputStream getStreamFromFileWithinJar(File jar, String fileInJar) throws IOException {
		String inputFile = "jar:file:/" + jar.getAbsolutePath().replaceAll("\\\\", "/") + "!/"
				+ fileInJar.replaceAll("\\\\", "/");
		URL u = new URL(inputFile);
		JarURLConnection conn = (JarURLConnection) u.openConnection();
		InputStream is = conn.getInputStream();
		return is;
	}

	public static void downloadFileWithProgress(String url, File dest, IProgressMonitor progress) throws Exception {
		// System.out.println("Downloading "+url);
		String md5 = null;
		if (dest.exists()) {
			md5 = getMD5(dest);
		}

		URL u = new URL(url);
		HttpURLConnection connection = (HttpURLConnection) u.openConnection();
		if (md5 != null)
			connection.setRequestProperty("If-None-Match", md5);
		connection.connect();

		// local copy is up-to-date
		if (connection.getResponseCode() == 304) {
			return;
		}
		createFileSafely(dest);

		BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest));

		final int len = connection.getContentLength();
		if (progress != null)
			progress.setMax(len);

		int readBytes = 0;
		byte[] block;

		while (readBytes < len) {
			block = new byte[8192];
			int readNow = in.read(block);
			if (readNow > 0)
				out.write(block, 0, readNow);
			if (progress != null)
				progress.setProgress(readBytes);
			readBytes += readNow;
		}
		out.flush();
		out.close();
		in.close();
	}

	public static void copyFile(File from, File to) throws Exception {
		createFileSafely(to);
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(from));
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(to));
		byte[] block;
		while (bis.available() > 0) {
			block = new byte[8192];
			final int readNow = bis.read(block);
			bos.write(block, 0, readNow);
		}
		bos.flush();
		bos.close();
		bis.close();
	}

	public static void writeFile(File dest, String str) throws Exception {
		createFileSafely(dest);
		FileWriter fw = new FileWriter(dest);
		fw.write(str);
		fw.flush();
		fw.close();
	}

	// this method is copied from original launcher, as the MD5-ing function
	// needs to be the same
	public static String getMD5(File file) throws Exception {
		DigestInputStream stream = null;
		try {
			stream = new DigestInputStream(new FileInputStream(file), MessageDigest.getInstance("MD5"));
			byte[] buffer = new byte[65536];

			int read = stream.read(buffer);
			while (read >= 1)
				read = stream.read(buffer);
		} catch (Exception ignored) {
			return null;
		} finally {
			stream.close();
		}

		return String.format("%1$032x", new BigInteger(1, stream.getMessageDigest().digest()));
	}

	public static void downloadAndUnpackFileWithProgress(String url, File dest, IProgressMonitor p) throws Exception {
		String urlWithPack = url.concat(".pack.xz");
		File f = File.createTempFile(dest.getName(), ".pack.xz");
		downloadFileWithProgress(urlWithPack, f, p);
		byte[] file = readFully(new FileInputStream(f));
		byte[] decompressed = FileUtils.readFully(new XZInputStream(new ByteArrayInputStream(file)));

		String end = new String(decompressed, decompressed.length - 4, 4);
		if (!end.equals("SIGN")) {
			System.out.println("Unpacking failed, signature missing " + end);
			return;
		}
		int x = decompressed.length;
		int len = ((decompressed[x - 8] & 0xFF)) | ((decompressed[x - 7] & 0xFF) << 8) | ((decompressed[x - 6] & 0xFF) << 16) | ((decompressed[x - 5] & 0xFF) << 24);
		File temp = File.createTempFile("art", ".pack");

		byte[] checksums = Arrays.copyOfRange(decompressed, decompressed.length - len - 8, decompressed.length - 8);
		OutputStream out = new FileOutputStream(temp);
		out.write(decompressed, 0, decompressed.length - len - 8);
		out.close();
		decompressed = null;
		file = null;
		System.gc();

		FileOutputStream jarBytes = new FileOutputStream(dest);
		JarOutputStream jos = new JarOutputStream(jarBytes);

		Pack200.newUnpacker().unpack(temp, jos);

		JarEntry checksumsFile = new JarEntry("checksums.sha1");
		checksumsFile.setTime(0);
		jos.putNextEntry(checksumsFile);
		jos.write(checksums);
		jos.closeEntry();

		jos.close();
		jarBytes.close();
		temp.delete();
		f.delete();
	}
	public static byte[] readFully(InputStream stream) throws IOException {
		byte[] data = new byte[4096];
		ByteArrayOutputStream entryBuffer = new ByteArrayOutputStream();
		int len;
		do {
			len = stream.read(data);
			if (len > 0) {
				entryBuffer.write(data, 0, len);
			}
		} while (len != -1);

		return entryBuffer.toByteArray();
	}
}
