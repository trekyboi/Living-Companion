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
    private int templarWardTicks;
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
        tickSpecializationSkills();
        tickMageSkills();

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

    /** Executes the Mage's automatic spell rotations on the server. */
    private void tickMageSkills() {
        CompanionClass archetype = companion.getCompanionClass();
        if (archetype != CompanionClass.SHAMAN && archetype != CompanionClass.ELEMENTALIST) return;

        LivingEntity owner = companion.getOwnerPlayer();
        LivingEntity target = companion.getTarget();
        boolean validTarget = target != null && target.isAlive()
                && target.level() == companion.level()
                && companion.distanceToSqr(target) <= 324.0D;

        if (archetype == CompanionClass.SHAMAN) {
            int attunement = rank(CompanionSkill.SHAMAN_SPIRIT_ATTUNEMENT);
            if (attunement > 0 && companion.tickCount % 40 == 0
                    && companion.getHealth() < companion.getMaxHealth()) {
                companion.heal(0.35F * attunement);
            }

            int bolt = rank(CompanionSkill.SHAMAN_SPIRIT_BOLT);
            if (bolt > 0 && validTarget && companion.distanceToSqr(target) >= 16.0D
                    && isReady(CompanionSkill.SHAMAN_SPIRIT_BOLT)
                    && startCooldown(CompanionSkill.SHAMAN_SPIRIT_BOLT)) {
                float damage = (2.5F + bolt * 1.5F)
                        + companion.getProgression().getAttribute(CompanionAttribute.WISDOM) * 0.04F;
                castBolt(target, damage, true, SoundEvents.AMETHYST_BLOCK_CHIME, 0.95F);
                battleLine("The spirits answer.", "A little help from beyond.", "Stay close. I can mend this.");
            }

            int tide = rank(CompanionSkill.SHAMAN_RESTORATION_TIDE);
            boolean needsHealing = companion.getHealth() <= companion.getMaxHealth() * 0.72F
                    || (owner != null && owner.isAlive() && owner.getHealth() <= owner.getMaxHealth() * 0.72F
                    && companion.distanceToSqr(owner) <= 196.0D);
            if (tide > 0 && needsHealing && isReady(CompanionSkill.SHAMAN_RESTORATION_TIDE)
                    && startCooldown(CompanionSkill.SHAMAN_RESTORATION_TIDE)) {
                float heal = 3.0F + tide * 2.5F
                        + companion.getProgression().getAttribute(CompanionAttribute.WISDOM) * 0.08F;
                companion.heal(heal);
                if (owner != null && owner.isAlive() && companion.distanceToSqr(owner) <= 196.0D) owner.heal(heal);
                playSpiritRenewal();
                battleLine("Let the old spirits carry some of that pain.", "Breathe. The worst has passed.",
                        "The land still has strength to give.");
            }

            int ultimate = rank(CompanionSkill.SHAMAN_ANCESTRAL_WRATH);
            if (ultimate > 0 && validTarget && companion.distanceToSqr(target) <= 100.0D
                    && isReady(CompanionSkill.SHAMAN_ANCESTRAL_WRATH)
                    && startCooldown(CompanionSkill.SHAMAN_ANCESTRAL_WRATH)) {
                damageNearbyHostiles(7.0F, 6.0D);
                companion.heal(8.0F);
                if (owner != null && owner.isAlive() && companion.distanceToSqr(owner) <= 144.0D) owner.heal(6.0F);
                playSpiritRenewal();
                battleLine("Ancestors, stand with us!");
            }
            return;
        }

        int focus = rank(CompanionSkill.ELEMENTALIST_ELEMENTAL_FOCUS);
        int bolt = rank(CompanionSkill.ELEMENTALIST_ARCANE_BOLT);
        if (bolt > 0 && validTarget && companion.distanceToSqr(target) >= 16.0D
                && isReady(CompanionSkill.ELEMENTALIST_ARCANE_BOLT)
                && startCooldown(CompanionSkill.ELEMENTALIST_ARCANE_BOLT)) {
            float damage = (3.0F + bolt * 1.75F + focus * 0.75F)
                    + companion.getProgression().getAttribute(CompanionAttribute.WISDOM) * 0.05F;
            castBolt(target, damage, false, SoundEvents.FIRECHARGE_USE, 1.1F);
            battleLine("Elemental force, obey!", "A clean channel. A cleaner result.", "Keep them at a distance.");
        }

        int surge = rank(CompanionSkill.ELEMENTALIST_PRIMAL_SURGE);
        if (surge > 0 && validTarget && companion.distanceToSqr(target) <= 100.0D
                && isReady(CompanionSkill.ELEMENTALIST_PRIMAL_SURGE)
                && startCooldown(CompanionSkill.ELEMENTALIST_PRIMAL_SURGE)) {
            damageNearbyHostiles(2.5F + surge * 2.0F + focus, 4.5D);
            playElementalBurst();
        }

        int ultimate = rank(CompanionSkill.ELEMENTALIST_CATACLYSM);
        if (ultimate > 0 && validTarget && companion.distanceToSqr(target) <= 144.0D
                && isReady(CompanionSkill.ELEMENTALIST_CATACLYSM)
                && startCooldown(CompanionSkill.ELEMENTALIST_CATACLYSM)) {
            damageNearbyHostiles(12.0F + focus * 2.0F, 7.0D);
            playElementalBurst();
            battleLine("Let the elements decide this!");
        }
    }

    private void castBolt(LivingEntity target, float damage, boolean spirit,
                          net.minecraft.sounds.SoundEvent sound, float pitch) {
        if (!(companion.level() instanceof ServerLevel level) || !target.isAlive()) return;
        com.livingcompanion.entity.CompanionMagicBolt projectile =
                new com.livingcompanion.entity.CompanionMagicBolt(
                        com.livingcompanion.registry.ModEntities.MAGIC_BOLT.get(), companion, level, damage, spirit);
        double sx = companion.getX();
        double sy = companion.getY() + companion.getBbHeight() * 0.65D;
        double dx = target.getX() - sx;
        double dy = target.getY() + target.getBbHeight() * 0.55D - sy;
        double dz = target.getZ() - companion.getZ();
        projectile.shoot(dx, dy, dz, spirit ? 1.35F : 1.65F, 0.0F);
        if (level.addFreshEntity(projectile)) {
            level.playSound(null, companion.blockPosition(), sound, SoundSource.PLAYERS, 0.8F, pitch);
        }
    }

    private void playSpiritRenewal() {
        if (!(companion.level() instanceof ServerLevel level)) return;
        level.sendParticles(ParticleTypes.HEART, companion.getX(), companion.getY() + 1.0D,
                companion.getZ(), 12, 0.6D, 0.7D, 0.6D, 0.02D);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, companion.getX(), companion.getY() + 1.0D,
                companion.getZ(), 28, 0.7D, 0.8D, 0.7D, 0.08D);
        level.sendParticles(ParticleTypes.ENCHANT, companion.getX(), companion.getY() + 1.0D,
                companion.getZ(), 18, 0.45D, 0.65D, 0.45D, 0.4D);
        level.playSound(null, companion.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.9F, 1.35F);
    }

    private void playElementalBurst() {
        if (!(companion.level() instanceof ServerLevel level)) return;
        level.sendParticles(ParticleTypes.FLAME, companion.getX(), companion.getY() + 0.8D,
                companion.getZ(), 48, 1.4D, 0.65D, 1.4D, 0.08D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, companion.getX(), companion.getY() + 0.8D,
                companion.getZ(), 28, 1.2D, 0.75D, 1.2D, 0.12D);
        level.sendParticles(ParticleTypes.CRIT, companion.getX(), companion.getY() + 0.8D,
                companion.getZ(), 24, 1.0D, 0.6D, 1.0D, 0.1D);
        level.playSound(null, companion.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.PLAYERS, 0.65F, 1.5F);
    }

    /**
     * Executes the specialization branch actives and ultimates automatically.
     * This keeps the companion self-sufficient while making every branch skill
     * meaningful without adding another hotbar or command layer.
     */
    private void tickSpecializationSkills() {
        CompanionSpecialization specialization = companion.getProgression().getSpecialization();
        LivingEntity target = companion.getTarget();
        boolean inCombat = target != null && target.isAlive()
                && companion.distanceToSqr(target) <= 144.0D;

        if (specialization == CompanionSpecialization.PALADIN_WARDEN) {
            int active = rank(CompanionSkill.PALADIN_WARDEN_SANCTUARY_PULSE);
            if (active > 0 && inCombat && isReady(CompanionSkill.PALADIN_WARDEN_SANCTUARY_PULSE)) {
                if (startCooldown(CompanionSkill.PALADIN_WARDEN_SANCTUARY_PULSE)) {
                    float heal = 1.5F * active;
                    companion.heal(heal);
                    LivingEntity owner = companion.getOwnerPlayer();
                    if (owner != null && owner.isAlive() && companion.distanceToSqr(owner) <= 144.0D) owner.heal(heal);
                    divineProtectionTicks = Math.max(divineProtectionTicks, 30 + active * 20);
                    playPrayerEffect();
                }
            }
            if (companion.getHealth() <= companion.getMaxHealth() * 0.30F
                    && rank(CompanionSkill.PALADIN_WARDEN_CIRCLE_OF_RENEWAL) > 0
                    && isReady(CompanionSkill.PALADIN_WARDEN_CIRCLE_OF_RENEWAL)
                    && startCooldown(CompanionSkill.PALADIN_WARDEN_CIRCLE_OF_RENEWAL)) {
                companion.heal(14.0F);
                LivingEntity owner = companion.getOwnerPlayer();
                if (owner != null && owner.isAlive() && companion.distanceToSqr(owner) <= 144.0D) owner.heal(10.0F);
                aegisTicks = Math.max(aegisTicks, 100);
                playAegisEffect();
            }
        } else if (specialization == CompanionSpecialization.PALADIN_CRUSADER) {
            int active = rank(CompanionSkill.PALADIN_CRUSADER_RADIANT_ASSAULT);
            if (active > 0 && inCombat && isReady(CompanionSkill.PALADIN_CRUSADER_RADIANT_ASSAULT)
                    && startCooldown(CompanionSkill.PALADIN_CRUSADER_RADIANT_ASSAULT)) {
                target.hurt(companion.damageSources().mobAttack(companion), 2.5F * active);
                holyStrikeVisualTicks = 10;
                if (companion.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.7D, target.getZ(),
                            10 + active * 4, 0.25D, 0.3D, 0.25D, 0.02D);
                    level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
                            SoundSource.PLAYERS, 0.9F, 1.25F);
                }
            }
            if (companion.getHealth() <= companion.getMaxHealth() * 0.30F
                    && rank(CompanionSkill.PALADIN_CRUSADER_JUDGMENT_OF_DAWN) > 0
                    && isReady(CompanionSkill.PALADIN_CRUSADER_JUDGMENT_OF_DAWN)
                    && startCooldown(CompanionSkill.PALADIN_CRUSADER_JUDGMENT_OF_DAWN)) {
                damageNearbyHostiles(8.0F, 7.0D);
                companion.heal(6.0F);
                playAegisEffect();
            }
        } else if (specialization == CompanionSpecialization.PALADIN_TEMPLAR) {
            int active = rank(CompanionSkill.PALADIN_TEMPLAR_AEGIS_LINK);
            LivingEntity owner = companion.getOwnerPlayer();
            if (active > 0 && owner != null && owner.isAlive()
                    && owner.getHealth() <= owner.getMaxHealth() * 0.75F
                    && companion.distanceToSqr(owner) <= 144.0D
                    && isReady(CompanionSkill.PALADIN_TEMPLAR_AEGIS_LINK)
                    && startCooldown(CompanionSkill.PALADIN_TEMPLAR_AEGIS_LINK)) {
                templarWardTicks = Math.max(templarWardTicks, 80 + active * 30);
                owner.heal(2.0F * active);
                playDivineWardEffect();
            }
            if (companion.getHealth() <= companion.getMaxHealth() * 0.30F
                    && rank(CompanionSkill.PALADIN_TEMPLAR_HOLY_STANDARD) > 0
                    && isReady(CompanionSkill.PALADIN_TEMPLAR_HOLY_STANDARD)
                    && startCooldown(CompanionSkill.PALADIN_TEMPLAR_HOLY_STANDARD)) {
                templarWardTicks = Math.max(templarWardTicks, 300);
                divineProtectionTicks = Math.max(divineProtectionTicks, 100);
                if (owner != null && owner.isAlive() && companion.distanceToSqr(owner) <= 144.0D) owner.heal(10.0F);
                playAegisEffect();
            }
        } else if (specialization == CompanionSpecialization.TANK_BULWARK) {
            int active = rank(CompanionSkill.TANK_BULWARK_FORTRESS_STANCE);
            if (active > 0 && inCombat && companion.getHealth() <= companion.getMaxHealth() * 0.75F
                    && isReady(CompanionSkill.TANK_BULWARK_FORTRESS_STANCE)
                    && startCooldown(CompanionSkill.TANK_BULWARK_FORTRESS_STANCE)) {
                shieldWallTicks = Math.max(shieldWallTicks, 50 + active * 25);
                tauntRange = 8.0D + active;
                tauntTicksRemaining = Math.max(tauntTicksRemaining, 50 + active * 20);
                applyTauntTargets();
                playShieldWallEffect();
            }
            if (companion.getHealth() <= companion.getMaxHealth() * 0.30F
                    && rank(CompanionSkill.TANK_BULWARK_UNBREAKABLE_LINE) > 0
                    && isReady(CompanionSkill.TANK_BULWARK_UNBREAKABLE_LINE)
                    && startCooldown(CompanionSkill.TANK_BULWARK_UNBREAKABLE_LINE)) {
                lastBastionTicks = Math.max(lastBastionTicks, 240);
                shieldWallTicks = Math.max(shieldWallTicks, 160);
                tauntRange = 12.0D;
                tauntTicksRemaining = Math.max(tauntTicksRemaining, 180);
                applyTauntTargets();
                playLastBastionEffect();
            }
        } else if (specialization == CompanionSpecialization.TANK_VANGUARD) {
            int active = rank(CompanionSkill.TANK_VANGUARD_WARCRY);
            if (active > 0 && inCombat && isReady(CompanionSkill.TANK_VANGUARD_WARCRY)
                    && startCooldown(CompanionSkill.TANK_VANGUARD_WARCRY)) {
                damageNearbyHostiles(1.5F * active, 6.0D);
                tauntRange = 8.0D + active * 2.0D;
                tauntTicksRemaining = Math.max(tauntTicksRemaining, 60 + active * 20);
                applyTauntTargets();
                playDefensiveEffect(ParticleTypes.ANGRY_VILLAGER, SoundEvents.SHIELD_BLOCK);
            }
            if (companion.getHealth() <= companion.getMaxHealth() * 0.30F
                    && rank(CompanionSkill.TANK_VANGUARD_WARPATH) > 0
                    && isReady(CompanionSkill.TANK_VANGUARD_WARPATH)
                    && startCooldown(CompanionSkill.TANK_VANGUARD_WARPATH)) {
                damageNearbyHostiles(7.0F, 9.0D);
                tauntRange = 12.0D;
                tauntTicksRemaining = Math.max(tauntTicksRemaining, 220);
                applyTauntTargets();
                companion.heal(7.0F);
                playLastBastionEffect();
            }
        } else if (specialization == CompanionSpecialization.TANK_JUGGERNAUT) {
            int active = rank(CompanionSkill.TANK_JUGGERNAUT_CRUSHING_BLOW);
            if (active > 0 && inCombat && isReady(CompanionSkill.TANK_JUGGERNAUT_CRUSHING_BLOW)
                    && startCooldown(CompanionSkill.TANK_JUGGERNAUT_CRUSHING_BLOW)) {
                target.hurt(companion.damageSources().mobAttack(companion), 3.0F * active);
                companion.heal(1.0F * active);
                if (companion.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 0.7D, target.getZ(),
                            12 + active * 4, 0.3D, 0.3D, 0.3D, 0.08D);
                }
            }
            if (companion.getHealth() <= companion.getMaxHealth() * 0.30F
                    && rank(CompanionSkill.TANK_JUGGERNAUT_UNSTOPPABLE_RAMPAGE) > 0
                    && isReady(CompanionSkill.TANK_JUGGERNAUT_UNSTOPPABLE_RAMPAGE)
                    && startCooldown(CompanionSkill.TANK_JUGGERNAUT_UNSTOPPABLE_RAMPAGE)) {
                damageNearbyHostiles(8.0F, 8.0D);
                companion.heal(12.0F);
                lastBastionTicks = Math.max(lastBastionTicks, 100);
                playLastBastionEffect();
            }
        }
    }

    private void damageNearbyHostiles(float damage, double radius) {
        if (!(companion.level() instanceof ServerLevel level)) return;
        for (Monster hostile : level.getEntitiesOfClass(Monster.class,
                companion.getBoundingBox().inflate(radius), Monster::isAlive)) {
            hostile.hurt(companion.damageSources().mobAttack(companion), damage);
        }
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
        if (templarWardTicks > 0) templarWardTicks--;
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
            if (templarWardTicks > 0) amount *= 0.70F;
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
        tag.putInt("TemplarWardTicks", templarWardTicks);
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
        templarWardTicks = Math.max(0, Math.min(300, tag.getInt("TemplarWardTicks")));
        lastBattleLine = tag.getString("LastBattleLine");
    }
}
