package com.amadornes.rscircuits.command;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.Collections;

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
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;

public class CommandExport extends CommandBase {

    @Override
    public String getCommandName() {

        return "export";
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

        // RayTraceResult hit = Minecraft.getMinecraft().objectMouseOver;
        // if (hit instanceof PartMOP) {
        // PartCircuit circuit = (PartCircuit) MultipartHelper.getPartContainer(server.getEntityWorld(), hit.getBlockPos())
        // .getPartInSlot(PartSlot.DOWN);
        // NBTTagCompound tag = new NBTTagCompound();
        // circuit.circuit.writeToNBT(tag);
        // if (tag != null) {
        // ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // try {
        // CompressedStreamTools.writeCompressed(tag, baos);
        // } catch (IOException e) {
        // throw new CommandException("Failed to save the circuit to a streamable format");
        // }
        // String data = new String(Base64.getEncoder().encode(baos.toByteArray()));
        // System.out.println(circuit.circuit.getName() + ((Supplier<String>) () -> {
        // StringBuilder s = new StringBuilder();
        // for (int i = circuit.circuit.getName().length(); i < 20; i++) {
        // s.append(" ");
        // }
        // return s.toString();
        // }).get() + " " + data);
        // }
        // }
        //
        // if (true)
        // return;

        if (sender instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sender;
            ItemStack stack = player.getHeldItemMainhand();
            if (stack != null && stack.getItem() instanceof ICircuitStorage) {
                NBTTagCompound tag = ((ICircuitStorage) stack.getItem()).getCircuitData(player, stack);
                if (tag != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try {
                        CompressedStreamTools.writeCompressed(tag, baos);
                    } catch (IOException e) {
                        throw new CommandException("Failed to save the circuit to a streamable format");
                    }
                    String data = new String(Base64.getEncoder().encode(baos.toByteArray()));
                    new Thread(() -> {
                        try {
                            URL url = GistPublisher.publish(Collections.singletonMap("blueprint", data));
                            TextComponentString link = new TextComponentString(url.toString());
                            ClickEvent evt = new ClickEvent(ClickEvent.Action.OPEN_URL, url.toString());
                            link.getStyle().setClickEvent(evt);
                            link.getStyle().setUnderlined(true);
                            link.getStyle().setColor(TextFormatting.BLUE);
                            player.addChatMessage(link);
                        } catch (Exception e) {
                            SCM.log.error("Error while uploading blueprint gist", e);
                            player.addChatMessage(new TextComponentString(TextFormatting.RED + "Error while uploading blueprint gist"));
                        }
                    }).start();
                } else {
                    throw new CommandException("The item must contain a circuit!");
                }
            } else {
                throw new CommandException("You must have a circuit/blueprint/redprint in your hand!");
            }
        } else {
            throw new CommandException("You must be a player to use this command!");
        }
    }

}
