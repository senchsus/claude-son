package com.colorfultnt;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Особые эффекты, которые срабатывают сразу после взрыва. */
public final class TntEffects {
	private TntEffects() {
	}

	/** Водный: заливает кратер водой (сфера радиусом 5). */
	public static void flood(ServerLevel level, BlockPos center) {
		forSphere(center, 5, pos -> {
			if (level.getBlockState(pos).isAir()) {
				level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
			}
		});
	}

	/** Песочный: над точкой взрыва «сыпется» песок (цилиндр радиусом 4 и высотой 8). */
	public static void sandRain(ServerLevel level, BlockPos center) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				if (dx * dx + dz * dz > 16) {
					continue;
				}
				for (int dy = 0; dy <= 7; dy++) {
					pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
					if (level.getBlockState(pos).isAir()) {
						level.setBlock(pos, Blocks.SAND.defaultBlockState(), 3);
					}
				}
			}
		}
	}

	/** Лавовый: оставляет после себя лавовое озеро (сфера радиусом 3). */
	public static void lavaPools(ServerLevel level, BlockPos center) {
		forSphere(center, 3, pos -> {
			if (level.getBlockState(pos).isAir()) {
				level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
			}
		});
	}

	/** Радужный: дополнительные взрывы вокруг и цветные кольца из частиц. */
	public static void rainbow(ServerLevel level, BlockPos center) {
		double cx = center.getX() + 0.5;
		double cy = center.getY() + 0.5;
		double cz = center.getZ() + 0.5;
		RandomSource random = level.getRandom();

		// 8 дополнительных взрывов по кругу
		for (int i = 0; i < 8; i++) {
			double angle = i * (Math.PI * 2 / 8) + random.nextDouble() * 0.3;
			double dist = 6 + random.nextDouble() * 3;
			level.explode(null, cx + Math.cos(angle) * dist, cy + random.nextInt(3) - 1, cz + Math.sin(angle) * dist,
					5.0f, Level.ExplosionInteraction.TNT);
		}

		// Радужные кольца частиц
		int points = 72;
		for (int ring = 0; ring < 6; ring++) {
			double radius = 2 + ring * 1.5;
			for (int k = 0; k < points; k++) {
				float hue = (float) k / points;
				int rgb = 0xFF000000 | Mth.hsvToRgb(hue, 1.0f, 1.0f);
				double angle = k * (Math.PI * 2 / points);
				level.sendParticles(new DustParticleOptions(rgb, 2.0f),
						cx + Math.cos(angle) * radius, cy + 1 + ring * 0.8, cz + Math.sin(angle) * radius,
						2, 0.1, 0.1, 0.1, 0.0);
			}
		}
	}

	private static void forSphere(BlockPos center, int radius, Consumer<BlockPos> action) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int r2 = radius * radius;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -radius; dy <= radius; dy++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (dx * dx + dy * dy + dz * dz <= r2) {
						pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
						action.accept(pos);
					}
				}
			}
		}
	}
}
