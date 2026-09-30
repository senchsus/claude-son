package com.colorfultnt;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Все виды динамита: id, мощность взрыва, цвет частиц, время запала (тики) и особый эффект после взрыва.
 * Если меняешь здесь id/цвета — не забудь про tools/generate_resources.py (текстуры, lang, рецепты).
 */
public enum TntType {
	// --- Обычные, по возрастанию мощности (у ванильного TNT мощность 4) ---
	WHITE("white_tnt", 2.0f, 0xF2F2F2, 60, null),
	YELLOW("yellow_tnt", 3.0f, 0xF5D72B, 70, null),
	ORANGE("orange_tnt", 5.0f, 0xF28C1E, 80, null),
	RED("red_tnt", 7.0f, 0xD92B2B, 80, null),
	GREEN("green_tnt", 9.0f, 0x3DBE3D, 90, null),
	BLUE("blue_tnt", 12.0f, 0x2F5FE0, 100, null),
	PURPLE("purple_tnt", 16.0f, 0x8E3BD1, 110, null),
	BLACK("black_tnt", 24.0f, 0x2A2A2A, 120, null),

	// --- Особые ---
	WATER("water_tnt", 3.0f, 0x2F8FE6, 80, TntEffects::flood),
	SAND("sand_tnt", 2.0f, 0xE0C878, 60, TntEffects::sandRain),
	LAVA("lava_tnt", 4.0f, 0xFF6A10, 90, TntEffects::lavaPools),
	RAINBOW("rainbow_tnt", 10.0f, 0xFFFFFF, 120, TntEffects::rainbow);

	@FunctionalInterface
	public interface Effect {
		void apply(ServerLevel level, BlockPos center);
	}

	public final String id;
	public final float power;
	public final int color;
	public final int fuseTicks;
	private final Effect effect;

	TntType(String id, float power, int color, int fuseTicks, Effect effect) {
		this.id = id;
		this.power = power;
		this.color = color;
		this.fuseTicks = fuseTicks;
		this.effect = effect;
	}

	/** Взрыв + особый эффект. Вызывается на сервере, когда запал догорел. */
	public void detonate(ServerLevel level, BlockPos pos) {
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.5;
		double z = pos.getZ() + 0.5;
		level.explode(null, x, y, z, power, Level.ExplosionInteraction.TNT);
		if (effect != null) {
			effect.apply(level, pos);
		}
	}
}
