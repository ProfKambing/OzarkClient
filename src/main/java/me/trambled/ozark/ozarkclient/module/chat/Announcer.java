package me.trambled.ozark.ozarkclient.module.chat;

import me.trambled.ozark.Ozark;
import me.trambled.ozark.ozarkclient.event.events.EventPacket;
import me.trambled.ozark.ozarkclient.module.Category;
import me.trambled.ozark.ozarkclient.module.Module;
import me.trambled.ozark.ozarkclient.module.Setting;
import me.zero.alpine.fork.listener.EventHandler;
import me.zero.alpine.fork.listener.Listener;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketUpdateSign;
import net.minecraft.network.play.server.SPacketUseBed;
import net.minecraft.util.math.Vec3d;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Announcer extends Module {

    public Announcer() {
        super(Category.CHAT);

        this.name = "Announcer";
        this.tag = "Announcer";
        this.description = "How to get ignored 101.";
    }

    Setting mining = create("Mining", "AnnouncerMining", true);
    Setting placing = create("Placing", "AnnouncerPlacing", true);
    Setting drops = create("Drops", "AnnouncerDrops", true);
    Setting eating = create("Eating", "AnnouncerEating", true);
    Setting beds = create("Beds", "AnnouncerBeds", true);
    Setting signs = create("Signs", "AnnouncerSigns", true);
    Setting movement = create("Movement", "AnnouncerMovement", true);
    Setting health = create("Health", "AnnouncerHealth", true);
    Setting world_time = create("World Time", "AnnouncerWorldTime", true);
    Setting min_distance = create("Min Distance", "AnnouncerMinDist", 12, 1, 100);
    Setting max_distance = create("Max Distance", "AnnouncerMaxDist", 144, 12, 1200);
    Setting delay = create("Delay Seconds", "AnnouncerDelay", 4, 0, 20);
    Setting queue_size = create("Queue Size", "AnnouncerQueueSize", 5, 1, 20);
    Setting units = create("Units", "AnnouncerUnits", "Meters", combobox("Meters", "Feet", "Yards", "Inches"));
    Setting movement_string = create("Movement", "AnnouncerMovement", "Aha x", combobox("Aha x", "Leyta", "FUCK"));
    Setting suffix = create("Suffix", "AnnouncerSuffix", true);
    Setting smol = create("Small Text", "AnnouncerSmallText", false);


    private static DecimalFormat df = new DecimalFormat();

    private static final Queue<String> message_q = new ConcurrentLinkedQueue<>();

    private static final Map<String, Integer> mined_blocks = new ConcurrentHashMap<>();
    private static final Map<String, Integer> placed_blocks = new ConcurrentHashMap<>();
    private static final Map<String, Integer> dropped_items = new ConcurrentHashMap<>();
    private static final Map<String, Integer> consumed_items = new ConcurrentHashMap<>();

    private boolean first_run;

    private static Vec3d thisTickPos;
    private static Vec3d lastTickPos;

    private static int delay_count;

    private static double distanceTraveled;

    private static float thisTickHealth;
    private static float lastTickHealth;
    private static float gainedHealth;
    private static float lostHealth;

    @EventHandler
    private final Listener<EventPacket.ReceivePacket> receive_listener = new Listener<>(event -> {
        if (mc.world == null) return;

        if (event.get_packet() instanceof SPacketUseBed && beds.get_value(true)) {
            queue_message("I am going to bed now, goodnight");
        } 
    });

    @EventHandler
    private final Listener<EventPacket.SendPacket> send_listener = new Listener<>(event -> {
        if (mc.world == null) return;
        if (event.get_packet() instanceof CPacketPlayerDigging) {
            CPacketPlayerDigging packet = (CPacketPlayerDigging) event.get_packet();

            if (mc.player.getHeldItemMainhand().getItem() != Items.AIR && (packet.getAction().equals(CPacketPlayerDigging.Action.DROP_ITEM) || packet.getAction().equals(CPacketPlayerDigging.Action.DROP_ALL_ITEMS))) {
                final String name = mc.player.inventory.getCurrentItem().getDisplayName();
                if (dropped_items.containsKey(name)) {
                    dropped_items.put(name, dropped_items.get(name) + 1);
                } else {
                    dropped_items.put(name, 1);
                }
            }

            if (packet.getAction().equals(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK)) {
                String name = mc.world.getBlockState(packet.getPosition()).getBlock().getLocalizedName();
                if (mined_blocks.containsKey(name)) {
                    mined_blocks.put(name, mined_blocks.get(name) + 1);
                } else {
                    mined_blocks.put(name, 1);
                }
            }


        } else {

            if (event.get_packet() instanceof CPacketUpdateSign && signs.get_value(true)) {
                queue_message("I just updated a sign with some epic text");
            }

            if (event.get_packet() instanceof CPacketPlayerTryUseItemOnBlock) {
                ItemStack stack = mc.player.inventory.getCurrentItem();
                if (stack.isEmpty()) return;
                if (stack.getItem() instanceof ItemBlock) {
                    String name = mc.player.inventory.getCurrentItem().getDisplayName();
                    if (placed_blocks.containsKey(name)) {
                        placed_blocks.put(name, placed_blocks.get(name) + 1);
                    } else {
                        placed_blocks.put(name, 1);
                    }
                    return;
                }
                if (stack.getItem() == Items.END_CRYSTAL) {
                    String name = "Crystals";
                    if (placed_blocks.containsKey(name)) {
                        placed_blocks.put(name, placed_blocks.get(name) + 1);
                    } else {
                        placed_blocks.put(name, 1);
                    }
                }
            }

        }
    });

    @Override
    public void update() {
        if (mc.player == null || mc.world == null) {
            this.set_disable();
            return;
        }
        try {
            get_tick_data();
        } catch (Exception ignored) {
            this.set_disable();
            return;
        }
            
        send_cycle();
    }

    private void get_tick_data() {

        lastTickPos = thisTickPos;
        thisTickPos = mc.player.getPositionVector();
        distanceTraveled += thisTickPos.distanceTo(lastTickPos);

        lastTickHealth = thisTickHealth;
        thisTickHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        float healthDiff = thisTickHealth - lastTickHealth;
        if (healthDiff < 0.0f) {
            lostHealth += healthDiff * -1.0f;
        } else {
            gainedHealth += healthDiff;
        }
    }

    public void send_cycle() {
        delay_count++;
        if (delay_count > delay.get_value(1) * 20) {
            delay_count = 0;
            composeGameTickData();
            composeEventData();
            for (String message : message_q) {
                this.send_message(message);
                message_q.remove(message);
                break;
            }
        }

    }

    private void send_message(String s) {
        if (suffix.get_value(true)) {
            String i = " \u2763 ";
            s += i + Ozark.smoth("powered by ozark");
        }
        if (smol.get_value(true)) {
            s = Ozark.smoth(s.toLowerCase());
        }
        mc.player.connection.sendPacket(new CPacketChatMessage(s.replaceAll("\u00a7", "")));
    }

    public void queue_message(String m) {
        if (message_q.size() > queue_size.get_value(1)) return;
        message_q.add(m);
    }

    @Override
    protected void enable() {
        first_run = true;
        df = new DecimalFormat("#.#");
        df.setRoundingMode(RoundingMode.CEILING);
        float health;
        Vec3d pos;
        lastTickPos = pos = mc.player.getPositionVector();
        thisTickPos = pos;
        distanceTraveled = 0.0;
        lastTickHealth = health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        thisTickHealth = health;
        lostHealth = 0.0f;
        gainedHealth = 0.0f;
        delay_count = 0;
    }

    public static double round(double unrounded, int precision) {
        BigDecimal bd = new BigDecimal(unrounded);
        BigDecimal rounded = bd.setScale(precision, BigDecimal.ROUND_HALF_UP);
        return rounded.doubleValue();
    }

    private void composeGameTickData() {
        CharSequence sb;
        if (first_run) {
            first_run = false;
            return;
        }
        // i got da messages fom sumit
        if (world_time.get_value(true)) {
            if (mc.world.getWorldTime() == 0) {
                queue_message("Top of the morning to you!");
            }
            if (mc.world.getWorldTime() == 6000) {
                queue_message("Good afternoon!");
            }
            if (mc.world.getWorldTime() == 13000) {
                queue_message("Sunset has now ended! You may eat your lunch now if you are a muslim.");
            }
            if (mc.world.getWorldTime() == 1400) {
                queue_message("Good night!");
            }
            if (mc.world.getWorldTime() == 18000) {
                queue_message("Its so dark right now...");
            }
        }

        if (distanceTraveled >= 1.0 && movement.get_value(true)) {
            if (distanceTraveled < (double)(this.delay.get_value(1) * this.min_distance.get_value(1))) {
                return;
            }
            if (distanceTraveled > (double)(this.delay.get_value(1) * this.max_distance.get_value(1))) {
                distanceTraveled = 0.0;
                return;
            }
            sb = new StringBuilder();
            if (movement_string.in("Aha x")) {
                ((StringBuilder)sb).append("aha x, I just traveled ");
            }
            if (movement_string.in("FUCK")) {
                ((StringBuilder)sb).append("FUCK, I just fucking traveled ");
            }
            if (movement_string.in("Leyta")) {
                ((StringBuilder)sb).append("leyta bitch, I just traveled ");
            }
            if (units.in("Feet")) {
                ((StringBuilder)sb).append(round(distanceTraveled*3.2808,2));
                if ((int)distanceTraveled == (double) 1) {
                    ((StringBuilder)sb).append(" Foot");
                } else {
                    ((StringBuilder)sb).append(" Feet");
                }
            } if (units.in("Yards")) {
                ((StringBuilder)sb).append(round(distanceTraveled*1.0936,2));
                if ((int)distanceTraveled == (double) 1) {
                    ((StringBuilder)sb).append(" Yard");
                } else {
                    ((StringBuilder)sb).append(" Yards");
                }
            } if (units.in("Inches")) {
                ((StringBuilder)sb).append(round(distanceTraveled*39.37,2));
                if ((int)distanceTraveled == (double) 1) {
                    ((StringBuilder)sb).append(" Inch");
                } else {
                    ((StringBuilder)sb).append(" Inches");
                }
            } if (units.in("Meters")) {
                ((StringBuilder)sb).append(round(distanceTraveled,2));
                if ((int)distanceTraveled == (double) 1) {
                    ((StringBuilder)sb).append(" Meter");
                } else {
                    ((StringBuilder)sb).append(" Meters");
                }
            }
            this.queue_message(sb.toString());
            distanceTraveled = 0.0;
        }
        if (health.get_value(true)) {
            if (lostHealth != 0.0f) {
                sb = "HECK! I just lost " + df.format(lostHealth) + " health D:";
                this.queue_message((String)sb);
                lostHealth = 0.0f;
            }
            if (gainedHealth != 0.0f) {
                sb = "#ezmode I now have " + df.format(gainedHealth) + " more health";
                this.queue_message((String)sb);
                gainedHealth = 0.0f;
            }
        }
    }

    private void composeEventData() {
        for (Map.Entry<String, Integer> kv : mined_blocks.entrySet()) {
            if (mining.get_value(true)) {
                this.queue_message("We be mining " + kv.getValue() + " " + kv.getKey() + " out here");
                mined_blocks.remove(kv.getKey());
            }
        }
        for (Map.Entry<String, Integer> kv : placed_blocks.entrySet()) {
            if (placing.get_value(true)) {
                this.queue_message("We be placing " + kv.getValue() + " " + kv.getKey() + " out here");
                placed_blocks.remove(kv.getKey());
            }
        }
        for (Map.Entry<String, Integer> kv : dropped_items.entrySet()) {
            if (drops.get_value(true)) {
                this.queue_message("I just dropped " + kv.getValue() + " " + kv.getKey() + ", whoops!");
                dropped_items.remove(kv.getKey());
            }
        }
        for (Map.Entry<String, Integer> kv : consumed_items.entrySet()) {
            if (eating.get_value(true)) {
                this.queue_message("NOM NOM, I just ate " + kv.getValue() + " " + kv.getKey() + ", yummy");
                consumed_items.remove(kv.getKey());
            }
        }
    }
}
