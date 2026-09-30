package com.colorfultnt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.colorfultnt.block.ColorfulTntBlock;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ColorfulTnt implements ModInitializer {
	public static final String MOD_ID = "colorful_tnt";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		for (TntType type : TntType.values()) {
			Identifier id = id(type.id);

			ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
			Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey,
					new ColorfulTntBlock(type, BlockBehaviour.Properties.of()
							.setId(blockKey)
							.instabreak()
							.sound(SoundType.GRASS)
							.ignitedByLava()));

			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
			Item item = Registry.register(BuiltInRegistries.ITEM, itemKey,
					new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));

			ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS)
					.register(entries -> entries.accept(item));
		}
		LOGGER.info("Colorful TNT: зарегистрировано {} видов динамита", TntType.values().length);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
