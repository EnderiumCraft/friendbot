package com.enderiumcraft.friendbot.entity;

import com.enderiumcraft.friendbot.FriendbotMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final ResourceKey<EntityType<?>> COMPANION_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, FriendbotMod.id("companion"));

	private ModEntities() {
	}

	public static void register() {
		// noSummon() keeps /summon from creating the companion until a client renderer exists.
		EntityType<CompanionEntity> companion = EntityType.Builder.of(CompanionEntity::new, MobCategory.MISC)
				.sized(0.6F, 1.8F)
				.noSummon()
				.build(COMPANION_KEY);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, COMPANION_KEY, companion);
		FabricDefaultAttributeRegistry.register(companion, CompanionEntity.createAttributes());
	}
}
