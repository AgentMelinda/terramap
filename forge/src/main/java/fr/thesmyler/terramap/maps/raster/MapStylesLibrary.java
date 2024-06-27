package fr.thesmyler.terramap.maps.raster;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

import fr.thesmyler.terramap.TerramapConfig;
import fr.thesmyler.terramap.maps.raster.imp.UrlTiledMap;
import net.smyler.smylib.text.Text;
import net.smyler.terramap.Terramap;
import net.smyler.terramap.util.geo.WebMercatorBounds;

import static java.lang.Integer.parseInt;
import static net.smyler.smylib.Preconditions.checkState;


public class MapStylesLibrary {

    private static final String BUILT_IN_MAPS = "assets/terramap/mapstyles.json";
    private static File configMapsFile;
    private static final Map<String, RasterTiledMap> baseMaps = new HashMap<>();
    private static final Map<String, UrlTiledMap> userMaps = new HashMap<>();

    /**
     * Get the default Terramap maps, loaded from the jar and from the online source
     * The returned map is a new one, and can be mutated safely.
     * Does not actually load the maps, this needs to be done beforehand with {@link MapStylesLibrary#loadBuiltIns()} and {@link MapStylesLibrary#loadFromOnline(String)}
     * 
     * @return a new map that contains id => TiledMap couples
     */
    public static Map<String, RasterTiledMap> getBaseMaps() {
        return new HashMap<>(baseMaps);
    }

    /**
     * Get the default Terramap maps, loaded from config/terramap_user_styles.json.
     * The returned map is a new one, and can be mutated safely.
     * 
     * @return a new map that contains id => TiledMap couples
     */
    public static Map<String, UrlTiledMap> getUserMaps() {
        return new HashMap<>(userMaps);
    }

    /**
     * Loads map styles from the mod's jar.
     */
    public static void loadBuiltIns() {
        TiledMapProvider.BUILT_IN.setLastError(null);
        String path = BUILT_IN_MAPS;
        try {
            // https://github.com/MinecraftForge/MinecraftForge/issues/5713
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
            checkState(in != null, "Resource not found: " + path);
            try(BufferedReader txtReader = new BufferedReader(new InputStreamReader(in))) {
                StringBuilder json = new StringBuilder();
                String line = txtReader.readLine();
                while(line != null) {
                    json.append(line);
                    line = txtReader.readLine();
                }
                baseMaps.putAll(loadFromJson(json.toString(), TiledMapProvider.BUILT_IN));
            }
        } catch(Exception e) {
            Terramap.instance().logger().fatal("Failed to read built-in map styles, Terramap is likely to not work properly!");
            Terramap.instance().logger().fatal("Path: {}", path);
            Terramap.instance().logger().catching(e);
            TiledMapProvider.BUILT_IN.setLastError(e);
        }

    }

    public static void loadInternals() {
        TiledMapProvider.INTERNAL.setLastError(null);
        try {
            // We currently have no internal styles
        } catch(Exception e) {
            Terramap.instance().logger().error("Failed to load internal map styles");
            Terramap.instance().logger().catching(e);
            TiledMapProvider.INTERNAL.setLastError(e);
        }
    }

