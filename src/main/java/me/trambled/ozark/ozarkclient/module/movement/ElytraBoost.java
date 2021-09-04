package me.trambled.ozark.ozarkclient.module.movement;

import me.trambled.ozark.ozarkclient.module.Category;
import me.trambled.ozark.ozarkclient.module.Module;


public class ElytraBoost extends Module {
    public ElytraBoost() {
        super(Category.MOVEMENT);

        this.name = "ElytraBoost";
        this.tag = "ElytraBoost";
        this.description = "Fly infinite";
    }


    @Override
    public void update() {
        if (mc.player.isElytraFlying()) {
            mc.player.rotationPitch = 40;
        }
    }

    @Override
    public void disable() {
        if (mc.player.isElytraFlying()) {
            mc.player.rotationPitch = -40;
        }
    }
}