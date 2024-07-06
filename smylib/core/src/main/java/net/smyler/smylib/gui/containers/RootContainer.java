package net.smyler.smylib.gui.containers;

import net.smyler.smylib.gui.widgets.Widget;
import net.smyler.smylib.gui.UiDrawContext;
import org.jetbrains.annotations.Nullable;


/**
 * A container with no parent.
 * The key difference is that this container is in charge of drawing tooltips.
 *
 * @author Smyler
 */
public abstract class RootContainer extends WidgetContainer {

    private long startHoverTime;
    private Widget lastHoveredWidget;
    private float lastRenderMouseX, lastRenderMouseY;

    public RootContainer() {
        super(0);
    }

    @Override
    public void draw(UiDrawContext context, float x, float y, float mouseX, float mouseY, boolean screenHovered, boolean screenFocused, @Nullable WidgetContainer alwaysNull) {
        super.draw(context, x, y, mouseX, mouseY, screenHovered, screenFocused, null);
        Widget hoveredWidget = this.getHoveredWidget();
        boolean mouseMoved = mouseX != this.lastRenderMouseX && mouseY != this.lastRenderMouseY;
        if(mouseMoved || (hoveredWidget != null && !hoveredWidget.equals(this.lastHoveredWidget))) {
            this.startHoverTime = System.currentTimeMillis();
        }
        if(
                hoveredWidget != null
                        && hoveredWidget.getTooltipText() != null
                        && !hoveredWidget.getTooltipText().isEmpty()
                        && this.startHoverTime + hoveredWidget.getTooltipDelay() <= System.currentTimeMillis()
        ) {
            context.drawTooltip(hoveredWidget.getTooltipText(), mouseX, mouseY);
        }
        this.lastHoveredWidget = hoveredWidget;
        this.lastRenderMouseX = mouseX;
        this.lastRenderMouseY = mouseY;
    }

}
