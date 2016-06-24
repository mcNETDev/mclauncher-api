package sk.tomsik68.mclauncher.impl.versions.mcdownload;

import java.io.File;
import java.io.InputStream;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import sk.tomsik68.mclauncher.api.versions.IVersion;
import sk.tomsik68.mclauncher.api.versions.IVersionList;
import sk.tomsik68.mclauncher.api.versions.LatestVersionInformation;
import sk.tomsik68.mclauncher.impl.common.Observable;
import sk.tomsik68.mclauncher.util.FileUtils;

public class MCDownloadOnlineForgeVersionList extends Observable<String> implements IVersionList {
	public static final String FORGE_INSTALLER = "http://files.minecraftforge.net/maven/net/minecraftforge/forge/<VERSION>/forge-<VERSION>-installer.jar";
	public static final String FORGE_UNIVERSAL = "http://files.minecraftforge.net/maven/net/minecraftforge/forge/<VERSION>/forge-<VERSION>-universal.jar";

	MCDownloadOnlineForgeVersionList() {
	}

	@Override
	public void startDownload() throws Exception {
		// TODO Forge dont provide a Version list :-(
	}

	@Override
	public IVersion retrieveVersionInfo(String id) throws Exception {
		String url = FORGE_UNIVERSAL.replaceAll("<VERSION>", id);
		File f = File.createTempFile("forge-" + id + "-universal", ".jar");
		FileUtils.downloadFileWithProgress(url, f, null);
		InputStream in = FileUtils.getStreamFromFileWithinJar(f, "version.json");
		MCDownloadVersion ver = new MCDownloadVersion((JSONObject) JSONValue.parse(in));
		ver.setId(id); //Because forge set the ID in the json file to: 1.9-forge1.9-12.16.1.1889 instead of 1.9-12.16.1.1889
		ver.setForge(true);
		return ver;
	}

	@Override
	public LatestVersionInformation getLatestVersionInformation() throws Exception {
		// TODO Forge dont provide a Version list :-(
		return null;
	}

}
