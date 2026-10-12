package com.enderiumcraft.friendbot;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FriendbotMod implements ModInitializer {
	public static final String MOD_ID = "friendbot";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Friendbot loaded.");
	}
}
