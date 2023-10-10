/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package com.viaversion.viaversion.bukkit.platform;

import com.viaversion.viaversion.ViaVersionPlugin;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.platform.ViaPlatformLoader;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.bukkit.classgenerator.ClassGenerator;
import com.viaversion.viaversion.bukkit.listeners.UpdateListener;
import com.viaversion.viaversion.bukkit.listeners.multiversion.PlayerSneakListener;
import com.viaversion.viaversion.bukkit.listeners.protocol1_15to1_14_4.EntityToggleGlideListener;
import com.viaversion.viaversion.bukkit.listeners.protocol1_9to1_8.ArmorListener;
import com.viaversion.viaversion.bukkit.listeners.protocol1_9to1_8.BlockListener;
import com.viaversion.viaversion.bukkit.listeners.protocol1_9to1_8.DeathListener;
import com.viaversion.viaversion.bukkit.listeners.protocol1_9to1_8.HandItemCache;
import com.viaversion.viaversion.bukkit.listeners.protocol1_9to1_8.PaperPatch;
import com.viaversion.viaversion.bukkit.providers.BukkitBlockConnectionProvider;
import com.viaversion.viaversion.bukkit.providers.BukkitInventoryQuickMoveProvider;
import com.viaversion.viaversion.bukkit.providers.BukkitViaMovementTransmitter;
import com.viaversion.viaversion.protocols.protocol1_12to1_11_1.providers.InventoryQuickMoveProvider;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.blockconnections.ConnectionData;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.blockconnections.providers.BlockConnectionProvider;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.HandItemProvider;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.MovementTransmitterProvider;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class BukkitViaLoader
implements ViaPlatformLoader {
    private final ViaVersionPlugin plugin;
    private final Set<Listener> listeners = new HashSet<Listener>();
    private final Set<BukkitTask> tasks = new HashSet<BukkitTask>();
    private HandItemCache handItemCache;

    public BukkitViaLoader(ViaVersionPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerListener(Listener listener) {
        Bukkit.getPluginManager().registerEvents(this.storeListener(listener), (Plugin)this.plugin);
    }

    public <T extends Listener> T storeListener(T listener) {
        this.listeners.add(listener);
        return listener;
    }

    @Override
    public void load() {
        this.registerListener(new UpdateListener());
        ViaVersionPlugin plugin = (ViaVersionPlugin)Bukkit.getPluginManager().getPlugin("ViaVersion");
        ClassGenerator.registerPSConnectListener(plugin);
        int serverProtocolVersion = Via.getAPI().getServerVersion().lowestSupportedVersion();
        if (serverProtocolVersion < ProtocolVersion.v1_9.getVersion()) {
            this.storeListener(new ArmorListener((Plugin)plugin)).register();
            this.storeListener(new DeathListener((Plugin)plugin)).register();
            this.storeListener(new BlockListener((Plugin)plugin)).register();
            if (plugin.getConf().isItemCache()) {
                this.handItemCache = new HandItemCache();
                this.tasks.add(this.handItemCache.runTaskTimerAsynchronously((Plugin)plugin, 1L, 1L));
            }
        }
        if (serverProtocolVersion < ProtocolVersion.v1_14.getVersion()) {
            boolean use1_9Fix;
            boolean bl = use1_9Fix = plugin.getConf().is1_9HitboxFix() && serverProtocolVersion < ProtocolVersion.v1_9.getVersion();
            if (use1_9Fix || plugin.getConf().is1_14HitboxFix()) {
                try {
                    this.storeListener(new PlayerSneakListener(plugin, use1_9Fix, plugin.getConf().is1_14HitboxFix())).register();
                }
                catch (ReflectiveOperationException e2) {
                    Via.getPlatform().getLogger().warning("Could not load hitbox fix - please report this on our GitHub");
                    e2.printStackTrace();
                }
            }
        }
        if (serverProtocolVersion < ProtocolVersion.v1_15.getVersion()) {
            try {
                Class.forName("org.bukkit.event.entity.EntityToggleGlideEvent");
                this.storeListener(new EntityToggleGlideListener(plugin)).register();
            }
            catch (ClassNotFoundException use1_9Fix) {
                // empty catch block
            }
        }
        if (serverProtocolVersion < ProtocolVersion.v1_12.getVersion() && !Boolean.getBoolean("com.viaversion.ignorePaperBlockPlacePatch")) {
            boolean paper = true;
            try {
                Class.forName("org.github.paperspigot.PaperSpigotConfig");
            }
            catch (ClassNotFoundException ignored) {
                try {
                    Class.forName("com.destroystokyo.paper.PaperConfig");
                }
                catch (ClassNotFoundException alsoIgnored) {
                    paper = false;
                }
            }
            if (paper) {
                this.storeListener(new PaperPatch((Plugin)plugin)).register();
            }
        }
        if (serverProtocolVersion < ProtocolVersion.v1_9.getVersion()) {
            Via.getManager().getProviders().use(MovementTransmitterProvider.class, new BukkitViaMovementTransmitter());
            Via.getManager().getProviders().use(HandItemProvider.class, new HandItemProvider(){

                @Override
                public Item getHandItem(UserConnection info) {
                    if (BukkitViaLoader.this.handItemCache != null) {
                        return BukkitViaLoader.this.handItemCache.getHandItem(info.getProtocolInfo().getUuid());
                    }
                    try {
                        return (Item)Bukkit.getScheduler().callSyncMethod(Bukkit.getPluginManager().getPlugin("ViaVersion"), () -> {
                            UUID playerUUID = info.getProtocolInfo().getUuid();
                            Player player = Bukkit.getPlayer((UUID)playerUUID);
                            if (player != null) {
                                return HandItemCache.convert(player.getItemInHand());
                            }
                            return null;
                        }).get(10L, TimeUnit.SECONDS);
                    }
                    catch (Exception e2) {
                        Via.getPlatform().getLogger().severe("Error fetching hand item: " + e2.getClass().getName());
                        if (Via.getManager().isDebug()) {
                            e2.printStackTrace();
                        }
                        return null;
                    }
                }
            });
        }
        if (serverProtocolVersion < ProtocolVersion.v1_12.getVersion() && plugin.getConf().is1_12QuickMoveActionFix()) {
            Via.getManager().getProviders().use(InventoryQuickMoveProvider.class, new BukkitInventoryQuickMoveProvider());
        }
        if (serverProtocolVersion < ProtocolVersion.v1_13.getVersion() && Via.getConfig().getBlockConnectionMethod().equalsIgnoreCase("world")) {
            BukkitBlockConnectionProvider blockConnectionProvider = new BukkitBlockConnectionProvider();
            Via.getManager().getProviders().use(BlockConnectionProvider.class, blockConnectionProvider);
            ConnectionData.blockConnectionProvider = blockConnectionProvider;
        }
    }

    @Override
    public void unload() {
        for (Listener listener : this.listeners) {
            HandlerList.unregisterAll((Listener)listener);
        }
        this.listeners.clear();
        for (BukkitTask task : this.tasks) {
            task.cancel();
        }
        this.tasks.clear();
    }
}

