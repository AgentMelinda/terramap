package fr.thesmyler.terramap.gui.widgets.markers.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.smyler.smylib.gui.widgets.buttons.ToggleButtonWidget;
import fr.thesmyler.terramap.TerramapClientContext;
import fr.thesmyler.terramap.gui.widgets.map.MapWidget;
import fr.thesmyler.terramap.gui.widgets.markers.markers.Marker;
import fr.thesmyler.terramap.gui.widgets.markers.markers.entities.MobMarker;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.IMob;

import static net.smyler.smylib.gui.sprites.SmyLibSprites.*;
import static net.smyler.smylib.gui.sprites.SmyLibSprites.BUTTON_VISIBILITY_OFF_15_HIGHLIGHTED;

public class MobMarkerController extends MarkerController<MobMarker> {

    public static final String ID = "mobs";

    protected final ToggleButtonWidget button = new ToggleButtonWidget(10, 14, 14,
            BUTTON_VISIBILITY_ON_15.sprite, BUTTON_VISIBILITY_OFF_15.sprite,
            BUTTON_VISIBILITY_ON_15_DISABLED.sprite, BUTTON_VISIBILITY_OFF_15_DISABLED.sprite,
            BUTTON_VISIBILITY_ON_15_HIGHLIGHTED.sprite, BUTTON_VISIBILITY_OFF_15_HIGHLIGHTED.sprite,
            this.isVisible(), null);

    public MobMarkerController() {
        super(ID, 700, MobMarker.class);
        this.button.setOnChange(this::setVisibility);
        this.button.setTooltip(I18n.format("terramap.terramapscreen.markercontrollers.buttons.mobs"));
    }

    @Override
    public MobMarker[] getNewMarkers(Marker[] existingMarkers, MapWidget map) {
        if(TerramapClientContext.getContext().getProjection() == null) return new MobMarker[0];
        Map<UUID, Entity> entities = new HashMap<>();
        for(Entity entity: TerramapClientContext.getContext().getEntities()) {
            if(entity instanceof IMob) {
                entities.put(entity.getPersistentID(), entity);
            }
        }
        for(Marker rawMarker: existingMarkers) {
            MobMarker marker = (MobMarker) rawMarker;
            entities.remove(marker.getEntity().getUniqueID());
        }
        MobMarker[] newMarkers = new MobMarker[entities.size()];
        int i = 0;
        for(Entity entity: entities.values()) {
            newMarkers[i++] = new MobMarker(this, entity);
        }
        return newMarkers;
    }

    @Override
    public boolean showButton() {
        return TerramapClientContext.getContext().allowsMobRadar() && TerramapClientContext.getContext().getProjection() != null;
    }

    @Override
    public ToggleButtonWidget getButton() {
        return this.button;
    }

    @Override
    public boolean isVisible() {
        return super.isVisible() && TerramapClientContext.getContext().allowsMobRadar();
    }

    @Override
    public String getSaveName() {
        return ID;
    }

}
