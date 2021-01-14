package com.amadornes.rscircuits.command;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.command.CommandTreeBase;

public class CommandSCM extends CommandTreeBase {

    @Override
    public String getCommandName() {

        return "scm";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {

        return "scm <import/export>";
    }

    @Override
    public int getRequiredPermissionLevel() {

        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {

        return true;
    }

}
