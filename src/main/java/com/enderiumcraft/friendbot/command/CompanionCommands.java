package com.enderiumcraft.friendbot.command;

import com.enderiumcraft.friendbot.entity.CompanionEntity;
import com.enderiumcraft.friendbot.entity.ModEntities;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /friendbot spawn} puts a companion where the command was run. There is no permission
 * check yet, because this mod is meant for singleplayer worlds.
 */
public final class CompanionCommands {
	private CompanionCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(Commands.literal("friendbot")
						.then(Commands.literal("spawn").executes(context -> spawn(context.getSource())))));
	}

	private static int spawn(CommandSourceStack source) {
		ServerLevel level = source.getLevel();
		Vec3 position = source.getPosition();
		CompanionEntity companion = new CompanionEntity(ModEntities.COMPANION, level);
		companion.setPos(position.x, position.y, position.z);
		level.addFreshEntity(companion);
		source.sendSuccess(() -> Component.literal("A companion has arrived."), false);
		return 1;
	}
}
