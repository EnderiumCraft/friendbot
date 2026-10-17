package com.enderiumcraft.friendbot.entity;

import com.enderiumcraft.friendbot.body.Goal;
import com.enderiumcraft.friendbot.body.Steering;
import com.enderiumcraft.friendbot.body.Vec2;
import com.enderiumcraft.friendbot.mind.Decision;
import com.enderiumcraft.friendbot.mind.Encounter;
import com.enderiumcraft.friendbot.mind.Intent;
import com.enderiumcraft.friendbot.mind.Mind;
import com.enderiumcraft.friendbot.mind.MindCodec;
import com.enderiumcraft.friendbot.mind.Need;
import com.enderiumcraft.friendbot.mind.Percept;
import com.enderiumcraft.friendbot.mind.Personality;
import com.enderiumcraft.friendbot.mind.UtilityProvider;
import com.enderiumcraft.friendbot.mind.Utterance;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The companion as a Minecraft entity. It does not think for itself: each tick it senses the
 * world, asks its {@link Mind} what to do, and carries out the answer. Players can hit it,
 * greet it with an empty hand, or hand it food, and it remembers across saves.
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
	/** How much one piece of food fills the food need. */
	static final double FOOD_VALUE = 0.6;
	/** A player can greet the companion with an empty hand at most this often, in ticks. */
	static final int GREET_COOLDOWN_TICKS = 100;

	private static final String MIND_KEY = "Mind";
	private static final String FOOD_KEY = "Food";

	private Mind mind = newMind();
	/** Pieces of food the companion has been given and has not eaten yet. */
	private int foodCount;
	private int lastGreetTick = -GREET_COOLDOWN_TICKS;

	public CompanionEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.1);
	}

	private static Mind newMind() {
		return new Mind(NAME, PERSONALITY, new UtilityProvider());
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
				foodCount > 0,
				level().isNight());

		Optional<Decision> decision = mind.think(percept);
		if (decision.isPresent()) {
			decision.get().say().ifPresent(this::say);
			if (decision.get().intent() == Intent.EAT && foodCount > 0) {
				foodCount--;
				mind.restore(Need.FOOD, FOOD_VALUE);
			}
			steer(decision.get().intent(), player, threat);
		} else if (tickCount % REPLAN_TICKS == 0 && isFollowingOrFleeing(mind.intent())) {
			steer(mind.intent(), player, threat);
		}

		player.ifPresent(p -> getLookControl().setLookAt(p, 30.0F, 30.0F));
	}

	/** A hit from a player is remembered, and the companion answers it on its next decision. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() instanceof Player attacker) {
			mind.recordEncounter(attacker.getName().getString(), Encounter.HURT_ME);
		}
		return super.hurtServer(level, source, amount);
	}

	/**
	 * Right-clicking with food gives it to the companion. Right-clicking with anything else says
	 * hello, at most once every few seconds.
	 */
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		ItemStack held = player.getItemInHand(hand);
		String name = player.getName().getString();
		if (held.has(DataComponents.FOOD)) {
			if (!player.getAbilities().instabuild) {
				held.shrink(1);
			}
			foodCount++;
			mind.recordEncounter(name, Encounter.GAVE_FOOD);
		} else if (tickCount - lastGreetTick >= GREET_COOLDOWN_TICKS) {
			lastGreetTick = tickCount;
			mind.recordEncounter(name, Encounter.GREETED);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString(MIND_KEY, MindCodec.encode(mind.snapshot()));
		output.putInt(FOOD_KEY, foodCount);
	}

	@Override
	public void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		MindCodec.decode(input.getStringOr(MIND_KEY, ""))
				.ifPresent(snapshot -> mind = Mind.restore(NAME, PERSONALITY, new UtilityProvider(), snapshot));
		foodCount = Math.max(0, input.getIntOr(FOOD_KEY, 0));
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

	/**
	 * Says a line in chat to the player it is for. Nothing is said if that player has left, so a
	 * reply never goes to someone it was not meant for.
	 */
	private void say(Utterance utterance) {
		if (!(level() instanceof ServerLevel serverLevel)) {
			return;
		}
		Player listener = serverLevel.getServer().getPlayerList().getPlayerByName(utterance.listener());
		if (listener != null) {
			listener.sendSystemMessage(Component.literal("<" + NAME + "> " + utterance.text()));
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

	private static Vec2 groundPosition(Entity entity) {
		return new Vec2(entity.getX(), entity.getZ());
	}
}
