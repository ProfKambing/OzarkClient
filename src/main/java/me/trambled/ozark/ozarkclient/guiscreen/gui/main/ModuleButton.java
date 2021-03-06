package me.trambled.ozark.ozarkclient.guiscreen.gui.main;

import me.trambled.ozark.Ozark;
import me.trambled.ozark.ozarkclient.guiscreen.gui.main.widgets.*;
import me.trambled.ozark.ozarkclient.module.Module;
import me.trambled.ozark.ozarkclient.module.Setting;
import me.trambled.ozark.ozarkclient.util.render.GuiUtil;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;

import java.util.ArrayList;

import static me.trambled.ozark.ozarkclient.util.misc.WrapperUtil.mc;

// Hacks.


public class ModuleButton {
	private final Module module;
	private final Frame master;

	private final ArrayList<AbstractWidget> widget;

	private final String module_name;

	private boolean opened;

	private int x;
	private int y;

	private int width;
	private int height;

	private int opened_height;

	private int save_y;

	private final GuiUtil font = new GuiUtil(1);

	private final int border_a    = 200;
	private final int border_size = 1;

	private final int master_height_cache;

	public int settings_height;
	public static boolean is_hovering;

	private int count;

	public ModuleButton(Module module, Frame master) {
		/**
		 * A value to save the y. When move the frame the save_y does the work.
		 * @param save_y;
		 **/

		this.module = module;
		this.master = master;

		this.widget = new ArrayList();

		this.module_name = module.get_name();

		this.x = 0;
		this.y = 0;

		this.width  = font.get_string_width(module.get_name()) + 5;

		this.height = font.get_string_height();

		this.opened_height = this.height;

		this.save_y = 0;

		this.opened = false;

		this.master_height_cache = master.get_height();

		this.settings_height = this.y + 10;

		this.count = 0;

		for (Setting settings : Ozark.get_setting_manager().get_settings_with_module(module)) {
			if (settings.get_type().equals("button")) {
				this.widget.add(new Button(master, this, settings.get_tag(), this.settings_height));

				this.settings_height += 10;

				this.count++;
			}

			if (settings.get_type().equals("combobox")) {
				this.widget.add(new Combobox(master, this, settings.get_tag(), this.settings_height));

				this.settings_height += 10;

				this.count++;
			}

			if (settings.get_type().equals("label")) {
				this.widget.add(new Label(master, this, settings.get_tag(), this.settings_height));

				this.settings_height += 10;

				this.count++;
			}

			if (settings.get_type().equals("doubleslider") || settings.get_type().equals("integerslider")) {
				this.widget.add(new Slider(master, this, settings.get_tag(), this.settings_height));

				this.settings_height += 10;

				this.count++;
			}
		}

		int size = Ozark.get_setting_manager().get_settings_with_module(module).size();

		if (this.count >= size) {
			this.widget.add(new ButtonBind(master, this, "bind", this.settings_height));

			this.settings_height += 10;
		}
	}

	public Module get_module() {
		return this.module;
	}

	public Frame get_master() {
		return this.master;
	}

	public void set_pressed(boolean value) {
		this.module.set_active(value);
	}

	public void set_width(int width) {
		this.width = width;
	}

	public void set_height(int height) {
		this.height = height;
	}

	public void set_x(int x) {
		this.x = x;
	}

	public void set_y(int y) {
		this.y = y;
	}

	public void set_open(boolean value) {
		this.opened = value;
	}

	public boolean get_state() {
		return this.module.is_active();
	}

	public int get_settings_height() {
		return this.settings_height;
	}

	public int get_cache_height() {
		return this.master_height_cache;
	}

	public int get_width() {
		return this.width;
	}

	public int get_height() {
		return this.height;
	}

	public int get_x() {
		return this.x;
	}

	public int get_y() {
		return this.y;
	}

	public int get_save_y() {
		return this.save_y;
	}

	public boolean is_open() {
		return this.opened;
	}

	public boolean is_binding() {
		boolean value_requested = false;

		for (AbstractWidget widgets : this.widget) {
			if (widgets.is_binding()) {
				value_requested = true;
			}
		}

		return value_requested;
	}

	public boolean motion(int mx, int my) {
		return mx >= get_x() && my >= get_save_y() && mx <= get_x() + get_width() && my <= get_save_y() + get_height();
	}

	public void does_widgets_can(boolean can) {
		for (AbstractWidget widgets : this.widget) {
			widgets.does_can(can);
		}
	}

	public void bind(char char_, int key) {
		for (AbstractWidget widgets : this.widget) {
			widgets.bind(char_, key);
		}
	}

	public void mouse(int mx, int my, int mouse) {
		for (AbstractWidget widgets : this.widget) {
			widgets.mouse(mx, my, mouse);
		}

		is_hovering = motion(mx, my);

		if (mouse == 0) {
			if (motion(mx, my)) {
				this.master.does_can(false);

				set_pressed(!get_state());
				
				mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			}
		}

		if (mouse == 1) {
			if (motion(mx, my)) {
				this.master.does_can(false);

				set_open(!is_open());

				this.master.refresh_frame(this, 0);
				
				mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			}
		}
	}

	public void button_release(int mx, int my, int mouse) {
		for (AbstractWidget widgets : this.widget) {
			widgets.release(mx, my, mouse);
		}

		this.master.does_can(true);
	}

	public void render(int mx, int my, int separe) {
		set_width(this.master.get_width() - separe);

		this.save_y = this.y + this.master.get_y() - 10;

		int nm_r = Ozark.main_gui.theme_widget_name_r;
		int nm_g = Ozark.main_gui.theme_widget_name_g;
		int nm_b = Ozark.main_gui.theme_widget_name_b;
		int nm_a = Ozark.main_gui.theme_widget_name_a;

		int bg_r = Ozark.main_gui.theme_widget_background_r;
		int bg_g = Ozark.main_gui.theme_widget_background_g;
		int bg_b = Ozark.main_gui.theme_widget_background_b;
		int bg_a = Ozark.main_gui.theme_widget_background_a;

		int bd_r = Ozark.main_gui.theme_widget_border_r;
		int bd_g = Ozark.main_gui.theme_widget_border_g;
		int bd_b = Ozark.main_gui.theme_widget_border_b;

		if (this.module.is_active()) {
			GuiUtil.draw_rect(this.x, this.save_y, this.x + this.width - separe, this.save_y + this.height, bg_r, bg_g, bg_b, bg_a);

			GuiUtil.draw_string(this.module_name, this.x + separe, this.save_y, nm_r, nm_g, nm_b, nm_a);
		} else {
			GuiUtil.draw_string(this.module_name, this.x + separe, this.save_y, nm_r, nm_g, nm_b, nm_a);
		}

		for (AbstractWidget widgets : this.widget) {
			widgets.set_x(get_x());

			boolean is_passing_in_widget = this.opened && widgets.motion_pass(mx, my);

			if (motion(mx, my) || is_passing_in_widget) {
				GuiUtil.draw_rect(this.master.get_x() - 1, this.save_y, this.master.get_width() + 1, this.opened_height, bd_r, bd_g, bd_b, border_a, this.border_size, "right-left");
			}

			if (this.opened) {
				this.opened_height = this.height + this.settings_height - 10;

				widgets.render(get_save_y(), separe, mx, my);
			} else {
				this.opened_height = this.height;
			}
		}
	}
}