    /**
     * Loads map styles from the online file.
     * Resolve the TXT field of the given hostname,
     * parses it as redirect as would most browsers
     * and does a request to the corresponding url.
     * The body of that request is then parsed as a map style json config file.
     * <br>
     * This should be called after {@link #loadBuiltIns()} so it overwrites it.
     * 
     * @param hostname - the hostname to lookup
     */
    public static void loadFromOnline(String hostname) {
        TiledMapProvider.ONLINE.setLastError(null);
        String url;
        try {
            url = resolveUpdateURL(hostname);
        } catch (UnknownHostException | NamingException e1) {
            Terramap.instance().logger().error("Failed to resolve map styles urls!");
            Terramap.instance().logger().catching(e1);
            return;
        }

        Terramap.instance().http().get(url).whenComplete((b, e) -> {
            if(e != null) {
                Terramap.instance().logger().error("Failed to download updated map style file!");
                Terramap.instance().logger().catching(e);
                TiledMapProvider.ONLINE.setLastError(e);
            }
            try(BufferedReader txtReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(b)))) {
                StringBuilder json = new StringBuilder();
                String line = txtReader.readLine();
                while(line != null) {
                    json.append(line);
                    line = txtReader.readLine();
                }
                baseMaps.putAll(loadFromJson(json.toString(), TiledMapProvider.ONLINE));
            } catch(Exception f) {
                Terramap.instance().logger().error("Failed to parse updated map style file!");
                Terramap.instance().logger().catching(e);
                TiledMapProvider.ONLINE.setLastError(e);
            }
        });
    }

    /**
     * Load map styles defined in config/terramap_user_styles.json.
     * The file to load from needs to be set first with {@link #setConfigMapFile(File)}
     */
    public static void loadFromConfigFile() {
        TiledMapProvider.CUSTOM.setLastError(null);
        if(configMapsFile == null) {
            Terramap.instance().logger().error("Map config file was null!");
            TiledMapProvider.CUSTOM.setLastError(new NullPointerException("Map style config files was null"));
            return;
        }
        if(!configMapsFile.exists()) {
            try {
                Terramap.instance().logger().debug("Map config file did not exist, creating a blank one.");
                MapStyleFile mapFile = new MapStyleFile(new MapFileMetadata(0, "Add custom map styles here. See an example at styles.terramap.thesmyler.fr (open in your browser, do not add http or https prefix)"));
                Files.write(configMapsFile.toPath(), Terramap.instance().gsonPretty().toJson(mapFile).getBytes(Charset.defaultCharset()));
            } catch (IOException e) {
                Terramap.instance().logger().error("Failed to create map style config file!");
                Terramap.instance().logger().catching(e);
                TiledMapProvider.CUSTOM.setLastError(e);

            }
        } else {
            try {
                userMaps.putAll(loadFromFile(configMapsFile, TiledMapProvider.CUSTOM));
            } catch (Exception e) {
                Terramap.instance().logger().error("Failed to read map style config file!");
                Terramap.instance().logger().catching(e);
                TiledMapProvider.CUSTOM.setLastError(e);
            }
        }
    }

    /**
     * Set the config file that should contain the user's custom map syles
     * It will be created if it does not exist.
     * 
     * @param file - the json config file
     */
    public static void setConfigMapFile(File file) {
        configMapsFile = file;
    }

    /**
     * Reload map styles from the jar, online, and config/terramap_user_styles.json
     */
    public static void reload() {
        baseMaps.clear();
        userMaps.clear();
        loadBuiltIns();
        loadInternals();
        loadFromOnline(Terramap.STYLE_UPDATE_HOSTNAME);
        loadFromConfigFile();
    }

    private static UrlTiledMap readFromSaved(String id, SavedMapStyle saved, TiledMapProvider provider, long version, String comment) {
        String[] patterns = saved.urls;
        if(patterns == null || patterns.length == 0) {
            if(saved.url != null) {
                // This is a legacy source, it only has one url
                patterns = new String[] {saved.url};
            } else throw new IllegalArgumentException("Could not find any valid url for map style " + id + "-" + provider + "v" + version);
        }
        UrlTiledMap map = new UrlTiledMap(
                patterns,
                saved.min_zoom,
                saved.max_zoom,
                id,
                saved.display_priority,
                saved.allow_on_minimap,
                provider,
                version,
                comment,
                saved.max_concurrent_requests,
                saved.debug
        );
        saved.name.forEach(map::setNameTranslation);
        saved.copyright.forEach(map::setTranslatedCopyright);
        if (saved.bounds != null) {
            saved.bounds.forEach((i, b) -> {
                try {
                    int zoomLevel = parseInt(i);
                    map.setBounds(zoomLevel, b);
                } catch (NumberFormatException e) {
                    Terramap.instance().logger().warn("Ignoring invalid zoom level: {}: {}", i, e.getMessage());
                }
            });
        }
        return map;
    }

    private static Map<String, UrlTiledMap> loadFromFile(File file, TiledMapProvider provider) throws IOException {
        String json = String.join("", Files.readAllLines(file.toPath()));
        return loadFromJson(json, provider);
    }

    private static Map<String, UrlTiledMap> loadFromJson(String json, TiledMapProvider provider) {
        MapStyleFile savedStyles = Terramap.instance().gson().fromJson(json, MapStyleFile.class);
        Map<String, UrlTiledMap> styles = new HashMap<>();
        for(String id: savedStyles.maps.keySet()) {
            UrlTiledMap style = readFromSaved(id, savedStyles.maps.get(id), provider, savedStyles.metadata.version, savedStyles.metadata.comment);
            if(!TerramapConfig.enableDebugMaps && style.isDebug()) {
                Terramap.instance().logger().info("Not loading debug map style {}", style.getId());
                continue;
            }
            styles.put(id, style);
        }
        return styles;
    }

    private static String resolveUpdateURL(String hostname) throws UnknownHostException, NamingException {
        InetAddress inetAddress = InetAddress.getByName(hostname);
        InitialDirContext iDirC = new InitialDirContext();
        Attributes attributes = iDirC.getAttributes("dns:/" + inetAddress.getHostName(), new String[] {"TXT"});
        String attribute;
        try {
            attribute =  attributes.get("TXT").get().toString();
        } catch(NullPointerException e) {
            throw new UnknownHostException(String.format("No txt record was found at %s ?? Something is wrong, either with the name server or with your dns provider!", hostname));
        }
        try {
            return attribute.split("\\|")[1].replace("${version}", Terramap.instance().version());
        } catch(IndexOutOfBoundsException e) {
            throw new UnknownHostException("TXT record was malformatted");
        }
    }

    public static File getFile() {
        return configMapsFile;
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static class SavedMapStyle {

        String url; // Used by legacy versions
        String[] urls;
        Map<String, String> name;
        Map<String, Text> copyright;
        int min_zoom;
        int max_zoom;
        int display_priority;
        boolean allow_on_minimap;
        int max_concurrent_requests = 2;
        boolean debug;
        Map<String, WebMercatorBounds> bounds;

    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static class MapStyleFile {

        Map<String, SavedMapStyle> maps;
        MapFileMetadata metadata;

        MapStyleFile(MapFileMetadata metadata) {
            this.metadata = metadata;
            this.maps = new HashMap<>();
        }

    }

    private static class MapFileMetadata {

        long version;
        String comment;

        MapFileMetadata(long version, String comment) {
            this.comment = comment;
            this.version = version;
        }

    }
}
