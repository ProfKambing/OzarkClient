package me.trambled.ozark.ozarkclient.event.events;

import me.trambled.ozark.ozarkclient.event.Event;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.math.Vec3d;

import static me.trambled.ozark.ozarkclient.util.misc.WrapperUtil.mc;

// External.


public class EventRender extends Event {
	private final ScaledResolution res = new ScaledResolution(mc);
	private final Tessellator tessellator;
	private final Vec3d       render_pos;

	public EventRender(Tessellator tessellator, Vec3d pos) {
		super();

		this.tessellator = tessellator;
		this.render_pos  = pos;
	}

	public Tessellator get_tessellator() {
		return this.tessellator;
	}

	public Vec3d get_render_pos() {
		return render_pos;
	}

	public BufferBuilder get_buffer_build() {
		return this.tessellator.getBuffer();
	}

	public void set_translation(Vec3d pos) {
		get_buffer_build().setTranslation(- pos.x, - pos.y, - pos.z);
	}

	public void reset_translation() {
		set_translation(render_pos);
	}

	public double get_screen_width() {
		return res.getScaledWidth_double();
	  }
	  
	  public double get_screen_height() {
		return res.getScaledHeight_double();
	  }
}