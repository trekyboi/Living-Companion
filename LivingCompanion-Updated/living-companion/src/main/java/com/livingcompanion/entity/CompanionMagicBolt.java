package com.livingcompanion.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Server-authoritative physical spell bolt used by the Shaman and Elementalist.
 * The item renderer supplies a compact vanilla-style projectile model; damage
 * is applied only when the projectile actually collides with a hostile mob.
 */
public final class CompanionMagicBolt extends ThrowableItemProjectile {
    private float spellDamage = 4.0F;
    private boolean spirit;

    public CompanionMagicBolt(EntityType<? extends CompanionMagicBolt> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public CompanionMagicBolt(EntityType<? extends CompanionMagicBolt> type, LivingEntity shooter,
                              Level level, float damage, boolean spirit) {
        super(type, shooter, level);
        this.spellDamage = Math.max(0.0F, damage);
        this.spirit = spirit;
        this.setItem(new ItemStack(spirit ? Items.AMETHYST_SHARD : Items.FIRE_CHARGE));
        this.setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.AMETHYST_SHARD;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isRemoved()) return;

        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            if (this.tickCount > 100) {
                this.discard();
                return;
            }
            serverLevel.sendParticles(this.spirit ? ParticleTypes.WITCH : ParticleTypes.ELECTRIC_SPARK,
                    this.getX(), this.getY(), this.getZ(), 2, 0.035D, 0.035D, 0.035D, 0.01D);
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (!(entity instanceof Monster)) return false;
        if (!super.canHitEntity(entity)) return false;

        Entity owner = this.getOwner();
        if (entity == owner) return false;
        if (owner instanceof CompanionEntity shooter && entity instanceof CompanionEntity companion) {
            return shooter.getOwnerUUID() == null
                    || !shooter.getOwnerUUID().equals(companion.getOwnerUUID());
        }
        return true;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide || !(result.getEntity() instanceof LivingEntity target)) return;

        Entity owner = this.getOwner();
        Entity causingEntity = owner == null ? this : owner;
        target.hurt(this.damageSources().indirectMagic(this, causingEntity), this.spellDamage);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(this.spirit ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.FLAME,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    12, 0.28D, 0.32D, 0.28D, 0.04D);
            serverLevel.playSound(null, target.blockPosition(),
                    this.spirit ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.FIRECHARGE_USE,
                    SoundSource.PLAYERS, 0.7F, this.spirit ? 1.3F : 1.5F);
        }
    }
}
