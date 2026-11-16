package com.livingcompanion.entity;

import com.livingcompanion.config.CompanionConfig;
import com.livingcompanion.progression.CompanionClass;
import com.livingcompanion.progression.CompanionProgression;
import com.livingcompanion.progression.CompanionAttribute;
import com.livingcompanion.progression.CompanionRelationship;
import com.livingcompanion.progression.CompanionAchievement;
import com.livingcompanion.ai.FollowOwnerGoal;
import com.livingcompanion.ai.CompanionIdleRoutineGoal;
import com.livingcompanion.inventory.CompanionInventory;
import com.livingcompanion.menu.CompanionInventoryMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

/**
 * The companion: a persistent humanoid adventurer.
 *
 * <p>Uses the vanilla humanoid player model and animation pipeline. The entity
 * owns its follow/defend command state, uses vanilla melee AI, and accepts
 * equipment items with ordinary Minecraft attribute modifiers.</p>
 *
 * <p>Extends {@link PathfinderMob} rather than {@code TamableAnimal}: we run our
 * own command state machine and must not inherit vanilla sit/tame/breed goals.</p>
 */
public class CompanionEntity extends PathfinderMob {

    /** Owner UUID, synched to clients (rendering/nameplate logic later). */
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_OWNER_PROTECTION =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BOOLEAN);
    /** Bit flags for currently active skill visuals; synchronized for client rendering. */
    private static final EntityDataAccessor<Integer> DATA_SKILL_VISUAL_FLAGS =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);
    /** Cached owner name for messages; survives owner logout. */
    private String ownerNameCache = "";

    /** True when this instance was reconstructed from world NBT (keep saved health). */
    private boolean loadedFromSave = false;

    /** One-shot flag: config attributes are (re)applied on the first server tick. */
    private boolean configApplied = false;

    /** Set when resurrection must restore full health after configured attributes are applied. */
    private boolean restoreFullHealthAfterConfig = false;

    /** Persistent carried inventory shown through the companion's player-style menu. */
    private final CompanionInventory companionInventory = new CompanionInventory(this);
    private final CompanionProgression progression = new CompanionProgression();
    private final CompanionRelationship relationship = new CompanionRelationship();
    private int relationshipInteractionCooldownTicks = 0;
    private int worldAwarenessTicks = 0;
    private String lastBiomeId = "";
    private boolean insideVillage = false;
    private final CompanionSkillRuntime skillRuntime = new CompanionSkillRuntime(this);
    private CompanionClass companionClass = CompanionClass.UNASSIGNED;

    /** Custom survival food state. Food drains over time and is replenished from carried supplies. */
    private int foodLevel = 20;
    private float foodSaturation = 5.0F;
    private int foodDrainTicks = 0;
    private int starvationTicks = 0;
    private int eatingCooldownTicks = 0;
    private boolean warnedLowFood = false;
    private static final int MAX_FOOD_LEVEL = 20;
    private static final int PASSIVE_FOOD_INTERVAL = 3600; // one point every three minutes
    private static final int COMBAT_FOOD_INTERVAL = 1800;  // combat doubles food use

    public enum Command { FOLLOW, STAY, DEFEND }
    private Command command = Command.FOLLOW;
    private long nextDialogueTick = 0L;
    private final List<String> memories = new ArrayList<>();
    private static final int MAX_MEMORIES = 12;
    private String lastDialogueLine = "";
    private static final List<String> IDLE_LINES = List.of(
            "The road ahead looks quiet. I don't trust it.", "I've seen worse roads. Usually right before things get interesting.",
            "If we keep this pace, we'll arrive sometime this week.", "I was promised adventure. Nobody mentioned the walking.",
            "You ever notice how trouble waits until we've unpacked?", "I like this stretch. Fewer things trying to eat us.",
            "I should polish the armor. It has developed a personality.", "I've been on worse journeys. The food was better, though.",
            "I wonder how far the next village is. My feet are beginning to file a complaint.",
            "You're very quiet today. I'll assume that's a good sign.", "I could get used to this. The lack of immediate danger, mostly.",
            "If you hear something behind us, I heard it first. Probably.", "I was thinking about taking up fishing. Less stabbing.",
            "The important thing is to look confident. People rarely check.", "I hope you have a plan. I left mine somewhere near the last skeleton.",
            "We make a decent team. Your survival rate has improved noticeably.", "I have a feeling this is going to be a long day.",
            "I used to travel with a map. Then I met you.", "A quiet road, a clear sky. Something is definitely planning something.",
            "I don't mind the miles. I mind the company of spiders."
    );
    private static final List<String> EXPLORATION_LINES = List.of(
            "New ground. Keep your eyes open; places like this have a way of hiding trouble.",
            "I haven't been here before. That's either exciting or a warning.",
            "Take a moment. It's a fine view. Even if something is probably watching us.",
            "We should remember this place. It might be useful later.",
            "Every good expedition starts with someone saying, 'It can't be that dangerous.'",
            "Let's have a look around. Carefully. I have grown attached to my current number of limbs.",
            "I wonder what people built here. I also wonder what chased them away.",
            "There's always something worth finding if you look past the monsters."
    );
    private static final List<String> NIGHT_LINES = List.of(
            "Night's settling in. Stay close; the dark makes everything ambitious.",
            "I can take first watch. I don't sleep much in this armor anyway.",
            "The stars are nice. The things crawling under them are less charming.",
            "If we camp here, I'll keep an eye on the perimeter.",
            "I dislike how quiet it gets just before something screams.",
            "We should find shelter. I would prefer to meet the undead on our terms."
    );
    private static final List<String> WEATHER_LINES = List.of(
            "This rain is getting into places armor was never designed to protect.",
            "Lovely weather. Perfect for catching a cold and a stray arrow.",
            "I hope that storm passes soon. My helmet is starting to sound like a drum.",
            "The wind is picking up. Keep your footing.",
            "If the weather gets any worse, even the zombies will stay indoors.",
            "I miss dry socks. I miss them more than I miss most people."
    );
    private static final List<String> COMBAT_LINES = List.of(
            "Stay behind me. I'll keep the unpleasant ones busy.",
            "One at a time, if you please. There's a queue.",
            "I count at least three problems. I'll start with the loudest.",
            "Keep moving. Standing still makes you a very convenient target.",
            "I was enjoying the quiet. Naturally, that had to end.",
            "Watch the flanks. They rarely have the manners to attack from the front.",
            "I've got the front. You handle anything that gets clever.",
            "That one looks determined. Let's correct that."
    );
    private static final List<String> OWNER_HURT_LINES = List.of(
            "You're hurt. Fall back a step and let me handle this.",
            "Easy. You're bleeding, and I'm running out of patience with these things.",
            "Stay with me. We can sort this out.",
            "You're taking too many hits. Let me get between you and them.",
            "I can cover you. Take a breath and recover."
    );
    private static final List<String> COMPANION_HURT_LINES = List.of(
            "That one got through. I'll be more careful.",
            "Armor's holding. My pride took the worse of it.",
            "I've had better days. Still here, though.",
            "A scratch. A rather enthusiastic scratch.",
            "I'll be fine. Keep your attention on the fight."
    );
    /** Paladin dialogue: measured, reassuring, and guided by a quiet sense of duty. */
    private static final List<String> PALADIN_LINES = List.of(
            "A little patience can save a great deal of blood.",
            "Keep your courage. Fear is useful when it reminds us to pay attention.",
            "There are easier roads. I suspect we would find trouble on those too.",
            "If someone needs help out here, we have the strength to give it.",
            "I keep watch because someone has to. I happen to be good at it.",
            "Take a moment to breathe. The next challenge will still be there.",
            "A blade can end a fight. A steady hand can prevent one.",
            "I would like to see this world with fewer graves and more lanterns."
    );
    /** Tank dialogue: practical, dry-humoured, and quietly protective. */
    private static final List<String> TANK_LINES = List.of(
            "The plan is simple. We stand here, and trouble breaks itself against us.",
            "I checked the route. It has the usual number of things trying to kill us.",
            "You handle the clever part. I'll handle anything that gets too close.",
            "Good armor, a clear path, and a sensible amount of violence. That's a fine day.",
            "I don't mind carrying the front line. Just keep the supplies coming.",
            "That noise was probably harmless. I intend to hit it anyway.",
            "We should rest soon. Even steel gets tired of being hit.",
            "If the world insists on throwing monsters at us, the world can expect a sore fist."
    );
    private static final List<String> PALADIN_COMBAT_LINES = List.of(
            "Stand firm. I will keep them away from you.",
            "Hold your ground. We leave together.",
            "I will give you room. Move when you see it.",
            "Stay behind my shield. This ends here."
    );
    private static final List<String> TANK_COMBAT_LINES = List.of(
            "Behind me. I'll make a wall out of myself.",
            "Come on, then. I've got room for all of you.",
            "Keep swinging. My armor enjoys the attention.",
            "I've had doors hit harder. Keep moving."
    );
    private static final List<String> MEMORY_LINES = List.of(
            "I still remember when %s. That was a day worth keeping.",
            "Funny how a journey stays with you. Especially the part where %s.",
            "We've come a long way since %s. I remember it clearly.",
            "I was just thinking about how %s. Good times. Mostly."
    );

    public CompanionEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(false);   // Phase 7 uses a controlled pickup goal instead
        this.setPersistenceRequired();
        this.xpReward = 0;              // he is a companion, not a mob farm
    }

    // ------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------

    /**
     * Registered at registry time with safe defaults. {@code Mob.createMobAttributes()}
     * already provides the living/mob attribute set; we override the bases we care about.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.4D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D);
    }

    /**
     * Applies live config values. Called on the first server tick of every
     * instance (fresh spawn, world reload, and — later — resurrection), so
     * config edits take effect without re-registration.
     */
    private void applyConfigAttributes() {
        setBase(Attributes.MAX_HEALTH, CompanionConfig.MAX_HEALTH.get());
        setBase(Attributes.MOVEMENT_SPEED, CompanionConfig.MOVEMENT_SPEED.get());
        setBase(Attributes.ATTACK_DAMAGE, CompanionConfig.ATTACK_DAMAGE.get());
        setBase(Attributes.FOLLOW_RANGE, CompanionConfig.HOSTILE_SCAN_RANGE.get());
    }

    private void setBase(Attribute attribute, double value) {
        var instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    // ------------------------------------------------------------------
    // Entity data / owner binding
    // ------------------------------------------------------------------

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_OWNER_UUID, Optional.empty());
        this.entityData.define(DATA_OWNER_PROTECTION, false);
        this.entityData.define(DATA_SKILL_VISUAL_FLAGS, 0);
    }

    public CompanionInventory getCompanionInventory() {
        return this.companionInventory;
    }

    public CompanionClass getCompanionClass() {
        return this.companionClass;
    }

    public CompanionProgression getProgression() {
        return this.progression;
    }

    public CompanionRelationship getRelationship() {
        return this.relationship;
    }

    /** Awards familiarity for meaningful shared events. */
    public void addRelationshipExperience(int amount) {
        if (this.level().isClientSide || amount <= 0) return;
        int gained = this.relationship.addExperience(amount);
        updateRelationshipAchievements();
        if (gained > 0) {
            Player owner = getOwnerPlayer();
            if (owner != null) {
                owner.sendSystemMessage(Component.literal(this.getName().getString()
                        + " feels closer to you. Relationship: " + this.relationship.getTitle() + "."));
            }
            commentOnEvent(
                    "We've grown into a good team. I trust your judgement.",
                    "I know your ways by now. That makes the road easier.",
                    "Somewhere along the way, this stopped feeling like an assignment."
            );
        }
    }

    public void awardAchievement(CompanionAchievement achievement) {
        if (this.level().isClientSide || achievement == null) return;
        if (this.progression.awardAchievement(achievement)) {
            Player owner = getOwnerPlayer();
            if (owner != null) {
                owner.sendSystemMessage(Component.literal("Achievement unlocked: "
                        + achievement.getTitle() + " — " + achievement.getDescription()));
            }
        }
    }

    private void updateRelationshipAchievements() {
        if (this.relationship.getLevel() >= 5) awardAchievement(CompanionAchievement.TRUSTED);
        if (this.relationship.getLevel() >= 8) awardAchievement(CompanionAchievement.DEVOTED);
        if (this.relationship.getLevel() >= 10) awardAchievement(CompanionAchievement.UNBREAKABLE);
    }

    private void tickRelationship() {
        if (this.relationshipInteractionCooldownTicks > 0) this.relationshipInteractionCooldownTicks--;
        Player owner = getOwnerPlayer();
        if (owner != null && owner.isAlive() && owner.level() == this.level()
                && this.distanceToSqr(owner) <= 12.0D * 12.0D) {
            int previousLevel = this.relationship.getLevel();
            this.relationship.tickNearOwner();
            updateRelationshipAchievements();
            if (this.relationship.getLevel() > previousLevel) {
                owner.sendSystemMessage(Component.literal(this.getName().getString()
                        + " feels closer to you. Relationship: " + this.relationship.getTitle() + "."));
            }
        }
    }

    public CompanionSkillRuntime getSkillRuntime() {
        return this.skillRuntime;
    }

    public int getFoodLevel() {
        return this.foodLevel;
    }

    public int getMaxFoodLevel() {
        return MAX_FOOD_LEVEL;
    }

    /** Client-readable flags describing which persistent skill effects are active. */
    public int getSkillVisualFlags() {
        return this.entityData.get(DATA_SKILL_VISUAL_FLAGS);
    }

    /** Server-authoritative update; SynchedEntityData distributes changes to tracking clients. */
    public void setSkillVisualFlags(int flags) {
        if (!this.level().isClientSide && this.entityData.get(DATA_SKILL_VISUAL_FLAGS) != flags) {
            this.entityData.set(DATA_SKILL_VISUAL_FLAGS, flags);
        }
    }

    public float interceptOwnerDamage(ServerPlayer owner, DamageSource source, float amount) {
        return this.skillRuntime.mitigateOwnerDamage(owner, source, amount);
    }

    /** Assigns the character's permanent starting archetype. */
    public void setCompanionClass(CompanionClass companionClass) {
        if (companionClass == null || companionClass == CompanionClass.UNASSIGNED) return;
        this.companionClass = companionClass;
        if (!this.level().isClientSide) {
            applyProgressionAttributes(true);
            inventoryChanged();
        }
    }

    /** Sets a player-chosen name with a strict length and control-character limit. */
    public boolean setCompanionName(String name) {
        if (name == null) return false;
        String clean = name.replaceAll("[\\p{Cntrl}]", "").trim();
        if (clean.isEmpty() || clean.length() > 24) return false;
        this.setCustomName(Component.literal(clean));
        this.setCustomNameVisible(true);
        if (!this.level().isClientSide) inventoryChanged();
        return true;
    }

    /** Awards combat experience and returns the number of levels gained. Server only. */
    public int awardExperience(int amount) {
        if (this.level().isClientSide || amount <= 0) return 0;
        int oldLevel = this.progression.getLevel();
        this.progression.addExperience(amount);
        int gained = this.progression.getLevel() - oldLevel;
        if (gained > 0) {
            applyProgressionAttributes(true);
            if (this.progression.getLevel() >= 10) awardAchievement(CompanionAchievement.VETERAN);
            if (this.progression.getLevel() >= 25) awardAchievement(CompanionAchievement.ELITE);
            if (this.progression.getLevel() >= 50) awardAchievement(CompanionAchievement.LEGEND);
            Player owner = getOwnerPlayer();
            if (owner != null) {
                owner.sendSystemMessage(Component.literal(this.getName().getString()
                        + " reached level " + this.progression.getLevel()
                        + "! +" + gained + " skill point" + (gained == 1 ? "" : "s")
                        + " and +" + (gained * 2) + " attribute points."));
            }
        }
        inventoryChanged();
        return gained;
    }

    /** Rebuilds class/level stats from config bases; health is preserved proportionally. */
    private void applyProgressionAttributes(boolean preserveHealthRatio) {
        float oldHealth = this.getHealth();
        float oldMaxHealth = this.getMaxHealth();
        double levelBonus = Math.sqrt(this.progression.getLevel() - 1);
        double vigor = this.progression.getAttribute(CompanionAttribute.VIGOR);
        double might = this.progression.getAttribute(CompanionAttribute.MIGHT);
        double fortitude = this.progression.getAttribute(CompanionAttribute.FORTITUDE);
        double agility = this.progression.getAttribute(CompanionAttribute.AGILITY);

        double health = CompanionConfig.MAX_HEALTH.get() + vigor * 1.0D;
        double attack = CompanionConfig.ATTACK_DAMAGE.get() + might * 0.15D;
        double armor = fortitude * 0.4D;
        double toughness = 0.0D;
        double knockback = 0.2D;
        double movement = CompanionConfig.MOVEMENT_SPEED.get() + agility * 0.002D;

        switch (this.companionClass) {
            case PALADIN -> {
                health += 8.0D + levelBonus * 0.8D;
                attack += 1.0D + levelBonus * 0.18D;
                armor += 3.0D + levelBonus * 0.08D;
                toughness = 1.0D + levelBonus * 0.04D;
            }
            case TANK -> {
                health += 16.0D + levelBonus * 1.2D;
                attack += levelBonus * 0.12D;
                armor += 6.0D + levelBonus * 0.16D;
                toughness = 2.0D + levelBonus * 0.06D;
                knockback = Math.min(0.7D, 0.5D + levelBonus * 0.025D);
            }
            case UNASSIGNED -> {
                health += levelBonus * 0.5D;
                attack += levelBonus * 0.1D;
            }
        }

        CompanionSpecialization specialization = this.progression.getSpecialization();
        if (specialization == CompanionSpecialization.PALADIN_WARDEN) {
            health += 4.0D;
            armor += 1.0D;
        } else if (specialization == CompanionSpecialization.PALADIN_CRUSADER) {
            attack += 1.5D;
        } else if (specialization == CompanionSpecialization.TANK_BULWARK) {
            armor += 2.0D;
            toughness += 1.0D;
        } else if (specialization == CompanionSpecialization.TANK_VANGUARD) {
            attack += 1.0D;
            movement += 0.01D;
        }

        setBase(Attributes.MAX_HEALTH, health);
        setBase(Attributes.ATTACK_DAMAGE, attack);
        setBase(Attributes.ARMOR, armor);
        setBase(Attributes.ARMOR_TOUGHNESS, toughness);
        setBase(Attributes.KNOCKBACK_RESISTANCE, knockback);
        setBase(Attributes.MOVEMENT_SPEED, movement);
        if (preserveHealthRatio && oldMaxHealth > 0.0F) {
            this.setHealth(Math.min((oldHealth / oldMaxHealth) * this.getMaxHealth(), this.getMaxHealth()));
        } else {
            this.setHealth(this.getMaxHealth());
        }
    }

    public void refreshProgressionAttributes() {
        if (!this.level().isClientSide) applyProgressionAttributes(true);
        inventoryChanged();
    }

    /** Marks the entity chunk dirty when carried inventory contents change. */
    public void inventoryChanged() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.getChunkAt(this.blockPosition()).setUnsaved(true);
        }
    }

    public boolean isProtectedFromOwner() {
        return this.entityData.get(DATA_OWNER_PROTECTION);
    }

    public void setProtectedFromOwner(boolean protectedFromOwner) {
        this.entityData.set(DATA_OWNER_PROTECTION, protectedFromOwner);
        if (!this.level().isClientSide) this.inventoryChanged();
    }

    private boolean isOwnerDamage(DamageSource source) {
        UUID owner = getOwnerUUID();
        if (owner == null) return false;
        Entity attacker = source.getEntity();
        if (attacker != null && owner.equals(attacker.getUUID())) return true;
        Entity direct = source.getDirectEntity();
        return direct instanceof Projectile projectile && projectile.getOwner() != null
                && owner.equals(projectile.getOwner().getUUID());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isProtectedFromOwner() && isOwnerDamage(source)) return false;
        if (!this.level().isClientSide) {
            amount = this.skillRuntime.mitigateIncomingDamage(amount);
        }
        return super.hurt(source, amount);
    }

    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID).orElse(null);
    }

    public void setOwnerUUID(UUID uuid) {
        this.entityData.set(DATA_OWNER_UUID, Optional.ofNullable(uuid));
    }

    public String getOwnerNameCache() {
        return this.ownerNameCache;
    }

    /** Resolves the owner if online; null while the owner is logged out. */
    public Player getOwnerPlayer() {
        UUID uuid = getOwnerUUID();
        return uuid == null || !(this.level() instanceof ServerLevel serverLevel) ? null : serverLevel.getPlayerByUUID(uuid);
    }

    public boolean isOwnedBy(Player player) {
        UUID owner = getOwnerUUID();
        return owner != null && owner.equals(player.getUUID());
    }

    /** Idempotent owner binding with a one-time acknowledgement message. */
    public void bindOwner(Player player) {
        if (player.getUUID().equals(getOwnerUUID())) {
            return;
        }
        setOwnerUUID(player.getUUID());
        this.ownerNameCache = player.getGameProfile().getName();
        player.sendSystemMessage(Component.translatable("message.livingcompanion.bound", this.getName()));
    }

    // ------------------------------------------------------------------
    // Following, combat, and idle behavior
    // ------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.15D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new CompanionIdleRoutineGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class,
                10, true, false, target -> shouldTargetHostile((Monster) target)));
    }

    private boolean shouldTargetHostile(Monster hostile) {
        // STAY disables proactive target acquisition. HurtByTargetGoal remains active,
        // so the companion can still respond when directly attacked.
        if (this.command == Command.STAY) return false;

        double range = CompanionConfig.HOSTILE_SCAN_RANGE.get();
        Player owner = getOwnerPlayer();
        if (this.command == Command.DEFEND && owner != null && owner.isAlive()
                && owner.level() == this.level()) {
            return hostile.distanceToSqr(owner) <= range * range;
        }
        return hostile.distanceToSqr(this) <= range * range;
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && !this.configApplied) {
            applyConfigAttributes();
            applyProgressionAttributes(false);
            if (!this.loadedFromSave || this.restoreFullHealthAfterConfig) {
                this.setHealth(this.getMaxHealth()); // fresh spawns and resurrected companions start at configured max
                this.restoreFullHealthAfterConfig = false;
            }
            this.configApplied = true;
        }
        if (!this.level().isClientSide) {
            this.skillRuntime.tick();
            tickFoodSystem();
            tickRelationship();
            tickWorldAwareness();
            if (this.tickCount % 20 == 0) speakOccasionally();
        }
    }

    /**
     * Detects biome transitions and village visits at a low polling rate.
     * Existing memory events cover boss fights, dimensions, and valuable discoveries.
     */
    private void tickWorldAwareness() {
        if (++this.worldAwarenessTicks < 100 || !(this.level() instanceof ServerLevel serverLevel)) return;
        this.worldAwarenessTicks = 0;

        String biomeId = this.level().getBiome(this.blockPosition()).unwrapKey()
                .map(key -> key.location().toString()).orElse("unknown");
        if (this.lastBiomeId.isEmpty()) {
            this.lastBiomeId = biomeId;
        } else if (!biomeId.equals(this.lastBiomeId)) {
            this.lastBiomeId = biomeId;
            String biomeName = biomeId.substring(biomeId.indexOf(':') + 1).replace('_', ' ');
            remember("we travelled through the " + biomeName + " biome");
            if (biomeId.contains("desert")) {
                commentOnEvent("Desert ahead. Keep water and supplies close.",
                        "Sand as far as I can see. I already miss trees.",
                        "Watch the horizon. Open ground gives trouble plenty of room.");
            } else if (biomeId.contains("snow") || biomeId.contains("frozen") || biomeId.contains("ice")) {
                commentOnEvent("Cold country. Keep moving and watch your footing.",
                        "I can feel the chill through the armor. That's impressive, really.",
                        "Snow hides holes, cliffs, and the occasional unpleasant surprise.");
            } else if (biomeId.contains("jungle")) {
                commentOnEvent("Dense jungle. Stay close; visibility is poor.",
                        "Every leaf seems to be hiding something with teeth.",
                        "Keep an eye out for ruins. This place has a long memory.");
            } else if (biomeId.contains("ocean") || biomeId.contains("beach")) {
                commentOnEvent("We've reached the coast. The open water makes me uneasy.",
                        "A long way to swim. I hope you brought a boat.",
                        "The sea is peaceful from here. I intend to keep it that way.");
            } else if (biomeId.contains("badlands") || biomeId.contains("mesa")) {
                commentOnEvent("Strange country. The stone looks almost painted.",
                        "This place has seen a lot of sun and very little rain.",
                        "Keep looking. There may be something buried out here.");
            } else if (biomeId.contains("swamp") || biomeId.contains("mangrove")) {
                commentOnEvent("A swamp. Wonderful. Mud, water, and things that bite.",
                        "Watch your step. The ground here has questionable intentions.",
                        "I would rather camp somewhere dry.");
            } else {
                commentOnEvent("New terrain. Let's see what this place has to offer.",
                        "The landscape has changed. Keep your eyes open.",
                        "Another stretch of the world to learn.");
            }
        }

        boolean nowInVillage = serverLevel.structureManager()
                .getStructureWithPieceAt(this.blockPosition(), StructureTags.VILLAGE).isValid();
        if (nowInVillage && !this.insideVillage) {
            remember("we visited a village");
            commentOnEvent("A village. Let's see if anyone here needs a hand.",
                    "People have made a life out here. Worth keeping safe.",
                    "A proper settlement. I hope the walls are stronger than they look.");
        }
        this.insideVillage = nowInVillage;
    }

    /**
     * Drains food gradually, consumes the best available carried food, and applies starvation.
     * All state changes happen on the server so food cannot be duplicated by client prediction.
     */
    private void tickFoodSystem() {
        if (this.eatingCooldownTicks > 0) this.eatingCooldownTicks--;

        int interval = this.getTarget() != null && this.getTarget().isAlive()
                ? COMBAT_FOOD_INTERVAL : PASSIVE_FOOD_INTERVAL;
        this.foodDrainTicks++;
        if (this.foodDrainTicks >= interval) {
            this.foodDrainTicks = 0;
            if (this.foodSaturation > 0.0F) {
                this.foodSaturation = Math.max(0.0F, this.foodSaturation - 1.0F);
            } else if (this.foodLevel > 0) {
                this.foodLevel--;
                inventoryChanged();
            }
        }

        if (this.foodLevel <= 14 && this.eatingCooldownTicks <= 0) {
            consumeCarriedFood();
        }

        if (this.foodLevel <= 5 && !this.warnedLowFood) {
            this.warnedLowFood = true;
            commentOnEvent(
                    "I'm running low on food. We should keep an eye out for supplies.",
                    "A little food would help. I can keep going, though.",
                    "Our rations are getting thin. Keep some food handy."
            );
        } else if (this.foodLevel > 10) {
            this.warnedLowFood = false;
        }

        if (this.foodLevel <= 0) {
            this.starvationTicks++;
            if (this.starvationTicks >= 200) {
                this.starvationTicks = 0;
                this.hurt(this.damageSources().starve(), 1.0F);
            }
        } else {
            this.starvationTicks = 0;
        }
    }

    /** Selects the most nutritious food stack in the companion's carried storage. */
    private void consumeCarriedFood() {
        int bestSlot = -1;
        int bestNutrition = 0;
        float bestSaturation = -1.0F;

        for (int slot = 0; slot < CompanionInventory.STORAGE_SIZE; slot++) {
            ItemStack stack = this.companionInventory.getItem(slot);
            if (stack.isEmpty()) continue;
            net.minecraft.world.food.FoodProperties food = stack.getFoodProperties(this);
            if (food == null || food.getNutrition() <= 0) continue;
            float saturationValue = food.getNutrition() * food.getSaturationModifier();
            if (food.getNutrition() > bestNutrition
                    || (food.getNutrition() == bestNutrition && saturationValue > bestSaturation)) {
                bestSlot = slot;
                bestNutrition = food.getNutrition();
                bestSaturation = saturationValue;
            }
        }

        if (bestSlot < 0) {
            this.eatingCooldownTicks = 200;
            return;
        }

        ItemStack foodStack = this.companionInventory.getItem(bestSlot);
        net.minecraft.world.food.FoodProperties food = foodStack.getFoodProperties(this);
        int nutrition = food.getNutrition();
        float saturation = nutrition * food.getSaturationModifier() * 2.0F;
        ItemStack remaining = foodStack.finishUsingItem(this.level(), this);
        this.companionInventory.setItem(bestSlot, remaining);
        this.foodLevel = Math.min(MAX_FOOD_LEVEL, this.foodLevel + nutrition);
        this.foodSaturation = Math.min(this.foodLevel, this.foodSaturation + saturation);
        this.eatingCooldownTicks = 100;
        this.starvationTicks = 0;
        this.inventoryChanged();
        commentOnEvent(
                "That hit the spot. I'll keep watch while we travel.",
                "Much better. A full stomach makes the road easier.",
                "Good. I was starting to think about the rations."
        );
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, SpawnGroupData spawnGroupData, CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, tag);
        // Fallback binding for /summon, dispensers, etc. The spawn egg binds explicitly.
        if (getOwnerUUID() == null) {
            Player nearest = level.getLevel().getNearestPlayer(this, 12.0D);
            if (nearest != null) {
                bindOwner(nearest);
            }
        }
        return result;
    }

    /** Companion never despawns with distance. */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** He is a fellow adventurer, not livestock. */
    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID owner = getOwnerUUID();
        if (owner != null) {
            tag.putUUID("OwnerUUID", owner);
        }
        if (!this.ownerNameCache.isEmpty()) {
            tag.putString("OwnerName", this.ownerNameCache);
        }
        tag.putString("Command", this.command.name());
        tag.putString("CompanionClass", this.companionClass.name());
        tag.put("Progression", this.progression.save());
        tag.put("Relationship", this.relationship.save());
        tag.putInt("RelationshipInteractionCooldown", this.relationshipInteractionCooldownTicks);
        tag.putString("LastBiomeId", this.lastBiomeId);
        tag.putBoolean("InsideVillage", this.insideVillage);
        tag.put("SkillRuntime", this.skillRuntime.save());
        tag.putBoolean("ProtectFromOwner", isProtectedFromOwner());
        tag.putInt("FoodLevel", this.foodLevel);
        tag.putFloat("FoodSaturation", this.foodSaturation);
        tag.putInt("FoodDrainTicks", this.foodDrainTicks);
        tag.putInt("StarvationTicks", this.starvationTicks);
        tag.putBoolean("WarnedLowFood", this.warnedLowFood);
        ListTag savedMemories = new ListTag();
        for (String memory : this.memories) savedMemories.add(StringTag.valueOf(memory));
        tag.put("Memories", savedMemories);
        this.companionInventory.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.loadedFromSave = true;
        if (tag.hasUUID("OwnerUUID")) {
            setOwnerUUID(tag.getUUID("OwnerUUID"));
        }
        this.ownerNameCache = tag.getString("OwnerName");
        if (tag.contains("CompanionClass")) {
            try {
                this.companionClass = CompanionClass.valueOf(tag.getString("CompanionClass"));
            } catch (IllegalArgumentException ignored) {
                this.companionClass = CompanionClass.UNASSIGNED;
            }
        }
        if (tag.contains("Progression", Tag.TAG_COMPOUND)) {
            this.progression.load(tag.getCompound("Progression"), this.companionClass);
        }
        if (tag.contains("Relationship", Tag.TAG_COMPOUND)) {
            this.relationship.load(tag.getCompound("Relationship"));
        }
        this.relationshipInteractionCooldownTicks = Math.max(0, tag.getInt("RelationshipInteractionCooldown"));
        this.lastBiomeId = tag.getString("LastBiomeId");
        this.insideVillage = tag.getBoolean("InsideVillage");
        if (tag.contains("SkillRuntime", Tag.TAG_COMPOUND)) {
            this.skillRuntime.load(tag.getCompound("SkillRuntime"));
        }
        setProtectedFromOwner(tag.getBoolean("ProtectFromOwner"));
        this.foodLevel = tag.contains("FoodLevel", Tag.TAG_INT)
                ? Math.max(0, Math.min(MAX_FOOD_LEVEL, tag.getInt("FoodLevel"))) : MAX_FOOD_LEVEL;
        this.foodSaturation = tag.contains("FoodSaturation", Tag.TAG_FLOAT)
                ? Math.max(0.0F, Math.min(this.foodLevel, tag.getFloat("FoodSaturation"))) : 5.0F;
        this.foodDrainTicks = Math.max(0, tag.getInt("FoodDrainTicks"));
        this.starvationTicks = Math.max(0, tag.getInt("StarvationTicks"));
        this.warnedLowFood = tag.getBoolean("WarnedLowFood");
        if (tag.contains("Command")) {
            try {
                this.command = Command.valueOf(tag.getString("Command"));
            } catch (IllegalArgumentException ignored) {
                this.command = Command.FOLLOW;
            }
        }
        this.companionInventory.load(tag);
        this.memories.clear();
        ListTag savedMemories = tag.getList("Memories", Tag.TAG_STRING);
        for (int i = 0; i < savedMemories.size() && this.memories.size() < MAX_MEMORIES; i++) {
            this.memories.add(savedMemories.getString(i));
        }
    }

    /** Delivers a short event-specific line through the same dialogue cooldown. */
    public void commentOnEvent(String... lines) {
        if (this.level().isClientSide || !CompanionConfig.DIALOGUE_ENABLED.get()
                || lines == null || lines.length == 0) return;
        Player owner = getOwnerPlayer();
        long now = this.level().getGameTime();
        if (owner == null || !owner.isAlive() || owner.level() != this.level()
                || this.distanceToSqr(owner) > 24.0D * 24.0D || now < this.nextDialogueTick) return;
        String line = lines[this.random.nextInt(lines.length)];
        if (line.equals(this.lastDialogueLine) && lines.length > 1) {
            line = lines[(this.random.nextInt(lines.length - 1) + 1) % lines.length];
        }
        speakLine(line, now);
    }

    /** Stores a short, unique memory. The newest twelve memories survive world saves. */
    public void remember(String memory) {
        if (memory == null || memory.isBlank() || this.memories.contains(memory)) return;
        this.memories.add(memory);
        while (this.memories.size() > MAX_MEMORIES) this.memories.remove(0);
    }

    public List<String> getMemories() {
        return List.copyOf(this.memories);
    }

    /** Requests full-health restoration after the first-tick config pass. */
    public void markForFullHealthRestore() {
        this.restoreFullHealthAfterConfig = true;
    }

    // ------------------------------------------------------------------
    // Interaction and commands
    // ------------------------------------------------------------------

    public Command getCommand() {
        return this.command;
    }

    private void setCommand(Command next) {
        this.command = next;
        this.getNavigation().stop();

        // Clear an existing hunt target when ordered to hold position.
        if (next == Command.STAY) {
            this.setTarget(null);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide) {
            UUID owner = getOwnerUUID();
            if (owner == null) {
                bindOwner(player);
                setCommand(Command.FOLLOW);
            } else if (!owner.equals(player.getUUID())) {
                player.sendSystemMessage(Component.translatable("message.livingcompanion.not_yours",
                        this.ownerNameCache.isEmpty() ? "someone" : this.ownerNameCache));
            } else if (player.isShiftKeyDown()) {
                if (this.relationshipInteractionCooldownTicks <= 0) {
                    addRelationshipExperience(5);
                    this.relationshipInteractionCooldownTicks = 600;
                }
                Command next = switch (this.command) {
                    case FOLLOW -> Command.STAY;
                    case STAY -> Command.DEFEND;
                    case DEFEND -> Command.FOLLOW;
                };
                setCommand(next);
                player.sendSystemMessage(Component.literal("Knight: " + switch (next) {
                    case FOLLOW -> "I'll follow your lead.";
                    case STAY -> "I'll hold this position.";
                    case DEFEND -> "I'll stay close and protect you.";
                }));
            } else if (player instanceof ServerPlayer serverPlayer) {
                if (this.relationshipInteractionCooldownTicks <= 0) {
                    addRelationshipExperience(5);
                    this.relationshipInteractionCooldownTicks = 600;
                }
                NetworkHooks.openScreen(serverPlayer,
                        new SimpleMenuProvider((id, inventory, opener) ->
                                new CompanionInventoryMenu(id, inventory, this),
                                Component.literal("Knight's Inventory")),
                        buffer -> buffer.writeVarInt(this.getId()));
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    /** Transfers a weapon, shield, or armor piece from the player's hand. */
    private boolean tryEquipFromPlayer(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) return false;

        EquipmentSlot slot = null;
        if (held.getItem() instanceof ShieldItem) {
            slot = EquipmentSlot.OFFHAND;
        } else if (held.getItem() instanceof ArmorItem armor) {
            slot = armor.getEquipmentSlot();
        } else if (held.getAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE)) {
            // This also accepts modded weapons that expose normal attack-damage attributes.
            slot = EquipmentSlot.MAINHAND;
        }
        if (slot == null) return false;

        ItemStack previous = this.getItemBySlot(slot);
        if (!previous.isEmpty()) this.spawnAtLocation(previous.copy());
        this.setItemSlot(slot, held.copyWithCount(1));
        if (!player.getAbilities().instabuild) held.shrink(1);
        player.sendSystemMessage(Component.literal("Knight: I'll put this to good use."));
        return true;
    }

    /** Context-aware dialogue with broad pools, memory callbacks, and repetition protection. */
    private void speakOccasionally() {
        if (!CompanionConfig.DIALOGUE_ENABLED.get() || this.getOwnerUUID() == null) return;
        Player owner = getOwnerPlayer();
        if (owner == null || !owner.isAlive() || owner.level() != this.level()) return;
        long now = this.level().getGameTime();
        if (now < nextDialogueTick || this.distanceToSqr(owner) > 24.0D * 24.0D) return;

        List<String> pool;
        if (this.getTarget() != null && this.getTarget().isAlive()) {
            if (this.companionClass == CompanionClass.PALADIN) pool = PALADIN_COMBAT_LINES;
            else if (this.companionClass == CompanionClass.TANK) pool = TANK_COMBAT_LINES;
            else pool = COMBAT_LINES;
        } else if (this.foodLevel <= 5) {
            pool = List.of(
                    "We should find food soon. I'm running on fumes.",
                    "I could use a meal. The next fight may come at a bad time.",
                    "Rations are low. Keep an eye out for something edible."
            );
        } else if (owner.getHealth() <= owner.getMaxHealth() * 0.35F) {
            pool = OWNER_HURT_LINES;
        } else if (this.getHealth() <= this.getMaxHealth() * 0.35F) {
            pool = COMPANION_HURT_LINES;
        } else if (this.level().isRaining() || this.level().isThundering()) {
            pool = WEATHER_LINES;
        } else if (this.level().getDayTime() % 24000L >= 13000L
                || this.level().getDayTime() % 24000L < 1000L) {
            pool = NIGHT_LINES;
        } else if (!this.memories.isEmpty() && this.random.nextInt(5) == 0) {
            String memory = this.memories.get(this.random.nextInt(this.memories.size()));
            String template = MEMORY_LINES.get(this.random.nextInt(MEMORY_LINES.size()));
            speakLine(String.format(template, memory), now);
            return;
        } else if (this.random.nextInt(4) == 0) {
            pool = EXPLORATION_LINES;
        } else if (this.random.nextInt(3) == 0 && this.companionClass == CompanionClass.PALADIN) {
            pool = PALADIN_LINES;
        } else if (this.random.nextInt(3) == 0 && this.companionClass == CompanionClass.TANK) {
            pool = TANK_LINES;
        } else {
            pool = IDLE_LINES;
        }

        speakLine(pool.get(this.random.nextInt(pool.size())), now);
    }

    private void speakLine(String line, long now) {
        if (line == null || line.isBlank() || line.equals(this.lastDialogueLine)) return;
        Player owner = getOwnerPlayer();
        if (owner == null) return;
        String speaker = this.hasCustomName() ? this.getName().getString() : "Knight";
        owner.sendSystemMessage(Component.literal(speaker + ": " + line));
        this.lastDialogueLine = line;
        int cooldown = CompanionConfig.DIALOGUE_COOLDOWN_SECONDS.get();
        this.nextDialogueTick = now + (cooldown + this.random.nextInt(Math.max(1, cooldown / 2 + 1))) * 20L;
    }

    /**
     * Keeps the vanilla swing for fallback attacks.
     *
     * Better Mob Combat bypasses this method when it successfully handles a
     * registered Better Combat weapon through MeleeAttackGoal. When it cannot
     * handle the held item, vanilla combat reaches this method and still needs
     * its ordinary swing animation even if Better Mob Combat is installed.
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        this.swing(InteractionHand.MAIN_HAND);
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity livingTarget) {
            this.skillRuntime.onSuccessfulMeleeHit(livingTarget);
        }
        return hit;
    }

    // ------------------------------------------------------------------
    // Sound (humanoid, not monster)
    // ------------------------------------------------------------------

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }
}
