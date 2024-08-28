package me.clutchmasterftw.wrecknpcs;

import com.bencodez.votingplugin.VotingPluginMain;
import com.bencodez.votingplugin.topvoter.TopVoterPlayer;
import com.earth2me.essentials.Essentials;
import net.citizensnpcs.Citizens;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.CitizensPlugin;
import net.citizensnpcs.api.event.CitizensEnableEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.SkinTrait;
import net.craftingstore.bukkit.CraftingStoreBukkit;
import net.craftingstore.core.CraftingStore;
import net.craftingstore.core.CraftingStoreAPI;
import net.craftingstore.core.exceptions.CraftingStoreApiException;
import net.craftingstore.core.models.api.ApiTopDonator;
import net.essentialsx.api.v2.services.BalanceTop;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

public final class WreckNPCs extends JavaPlugin implements Listener {
    public static WreckNPCs getPlugin() {
        return plugin;
    }

    private static WreckNPCs plugin;

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;

        Bukkit.getServer().getPluginManager().registerEvents(this, this);

        Logger logger = getLogger();
        logger.info("WreckNPCs has successfully loaded!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onCitizensEnable(CitizensEnableEvent e) {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                NPCRegistry registry = CitizensAPI.getNPCRegistry();
                NPC voteNPC = registry.getById(4);
                NPC balanceNPC = registry.getById(6);
                NPC donatorNPC = registry.getById(5);

                SkinTrait voteSkinTrait = voteNPC.getOrAddTrait(SkinTrait.class);
                SkinTrait balanceSkinTrait = balanceNPC.getOrAddTrait(SkinTrait.class);
                SkinTrait donatorSkinTrait = donatorNPC.getOrAddTrait(SkinTrait.class);

                Essentials essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
                BalanceTop balanceTop = essentials.getBalanceTop();
                CompletableFuture<Void> futureTask = balanceTop.calculateBalanceTopMapAsync();
                //Unfortunately, I cannot use a lambda function here, Citizens doesn't like this :/
//                futureTask.thenAccept(result -> {
                    Map<String, UUIDValuePair> topPlayers = getLeaderStatuses();
                    UUIDValuePair topVoter = topPlayers.get("top-voter");
                    UUIDValuePair topBalance = topPlayers.get("top-balance");
                    UUIDValuePair topDonator = topPlayers.get("top-donator");

//                    voteSkinTrait.setSkinPersistent(topVoter.getUUID().toString(), null, null);
                    voteSkinTrait.setSkinName(topVoter.getPlayer().getName(), true);
                    voteNPC.setName(topVoter.getPlayer().getName() + " - " + ChatColor.BOLD + formatDouble(topVoter.getValue()) + ChatColor.RESET + " Votes");

//                    balanceSkinTrait.setSkinPersistent(topBalance.getUUID().toString(), null, null);
                    balanceSkinTrait.setSkinName(topBalance.getPlayer().getName(), true);
                    balanceNPC.setName(topBalance.getPlayer().getName() + " - " + ChatColor.DARK_GREEN + ChatColor.BOLD + "$" + formatDouble(topBalance.getValue()) + ChatColor.RESET);

//                    donatorSkinTrait.setSkinPersistent(topDonator.getUUID().toString(), null, null);
                    donatorSkinTrait.setSkinName(topDonator.getPlayer().getName(), true);
                    donatorNPC.setName(topDonator.getPlayer().getName() + " - " + ChatColor.AQUA + ChatColor.BOLD + "$" + formatDouble(topDonator.getValue()) + ChatColor.RESET);

//                    respawnNPC(voteNPC);
//                    respawnNPC(balanceNPC);
//                    respawnNPC(donatorNPC);
//                });
            }
        }, 20, 6000);
        // Occurs every 5 mins
    }

    public Map<String, UUIDValuePair> getLeaderStatuses() {
        Logger logger = getLogger();

        UUIDValuePair topVoter = null;
        UUIDValuePair topBalance = null;
        UUIDValuePair topDonator = null;

        Map<String, UUIDValuePair> topPlayers = new HashMap<String, UUIDValuePair>();

        // Top Voter
        VotingPluginMain votingPlugin = (VotingPluginMain) Bukkit.getPluginManager().getPlugin("VotingPlugin");
        LinkedHashMap<TopVoterPlayer, Integer> topVoterLastMonth = votingPlugin.getLastMonthTopVoter();
        if(topVoterLastMonth == null || topVoterLastMonth.isEmpty()) {
            // There were no votes last month, or there was some problem
            topVoter = new UUIDValuePair(Bukkit.getOfflinePlayer("ClutchMasterFTW").getUniqueId(), 0);
        } else {
            for(Map.Entry<TopVoterPlayer, Integer> entry:topVoterLastMonth.entrySet()) {
                topVoter = new UUIDValuePair(entry.getKey().getUuid(), entry.getValue());
                break;
            }
        }

        // Top Balance
        Essentials essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        BalanceTop balanceTop = essentials.getBalanceTop();
        if(balanceTop == null) {
            // There are no registered player with balances on the server, or there was some problem
            topBalance = new UUIDValuePair(Bukkit.getOfflinePlayer("ClutchMasterFTW").getUniqueId(), 0);
        } else {
            Map<UUID, BalanceTop.Entry> balanceTopMap = balanceTop.getBalanceTopCache();

            for(BalanceTop.Entry entry:balanceTopMap.values()) {
                topBalance = new UUIDValuePair(entry.getUuid(), entry.getBalance().doubleValue());
                break;
            }
        }

        // Top Donator
        CraftingStoreBukkit craftingStore = (CraftingStoreBukkit) Bukkit.getPluginManager().getPlugin("CraftingStore");
        CraftingStoreAPI craftingStoreAPI = craftingStore.getCraftingStore().getApi();
        if(craftingStoreAPI == null) {
            logger.severe("There was an issue initializing CraftingStore!");
        }
        try {
            Future<ApiTopDonator[]> futureTopDonators = craftingStoreAPI.getTopDonators();
            ApiTopDonator[] topDonators = futureTopDonators.get();
//            for(ApiTopDonator donator:topDonators) {
//
//            }
            if(topDonators != null && topDonators.length != 0) {
                topDonator = new UUIDValuePair(UUID.fromString(topDonators[0].getUuid()), topDonators[0].getTotal());
            } else {
                // There were no entries.
                topDonator = new UUIDValuePair(Bukkit.getOfflinePlayer("ClutchMasterFTW").getUniqueId(), 0);
            }
        } catch (CraftingStoreApiException | ExecutionException | InterruptedException e) {
            logger.severe(e.toString());
        }

        topPlayers.put("top-voter", topVoter);
        topPlayers.put("top-balance", topBalance);
        topPlayers.put("top-donator", topDonator);

        return topPlayers;
    }

    public void respawnNPC(NPC npc) {
        Location location = npc.getStoredLocation();

        npc.despawn();
        npc.spawn(location);

        getLogger().info("Respawned " + npc.getName() + "!");
    }

    public static String formatDouble(double value) {
        DecimalFormat formatter = new DecimalFormat("#,###.##");
        return formatter.format(value);
    }
}