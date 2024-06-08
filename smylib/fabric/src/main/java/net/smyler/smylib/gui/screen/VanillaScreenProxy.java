package net.smyler.smylib.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.smyler.smylib.game.GameClient;
import net.smyler.smylib.gui.WrappedGuiGraphics;

import static com.google.common.base.Preconditions.checkState;
import static net.smyler.smylib.SmyLib.getGameClient;

public class VanillaScreenProxy extends net.minecraft.client.gui.screens.Screen {
    private final Screen screen;

    public VanillaScreenProxy(Screen screen) {
        super(Component.literal("SmyLib screen"));
        this.screen = screen;
    }


    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, float partialTicks) {

        GameClient game = getGameClient();
        WrappedGuiGraphics uiDrawContext = (WrappedGuiGraphics) game.guiDrawContext();
        checkState(
                uiDrawContext.vanillaGraphics == guiGraphics,
                "Rendering a screen, but the vanilla graphic context is different"
        );
        float mouseX = game.mouse().x();
        float mouseY = game.mouse().y();

        this.screen.onUpdate(mouseX, mouseY, null);
        this.drawBackground(guiGraphics);
        super.render(guiGraphics, x, y, partialTicks);
        //this.screen.draw(uiDrawContext, 0, 0, mouseX, mouseY, true, true, null);
    }

    @Override
    protected void init() {
        super.init();
        this.screen.width = this.width;
        this.screen.height = this.height;
        this.screen.init();
    }

    @Override
    public void onClose() {
        this.screen.onClosed();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return this.screen.shouldPauseGame();
    }

    //FIXME mouse and keyboard support in screen proxy

    @Override
    public boolean mouseClicked(double d, double e, int i) {
        return super.mouseClicked(d, e, i);
    }

    @Override
    public boolean mouseReleased(double d, double e, int i) {
        return super.mouseReleased(d, e, i);
    }

    @Override
    public boolean mouseDragged(double d, double e, int i, double f, double g) {
        return super.mouseDragged(d, e, i, f, g);
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f) {
        return super.mouseScrolled(d, e, f);
    }

    @Override
    public boolean keyReleased(int i, int j, int k) {
        return super.keyReleased(i, j, k);
    }

    @Override
    public boolean charTyped(char c, int i) {
        return super.charTyped(c, i);
    }

    @Override
    public void mouseMoved(double d, double e) {
        super.mouseMoved(d, e);
    }

    public net.smyler.smylib.gui.screen.Screen getScreen() {
        return screen;
    }

    private void drawBackground(GuiGraphics guiGraphics) {
        switch(this.screen.background) {
            case NONE:
                break;
            case DEFAULT:
                this.renderBackground(guiGraphics);
                break;
            case DIRT:
                this.renderDirtBackground(guiGraphics);
                break;
            case OVERLAY:
                guiGraphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
                break;
        }
    }

}
