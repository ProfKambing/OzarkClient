package me.trambled.ozark.ozarkclient.module.gui;

import com.mojang.realmsclient.gui.ChatFormatting;
import me.trambled.ozark.Ozark;
import me.trambled.ozark.ozarkclient.event.EventHandler;
import me.trambled.ozark.ozarkclient.event.events.EventRender;
import me.trambled.ozark.ozarkclient.module.Category;
import me.trambled.ozark.ozarkclient.module.Module;
import me.trambled.ozark.ozarkclient.module.Setting;
import me.trambled.ozark.ozarkclient.util.font.FontUtil;
import me.trambled.ozark.ozarkclient.util.player.PlayerUtil;
import me.trambled.ozark.ozarkclient.util.render.RainbowUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static me.trambled.ozark.ozarkclient.module.Module.mc;
import static me.trambled.ozark.ozarkclient.util.font.FontUtil.drawString;

public class InfoList extends Module {
    int i = 0;
    int[] counter1 = {1};
    public InfoList() {
        super(Category.GUI);
        this.name = "InfoList";
        this.tag = "InfoList";
        this.description = "phobos infolist";

    }
    Setting color = create("ColorMode", "ColorMode", "Rainbow", combobox("Rainbow", "Alpha Step", "Static"));
    Setting ping = create("Ping", "Ping", true);
    Setting speed = create("Speed", "Speed", true);
    Setting tps = create("TPS", "TPS", true);
    Setting fps = create("FPS", "FPS", true);
    Setting grayNess = create("gray", "gray", false);
    Setting potions = create("potions", "potions", false);


    @Override
    public void render(EventRender event) {

        int width = mc.displayWidth;
        String grayString = this.grayNess.get_value(true) ? String.valueOf(ChatFormatting.GRAY) : "";
        if (this.potions.get_value(true)) {
            List<PotionEffect> effects = new ArrayList<>((Minecraft.getMinecraft()).player.getActivePotionEffects());
            for (PotionEffect potionEffect : effects) {
                String str = getColoredPotionString(potionEffect);
                drawString(str, (width - FontUtil.getFontWidth(str) - 2), (2 + i++ * 10), potionEffect.getPotion().getLiquidColor());
            }
        }
        if (this.speed.get_value(true)) {
            String str = grayString + "Speed " + ChatFormatting.WHITE + PlayerUtil.speed() + " km/h";
            drawString(str, (width - FontUtil.getFontWidth(str) - 2), (2 + i++ * 10), generateColor(color));
            counter1[0] = counter1[0] + 1;
        }
        if (this.tps.get_value(true)) {
            String str = grayString + "TPS " + ChatFormatting.WHITE + getTPS();
            drawString(str, (width - FontUtil.getFontWidth(str) - 2), (2 + i++ * 10), generateColor(color));
            counter1[0] = counter1[0] + 1;
        }
        String fpsText = grayString + "FPS " + ChatFormatting.WHITE + Minecraft.getDebugFPS();
        String str1 = grayString + "Ping " + ChatFormatting.WHITE + getPing();
        if (FontUtil.getFontWidth(str1) > FontUtil.getFontWidth(fpsText)) {
            if (this.ping.get_value(true)) {
                drawString(str1, (width - FontUtil.getFontWidth(str1) - 2), (2 + i++ * 10), generateColor(color));
                counter1[0] = counter1[0] + 1;
            }
            if (this.fps.get_value(true)) {
                drawString(fpsText, (width - FontUtil.getFontWidth(fpsText) - 2), (2 + i++ * 10), generateColor(color));
                counter1[0] = counter1[0] + 1;
            }
        } else {
            if (this.fps.get_value(true)) {
                drawString(fpsText, (width - FontUtil.getFontWidth(fpsText) - 2), (2 + i++ * 10), generateColor(color));
                counter1[0] = counter1[0] + 1;
            }
            if (this.ping.get_value(true)) {
                drawString(str1, (width - FontUtil.getFontWidth(str1) - 2), (2 + i++ * 10), generateColor(color));
                counter1[0] = counter1[0] + 1;
            }
        }
    }


    private final Map<EntityPlayer, PotionList> potions2 = new ConcurrentHashMap<EntityPlayer, PotionList>();

    public List<PotionEffect> getOwnPotions() {
        return this.getPlayerPotions(mc.player);
    }

    public List<PotionEffect> getPlayerPotions(EntityPlayer player) {
        PotionList list = this.potions2.get(player);
        List<PotionEffect> potions = new ArrayList<PotionEffect>();
        if (list != null) {
            potions = list.getEffects();
        }
        return potions;
    }

    public PotionEffect[] getImportantPotions(EntityPlayer player) {
        PotionEffect[] array = new PotionEffect[3];
        for (PotionEffect effect : this.getPlayerPotions(player)) {
            Potion potion = effect.getPotion();
            switch (I18n.format(potion.getName()).toLowerCase()) {
                case "strength": {
                    array[0] = effect;
                }
                case "weakness": {
                    array[1] = effect;
                }
                case "speed": {
                    array[2] = effect;
                }
            }
        }
        return array;
    }

    public String getPotionString(PotionEffect effect) {
        Potion potion = effect.getPotion();
        return I18n.format(potion.getName()) + " " + (effect.getAmplifier() + 1) + " " + ChatFormatting.WHITE + Potion.getPotionDurationString(effect, 1.0f);
    }

    public String getColoredPotionString(PotionEffect effect) {
        return this.getPotionString(effect);
    }

    public static class PotionList {
        private final List<PotionEffect> effects = new ArrayList<PotionEffect>();

        public void addEffect(PotionEffect effect) {
            if (effect != null) {
                this.effects.add(effect);
            }
        }

        public List<PotionEffect> getEffects() {
            return this.effects;
        }
    }

    public int getTPS() {
        try {
            return Math.round(EventHandler.INSTANCE.get_tick_rate());
        } catch (Exception e) {
            return 0;
        }
    }

    public int getPing() {
        try {
            return Objects.requireNonNull(mc.getConnection()).getPlayerInfo(mc.player.getUniqueID()).getResponseTime();
        } catch (Exception e) {
            return 0;
        }
    }
    public int generateColor(Setting mod) {
        Setting red = Ozark.get_setting_manager().get_setting_with_tag("Arraylist", "Red");
        Setting green = Ozark.get_setting_manager().get_setting_with_tag("Arraylist", "Green");
        Setting blue = Ozark.get_setting_manager().get_setting_with_tag("Arraylist", "Blue");
        switch (mod.get_current_value()) {
            case "Alpha Step":
                return RainbowUtil.alphaStep(new Color(red.get_value(1),blue.get_value(1),green.get_value(1)), 50, (i * 2) + 15).getRGB();
            case "Rainbow":
                return RainbowUtil.rainbow(i * 100);
            case "Static":
                new Color(red.get_value(1),green.get_value(1),blue.get_value(1)).getRGB();
        }
        return -1;
    }
}
