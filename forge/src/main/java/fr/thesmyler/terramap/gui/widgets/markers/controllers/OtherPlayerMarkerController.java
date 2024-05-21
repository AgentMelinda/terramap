package fr.thesmyler.terramap.gui.widgets.markers.controllers;

import java.util.Map;
import java.util.UUID;

import net.smyler.smylib.gui.widgets.buttons.ToggleButtonWidget;
import fr.thesmyler.terramap.MapContext;
import fr.thesmyler.terramap.TerramapClientContext;
import fr.thesmyler.terramap.gui.widgets.map.MapWidget;
import fr.thesmyler.terramap.gui.widgets.markers.markers.Marker;
import fr.thesmyler.terramap.gui.widgets.markers.markers.entities.OtherPlayerMarker;
import fr.thesmyler.terramap.network.playersync.TerramapPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

import static net.smyler.smylib.SmyLib.getGameClient;
import static net.smyler.smylib.gui.sprites.SmyLibSprites.*;
import static net.smyler.smylib.gui.sprites.SmyLibSprites.BUTTON_VISIBILITY_OFF_15_HIGHLIGHTED;

public class OtherPlayerMarkerController extends AbstractPlayerMarkerController<OtherPlayerMarker> {

    public static final String ID = "other_players";

    public OtherPlayerMarkerController() {
        super(ID, 800, OtherPlayerMarker.class, new ToggleButtonWidget(10, 14, 14,
                BUTTON_VISIBILITY_ON_15.sprite, BUTTON_VISIBILITY_OFF_15.sprite,
                BUTTON_VISIBILITY_ON_15_DISABLED.sprite, BUTTON_VISIBILITY_OFF_15_DISABLED.sprite,
                BUTTON_VISIBILITY_ON_15_HIGHLIGHTED.sprite, BUTTON_VISIBILITY_OFF_15_HIGHLIGHTED.sprite,
                false, null));
        this.button.setTooltip(getGameClient().translator().format("terramap.terramapscreen.markercontrollers.buttons.otherplayer"));
    }

    @Override
    public OtherPlayerMarker[] getNewMarkers(Marker[] existingMarkers, MapWidget map) {

        boolean minimap = map.getContext() == MapContext.MINIMAP;

        int factor = minimap? 2: 1;

        Map<UUID, TerramapPlayer> players = minimap ? TerramapClientContext.getContext().getLocalPlayersMap(): TerramapClientContext.getContext().getPlayerMap();
        for(Marker marker: existingMarkers) {
            TerramapPlayer player = ((OtherPlayerMarker) marker).getPlayer();
            players.remove(player.getUUID());
        }

        // The main player has its own controller
        EntityPlayerSP self = Minecraft.getMinecraft().player;
        if(self != null) players.remove(self.getUniqueID());

        OtherPlayerMarker[] newMarkers = new OtherPlayerMarker[players.size()];
        int i = 0;
        for(TerramapPlayer player: players.values()) {
            newMarkers[i++] = new OtherPlayerMarker(this, player, factor);
        }

        return newMarkers;
    }

    @Override
    public boolean showButton() {
        return TerramapClientContext.getContext().allowsPlayerRadar();
    }


    @Override
    public boolean isVisible() {
        return super.isVisible() && TerramapClientContext.getContext().allowsPlayerRadar();
    }

    @Override
    public String getSaveName() {
        return ID;
    }

}
