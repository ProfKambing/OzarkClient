package me.trambled.ozark.ozarkclient.module.combat;

import me.trambled.ozark.ozarkclient.module.Category;
import me.trambled.ozark.ozarkclient.module.Module;
import me.trambled.ozark.ozarkclient.module.Setting;

public class ObiAssist extends Module {

    /**
     @author kambing
     @since 30/8/21
     credits: salhack for calcs
     */

    public ObiAssist() {

        super(Category.COMBAT);

        this.name = "ObiAssist";
        this.tag = "ObiAssist";
        this.description = "Place obsidian if theres no possible placement for ca";

    }

    Setting packet = create("PacketSwitch", "PacketSwitch", true);
    Setting delay = create("Delay", "Delay")

}