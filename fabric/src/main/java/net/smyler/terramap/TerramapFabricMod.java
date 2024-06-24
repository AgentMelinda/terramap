package net.smyler.terramap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.Minecraft;
import net.smyler.smylib.SmyLib;
import net.smyler.smylib.game.WrappedMinecraft;
import net.smyler.smylib.json.TextJsonAdapter;
import net.smyler.smylib.text.Text;
import net.smyler.terramap.http.HttpClient;
import net.smyler.terramap.http.TerramapHttpClient;
import org.apache.logging.log4j.Logger;

import static org.apache.logging.log4j.LogManager.getLogger;

public class TerramapFabricMod implements ModInitializer, Terramap {

    private final Logger logger = getLogger("terramap");
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Text.class, new TextJsonAdapter())
            .create();
    private final Gson gsonPretty = new GsonBuilder()
            .registerTypeAdapter(Text.class, new TextJsonAdapter())
            .setPrettyPrinting()
            .create();
    private final HttpClient httpClient = new TerramapHttpClient(this.logger);

    @Override
    public void onInitialize() {
        this.logger.info("Initializing Terramap");
        Terramap.InstanceHolder.setInstance(this);
        SmyLib.initializeGameClient(new WrappedMinecraft(Minecraft.getInstance()), this.logger);
    }

    @Override
    public Logger logger() {
        return this.logger;
    }

    @Override
    public HttpClient http() {
        return this.httpClient;
    }

    @Override
    public Gson gson() {
        return this.gson;
    }

    @Override
    public Gson gsonPretty() {
        return this.gsonPretty;
    }

}
