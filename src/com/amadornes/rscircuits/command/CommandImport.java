package com.amadornes.rscircuits.command;

import java.io.ByteArrayInputStream;

import org.apache.commons.codec.binary.Base64;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.item.ICircuitStorage;
import com.amadornes.rscircuits.util.GistPublisher;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class CommandImport extends CommandBase {

    @Override
    public String getCommandName() {

        return "import";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {

        return "";
    }

    @Override
    public int getRequiredPermissionLevel() {

        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {

        return true;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {

        if (sender instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sender;
            ItemStack stack = player.getHeldItemMainhand();
            if (stack != null && stack.getItem() instanceof ICircuitStorage
                    && ((ICircuitStorage) stack.getItem()).canOverrideCircuitData(player, stack)) {
                if (args.length == 1) {
                    String url = args[0].replaceAll("\\.com/[^/]+/", "\\.com/");
                    if (url.matches("https?:\\/\\/gist\\.github\\.com\\/\\w{32}")) {
                        new Thread(() -> {
                            try {
                                String b64Data = GistPublisher.load(url, "blueprint");
                                byte[] data = Base64.decodeBase64(b64Data);
                                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                                NBTTagCompound tag = CompressedStreamTools.readCompressed(bais);
                                bais.close();

                                ActionResult<ItemStack> result = ((ICircuitStorage) stack.getItem()).overrideCircuitData(player, stack,
                                        tag);
                                if (result.getType() == EnumActionResult.SUCCESS) {
                                    player.setHeldItem(EnumHand.MAIN_HAND, result.getResult());
                                    player.addChatMessage(new TextComponentString(TextFormatting.GREEN + "Loaded blueprint!"));
                                } else {
                                    SCM.log.error("Failed to update blueprint!");
                                    player.addChatMessage(new TextComponentString(TextFormatting.RED + "Failed to update blueprint!"));
                                }
                            } catch (Exception e) {
                                SCM.log.error("Error while reading blueprint gist", e);
                                player.addChatMessage(new TextComponentString(TextFormatting.RED + "Error while reading blueprint gist"));
                            }
                        }).start();
                    } else {
                        throw new CommandException("Invalid URL!");
                    }
                } else {
                    throw new CommandException("No URL specified!");
                }
            } else {
                throw new CommandException("You must have an empty blueprint in your hand!");
            }
        } else {
            throw new CommandException("You must be a player to use this command!");
        }
    }

}
