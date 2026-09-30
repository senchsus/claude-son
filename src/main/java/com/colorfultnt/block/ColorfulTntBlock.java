package com.colorfultnt.block;

import org.jspecify.annotations.Nullable;

import com.colorfultnt.TntType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Динамит без отдельной сущности: при поджоге блок переходит в состояние lit=true,
 * через fuseTicks срабатывает tick() — блок убирается и происходит взрыв нужной мощности.
 * Поджечь можно огнивом, огненным шаром или редстоуном.
 */
public class ColorfulTntBlock extends Block {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	private final TntType type;

	public ColorfulTntBlock(TntType type, Properties properties) {
		super(properties);
		this.type = type;
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	public TntType getType() {
		return type;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!oldState.is(state.getBlock()) && level instanceof ServerLevel serverLevel && serverLevel.hasNeighborSignal(pos)) {
			ignite(serverLevel, pos, state);
		}
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
			@Nullable Orientation orientation, boolean movedByPiston) {
		if (level instanceof ServerLevel serverLevel && serverLevel.hasNeighborSignal(pos)) {
			ignite(serverLevel, pos, state);
		}
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (level instanceof ServerLevel serverLevel) {
			ignite(serverLevel, pos, state);
		}
		if (stack.is(Items.FLINT_AND_STEEL)) {
			stack.hurtAndBreak(1, player, hand);
		} else {
			stack.consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}

	private void ignite(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.getValue(LIT)) {
			return;
		}
		level.setBlock(pos, state.setValue(LIT, true), 3);
		level.scheduleTick(pos, this, type.fuseTicks);
		level.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0f, 1.0f);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) {
			return;
		}
		level.removeBlock(pos, false);
		type.detonate(level, pos);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) {
			return;
		}
		int rgb = type == TntType.RAINBOW
				? Mth.hsvToRgb((level.getGameTime() % 30) / 30.0f, 1.0f, 1.0f)
				: type.color;
		double x = pos.getX() + random.nextDouble();
		double z = pos.getZ() + random.nextDouble();
		level.addParticle(new DustParticleOptions(0xFF000000 | rgb, 1.3f), x, pos.getY() + 1.05, z, 0, 0.02, 0);
		level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 0, 0.05, 0);
	}
}
