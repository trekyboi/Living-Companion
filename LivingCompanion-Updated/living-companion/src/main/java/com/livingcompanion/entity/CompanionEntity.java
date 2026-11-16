package com.livingcompanion.entity;

import com.livingcompanion.config.CompanionConfig;
import com.livingcompanion.progression.CompanionClass;
import com.livingcompanion.progression.CompanionProgression;
import com.livingcompanion.ai.FollowOwnerGoal;
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
    private final CompanionSkillRuntime skillRuntime = new CompanionSkillRuntime(this);
    private CompanionClass companionClass = CompanionClass.UNASSIGNED;

    public enum Command { FOLLOW, STAY, DEFEND }
    private Command command = Command.FOLLOW;
    private long nextDialogueTick = 0L;
    private final List<String> memories = new ArrayList<>();
    private static final int MAX_MEMORIES = 12;
    private static final List<String> IDLE_LINES = List.of(
            "The road ahead looks quiet. I don't trust it.",
            "Keep your shield close. You never know what is waiting out there.",
            "I've seen worse roads. Usually right before things get interesting.",
            "I'll keep watch. Get some rest if you need it.",
            "A good blade and a steady companion. That's enough for most journeys."
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
            Player owner = getOwnerPlayer();
            if (owner != null) {
                owner.sendSystemMessage(Component.literal(this.getName().getString()
                        + " reached level " + this.progression.getLevel()
                        + "! +" + gained + " skill point" + (gained == 1 ? "" : "s") + "."));
            }
        }
        inventoryChanged();
        return gained;
    }

    /** Rebuilds class/level stats from config bases; health is preserved proportionally. */
    private void applyProgressionAttributes(boolean preserveHealthRatio) {
        float oldHealth = this.getHealth();
        float oldMaxHealth = this.getMaxHealth();
        double levelBonus = this.progression.getLevel() - 1;
        double health = CompanionConfig.MAX_HEALTH.get();
        double attack = CompanionConfig.ATTACK_DAMAGE.get();
        double armor = 0.0D;
        double toughness = 0.0D;
        double knockback = 0.2D;

        switch (this.companionClass) {
            case PALADIN -> {
                health += 8.0D + levelBonus * 0.8D;
                attack += 1.0D + levelBonus * 0.18D;
                armor = 3.0D + levelBonus * 0.08D;
                toughness = 1.0D + levelBonus * 0.04D;
            }
            case TANK -> {
                health += 16.0D + levelBonus * 1.2D;
                attack += levelBonus * 0.12D;
                armor = 6.0D + levelBonus * 0.16D;
                toughness = 2.0D + levelBonus * 0.06D;
                knockback = Math.min(0.85D, 0.5D + levelBonus * 0.006D);
            }
            case UNASSIGNED -> {
                health += levelBonus * 0.5D;
                attack += levelBonus * 0.1D;
            }
        }

        setBase(Attributes.MAX_HEALTH, health);
        setBase(Attributes.ATTACK_DAMAGE, attack);
        setBase(Attributes.ARMOR, armor);
        setBase(Attributes.ARMOR_TOUGHNESS, toughness);
        setBase(Attributes.KNOCKBACK_RESISTANCE, knockback);
        if (preserveHealthRatio && oldMaxHealth > 0.0F) {
            this.setHealth(Math.min((oldHealth / oldMaxHealth) * this.getMaxHealth(), this.getMaxHealth()));
        } else {
            this.setHealth(this.getMaxHealth());
        }
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
            if (this.tickCount % 20 == 0) speakOccasionally();
        }
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
        tag.put("SkillRuntime", this.skillRuntime.save());
        tag.putBoolean("ProtectFromOwner", isProtectedFromOwner());
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
        if (tag.contains("SkillRuntime", Tag.TAG_COMPOUND)) {
            this.skillRuntime.load(tag.getCompound("SkillRuntime"));
        }
        setProtectedFromOwner(tag.getBoolean("ProtectFromOwner"));
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

    /** Cooldown-based contextual dialogue with occasional references to saved event memories. */
    private void speakOccasionally() {
        if (!CompanionConfig.DIALOGUE_ENABLED.get() || this.getOwnerUUID() == null) return;
        Player owner = getOwnerPlayer();
        if (owner == null || !owner.isAlive() || owner.level() != this.level()) return;
        long now = this.level().getGameTime();
        if (now < nextDialogueTick || this.distanceToSqr(owner) > 24.0D * 24.0D) return;

        String line;
        if (owner.getHealth() <= owner.getMaxHealth() * 0.35F) {
            line = "You're looking hurt. Be careful.";
        } else if (this.getTarget() != null) {
            line = "Stay sharp. We've got company.";
        } else if (this.level().isRaining()) {
            line = "This weather is miserable. Keep moving.";
        } else if (this.level().getDayTime() % 24000L >= 13000L) {
            line = "Night is coming. We should watch our surroundings.";
        } else if (!this.memories.isEmpty() && this.random.nextInt(4) == 0) {
            line = "I still remember when " + this.memories.get(this.random.nextInt(this.memories.size())) + ".";
        } else {
            line = IDLE_LINES.get(this.random.nextInt(IDLE_LINES.size()));
        }
        owner.sendSystemMessage(Component.literal("Knight: " + line));
        int cooldown = CompanionConfig.DIALOGUE_COOLDOWN_SECONDS.get();
        nextDialogueTick = now + (cooldown + this.random.nextInt(Math.max(1, cooldown / 2 + 1))) * 20L;
    }

    /**
     * Keeps the vanilla arm swing when Better Mob Combat is absent.
     * Better Mob Combat supplies its own Player Animator attack pose when loaded.
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("bettermobcombat")) {
            this.swing(InteractionHand.MAIN_HAND);
        }
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
