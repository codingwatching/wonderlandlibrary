/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.scoreboard;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S3BPacketScoreboardObjective;
import net.minecraft.network.play.server.S3CPacketUpdateScore;
import net.minecraft.network.play.server.S3DPacketDisplayScoreboard;
import net.minecraft.network.play.server.S3EPacketTeams;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardSaveData;
import net.minecraft.server.MinecraftServer;

public class ServerScoreboard
extends Scoreboard {
    private final MinecraftServer scoreboardMCServer;
    private final Set<ScoreObjective> field_96553_b = Sets.newHashSet();
    private ScoreboardSaveData scoreboardSaveData;

    public ServerScoreboard(MinecraftServer mcServer) {
        this.scoreboardMCServer = mcServer;
    }

    @Override
    public void func_96536_a(Score p_96536_1_) {
        super.func_96536_a(p_96536_1_);
        if (this.field_96553_b.contains(p_96536_1_.getObjective())) {
            this.scoreboardMCServer.getConfigurationManager().sendPacketToAllPlayers(new S3CPacketUpdateScore(p_96536_1_));
        }
        this.func_96551_b();
    }

    @Override
    public void func_96516_a(String p_96516_1_) {
        super.func_96516_a(p_96516_1_);
        this.scoreboardMCServer.getConfigurationManager().sendPacketToAllPlayers(new S3CPacketUpdateScore(p_96516_1_));
        this.func_96551_b();
    }

    @Override
    public void func_178820_a(String p_178820_1_, ScoreObjective p_178820_2_) {
        super.func_178820_a(p_178820_1_, p_178820_2_);
        this.scoreboardMCServer.getConfigurationManager().sendPacketToAllPlayers(new S3CPacketUpdateScore(p_178820_1_, p_178820_2_));
        this.func_96551_b();
    }

    @Override
    public void setObjectiveInDisplaySlot(int p_96530_1_, ScoreObjective p_96530_2_) {
        ScoreObjective scoreobjective = this.getObjectiveInDisplaySlot(p_96530_1_);
        super.setObjectiveInDisplaySlot(p_96530_1_, p_96530_2_);
        if (scoreobjective != p_96530_2_ && scoreobjective != null) {
            if (this.func_96552_h(scoreobjective) > 0) {
                this.scoreboardMCServer.getConfigurationManager().sendPacketToAllPlayers(new S3DPacketDisplayScoreboard(p_96530_1_, p_96530_2_));
            } else {
                this.getPlayerIterator(scoreobjective);
            }
        }
        if (p_96530_2_ != null) {
            if (this.field_96553_b.contains(p_96530_2_)) {
                this.scoreboardMCServer.getConfigurationManager().sendPacketToAllPlayers(new S3DPacketDisplayScoreboard(p_96530_1_, p_96530_2_));
            } else {
                this.func_96549_e(p_96530_2_);
            }
        }
        this.func_96551_b();
    }

    @Override
    public boolean addPlayerToTeam(String player, String newTeam) {
        if (super.addPlayerToTeam(player, newTeam)) {
            ScorePlayerTeam scoreplayerteam = this.getTeam(newTeam);
            this.scoreboardMCServer.getConfigurationManager().sendPacketToAllPlayers(new S3EPacketTeams(scoreplayerteam, Arrays.asList(player), 3));
            this.func_96551_b();
            return true;
        }
        return false;
    }

    @Override
    public void removePlayerFromTeam(String p_96512_1_, ScorePlayerTeam p_96512_2_) {
        super.removePlayerFromTeam(p_96512_1_, p_96512_2_);
        this.scoreboardMCServer.getConfigurationManager().sendPacketToAllPlayers(new S3EPacketTeams(p_96512_2_, Arrays.asList(p_96512_1_), 4));
        this.func_96551_b();
    }

    @Override
    public void onScoreObjectiveAdded(ScoreObjective scoreObjectiveIn) {
        super.onScoreObjectiveAdded(scoreObjectiveIn);
        this.func_96551_b();
    }

    @Override
    public void func_96532_b(ScoreObjective p_96532_1_) {
        super.func_96532_b(p_96532_1_);
        if (this.field_96553_b.contains(p_96532_1_)) {
            this.scoreboardMCServer.getConfigurationManager().sendPacketToAllPlayers(new S3BPacketScoreboardObjective(p_96532_1_, 2));
        }
        this.func_96551_b();
    }

    @Override
    public void func_96533_c(ScoreObjective p_96533_1_) {
        super.func_96533_c(p_96533_1_);
        if (this.field_96553_b.contains(p_96533_1_)) {
            this.getPlayerIterator(p_96533_1_);
        }
        this.func_96551_b();
    }

    @Override
    public void broadcastTeamCreated(ScorePlayerTeam playerTeam) {
        super.broadcastTeamCreated(playerTeam);
        this.scoreboardMCServer.getConfigurationManager().sendPacketToAllPlayers(new S3EPacketTeams(playerTeam, 0));
        this.func_96551_b();
    }

    @Override
    public void sendTeamUpdate(ScorePlayerTeam playerTeam) {
        super.sendTeamUpdate(playerTeam);
        this.scoreboardMCServer.getConfigurationManager().sendPacketToAllPlayers(new S3EPacketTeams(playerTeam, 2));
        this.func_96551_b();
    }

    @Override
    public void func_96513_c(ScorePlayerTeam playerTeam) {
        super.func_96513_c(playerTeam);
        this.scoreboardMCServer.getConfigurationManager().sendPacketToAllPlayers(new S3EPacketTeams(playerTeam, 1));
        this.func_96551_b();
    }

    public void func_96547_a(ScoreboardSaveData p_96547_1_) {
        this.scoreboardSaveData = p_96547_1_;
    }

    protected void func_96551_b() {
        if (this.scoreboardSaveData != null) {
            this.scoreboardSaveData.markDirty();
        }
    }

    public List<Packet> func_96550_d(ScoreObjective p_96550_1_) {
        ArrayList<Packet> list = Lists.newArrayList();
        list.add(new S3BPacketScoreboardObjective(p_96550_1_, 0));
        for (int i2 = 0; i2 < 19; ++i2) {
            if (this.getObjectiveInDisplaySlot(i2) != p_96550_1_) continue;
            list.add(new S3DPacketDisplayScoreboard(i2, p_96550_1_));
        }
        for (Score score : this.getSortedScores(p_96550_1_)) {
            list.add(new S3CPacketUpdateScore(score));
        }
        return list;
    }

    public void func_96549_e(ScoreObjective p_96549_1_) {
        List<Packet> list = this.func_96550_d(p_96549_1_);
        for (EntityPlayerMP entityplayermp : this.scoreboardMCServer.getConfigurationManager().func_181057_v()) {
            for (Packet packet : list) {
                entityplayermp.playerNetServerHandler.sendPacket(packet);
            }
        }
        this.field_96553_b.add(p_96549_1_);
    }

    public List<Packet> func_96548_f(ScoreObjective p_96548_1_) {
        ArrayList<Packet> list = Lists.newArrayList();
        list.add(new S3BPacketScoreboardObjective(p_96548_1_, 1));
        for (int i2 = 0; i2 < 19; ++i2) {
            if (this.getObjectiveInDisplaySlot(i2) != p_96548_1_) continue;
            list.add(new S3DPacketDisplayScoreboard(i2, p_96548_1_));
        }
        return list;
    }

    public void getPlayerIterator(ScoreObjective p_96546_1_) {
        List<Packet> list = this.func_96548_f(p_96546_1_);
        for (EntityPlayerMP entityplayermp : this.scoreboardMCServer.getConfigurationManager().func_181057_v()) {
            for (Packet packet : list) {
                entityplayermp.playerNetServerHandler.sendPacket(packet);
            }
        }
        this.field_96553_b.remove(p_96546_1_);
    }

    public int func_96552_h(ScoreObjective p_96552_1_) {
        int i2 = 0;
        for (int j2 = 0; j2 < 19; ++j2) {
            if (this.getObjectiveInDisplaySlot(j2) != p_96552_1_) continue;
            ++i2;
        }
        return i2;
    }
}

