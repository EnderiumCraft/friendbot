package com.enderiumcraft.friendbot;

import com.enderiumcraft.friendbot.entity.ModEntities;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FriendbotMod implements ModInitializer {
	public static final String MOD_ID = "friendbot";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEntities.register();
		LOGGER.info("Friendbot loaded.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
