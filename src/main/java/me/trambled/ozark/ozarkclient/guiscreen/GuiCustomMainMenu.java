package me.trambled.ozark.ozarkclient.guiscreen;

import me.trambled.ozark.ozarkclient.util.render.RainbowUtil;
import me.trambled.ozark.ozarkclient.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;

// phobos
public class GuiCustomMainMenu extends GuiScreen {
    private final ResourceLocation resourceLocation = new ResourceLocation("custom/background.png");
    private int y;
    private int x;
    private float xOffset;
    private float yOffset;

    public static void drawCompleteImage(float posX, float posY, float width, float height) {
        GL11.glPushMatrix();
        GL11.glTranslatef(posX, posY, 0.0f);
        GL11.glBegin(7);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex3f(0.0f, 0.0f, 0.0f);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex3f(0.0f, height, 0.0f);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex3f(width, height, 0.0f);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex3f(width, 0.0f, 0.0f);
        GL11.glEnd();
        GL11.glPopMatrix();
    }

    public static boolean isHovered(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY < y + height;
    }

    public void initGui() {
        this.x = this.width / 2;
        this.y = this.height / 4 + 48;
        this.buttonList.add(new TextButton(0, this.x, this.y + 20, "Singleplayer"));
        this.buttonList.add(new TextButton(1, this.x, this.y + 44, "Multiplayer"));
        this.buttonList.add(new TextButton(2, this.x, this.y + 66, "Settings"));
        this.buttonList.add(new TextButton(2, this.x, this.y + 88, "Exit"));
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.shadeModel(7425);
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public void updateScreen() {
        super.updateScreen();
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (GuiCustomMainMenu.isHovered(this.x - RainbowUtil.get_string_width("Singleplayer") / 2, this.y + 20, RainbowUtil.get_string_width("Singleplayer"), RainbowUtil.get_string_height(), mouseX, mouseY)) {
            this.mc.displayGuiScreen(new GuiWorldSelection(this));
        } else if (GuiCustomMainMenu.isHovered(this.x - RainbowUtil.get_string_width("Multiplayer") / 2, this.y + 44, RainbowUtil.get_string_width("Multiplayer"), RainbowUtil.get_string_height(), mouseX, mouseY)) {
            this.mc.displayGuiScreen(new GuiMultiplayer(this));
        } else if (GuiCustomMainMenu.isHovered(this.x - RainbowUtil.get_string_width("Settings") / 2, this.y + 66, RainbowUtil.get_string_width("Settings"), RainbowUtil.get_string_height(), mouseX, mouseY)) {
            this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
        } else if (GuiCustomMainMenu.isHovered(this.x - RainbowUtil.get_string_width("Exit") / 2, this.y + 88, RainbowUtil.get_string_width("Exit"), RainbowUtil.get_string_height(), mouseX, mouseY)) {
            this.mc.shutdown();
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.xOffset = -1.0f * (((float) mouseX - (float) this.width / 2.0f) / ((float) this.width / 32.0f));
        this.yOffset = -1.0f * (((float) mouseY - (float) this.height / 2.0f) / ((float) this.height / 18.0f));
        this.x = this.width / 2;
        this.y = this.height / 4 + 48;
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        this.mc.getTextureManager().bindTexture(this.resourceLocation);
        GuiCustomMainMenu.drawCompleteImage(-16.0f + this.xOffset, -9.0f + this.yOffset, this.width + 32, this.height + 18);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public BufferedImage parseBackground(BufferedImage background) {
        int height;
        int width = 1920;
        int srcWidth = background.getWidth();
        int srcHeight = background.getHeight();
        for (height = 1080; width < srcWidth || height < srcHeight; width *= 2, height *= 2) {
        }
        BufferedImage imgNew = new BufferedImage(width, height, 2);
        Graphics g = imgNew.getGraphics();
        g.drawImage(background, 0, 0, null);
        g.dispose();
        return imgNew;
    }

    private static class TextButton
            extends GuiButton {
        public TextButton(int buttonId, int x, int y, String buttonText) {
            super(buttonId, x, y, RainbowUtil.get_string_width(buttonText), RainbowUtil.get_string_height(), buttonText);
        }

        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (this.visible) {
                this.enabled = true;
                this.hovered = (float) mouseX >= (float) this.x - (float) RainbowUtil.get_string_width(this.displayString) / 2.0f && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
                RainbowUtil.drawString(this.displayString, (float) this.x - (float) RainbowUtil.get_string_width(this.displayString) / 2.0f, this.y, Color.WHITE.getRGB());
                if (this.hovered) {
                    RenderUtil.drawLine((float) (this.x - 1) - (float) RainbowUtil.get_string_width(this.displayString) / 2.0f, this.y + 2 + RainbowUtil.get_string_height(), (float) this.x + (float) RainbowUtil.get_string_width(this.displayString) / 2.0f + 1.0f, this.y + 2 + RainbowUtil.get_string_height(), 1.0f, Color.WHITE.getRGB());
                }
            }
        }

        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
            return this.enabled && this.visible && (float) mouseX >= (float) this.x - (float) RainbowUtil.get_string_width(this.displayString) / 2.0f && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        }
    }
    public static final ScaledResolution getScaledRes() {
        return new ScaledResolution(Minecraft.getMinecraft());
    }}