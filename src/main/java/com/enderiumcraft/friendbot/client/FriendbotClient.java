package com.enderiumcraft.friendbot.client;

import com.enderiumcraft.friendbot.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class FriendbotClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.COMPANION, CompanionRenderer::new);
	}
}
