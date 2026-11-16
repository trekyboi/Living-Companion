package com.livingcompanion.entity;

import com.livingcompanion.progression.CompanionClass;
import com.livingcompanion.progression.CompanionSkill;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Server-authoritative execution state for companion skills.
 *
 * <p>Cooldowns and short-lived skill states are persisted with the companion.
 * Skill ranks remain owned by CompanionProgression.</p>
 */
final class CompanionSkillRuntime {
    private final CompanionEntity companion;
    private final Map<String, Integer> cooldowns = new HashMap<>();
    private int tauntTicksRemaining;
    private double tauntRange;

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
            applyTauntTargets();
            tauntTicksRemaining--;
        }
    }

    void onSuccessfulMeleeHit(LivingEntity target) {
        if (companion.getCompanionClass() != CompanionClass.PALADIN) return;
        int rank = rank(CompanionSkill.PALADIN_HOLY_STRIKE);
        if (rank <= 0 || !isReady(CompanionSkill.PALADIN_HOLY_STRIKE)) return;

        if (!startCooldown(CompanionSkill.PALADIN_HOLY_STRIKE)) return;
        float bonusDamage = 2.0F * rank;
        boolean damaged = target.hurt(companion.damageSources().mobAttack(companion), bonusDamage);
        if (damaged && companion.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.65D, target.getZ(),
                    8 + rank * 3, 0.25D, 0.25D, 0.25D, 0.08D);
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
                    SoundSource.PLAYERS, 0.8F, 1.15F);
        }
    }

    private void activateTaunt(int rank) {
        if (!startCooldown(CompanionSkill.TANK_TAUNT)) return;
        this.tauntRange = 6.0D + rank * 2.0D;
        this.tauntTicksRemaining = 40 + rank * 20;
        applyTauntTargets();
        if (companion.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    companion.getX(), companion.getY() + 1.4D, companion.getZ(),
                    8 + rank * 2, 0.65D, 0.45D, 0.65D, 0.0D);
            serverLevel.playSound(null, companion.blockPosition(), SoundEvents.SHIELD_BLOCK,
                    SoundSource.PLAYERS, 0.9F, 0.7F);
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

    private int rank(CompanionSkill skill) {
        return companion.getProgression().getSkillRank(skill.getId());
    }

    private boolean isReady(CompanionSkill skill) {
        return cooldowns.getOrDefault(skill.getId(), 0) <= 0;
    }

    private boolean startCooldown(CompanionSkill skill) {
        if (!isReady(skill)) return false;
        cooldowns.put(skill.getId(), skill.getCooldownTicks());
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
    }
}
