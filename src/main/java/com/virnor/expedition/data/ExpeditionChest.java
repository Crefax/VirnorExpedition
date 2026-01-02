package com.virnor.expedition.data;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ExpeditionChest {
    
    private final String id;
    private final Location location;
    private ExpeditionState state;
    private UUID ownerUUID;
    private long ownershipExpireTime;
    private long cooldownExpireTime;
    private List<UUID> mobUUIDs;
    private int mobsAlive;
    
    // Claim protection
    private volatile boolean claimLocked = false;
    private volatile long lastClaimTime = 0;
    private static final long CLAIM_COOLDOWN_MS = 3000; // 3 seconds between claims

    public ExpeditionChest(String id, Location location) {
        this.id = id;
        this.location = location;
        this.state = ExpeditionState.READY;
        this.mobUUIDs = new ArrayList<>();
        this.mobsAlive = 0;
    }

    public String getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public ExpeditionState getState() {
        return state;
    }

    public void setState(ExpeditionState state) {
        this.state = state;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public long getOwnershipExpireTime() {
        return ownershipExpireTime;
    }

    public void setOwnershipExpireTime(long ownershipExpireTime) {
        this.ownershipExpireTime = ownershipExpireTime;
    }

    public long getCooldownExpireTime() {
        return cooldownExpireTime;
    }

    public void setCooldownExpireTime(long cooldownExpireTime) {
        this.cooldownExpireTime = cooldownExpireTime;
    }

    public List<UUID> getMobUUIDs() {
        return mobUUIDs;
    }

    public void setMobUUIDs(List<UUID> mobUUIDs) {
        this.mobUUIDs = mobUUIDs;
    }

    public void addMobUUID(UUID uuid) {
        this.mobUUIDs.add(uuid);
    }

    public void removeMobUUID(UUID uuid) {
        this.mobUUIDs.remove(uuid);
    }

    public int getMobsAlive() {
        return mobsAlive;
    }

    public void setMobsAlive(int mobsAlive) {
        this.mobsAlive = mobsAlive;
    }

    public void decrementMobsAlive() {
        this.mobsAlive--;
    }

    public void clearMobs() {
        this.mobUUIDs.clear();
        this.mobsAlive = 0;
    }

    public boolean isOwnershipExpired() {
        return System.currentTimeMillis() > ownershipExpireTime;
    }

    public boolean isCooldownExpired() {
        return System.currentTimeMillis() > cooldownExpireTime;
    }

    public long getRemainingCooldown() {
        return Math.max(0, cooldownExpireTime - System.currentTimeMillis());
    }

    public long getRemainingOwnershipTime() {
        return Math.max(0, ownershipExpireTime - System.currentTimeMillis());
    }
    
    /**
     * Try to acquire claim lock. Returns true if lock acquired.
     */
    public synchronized boolean tryClaimLock() {
        long now = System.currentTimeMillis();
        if (claimLocked || (now - lastClaimTime) < CLAIM_COOLDOWN_MS) {
            return false;
        }
        claimLocked = true;
        return true;
    }
    
    /**
     * Release claim lock and record claim time.
     */
    public synchronized void releaseLock(boolean claimed) {
        if (claimed) {
            lastClaimTime = System.currentTimeMillis();
        }
        claimLocked = false;
    }
    
    /**
     * Check if claiming is allowed (not locked and cooldown passed).
     */
    public boolean canClaim() {
        return !claimLocked && (System.currentTimeMillis() - lastClaimTime) >= CLAIM_COOLDOWN_MS;
    }
}
