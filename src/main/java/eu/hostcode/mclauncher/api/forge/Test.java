package eu.hostcode.mclauncher.api.forge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;

import sk.tomsik68.mclauncher.api.common.ILaunchSettings;
import sk.tomsik68.mclauncher.api.common.IOperatingSystem;
import sk.tomsik68.mclauncher.api.common.MCLauncherAPI;
import sk.tomsik68.mclauncher.api.common.mc.MinecraftInstance;
import sk.tomsik68.mclauncher.api.login.ISession;
import sk.tomsik68.mclauncher.api.ui.IProgressMonitor;
import sk.tomsik68.mclauncher.api.versions.IVersion;
import sk.tomsik68.mclauncher.backend.GlobalAuthenticationSystem;
import sk.tomsik68.mclauncher.impl.common.Platform;
import sk.tomsik68.mclauncher.impl.versions.mcdownload.MCDownloadVersionList;
import sk.tomsik68.mclauncher.util.FileUtils;

public class Test {
	public static final File workDir = new File("D:\\mctest1\\");
	public static final MinecraftInstance instance = new MinecraftInstance(workDir);

	public static void main(String[] args) {
		MCLauncherAPI.log.setLevel(Level.FINE);
		new Test().testLauncher();
	}

	public void testLauncher() {
		ISession session = getSession();
		MCDownloadVersionList list = new MCDownloadVersionList(instance);
		try {
			//IVersion ver = list.retrieveVersionInfo("1.9-12.16.1.1892");
			IVersion ver = list.retrieveVersionInfo("1.9-12.16.1.1892");
			//IVersion ver = list.retrieveVersionInfo("1.7.10-10.13.4.1564-1.7.10");
			
			System.out.println("Starting Installer");
			ver.getInstaller().install(ver, instance, new IProgressMonitor() {

				@Override
				public void setStatus(String status) {
				}

				@Override
				public void setProgress(int progress) {
				}

				@Override
				public void setMax(int len) {
				}

				@Override
				public void incrementProgress(int amount) {
				}
			});
			System.out.println("Starting Launcher");
			List<String> launchCommand = ver.getLauncher().getLaunchCommand(session, instance, null, ver, new ILaunchSettings() {
				
				 @Override
                 public boolean isModifyAppletOptions() {
                     return false;
                 }

                 @Override
                 public String getInitHeap() {
                     return "2G";
                 }

                 @Override
                 public String getHeap() {
                     return "3G";
                 }

                 @Override
                 public Map<String, String> getCustomParameters() {
                     return null;
                 }

                 @Override
                 public List<String> getCommandPrefix() {
                     return Collections.emptyList();
                 }

                 @Override
                 public File getJavaLocation() {
                     return null;
                 }

                 @Override
                 public List<String> getJavaArguments() {
                     return null;
                 }
			}, null);
			for (String cmd : launchCommand) {
				System.out.print(cmd + " ");
			}
			System.out.println();
			ProcessBuilder pb = new ProcessBuilder(launchCommand);
			pb.redirectError(new File("mcerr.log"));
			pb.redirectOutput(new File("mcout.log"));
			pb.directory(instance.getLocation());
			Process proc = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line;
			while (isProcessAlive(proc)) {
				line = br.readLine();
				if (line != null && line.length() > 0)
					System.out.println(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void test1() {
		ISession session = getSession();
		System.out.println("Start Test1");
		MCDownloadVersionList list = new MCDownloadVersionList(instance);
		try {
			IVersion ver = list.retrieveVersionInfo("1.9-12.16.1.1891");
			ver.getInstaller().install(ver, instance, new IProgressMonitor() {

				@Override
				public void setStatus(String status) {
					System.out.println(status);
				}

				@Override
				public void setProgress(int progress) {
				}

				@Override
				public void setMax(int len) {
				}

				@Override
				public void incrementProgress(int amount) {
				}
			});
			List<String> launchCommand = ver.getLauncher().getLaunchCommand(session, instance, null, ver, null, null);
			for (String cmd : launchCommand) {
				System.out.print(cmd + " ");
			}
			System.out.println();
			ProcessBuilder pb = new ProcessBuilder(launchCommand);
			pb.redirectError(new File("mcerr.log"));
			pb.redirectOutput(new File("mcout.log"));
			pb.directory(instance.getLocation());
			Process proc = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line;
			while (isProcessAlive(proc)) {
				line = br.readLine();
				if (line != null && line.length() > 0)
					System.out.println(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static boolean isProcessAlive(Process proc) {
		try {
			System.out.println("Process exited with error code:" + proc.exitValue());
			return false;
		} catch (Exception e) {
			return true;
		}

	}

	public ISession getSession() {
		try {
			Scanner s = new Scanner(System.in);
			String email = "";
			String pass = "";
			ISession session = GlobalAuthenticationSystem.doPasswordLogin(email, pass);
			s.close();
			return session;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
