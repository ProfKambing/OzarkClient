package me.trambled.ozark.ozarkclient.module.movement;

import me.trambled.ozark.ozarkclient.module.Category;
import me.trambled.ozark.ozarkclient.module.Module;
import me.trambled.ozark.ozarkclient.module.Setting;
import me.trambled.ozark.ozarkclient.util.world.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;

public class YPort extends Module {

    public YPort() {
        super(Category.MOVEMENT);

        this.name = "YPort";
        this.tag = "YPort";
        this.description = "up and down";
    }


    Setting yPortspeed = create("Speed", "YpSpeed", 0.06, 0.01, 0.15);
    Setting jumpHeight = create("Jump speed", "YpJump", 0.41, 0, 1);
    Setting timerVal = create("Timer", "Yptimer", 1.15, 1, 1.5);

    private boolean slowDown;
    private double playerSpeed;
    private TimerUtil timer = new TimerUtil();

    public void enable() {
        playerSpeed = this.getBaseMoveSpeed();
    }

    public void disable() {
        timer.reset();
        this.resetTimer();
    }

    public void update() {
        if (mc.player == null || mc.world == null) {
            disable();
        }else{
            handleYPortSpeed();
        }
    }

    private void handleYPortSpeed() {
        if (isMoving(mc.player) || mc.player.isInWater() && mc.player.isInLava() || mc.player.collidedHorizontally) {
            return;
        }

        if (mc.player.onGround) {
            this.setTimer(1.15f);
            mc.player.jump();
            this.setSpeed(mc.player, this.getBaseMoveSpeed() + yPortspeed.get_value(1));
        } else {
            mc.player.motionY = -1;
            this.resetTimer();
        }
    }

    public static double getBaseMoveSpeed() {
        double baseSpeed = 0.2873;
        if (mc.getMinecraft().player != null && mc.getMinecraft().player.isPotionActive(Potion.getPotionById(1))) {
            final int amplifier = mc.getMinecraft().player.getActivePotionEffect(Potion.getPotionById(1)).getAmplifier();
            baseSpeed *= 1.0 + 0.2 * (amplifier + 1);
        }
        return baseSpeed;
    }
    public static void resetTimer() {
        Minecraft.getMinecraft().timer.tickLength = 50;
    }
    public static boolean isMoving(EntityLivingBase entity) {
        return entity.moveForward != 0 || entity.moveStrafing != 0;
    }
    public static void setTimer(float speed) {
        Minecraft.getMinecraft().timer.tickLength = 50.0f / speed;
    }
    public static void setSpeed(final EntityLivingBase entity, final double speed) {
        double[] dir = forward(speed);
        entity.motionX = dir[0];
        entity.motionZ = dir[1];
    }
    public static double[] forward(final double speed) {
        float forward = Minecraft.getMinecraft().player.movementInput.moveForward;
        float side = Minecraft.getMinecraft().player.movementInput.moveStrafe;
        float yaw = Minecraft.getMinecraft().player.prevRotationYaw + (Minecraft.getMinecraft().player.rotationYaw - Minecraft.getMinecraft().player.prevRotationYaw) * Minecraft.getMinecraft().getRenderPartialTicks();
        if (forward != 0.0f) {
            if (side > 0.0f) {
                yaw += ((forward > 0.0f) ? -45 : 45);
            } else if (side < 0.0f) {
                yaw += ((forward > 0.0f) ? 45 : -45);
            }
            side = 0.0f;
            if (forward > 0.0f) {
                forward = 1.0f;
            } else if (forward < 0.0f) {
                forward = -1.0f;
            }
        }
    final double sin = Math.sin(Math.toRadians(yaw + 90.0f));
    final double cos = Math.cos(Math.toRadians(yaw + 90.0f));
    final double posX = forward * speed * cos + side * speed * sin;
    final double posZ = forward * speed * sin - side * speed * cos;
        return new double[]{posX, posZ};
}
}
