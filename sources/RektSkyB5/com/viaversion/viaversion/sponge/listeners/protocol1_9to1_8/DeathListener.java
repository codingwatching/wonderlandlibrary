/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.spongepowered.api.entity.living.player.Player
 *  org.spongepowered.api.event.Listener
 *  org.spongepowered.api.event.Order
 *  org.spongepowered.api.event.entity.DestructEntityEvent$Death
 *  org.spongepowered.api.world.World
 */
package com.viaversion.viaversion.sponge.listeners.protocol1_9to1_8;

import com.viaversion.viaversion.SpongePlugin;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.ClientboundPackets1_9;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.Protocol1_9To1_8;
import com.viaversion.viaversion.sponge.listeners.ViaSpongeListener;
import java.util.Optional;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.world.World;

public class DeathListener
extends ViaSpongeListener {
    public DeathListener(SpongePlugin plugin) {
        super(plugin, Protocol1_9To1_8.class);
    }

    @Listener(order=Order.LAST)
    public void onDeath(DestructEntityEvent.Death e2) {
        if (!(e2.getTargetEntity() instanceof Player)) {
            return;
        }
        Player p2 = (Player)e2.getTargetEntity();
        if (this.isOnPipe(p2.getUniqueId()) && Via.getConfig().isShowNewDeathMessages() && this.checkGamerule(p2.getWorld())) {
            this.sendPacket(p2, e2.getMessage().toPlain());
        }
    }

    public boolean checkGamerule(World w2) {
        Optional gamerule = w2.getGameRule("showDeathMessages");
        if (gamerule.isPresent()) {
            try {
                return Boolean.parseBoolean((String)gamerule.get());
            }
            catch (Exception e2) {
                return false;
            }
        }
        return false;
    }

    private void sendPacket(final Player p2, final String msg) {
        Via.getPlatform().runSync(new Runnable(){

            @Override
            public void run() {
                PacketWrapper wrapper = PacketWrapper.create(ClientboundPackets1_9.COMBAT_EVENT, null, DeathListener.this.getUserConnection(p2.getUniqueId()));
                try {
                    int entityId = DeathListener.this.getEntityId(p2);
                    wrapper.write(Type.VAR_INT, 2);
                    wrapper.write(Type.VAR_INT, entityId);
                    wrapper.write(Type.INT, entityId);
                    Protocol1_9To1_8.FIX_JSON.write(wrapper, msg);
                    wrapper.scheduleSend(Protocol1_9To1_8.class);
                }
                catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        });
    }
}

