package me.trambled.ozark.ozarkclient.module.chat;


import com.mojang.realmsclient.gui.ChatFormatting;
import me.trambled.ozark.Ozark;
import me.trambled.ozark.ozarkclient.manager.NotificationManager;
import me.trambled.ozark.ozarkclient.module.Category;
import me.trambled.ozark.ozarkclient.module.Module;
import me.trambled.ozark.ozarkclient.module.Setting;
import me.trambled.ozark.ozarkclient.util.world.TimerUtil;

public final class Example extends Module {

    public Example() {
        super(Category.CHAT);

        this.name = "Example";
        this.tag = "Example";
        this.description = "Example";
    }

    Setting string = create("String setting", "String", "test2");
    Setting strings = create("String setting", "String1", "test22", "123");
    Setting strings2 = create("String setting", "String2", "test1", "1233");

    @Override
    public void update() {
        Ozark.get_notification_manager().add_notification(new NotificationManager.Notification("Example", new TimerUtil()));
    }
}

