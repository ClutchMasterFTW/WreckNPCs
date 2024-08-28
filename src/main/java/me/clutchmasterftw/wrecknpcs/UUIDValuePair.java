package me.clutchmasterftw.wrecknpcs;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class UUIDValuePair {
    private UUID uuid;
    private double value;

    public UUIDValuePair(UUID uuid, double value) {
        this.uuid = uuid;
        this.value = value;
    }

    public UUID getUUID() {
        return uuid;
    }

    public double getValue() {
        return value;
    }

    public OfflinePlayer getPlayer() {
        OfflinePlayer player;
        if(Bukkit.getPlayer(uuid) == null) {
            player = Bukkit.getOfflinePlayer(uuid);
        } else {
            player = Bukkit.getPlayer(uuid);
        }

        return player;
    }
}
