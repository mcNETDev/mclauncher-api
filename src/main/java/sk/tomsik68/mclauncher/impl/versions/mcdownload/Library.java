package sk.tomsik68.mclauncher.impl.versions.mcdownload;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import sk.tomsik68.mclauncher.api.common.IOperatingSystem;
import sk.tomsik68.mclauncher.api.common.MCLauncherAPI;
import sk.tomsik68.mclauncher.impl.common.Platform;
import sk.tomsik68.mclauncher.impl.versions.mcdownload.Rule.Action;
import sk.tomsik68.mclauncher.util.IExtractRules;
import sk.tomsik68.mclauncher.util.StringSubstitutor;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Represents a library that is needed to run minecraft.
 */
final class Library {
    private final StringSubstitutor libraryPathSubstitutor = new StringSubstitutor("${%s}");
    private final String name;
    private final HashMap<String, String> natives = new HashMap<String, String>();
    private final ArrayList<Rule> rules = new ArrayList<Rule>();
    private LibraryExtractRules extractRules;
    private final static String LIBRARY_BASE_URL = "https://libraries.minecraft.net/";
    private String url = LIBRARY_BASE_URL;

    public Library(JSONObject json) {
        name = json.get("name").toString();
        if (json.containsKey("natives")) {
            JSONObject nativesObj = (JSONObject) json.get("natives");
            for (String nativeKey : nativesObj.keySet()) {
                String key = nativeKey;
                String value = nativesObj.get(nativeKey).toString();

                natives.put(key, value);
            }
        }
        if (json.containsKey("rules")) {
            JSONArray rulz = (JSONArray) json.get("rules");
            for (int i = 0; i < rulz.size(); ++i) {
                rules.add(new Rule((JSONObject) rulz.get(i)));
            }
        }
        if (json.containsKey("extract")) {
            extractRules = new LibraryExtractRules((JSONObject) json.get("extract"));
        }
        if (json.containsKey("url")) {
            url = json.get("url").toString();
        }
    }

    public String getName() {
        return name;
    }

    /**
     * Returns name of library that holds natives for given operating system
     * @param os - IOperatingSystem to check
     * @return Name of library which holds natives for given OS
     */
    public String getNatives(IOperatingSystem os) {
        if (!natives.containsKey(os.getMinecraftName()))
            return natives.get(Platform.wrapName(os.getMinecraftName())).replace("${arch}", System.getProperty("sun.arch.data.model"));
        return natives.get(os.getMinecraftName()).replace("${arch}", os.getArchitecture());
    }

    /**
     * Returns relative path of library as string. Relative path is used in URLs, file paths.
     * You can read more about this on wiki...
     * @return Relative path of library.
     */
    public String getPath() {
        libraryPathSubstitutor.setVariable("arch", Platform.getCurrentPlatform().getArchitecture());
        String[] split = name.split(":");
        StringBuilder result = new StringBuilder();

        result = result.append(split[0].replace('.', '/'));// net/sf/jopt-simple
        result = result.append('/').append(split[1]).append('/').append(split[2]).append('/'); // /jopt-simple/4.4/
        result = result.append(split[1]).append('-').append(split[2]); // jopt-simple-4.4
        if (!natives.isEmpty()) {
            IOperatingSystem os = Platform.getCurrentPlatform();
            String osName = os.getMinecraftName();
            if(!natives.containsKey(osName))
                osName = Platform.wrapName(osName);
            result = result.append('-').append(natives.get(osName));
        }
        if(getName().startsWith("net.minecraftforge:forge:")){
			result = result.append("-universal");
		}
        result = result.append(".jar");
        return libraryPathSubstitutor.substitute(result.toString());
    }

    /**
     *
     * @return True if this library is compatible with the current operating system
     */
    boolean isCompatible() {
        Action action = Action.DISALLOW;
        for (Rule rule : rules) {
            // rule may only change resulting action if it's effective...
            if (rule.applies()) {
                action = rule.getAction();
                System.out.println("Rule: " + rule.toString());
            }
        }
        // the following condition is very important and can brackets can be ignored while reading(they're just to increase readability)
        // library is compatible if:
        //    (there are no rules) OR ((action is allow) AND (there are EITHER ((no natives) OR (natives for this platform are available))))
        return rules.isEmpty()
                || (action == Action.ALLOW && (!hasNatives() || natives.containsKey(Platform.getCurrentPlatform().getMinecraftName()) || natives.containsKey(Platform.wrapName(Platform.getCurrentPlatform().getMinecraftName())) ));
    }

    /**
     *
     * @return True if there are natives for any platform
     */
    public boolean hasNatives() {
        return !natives.isEmpty();
    }

    /**
     *
     * @return IExtractRules that apply to this library
     */
    public IExtractRules getExtractRules() {
        return extractRules;
    }

    /**
     *
     * @return String which contains URL where this library can be downloaded
     */
    public String getDownloadURL() {
        return url.concat(getPath());
    }
}
