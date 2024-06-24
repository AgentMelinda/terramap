package fr.thesmyler.terramap.maps.raster.imp;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;

import fr.thesmyler.terramap.maps.raster.RasterTile;
import net.smyler.smylib.Identifier;
import net.smyler.terramap.Terramap;
import net.smyler.terramap.util.geo.TilePosImmutable;


import static net.smyler.smylib.SmyLib.getGameClient;

/**
 * @author SmylerMC
 *
 */
public class UrlRasterTile implements RasterTile {

    private final TilePosImmutable pos;
    private final String url;
    private Identifier texture = null;
    private CompletableFuture<byte[]> textureTask;


    public UrlRasterTile(String urlPattern, TilePosImmutable pos) {
        this.pos = pos;
        this.url = urlPattern
                .replace("{x}", String.valueOf(this.getPosition().getX()))
                .replace("{y}", String.valueOf(this.getPosition().getY()))
                .replace("{z}", String.valueOf(this.getPosition().getZoom()));
    }

    public UrlRasterTile(String urlPattern, int zoom, int x, int y) {
        this(urlPattern, new TilePosImmutable(zoom, x , y));
    }

    public String getURL() {
        return this.url;
    }

    @Override
    public boolean isTextureAvailable() {
        if(texture != null) return true; // Don't try loading the texture if it has already been loaded
        try {
            this.tryLoadingTexture();
        } catch (Throwable e) {
            return false;
        }
        return this.texture != null;
    }

    @Override
    public Identifier getTexture() throws Throwable {
        if(this.texture == null) {
            if(this.textureTask == null) {
                this.textureTask = Terramap.instance().http().get(this.getURL());
            } else this.tryLoadingTexture();
        }
        return this.texture;
    }

    private void tryLoadingTexture() throws Throwable {
        //TODO Do that fully async, DynamicTexture::new is expensive
        if(this.textureTask != null && this.textureTask.isDone()){
            if(this.textureTask.isCompletedExceptionally()) {
                if(this.textureTask.isCancelled()) {
                    this.textureTask = null;
                } else {
                    try {
                        this.textureTask.get(); // That will throw an exception
                    } catch(ExecutionException e) {
                        throw e.getCause();
                    }
                }
                return;
            }
            byte[] buf = this.textureTask.get();
            if(buf == null) throw new IOException("404 response");
            try (ByteArrayInputStream is = new ByteArrayInputStream(buf)) {
                BufferedImage image = ImageIO.read(is);
                if(image == null) throw new IOException("Failed to read image! url: " + this.getURL());
                this.texture = getGameClient().guiDrawContext().loadDynamicTexture(image);
            }
        }
    }

    @Override
    public void cancelTextureLoading() {
        if(this.textureTask != null) {
            this.textureTask.cancel(true);
            this.textureTask = null;
        }
    }

    @Override
    public void unloadTexture() {
        this.cancelTextureLoading();
        if(this.texture != null) {
            getGameClient().guiDrawContext().unloadDynamicTexture(this.texture);
            this.texture = null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) return true;
        if(obj == null) return false;
        if(!(obj instanceof UrlRasterTile)) return false;
        UrlRasterTile other = (UrlRasterTile) obj;
        return other.url.equals(this.url);
    }

    @Override
    public TilePosImmutable getPosition() {
        return this.pos;
    }

    @Override
    public int hashCode() {
        return this.url.hashCode();
    }

}
