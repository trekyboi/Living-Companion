package com.livingcompanion.entity;

import com.livingcompanion.progression.CompanionAttribute;

import com.livingcompanion.progression.CompanionClass;
import com.livingcompanion.progression.CompanionSkill;
import com.livingcompanion.progression.CompanionSpecialization;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Server-authoritative execution state for companion skills.
 *
 * <p>Cooldowns and short-lived skill states are persisted with the companion.
 * Skill ranks remain owned by CompanionProgression.</p>
 */
public final class CompanionSkillRuntime {
    private final CompanionEntity companion;
    private final Map<String, Integer> cooldowns = new HashMap<>();
    private int tauntTicksRemaining;
    private double tauntRange;
    private int divineProtectionTicks;
    private int shieldWallTicks;
    private int prayerTicks;
    private int aegisTicks;
    private int lastBastionTicks;
    private int interceptVisualTicks;
    private int holyStrikeVisualTicks;
    private long nextBattleLineTick;
    private String lastBattleLine = "";

    CompanionSkillRuntime(CompanionEntity companion) {
        this.companion = companion;
    }

    void tick() {
        if (companion.level().isClientSide) return;

        Iterator<Map.Entry<String, Integer>> iterator = cooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) iterator.remove();
            else entry.setValue(remaining);
        }

        tickDurations();

        if (companion.tickCount % 20 == 0) {
            int graceRank = rank(CompanionSkill.PALADIN_DIVINE_GRACE);
            if (companion.getCompanionClass() == CompanionClass.PALADIN && graceRank > 0
                    && companion.getHealth() < companion.getMaxHealth()) {
                companion.heal(0.25F * graceRank);
            }
        }

        int divineRank = rank(CompanionSkill.PALADIN_DIVINE_PROTECTION);
        if (companion.getCompanionClass() == CompanionClass.PALADIN && divineRank > 0
                && companion.getHealth() <= companion.getMaxHealth() * 0.55F
                && isReady(CompanionSkill.PALADIN_DIVINE_PROTECTION)) {
            divineProtectionTicks = 60 + divineRank * 40
                    + (companion.getProgression().getSpecialization() == CompanionSpecialization.PALADIN_WARDEN ? 40 : 0);
            startCooldown(CompanionSkill.PALADIN_DIVINE_PROTECTION);
            playDivineWardEffect(); battleLine("Behind me. This is where the armor is.", "Please stop standing where the arrows go.", "I've got this. Probably.", "I was hoping to avoid paperwork today.", "Everyone still attached to their limbs? Good.", "I can hold this. Keep moving.", "That was close. For you, mostly.");
        }

        int shieldRank = rank(CompanionSkill.TANK_SHIELD_WALL);
        if (companion.getCompanionClass() == CompanionClass.TANK && shieldRank > 0
                && companion.getHealth() <= companion.getMaxHealth() * 0.45F
                && isReady(CompanionSkill.TANK_SHIELD_WALL)) {
            shieldWallTicks = 60 + shieldRank * 40
                    + (companion.getProgression().getSpecialization() == CompanionSpecialization.TANK_BULWARK ? 40 : 0);
            startCooldown(CompanionSkill.TANK_SHIELD_WALL);
            playShieldWallEffect(); battleLine("I've had worse. I think.", "That sounded expensive. For you.", "I'll add that to the list of things that didn't work.", "Still standing. Excellent start.", "Was that meant to hurt?", "I would appreciate a quieter opponent.", "Armor review: still worth it.");
        }

        int prayerRank = rank(CompanionSkill.PALADIN_PRAYER);
        if (companion.getCompanionClass() == CompanionClass.PALADIN && prayerRank > 0
                && isReady(CompanionSkill.PALADIN_PRAYER)) {
            LivingEntity owner = companion.getOwnerPlayer();
            boolean needsHealing = companion.getHealth() <= companion.getMaxHealth() * 0.70F
                    || (owner != null && owner.isAlive() && owner.getHealth() <= owner.getMaxHealth() * 0.65F
                    && companion.distanceToSqr(owner) <= 144.0D);
            if (needsHealing) activatePrayer(prayerRank);
        }

        int aegisRank = rank(CompanionSkill.PALADIN_AEGIS_OF_DAWN);
        if (companion.getCompanionClass() == CompanionClass.PALADIN && aegisRank > 0
                && companion.getHealth() <= companion.getMaxHealth() * 0.20F
                && aegisTicks <= 0 && isReady(CompanionSkill.PALADIN_AEGIS_OF_DAWN)) {
            aegisTicks = 160
                    + (companion.getProgression().getSpecialization() == CompanionSpecialization.PALADIN_WARDEN ? 40 : 0);
            startCooldown(CompanionSkill.PALADIN_AEGIS_OF_DAWN);
            playAegisEffect(); battleLine("Still alive. That's inconvenient for them.", "I am beginning to find this personal.", "I've got at least one more fight in me. Maybe two.", "Right. That got my attention.", "I was saving that for a worse day.", "You'll have to be more convincing than that.", "I am officially done being patient.");
        }

        int bastionRank = rank(CompanionSkill.TANK_LAST_BASTION);
        if (companion.getCompanionClass() == CompanionClass.TANK && bastionRank > 0
                && companion.getHealth() <= companion.getMaxHealth() * 0.20F
                && lastBastionTicks <= 0 && isReady(CompanionSkill.TANK_LAST_BASTION)) {
            lastBastionTicks = 160
                    + (companion.getProgression().getSpecialization() == CompanionSpecialization.TANK_BULWARK ? 40 : 0);
            startCooldown(CompanionSkill.TANK_LAST_BASTION);
            playLastBastionEffect(); battleLine("No.", "You'll have to go through me. Sorry about the queue.", "I was standing here first.", "I'm afraid that's a very short conversation.", "You can try. I've got time.", "This is becoming a habit.", "Find someone else. I'm occupied.");
            this.tauntRange = 12.0D;
            this.tauntTicksRemaining = 160;
            applyTauntTargets();
        }

        // Tank Taunt is an automatic combat skill. It activates when the Tank
        // has engaged a hostile target and is ready, then holds nearby aggro.
        if (companion.getCompanionClass() == CompanionClass.TANK
                && rank(CompanionSkill.TANK_TAUNT) > 0
                && companion.getTarget() != null
                && companion.getTarget().isAlive()
                && isReady(CompanionSkill.TANK_TAUNT)) {
            activateTaunt(rank(CompanionSkill.TANK_TAUNT));
        }

        if (tauntTicksRemaining > 0) {
            // Reassert aggro periodically instead of scanning every tick.
            if (tauntTicksRemaining % 10 == 0) applyTauntTargets();
            tauntTicksRemaining--;
        }

        syncVisualFlags();
    }

    /** Publishes persistent skill states to the client renderer through entity data. */
    private void syncVisualFlags() {
        int flags = 0;
        if (divineProtectionTicks > 0) flags |= 1;
        if (shieldWallTicks > 0) flags |= 1 << 1;
        if (prayerTicks > 0) flags |= 1 << 2;
        if (aegisTicks > 0) flags |= 1 << 3;
        if (lastBastionTicks > 0) flags |= 1 << 4;
        if (tauntTicksRemaining > 0) flags |= 1 << 5;
        if (interceptVisualTicks > 0) flags |= 1 << 6;
        if (holyStrikeVisualTicks > 0) flags |= 1 << 7;
        if (companion.getCompanionClass() == CompanionClass.TANK
                && rank(CompanionSkill.TANK_UNYIELDING) > 0) flags |= 1 << 8;
        if (companion.getCompanionClass() == CompanionClass.PALADIN
                && rank(CompanionSkill.PALADIN_DIVINE_GRACE) > 0) flags |= 1 << 9;
        companion.setSkillVisualFlags(flags);
    }

    void onSuccessfulMeleeHit(LivingEntity target) {
        if (!target.isAlive()) {
            battleLine("Well, that's sorted.", "One less problem.", "I expected more trouble, honestly.", "Right. Who's next?", "That went about as expected.", "Could've been worse. For me, anyway.", "I should probably check the armor.");
        }
        if (companion.getCompanionClass() == CompanionClass.TANK) {
            int furyRank = rank(CompanionSkill.TANK_VANGUARD_FURY);
            if (companion.getProgression().getSpecialization() == CompanionSpecialization.TANK_VANGUARD
                    && furyRank > 0 && target.isAlive()) {
                target.hurt(companion.damageSources().mobAttack(companion), 0.5F * furyRank);
            }
            int momentumRank = rank(CompanionSkill.TANK_JUGGERNAUT_MOMENTUM);
            if (companion.getProgression().getSpecialization() == CompanionSpecialization.TANK_JUGGERNAUT
                    && momentumRank > 0 && target.isAlive()) {
                target.hurt(companion.damageSources().mobAttack(companion), 1.0F * momentumRank);
            }
            return;
        }
        if (companion.getCompanionClass() != CompanionClass.PALADIN) return;
        int rank = rank(CompanionSkill.PALADIN_HOLY_STRIKE);
        if (rank <= 0 || !isReady(CompanionSkill.PALADIN_HOLY_STRIKE)) return;

        if (!startCooldown(CompanionSkill.PALADIN_HOLY_STRIKE)) return;
        float bonusDamage = 2.0F * rank;
        if (companion.getProgression().getSpecialization() == CompanionSpecialization.PALADIN_CRUSADER) {
            bonusDamage *= 1.30F;
        }
        int judgmentRank = companion.getProgression().getSkillRank(CompanionSkill.PALADIN_JUDGMENT.getId());
        if (judgmentRank > 0) bonusDamage += 0.75F * judgmentRank;
        boolean damaged = target.hurt(companion.damageSources().mobAttack(companion), bonusDamage);
        if (damaged && companion.level() instanceof ServerLevel serverLevel) {
            holyStrikeVisualTicks = 10;
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.65D, target.getZ(),
                    8 + rank * 3, 0.25D, 0.25D, 0.25D, 0.08D);
            serverLevel.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(), 7 + rank * 2, 0.18D, 0.3D, 0.18D, 0.015D);
            serverLevel.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + target.getBbHeight() * 0.7D, target.getZ(), 14 + rank * 4, 0.35D, 0.45D, 0.35D, 0.5D);
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
                    SoundSource.PLAYERS, 0.95F, 0.85F);
            battleLine("That one had some weight behind it.", "A little encouragement from above. And my sword.", "There. Feel better? Good.", "I meant to do that.", "That should leave a mark.", "Clean hit. I'll take it.", "Well, that settled that.");
        }
    }

    private void tickDurations() {
        if (divineProtectionTicks > 0) divineProtectionTicks--;
        if (shieldWallTicks > 0) shieldWallTicks--;
        if (prayerTicks > 0) prayerTicks--;
        if (aegisTicks > 0) aegisTicks--;
        if (lastBastionTicks > 0) lastBastionTicks--;
        if (interceptVisualTicks > 0) interceptVisualTicks--;
        if (holyStrikeVisualTicks > 0) holyStrikeVisualTicks--;
    }

    private void activatePrayer(int rank) {
        if (!startCooldown(CompanionSkill.PALADIN_PRAYER)) return;
        float amount = (3.0F + rank * 2.0F) * (1.0F + companion.getProgression().getAttribute(CompanionAttribute.WISDOM) * 0.01F);
        int sanctuaryRank = rank(CompanionSkill.PALADIN_SANCTUARY);
        if (companion.getProgression().getSpecialization() == CompanionSpecialization.PALADIN_WARDEN
                && sanctuaryRank > 0) amount *= 1.0F + 0.15F * sanctuaryRank;
        companion.heal(amount);
        LivingEntity owner = companion.getOwnerPlayer();
        if (owner != null && owner.isAlive() && companion.distanceToSqr(owner) <= 144.0D) {
            owner.heal(amount);
        }
        prayerTicks = 20;
        playPrayerEffect(); battleLine("There. Try not to undo that immediately.", "You're welcome. Try the dodging thing next time.", "Patched up. Your survival instincts are still a work in progress.", "Hold still. You're making this difficult.", "That should keep you going.", "Please make the next injury less dramatic.", "There. Back to being difficult.");
    }

    private void battleLine(String... lines) {
        if (!(companion.level() instanceof ServerLevel level) || lines.length == 0) return;
        long now = level.getGameTime();
        if (now < nextBattleLineTick) return;
        LivingEntity owner = companion.getOwnerPlayer();
        if (owner == null || !owner.isAlive() || companion.distanceToSqr(owner) > 576.0D) return;
        String line;
        if (lines.length == 1) {
            line = lines[0];
        } else {
            int index = ThreadLocalRandom.current().nextInt(lines.length);
            line = lines[index];
            if (line.equals(lastBattleLine)) {
                line = lines[(index + 1 + ThreadLocalRandom.current().nextInt(lines.length - 1)) % lines.length];
            }
        }
        lastBattleLine = line;
        owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(companion.getName().getString() + ": " + line));
        nextBattleLineTick = now + 180L;
    }

    private void playDivineWardEffect() {
        if (!(companion.level() instanceof ServerLevel level)) return;
        double x=companion.getX(), y=companion.getY()+0.5D, z=companion.getZ();
        for(int i=0;i<32;i++){ double a=Math.PI*2*i/32.0D; level.sendParticles(ParticleTypes.END_ROD,x+Math.cos(a)*1.15D,y,z+Math.sin(a)*1.15D,1,0,0.3D,0,0.01D); }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,x,y+0.5D,z,24,0.55D,0.8D,0.55D,0.08D);
        level.playSound(null,companion.blockPosition(),SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.PLAYERS,1.0F,1.25F);
    }
    private void playShieldWallEffect() {
        if (!(companion.level() instanceof ServerLevel level)) return;
        double x=companion.getX(), y=companion.getY()+0.8D, z=companion.getZ();
        level.sendParticles(ParticleTypes.CLOUD,x,y,z,24,0.7D,0.2D,0.7D,0.02D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,x,y,z,18,0.55D,0.8D,0.55D,0.04D);
        level.playSound(null,companion.blockPosition(),SoundEvents.SHIELD_BLOCK,SoundSource.PLAYERS,1.0F,0.8F);
    }
    private void playPrayerEffect() {
        if (!(companion.level() instanceof ServerLevel level)) return;
        double x=companion.getX(), y=companion.getY()+1.0D, z=companion.getZ();
        level.sendParticles(ParticleTypes.HEART,x,y,z,10,0.55D,0.65D,0.55D,0.02D);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,x,y,z,24,0.7D,0.8D,0.7D,0.08D);
        level.sendParticles(ParticleTypes.END_ROD,x,y+0.3D,z,14,0.35D,0.7D,0.35D,0.025D);
        level.playSound(null,companion.blockPosition(),SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.PLAYERS,0.9F,1.5F);
    }
    private void playAegisEffect() {
        if (!(companion.level() instanceof ServerLevel level)) return;
        double x=companion.getX(), y=companion.getY()+1.0D, z=companion.getZ();
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,x,y,z,45,0.7D,1.0D,0.7D,0.12D);
        level.sendParticles(ParticleTypes.END_ROD,x,y,z,30,0.85D,1.1D,0.85D,0.06D);
        level.playSound(null,companion.blockPosition(),SoundEvents.TOTEM_USE,SoundSource.PLAYERS,1.0F,1.0F);
    }
    private void playLastBastionEffect() {
        if (!(companion.level() instanceof ServerLevel level)) return;
        double x=companion.getX(), y=companion.getY()+0.25D, z=companion.getZ();
        level.sendParticles(ParticleTypes.CRIT,x,y+0.8D,z,32,0.75D,0.9D,0.75D,0.12D);
        level.sendParticles(ParticleTypes.CLOUD,x,y,z,28,0.75D,0.15D,0.75D,0.03D);
        level.playSound(null,companion.blockPosition(),SoundEvents.SHIELD_BLOCK,SoundSource.PLAYERS,1.0F,0.55F);
    }
    private void playInterceptEffect() {
        if (!(companion.level() instanceof ServerLevel level)) return;
        double x=companion.getX(), y=companion.getY()+1.0D, z=companion.getZ();
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,x,y,z,1,0,0,0,0);
        level.sendParticles(ParticleTypes.CRIT,x,y,z,18,0.45D,0.65D,0.45D,0.1D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,x,y,z,14,0.45D,0.6D,0.45D,0.08D);
        level.playSound(null,companion.blockPosition(),SoundEvents.SHIELD_BLOCK,SoundSource.PLAYERS,1.0F,1.2F);
    }
    private void playDefensiveEffect(net.minecraft.core.particles.ParticleOptions particle,
                                     net.minecraft.sounds.SoundEvent sound) {
        if (companion.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle, companion.getX(), companion.getY() + 1.0D,
                    companion.getZ(), 12, 0.45D, 0.65D, 0.45D, 0.03D);
            serverLevel.playSound(null, companion.blockPosition(), sound,
                    SoundSource.PLAYERS, 0.8F, 1.0F);
        }
    }

    float mitigateIncomingDamage(float amount) {
        float multiplier = 1.0F;
        if (divineProtectionTicks > 0) multiplier *= 0.65F;
        if (shieldWallTicks > 0) multiplier *= 0.50F;
        if (aegisTicks > 0) multiplier *= 0.35F;
        if (lastBastionTicks > 0) multiplier *= 0.25F;
        int unyieldingRank = rank(CompanionSkill.TANK_UNYIELDING);
        if (companion.getCompanionClass() == CompanionClass.TANK && unyieldingRank > 0) {
            float missingHealth = 1.0F - companion.getHealth() / companion.getMaxHealth();
            multiplier *= 1.0F - Math.min(0.30F, missingHealth * 0.10F * unyieldingRank);
        }
        return amount * multiplier;
    }

    float mitigateOwnerDamage(net.minecraft.server.level.ServerPlayer owner,
                              net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!companion.isAlive() || companion.distanceToSqr(owner) > 144.0D) return amount;

        if (companion.getCompanionClass() == CompanionClass.PALADIN) {
            if (divineProtectionTicks > 0) amount *= 0.65F;
            if (aegisTicks > 0) amount *= 0.35F;
            int vigilRank = rank(CompanionSkill.PALADIN_TEMPLAR_VIGIL);
            if (companion.getProgression().getSpecialization() == CompanionSpecialization.PALADIN_TEMPLAR
                    && vigilRank > 0) {
                amount *= 1.0F - Math.min(0.15F, 0.05F * vigilRank);
            }
            return amount;
        }

        int bulwarkRank = rank(CompanionSkill.TANK_BULWARK_OATH);
        if (companion.getCompanionClass() == CompanionClass.TANK
                && companion.getProgression().getSpecialization() == CompanionSpecialization.TANK_BULWARK
                && bulwarkRank > 0) {
            amount *= 1.0F - Math.min(0.15F, 0.05F * bulwarkRank);
        }

        int rank = rank(CompanionSkill.TANK_INTERCEPT);
        if (companion.getCompanionClass() != CompanionClass.TANK || rank <= 0
                || !isReady(CompanionSkill.TANK_INTERCEPT)) return amount;

        // Intercept is a bodyguard ability. It only redirects damage caused by
        // another living attacker; falls, fire, drowning, starvation, and self-damage
        // must remain the owner's responsibility.
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker) || livingAttacker == owner) {
            return amount;
        }
        if (!startCooldown(CompanionSkill.TANK_INTERCEPT)) return amount;

        companion.setTarget(livingAttacker);
        interceptVisualTicks = 14;
        float redirected = amount * (0.20F + 0.10F * rank);
        companion.hurt(source, redirected);
        playInterceptEffect(); battleLine("Pick on someone your own size. Actually, pick on me.", "I saw that. Bad choice.", "The person behind me is off limits.", "Eyes on me. That's the arrangement.", "You'll have to get through the armor first.", "I can take it. They can't.", "Try me. I'm already here.");
        return amount - redirected;
    }

    private void activateTaunt(int rank) {
        if (!startCooldown(CompanionSkill.TANK_TAUNT)) return;
        this.tauntRange = 6.0D + rank * 2.0D;
        this.tauntTicksRemaining = 40 + rank * 20;
        if (companion.getProgression().getSpecialization() == CompanionSpecialization.TANK_VANGUARD) {
            this.tauntRange += 2.0D;
            this.tauntTicksRemaining += 20;
        }
        applyTauntTargets();
        if (companion.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    companion.getX(), companion.getY() + 1.4D, companion.getZ(),
                    8 + rank * 2, 0.65D, 0.45D, 0.65D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.CRIT, companion.getX(), companion.getY() + 1.0D, companion.getZ(), 20 + rank * 4, 0.75D, 0.7D, 0.75D, 0.12D);
            serverLevel.playSound(null, companion.blockPosition(), SoundEvents.SHIELD_BLOCK,
                    SoundSource.PLAYERS, 1.0F, 0.65F);
            battleLine("Oi. I'm over here.", "You. Yes, the ugly one. Come here.", "Let's get this over with. I was enjoying the quiet.", "There's plenty of me to be annoyed at.", "Come on, then. I've got a schedule.", "Stop bothering everyone else.", "I promise I'm much more interesting.");
        }
    }

    private void applyTauntTargets() {
        if (!(companion.level() instanceof ServerLevel serverLevel)) return;
        AABB area = companion.getBoundingBox().inflate(tauntRange);
        for (Monster hostile : serverLevel.getEntitiesOfClass(Monster.class, area,
                Monster::isAlive)) {
            hostile.setTarget(companion);
        }
    }

    public int getCooldownRemaining(CompanionSkill skill) {
        return Math.max(0, cooldowns.getOrDefault(skill.getId(), 0));
    }

    public int getActiveTicksRemaining(CompanionSkill skill) {
        return switch (skill) {
            case PALADIN_DIVINE_PROTECTION -> divineProtectionTicks;
            case TANK_SHIELD_WALL -> shieldWallTicks;
            case PALADIN_PRAYER -> prayerTicks;
            case PALADIN_AEGIS_OF_DAWN -> aegisTicks;
            case TANK_LAST_BASTION -> lastBastionTicks;
            case TANK_TAUNT -> tauntTicksRemaining;
            default -> 0;
        };
    }

    private int rank(CompanionSkill skill) {
        return companion.getProgression().getSkillRank(skill.getId());
    }

    private boolean isReady(CompanionSkill skill) {
        return cooldowns.getOrDefault(skill.getId(), 0) <= 0;
    }

    private boolean startCooldown(CompanionSkill skill) {
        if (!isReady(skill)) return false;
        int wisdom = companion.getProgression().getAttribute(CompanionAttribute.WISDOM);
        int adjustedCooldown = Math.max(1, Math.round(skill.getCooldownTicks() * (1.0F - wisdom * 0.005F)));
        cooldowns.put(skill.getId(), adjustedCooldown);
        companion.inventoryChanged();
        return true;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        CompoundTag savedCooldowns = new CompoundTag();
        cooldowns.forEach(savedCooldowns::putInt);
        tag.put("Cooldowns", savedCooldowns);
        tag.putInt("TauntTicksRemaining", tauntTicksRemaining);
        tag.putDouble("TauntRange", tauntRange);
        tag.putInt("DivineProtectionTicks", divineProtectionTicks);
        tag.putInt("ShieldWallTicks", shieldWallTicks);
        tag.putInt("PrayerTicks", prayerTicks);
        tag.putInt("AegisTicks", aegisTicks);
        tag.putInt("LastBastionTicks", lastBastionTicks);
        tag.putInt("InterceptVisualTicks", interceptVisualTicks);
        tag.putInt("HolyStrikeVisualTicks", holyStrikeVisualTicks);
        tag.putString("LastBattleLine", lastBattleLine);
        return tag;
    }

    void load(CompoundTag tag) {
        cooldowns.clear();
        if (tag.contains("Cooldowns", 10)) {
            CompoundTag savedCooldowns = tag.getCompound("Cooldowns");
            for (String key : savedCooldowns.getAllKeys()) {
                CompanionSkill skill = CompanionSkill.byId(key);
                int remaining = savedCooldowns.getInt(key);
                if (skill != null && remaining > 0) {
                    cooldowns.put(key, Math.min(remaining, skill.getCooldownTicks()));
                }
            }
        }
        tauntTicksRemaining = Math.max(0, Math.min(100, tag.getInt("TauntTicksRemaining")));
        tauntRange = Math.max(0.0D, Math.min(12.0D, tag.getDouble("TauntRange")));
        divineProtectionTicks = Math.max(0, Math.min(180, tag.getInt("DivineProtectionTicks")));
        shieldWallTicks = Math.max(0, Math.min(180, tag.getInt("ShieldWallTicks")));
        prayerTicks = Math.max(0, Math.min(20, tag.getInt("PrayerTicks")));
        aegisTicks = Math.max(0, Math.min(160, tag.getInt("AegisTicks")));
        lastBastionTicks = Math.max(0, Math.min(160, tag.getInt("LastBastionTicks")));
        interceptVisualTicks = Math.max(0, Math.min(14, tag.getInt("InterceptVisualTicks")));
        holyStrikeVisualTicks = Math.max(0, Math.min(10, tag.getInt("HolyStrikeVisualTicks")));
        lastBattleLine = tag.getString("LastBattleLine");
    }
}
