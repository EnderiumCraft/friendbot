package com.enderiumcraft.friendbot.entity;

import com.enderiumcraft.friendbot.body.Goal;
import com.enderiumcraft.friendbot.body.Steering;
import com.enderiumcraft.friendbot.body.Vec2;
import com.enderiumcraft.friendbot.mind.Decision;
import com.enderiumcraft.friendbot.mind.Intent;
import com.enderiumcraft.friendbot.mind.Mind;
import com.enderiumcraft.friendbot.mind.Percept;
import com.enderiumcraft.friendbot.mind.Personality;
import com.enderiumcraft.friendbot.mind.UtilityProvider;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The companion as a Minecraft entity. It does not think for itself: each tick it senses the
 * world, asks its {@link Mind} what to do, and carries out the answer.
 */
public class CompanionEntity extends PathfinderMob {
	static final String NAME = "Sam";
	static final Personality PERSONALITY = new Personality(0.7, 0.4, 0.5);
	/** How far the companion can notice a player, in blocks. */
	static final double SIGHT_RANGE = 24.0;
	/** How far the companion can notice a hostile mob, in blocks. */
	static final double THREAT_RANGE = 8.0;
	/** While approaching or fleeing, walk targets are refreshed this often, in ticks. */
	static final int REPLAN_TICKS = 10;

	private final Mind mind = new Mind(NAME, PERSONALITY, new UtilityProvider());

	public CompanionEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.1);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			return;
		}

		Optional<Player> player = Optional.ofNullable(level().getNearestPlayer(this, SIGHT_RANGE));
		Optional<Entity> threat = nearestThreat();
		Percept percept = new Percept(
				level().getGameTime(),
				player.map(p -> new Percept.Sighting(p.getName().getString(), distanceTo(p))),
				threat.isPresent(),
				getHealth() / getMaxHealth(),
				false, // Placeholder: the companion cannot carry food yet.
				isNight());

		Optional<Decision> decision = mind.think(percept);
		if (decision.isPresent()) {
			decision.get().say().ifPresent(line -> say(line, player));
			steer(decision.get().intent(), player, threat);
		} else if (tickCount % REPLAN_TICKS == 0 && isFollowingOrFleeing(mind.intent())) {
			steer(mind.intent(), player, threat);
		}

		player.ifPresent(p -> getLookControl().setLookAt(p, 30.0F, 30.0F));
	}

	private static boolean isFollowingOrFleeing(Intent intent) {
		return intent == Intent.APPROACH_PLAYER || intent == Intent.FLEE;
	}

	private void steer(Intent intent, Optional<Player> player, Optional<Entity> threat) {
		Optional<Goal> goal = Steering.plan(
				intent,
				groundPosition(this),
				player.map(CompanionEntity::groundPosition),
				threat.map(CompanionEntity::groundPosition),
				random.nextDouble());

		if (goal.isPresent()) {
			Vec2 target = goal.get().target();
			getNavigation().moveTo(target.x(), getY(), target.z(), goal.get().speed());
		} else {
			getNavigation().stop();
		}
	}

	/** Speaks in chat as the companion: to the player it is talking to, or to everyone if there is no one. */
	private void say(String line, Optional<Player> listener) {
		if (!(level() instanceof ServerLevel serverLevel)) {
			return;
		}
		Component message = Component.literal("<" + NAME + "> " + line);
		if (listener.isPresent()) {
			listener.get().sendSystemMessage(message);
		} else {
			serverLevel.getServer().getPlayerList().broadcastSystemMessage(message, false);
		}
	}

	/**
	 * The closest hostile mob within {@link #THREAT_RANGE}. Neutral mobs such as endermen are
	 * ignored because they only fight back once provoked. That is a gap to close later.
	 */
	private Optional<Entity> nearestThreat() {
		double rangeSquared = THREAT_RANGE * THREAT_RANGE;
		return level().getEntities(this, getBoundingBox().inflate(THREAT_RANGE),
						entity -> entity instanceof Enemy && !(entity instanceof NeutralMob))
				.stream()
				.filter(entity -> distanceToSqr(entity) <= rangeSquared)
				.min(Comparator.comparingDouble(this::distanceToSqr));
	}

	private boolean isNight() {
		long time = level().getDayTime() % 24000L;
		return time >= 13000L && time < 23000L;
	}

	private static Vec2 groundPosition(Entity entity) {
		return new Vec2(entity.getX(), entity.getZ());
	}
}
