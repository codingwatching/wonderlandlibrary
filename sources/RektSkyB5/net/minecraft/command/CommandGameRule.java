/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.command;

import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandResultStats;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.GameRules;

public class CommandGameRule
extends CommandBase {
    @Override
    public String getCommandName() {
        return "gamerule";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "commands.gamerule.usage";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        GameRules gamerules = this.getGameRules();
        String s2 = args.length > 0 ? args[0] : "";
        String s1 = args.length > 1 ? CommandGameRule.buildString(args, 1) : "";
        switch (args.length) {
            case 0: {
                sender.addChatMessage(new ChatComponentText(CommandGameRule.joinNiceString(gamerules.getRules())));
                break;
            }
            case 1: {
                if (!gamerules.hasRule(s2)) {
                    throw new CommandException("commands.gamerule.norule", s2);
                }
                String s22 = gamerules.getString(s2);
                sender.addChatMessage(new ChatComponentText(s2).appendText(" = ").appendText(s22));
                sender.setCommandStat(CommandResultStats.Type.QUERY_RESULT, gamerules.getInt(s2));
                break;
            }
            default: {
                if (gamerules.areSameType(s2, GameRules.ValueType.BOOLEAN_VALUE) && !"true".equals(s1) && !"false".equals(s1)) {
                    throw new CommandException("commands.generic.boolean.invalid", s1);
                }
                gamerules.setOrCreateGameRule(s2, s1);
                CommandGameRule.func_175773_a(gamerules, s2);
                CommandGameRule.notifyOperators(sender, (ICommand)this, "commands.gamerule.success", new Object[0]);
            }
        }
    }

    public static void func_175773_a(GameRules p_175773_0_, String p_175773_1_) {
        if ("reducedDebugInfo".equals(p_175773_1_)) {
            byte b0 = (byte)(p_175773_0_.getBoolean(p_175773_1_) ? 22 : 23);
            for (EntityPlayerMP entityplayermp : MinecraftServer.getServer().getConfigurationManager().func_181057_v()) {
                entityplayermp.playerNetServerHandler.sendPacket(new S19PacketEntityStatus(entityplayermp, b0));
            }
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        GameRules gamerules;
        if (args.length == 1) {
            return CommandGameRule.getListOfStringsMatchingLastWord(args, this.getGameRules().getRules());
        }
        if (args.length == 2 && (gamerules = this.getGameRules()).areSameType(args[0], GameRules.ValueType.BOOLEAN_VALUE)) {
            return CommandGameRule.getListOfStringsMatchingLastWord(args, "true", "false");
        }
        return null;
    }

    private GameRules getGameRules() {
        return MinecraftServer.getServer().worldServerForDimension(0).getGameRules();
    }
}

