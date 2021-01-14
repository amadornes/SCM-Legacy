package com.amadornes.rscircuits.client;

import static com.amadornes.rscircuits.SCM.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Consumer;

import org.lwjgl.input.Keyboard;

import com.amadornes.rscircuits.CommonProxy;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.api.component.IComponentFactory;
import com.amadornes.rscircuits.api.component.IComponentFactory.EnumInstantanceUse;
import com.amadornes.rscircuits.api.component.IComponentFactory.EnumPlacementType;
import com.amadornes.rscircuits.api.component.IComponentFactory.IDrawHandler;
import com.amadornes.rscircuits.api.component.IComponentFactory.IDrawListener;
import com.amadornes.rscircuits.api.component.IPaintableComponent;
import com.amadornes.rscircuits.circuit.Circuit;
import com.amadornes.rscircuits.client.gui.GuiColorPalette;
import com.amadornes.rscircuits.client.gui.GuiRegulate;
import com.amadornes.rscircuits.client.gui.GuiTutorial;
import com.amadornes.rscircuits.component.ComponentRegistry;
import com.amadornes.rscircuits.component.circuit.ComponentCircuit;
import com.amadornes.rscircuits.component.digital.ComponentLever;
import com.amadornes.rscircuits.init.SCMBlocks;
import com.amadornes.rscircuits.init.SCMItems;
import com.amadornes.rscircuits.item.EnumResourceType;
import com.amadornes.rscircuits.item.IScrollableItem;
import com.amadornes.rscircuits.item.ItemMonocle;
import com.amadornes.rscircuits.network.NetworkHandler;
import com.amadornes.rscircuits.network.PacketColorPick;
import com.amadornes.rscircuits.network.PacketPlacementData;
import com.amadornes.rscircuits.part.PartCircuit;
import com.amadornes.rscircuits.util.BoolFunction;
import com.amadornes.rscircuits.util.IntBoolFunction;
import com.amadornes.rscircuits.util.RedstoneUtils;
import com.google.common.base.Supplier;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import mcmultipart.client.multipart.MultipartRegistryClient;
import mcmultipart.client.multipart.MultipartStateMapper;
import mcmultipart.multipart.MultipartRegistry;
import mcmultipart.raytrace.PartMOP;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumUsage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class ClientProxy extends CommonProxy {

    private SoundEvent sound_place, sound_xycraft, sound_dw20;

    @Override
    public void preInit() {

        MinecraftForge.EVENT_BUS.register(SimpleModelFontRenderer.class);

        ItemMonocle.initClient();

        for (EnumResourceType res : EnumResourceType.VALUES) {
            ModelLoader.setCustomModelResourceLocation(SCMItems.resource, res.ordinal(),
                    new ModelResourceLocation(MODID + ":resource", "inventory_" + res.name().toLowerCase()));
        }
        ModelLoader.setCustomModelResourceLocation(SCMItems.circuit, 0, new ModelResourceLocation(MODID + ":circuit", "inventory"));
        ModelLoader.setCustomModelResourceLocation(SCMItems.component_tray, 0,
                new ModelResourceLocation(MODID + ":component_tray", "inventory"));
        ModelLoader.setCustomModelResourceLocation(SCMItems.palette_and_brush, 0,
                new ModelResourceLocation(MODID + ":pallette_and_brush", "inventory"));
        ModelLoader.setCustomModelResourceLocation(SCMItems.screwdriver, 0, new ModelResourceLocation(MODID + ":screwdriver", "inventory"));
        ModelLoader.setCustomModelResourceLocation(SCMItems.screwdriver, 1,
                new ModelResourceLocation(MODID + ":screwdriver", "inventory_clippy"));
        ModelLoader.setCustomModelResourceLocation(SCMItems.redwire, 0, new ModelResourceLocation(MODID + ":redwire", "inventory"));
        ModelLoader.setCustomModelResourceLocation(SCMItems.monocle, 0, new ModelResourceLocation(MODID + ":monocle", "inventory"));
        ModelLoader.setCustomModelResourceLocation(SCMItems.monocle, 1,
                new ModelResourceLocation(MODID + ":monocle", "inventory_magnificent"));
        ModelLoader.setCustomModelResourceLocation(SCMItems.blueprint, 0,
                new ModelResourceLocation(MODID + ":blueprint", "inventory_blueprint_empty"));
        ModelLoader.setCustomModelResourceLocation(SCMItems.blueprint, 1,
                new ModelResourceLocation(MODID + ":blueprint", "inventory_blueprint"));
        ModelLoader.setCustomModelResourceLocation(SCMItems.blueprint, 2,
                new ModelResourceLocation(MODID + ":blueprint", "inventory_redprint_empty"));
        ModelLoader.setCustomModelResourceLocation(SCMItems.blueprint, 3,
                new ModelResourceLocation(MODID + ":blueprint", "inventory_redprint"));
        ModelLoader.setCustomModelResourceLocation(SCMItems.blueprint, 4,
                new ModelResourceLocation(MODID + ":blueprint", "inventory_blueprint"));
        ModelLoader.setCustomModelResourceLocation(SCMItems.multimeter, 0, new ModelResourceLocation(MODID + ":multimeter", "inventory"));
        ModelLoader.setCustomModelResourceLocation(SCMItems.squeegee, 0, new ModelResourceLocation(MODID + ":squeegee", "inventory"));
        ModelLoader.setCustomModelResourceLocation(SCMBlocks.update_detector_item, 0,
                new ModelResourceLocation(MODID + ":update_detector", "inventory"));
        ModelLoader.setCustomModelResourceLocation(SCMItems.punchcard, 0, new ModelResourceLocation(MODID + ":punchcard", "inventory"));

        ModelLoaderRegistry.registerLoader(EmptyModelLoader.INSTANCE);
        MultipartRegistryClient.registerSpecialPartStateMapper(new ResourceLocation(MODID, "circuit"), new CircuitStateMapper());
        MultipartRegistryClient.bindMultipartSpecialRenderer(PartCircuit.class, new MSRCircuit());

        ResourceLocation location = new ResourceLocation(MODID, "component.place");
        sound_place = new SoundEvent(location);
        GameRegistry.register(sound_place, location);
        location = new ResourceLocation(MODID, "component.place.soaryn");
        sound_xycraft = new SoundEvent(location);
        GameRegistry.register(sound_xycraft, location);
        location = new ResourceLocation(MODID, "component.place.dire");
        sound_dw20 = new SoundEvent(location);
        GameRegistry.register(sound_dw20, location);
    }

    @Override
    public EntityPlayer getPlayer() {

        return Minecraft.getMinecraft().thePlayer;
    }

    @Override
    public RayTraceResult getHit() {

        return Minecraft.getMinecraft().objectMouseOver;
    }

    @Override
    public void playPlaceSound(BlockPos pos) {

        float vol = 0.125F;
        Minecraft mc = Minecraft.getMinecraft();
        UUID uuid = mc.thePlayer.getGameProfile().getId();
        if (uuid.toString().equalsIgnoreCase("4f3a8d1e-33c1-44e7-bce8-e683027c7dac")) {
            vol *= 0.125F;
            if (Math.random() < 0.005) {
                mc.theWorld.playSound(pos, sound_xycraft, SoundCategory.BLOCKS, vol, 1.0F, false);
                return;
            }
        } else if (uuid.toString().equalsIgnoreCase("bbb87dbe-690f-4205-bdc5-72ffb8ebc29d")) {
            if (Math.random() < 0.002) {
                mc.theWorld.playSound(pos, sound_dw20, SoundCategory.BLOCKS, vol, 1.0F, false);
                return;
            }
        }
        mc.theWorld.playSound(pos, sound_place, SoundCategory.BLOCKS, vol, 0.85F + (float) (Math.random() * 0.05), false);
    }

    @Override
    public void displayTimerGui(IntBoolFunction<String> titleSupplier, BoolFunction<String> shortSupplier, Supplier<Integer> getter,
            Consumer<Integer> setter, int min, int max, double shiftMul) {

        Minecraft.getMinecraft().displayGuiScreen(new GuiRegulate(titleSupplier, shortSupplier, getter, setter, min, max, shiftMul));
    }

    @SubscribeEvent
    public void onModelBake(ModelBakeEvent event) {

        ResourceLocation circuit = new ResourceLocation(MODID, "circuit");
        for (IBlockState state : MultipartRegistry.getDefaultState(circuit).getValidStates()) {
            ModelResourceLocation mrl = new ModelResourceLocation(circuit,
                    MultipartStateMapper.instance.getPropertyString(state.getProperties()));
            event.getModelRegistry().putObject(mrl, new ModelWrapperCircuit(event.getModelRegistry().getObject(mrl)));
        }

        ResourceLocation innerCircuit = new ResourceLocation(MODID, "component/circuit");
        for (IBlockState state : ComponentRegistry.INSTANCE.getState(ComponentCircuit.NAME).getValidStates()) {
            ModelResourceLocation mrl = new ModelResourceLocation(innerCircuit,
                    MultipartStateMapper.instance.getPropertyString(state.getProperties()).replace("multipart", "normal"));
            event.getModelRegistry().putObject(mrl, new ModelWrapperInnerCircuit(event.getModelRegistry().getObject(mrl)));
        }

        ResourceLocation lever = new ResourceLocation(MODID, "component/lever");
        for (IBlockState state : ComponentRegistry.INSTANCE.getState(ComponentLever.NAME).getValidStates()) {
            ModelResourceLocation mrl = new ModelResourceLocation(lever,
                    MultipartStateMapper.instance.getPropertyString(state.getProperties()));
            event.getModelRegistry().putObject(mrl,
                    ModelTransformer.transform(event.getModelRegistry().getObject(mrl), (quad, type, usage, data) -> {
                        if (usage == EnumUsage.POSITION) {
                            data[0] -= 0.5F;
                            data[2] -= 0.5F;

                            float ratio = 16 / 8F;
                            data[0] *= ratio;
                            data[1] *= ratio;
                            data[2] *= ratio;

                            data[0] += 0.5F;
                            data[2] += 0.5F;
                        }
                        return data;
                    }, state, 0L));
        }
    }

    @SuppressWarnings("unused")
    private BlockPos placePosLast;

    private ItemStack drawStack;
    private EnumHand drawHand;
    private ICircuit drawCircuit;
    private Map<BlockPos, Object> drawDataMap;
    private IComponentFactory<?> drawFactory;

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public <T> void drawInteractionPrevention(PlayerInteractEvent.RightClickBlock event) {

        if (event.getWorld() != null && event.getWorld().isRemote && event.getHand() == EnumHand.MAIN_HAND) {
            if (event.getItemStack() != null
                    && (event.getItemStack().getItem() == SCMItems.screwdriver || event.getItemStack().getItem() == SCMItems.multimeter)) {
                return;
            }
            RayTraceResult hit = Minecraft.getMinecraft().objectMouseOver;
            if (hit instanceof PartMOP && ((PartMOP) hit).partHit instanceof PartCircuit) {
                PartMOP mop = (PartMOP) hit;
                PartCircuit partCircuit = (PartCircuit) mop.partHit;

                if (partCircuit.isEncapsulated()) {
                    return;
                }

                Vec3d extrudedProjection = RedstoneUtils.projectComponent(new Vec3d(//
                        hit.hitVec.xCoord - hit.getBlockPos().getX(), //
                        hit.hitVec.yCoord - hit.getBlockPos().getY(), //
                        hit.hitVec.zCoord - hit.getBlockPos().getZ()), partCircuit.getFace(), BlockPos.ORIGIN);
                BlockPos clickedPos = new BlockPos(extrudedProjection);

                Circuit circuit = partCircuit.circuit.getCircuit(clickedPos);
                if (circuit != null && !partCircuit.isSad() && !partCircuit.isEncapsulated() && circuit.getPos() != null
                        && circuit.getFace() != null) {
                    clickedPos = RedstoneUtils.limitPositionToBounds(clickedPos);
                    extrudedProjection = RedstoneUtils.limitPositionToBounds(extrudedProjection);
                    if (handleDrawTick(event, circuit, clickedPos, extrudedProjection, mop)) {
                        event.setCanceled(true);
                    } else if (handlePaint(event, circuit, clickedPos, extrudedProjection, mop)) {
                        event.setCanceled(true);
                    }
                }
            } else {
                drawStack = null;
                drawHand = null;
                drawDataMap = null;
                if (drawFactory instanceof IDrawListener) {
                    ((IDrawListener<T>) drawFactory).onFinishDrawing(event.getEntityPlayer());
                }
                drawFactory = null;
            }
        }
    }

    private boolean handlePaint(PlayerInteractEvent.RightClickBlock event, ICircuit circuit, BlockPos clickedPos, Vec3d extrudedProjection,
            PartMOP hit) {

        EntityPlayer player = event.getEntityPlayer();
        ItemStack stack = player.getHeldItemMainhand();
        EnumHand hand = EnumHand.MAIN_HAND;
        if (stack == null || stack.getItem() != SCMItems.palette_and_brush) {
            stack = player.getHeldItemOffhand();
            hand = EnumHand.OFF_HAND;
        }
        if (stack != null && stack.getItem() == SCMItems.palette_and_brush) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null) {
                stack.setTagCompound(tag = new NBTTagCompound());
            }
            int active = tag.getInteger("color");
            if (hit.hitInfo instanceof IPaintableComponent) {
                if (((IPaintableComponent) hit.hitInfo).paint(EnumDyeColor.byMetadata(active))) {
                    player.swingArm(hand);
                    return true;
                }
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private <T> boolean handleDrawTick(PlayerInteractEvent.RightClickBlock event, ICircuit circuit, BlockPos clickedPos,
            Vec3d extrudedProjection, PartMOP hit) {

        if (hit.hitInfo != null && hit.hitInfo instanceof IComponent && ((IComponent) hit.hitInfo).getPos().equals(clickedPos)) {
            Vec3d subPos = extrudedProjection.subtract(new Vec3d(clickedPos));
            if (subPos.xCoord == 0) {
                clickedPos = clickedPos.add(-1, 0, 0);
            } else if (subPos.zCoord == 0) {
                clickedPos = clickedPos.add(0, 0, -1);
            }
        }

        ItemStack heldItem = RedstoneUtils.unwrap(event.getEntityPlayer().getHeldItem(event.getHand()));
        if (heldItem != null) {
            if (event.getHand() == EnumHand.MAIN_HAND) {
                IComponentFactory<T> factory = (IComponentFactory<T>) ComponentRegistry.INSTANCE.getFactory(heldItem,
                        event.getEntityPlayer());
                if (factory != null) {
                    EnumPlacementType type = factory.getPlacementType(heldItem, event.getEntityPlayer());
                    if (type != null) {
                        if (type == EnumPlacementType.SINGLE) {
                            Map<BlockPos, T> dataMap = new HashMap<BlockPos, T>();
                            T data = factory.getPlacementData(circuit, clickedPos, EnumCircuitSide.BOTTOM, extrudedProjection, heldItem,
                                    event.getEntityPlayer(), type, null, dataMap, EnumInstantanceUse.PLACEMENT);
                            if (data != null && dataMap.get(clickedPos) != data
                                    && factory.placeComponent(circuit, clickedPos, data, type, dataMap, true)) {
                                event.getEntityPlayer().swingArm(event.getHand());
                                event.setCanceled(true);
                                event.setUseItem(Result.DENY);
                                placePosLast = new BlockPos(hit.getBlockPos().getX() * 8 + clickedPos.getX(),
                                        hit.getBlockPos().getY() * 8 + clickedPos.getY(), hit.getBlockPos().getZ() * 8 + clickedPos.getZ());
                                playPlaceSound(circuit.getPos());

                                dataMap.put(clickedPos, data);
                                ByteBuf buf = Unpooled.buffer();
                                factory.serialize(new PacketBuffer(buf), dataMap, getPlayer());
                                NetworkHandler.instance.sendToServer(new PacketPlacementData(ComponentRegistry.INSTANCE.getName(factory),
                                        circuit.getPos(), circuit.getFace(), buf.array(), EnumPlacementType.SINGLE));
                                return true;
                            }
                        } else if (type == EnumPlacementType.DRAW) {
                            BlockPos packedCoords = RedstoneUtils.packCoords(RedstoneUtils.correctOffset(
                                    circuit.getPos().subtract((this.drawCircuit != null ? this.drawCircuit : circuit).getPos()),
                                    circuit.getFace()), circuit.getFace(), clickedPos);
                            if (factory instanceof IDrawListener) {
                                ((IDrawListener<T>) factory).onStartDrawing(event.getEntityPlayer());
                            }
                            Map<BlockPos, T> drawDataMap = this.drawDataMap != null ? (HashMap<BlockPos, T>) this.drawDataMap
                                    : new HashMap<>();
                            T data = factory.getPlacementData(circuit, clickedPos, EnumCircuitSide.BOTTOM, extrudedProjection, heldItem,
                                    event.getEntityPlayer(), type, drawDataMap.get(packedCoords), drawDataMap,
                                    EnumInstantanceUse.PLACEMENT);
                            if (data != null && factory.placeComponent(circuit, clickedPos, data, type, drawDataMap, true)) {
                                event.setCanceled(true);
                                event.setUseItem(Result.DENY);

                                if (drawDataMap.get(clickedPos) != data) {
                                    drawDataMap.put(packedCoords, data);

                                    if (drawStack == null) {
                                        this.drawStack = heldItem;
                                        this.drawHand = event.getHand();
                                        this.drawCircuit = circuit;
                                        this.drawDataMap = (Map<BlockPos, Object>) drawDataMap;
                                        this.drawFactory = factory;
                                    }
                                }
                                return true;
                            } else {
                                if (factory instanceof IDrawListener) {
                                    ((IDrawListener<T>) factory).onFinishDrawing(event.getEntityPlayer());
                                }
                            }
                        } else if (type == EnumPlacementType.LINE) {

                        }
                    }
                }
            }
        }
        return false;
    }

    private int breakHeight = -1;

    @SubscribeEvent
    public <T> void drawBreakPrevention(PlayerInteractEvent.LeftClickBlock event) {

        ItemStack stack = event.getItemStack();
        if (event.getWorld() != null && event.getWorld().isRemote && stack != null && stack.getItem() == SCMItems.screwdriver) {
            event.setCanceled(true);
            event.setUseItem(Result.DENY);
            event.setUseBlock(Result.DENY);
        }
        // ItemStack stack = event.getItemStack();
        // if (event.getWorld() != null && event.getWorld().isRemote && stack != null && stack.getItem() == SCM.screwdriver) {
        // RayTraceResult hit = Minecraft.getMinecraft().objectMouseOver;
        // if (hit instanceof PartMOP && ((PartMOP) hit).partHit instanceof PartCircuit) {
        // PartMOP mop = (PartMOP) hit;
        // IComponent comp = (IComponent) mop.hitInfo;
        // if (comp != null) {
        // if (breakHeight == -1 || comp.getPos().getY() == breakHeight) {
        // breakHeight = comp.getPos().getY();
        // Vec3d hitPos = RedstoneUtils.correctOffset(
        // new Vec3d((hit.hitVec.xCoord - comp.getCircuit().getPos().getX() - 1 / 16D) * 8 - comp.getPos().getX(),
        // (hit.hitVec.yCoord - comp.getCircuit().getPos().getY() - 2 / 16D) * 8 - comp.getPos().getY(),
        // (hit.hitVec.zCoord - comp.getCircuit().getPos().getZ() - 1 / 16D) * 8 - comp.getPos().getZ()),
        // ((PartCircuit) mop.partHit).getFace());
        // if (comp.harvest(event.getEntityPlayer(), hitPos)) {
        // event.getEntityPlayer().swingArm(event.getHand());
        // }
        // }
        // } else if (breakHeight == -1) {
        // breakHeight = -2;
        // }
        // if (comp != null || breakHeight >= 0) {
        // event.setCanceled(true);
        // event.setUseItem(Result.DENY);
        // }
        // } else {
        // breakHeight = -1;
        // }
        // }
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public <T> void onClientTick(TickEvent.ClientTickEvent event) {

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        ItemStack heldStackMain = mc.thePlayer.getHeldItemMainhand();
        ItemStack heldStackOff = mc.thePlayer.getHeldItemOffhand();
        ItemStack heldStack = heldStackMain;
        if (heldStack == null || heldStack.getItem() != SCMItems.palette_and_brush) {
            heldStack = heldStackOff;
        }
        if (heldStack != null && heldStack.getItem() == SCMItems.palette_and_brush && Keyboard.isKeyDown(Keyboard.KEY_LMENU)
                && mc.currentScreen == null) {
            NBTTagCompound tag = heldStack.getTagCompound();
            if (tag == null) {
                heldStack.setTagCompound(tag = new NBTTagCompound());
            }
            int active = tag.getInteger("color");
            mc.displayGuiScreen(new GuiColorPalette(active, i -> NetworkHandler.instance.sendToServer(new PacketColorPick(i))));
        }
        if (heldStackMain != null && heldStackMain.getItem() == SCMItems.screwdriver && Keyboard.isKeyDown(Keyboard.KEY_LMENU)
                && mc.currentScreen == null) {
            mc.displayGuiScreen(new GuiTutorial());
        }

        if (breakHeight != -1 && !mc.gameSettings.keyBindAttack.isKeyDown()) {
            breakHeight = -1;
        }

        if (drawStack != null) {
            Minecraft.getMinecraft().thePlayer.getCooldownTracker().removeCooldown(drawStack.getItem());
            if (!mc.gameSettings.keyBindUseItem.isKeyDown()) {
                final IComponentFactory<T> drawFactory = (IComponentFactory<T>) this.drawFactory;
                EntityPlayer player = mc.thePlayer;
                if (drawFactory instanceof IDrawHandler) {
                    if (((IDrawHandler<T>) drawFactory).finishDrawing(drawCircuit, (Map<BlockPos, T>) drawDataMap, drawStack, player)) {
                        player.swingArm(drawHand);
                    }
                } else {
                    boolean success = false;
                    for (Entry<BlockPos, Object> e : drawDataMap.entrySet()) {
                        success |= drawFactory.placeComponent(drawCircuit, e.getKey(), (T) e.getValue(), EnumPlacementType.DRAW,
                                (Map<BlockPos, T>) drawDataMap, true);
                    }
                    if (success) {
                        player.swingArm(drawHand);
                        playPlaceSound(drawCircuit.getPos());

                        ByteBuf buf = Unpooled.buffer();
                        drawFactory.serialize(new PacketBuffer(buf), (Map<BlockPos, T>) drawDataMap, player);
                        NetworkHandler.instance.sendToServer(new PacketPlacementData(ComponentRegistry.INSTANCE.getName(drawFactory),
                                drawCircuit.getPos(), drawCircuit.getFace(), buf.array(), EnumPlacementType.DRAW));
                    }
                }
                if (drawFactory instanceof IDrawListener) {
                    ((IDrawListener<T>) drawFactory).onFinishDrawing(player);
                }

                drawStack = null;
                drawCircuit = null;
                drawDataMap = null;
                this.drawFactory = null;
            }
        }
    }

    // @SubscribeEvent(priority = EventPriority.HIGHEST)
    // public void onDrawDragHighlight(DrawBlockHighlightEvent event) {
    //
    // if (drawStack != null) {
    // drawFactory.drawPlacement(drawStack, Minecraft.getMinecraft().thePlayer, drawData);
    // }
    // }

    @SubscribeEvent
    public void onRenderOverlayPre(RenderGameOverlayEvent.Pre event) {

        // Minecraft mc = Minecraft.getMinecraft();
        // ItemStack stack = mc.thePlayer.getHeldItemMainhand();
        // if (stack != null && stack.getItem() == SCM.component_tray && (event.getType() == ElementType.AIR
        // || event.getType() == ElementType.HEALTH || event.getType() == ElementType.ARMOR || event.getType() == ElementType.FOOD)) {
        // GlStateManager.translate(0, -19, 0);
        // }
    }

    @SubscribeEvent
    public void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {

        // Minecraft mc = Minecraft.getMinecraft();
        // ItemStack stack = mc.thePlayer.getHeldItemMainhand();
        // if (stack != null && stack.getItem() == SCM.component_tray && (event.getType() == ElementType.AIR
        // || event.getType() == ElementType.HEALTH || event.getType() == ElementType.ARMOR || event.getType() == ElementType.FOOD)) {
        // GlStateManager.translate(0, 19, 0);
        // }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {

        Minecraft mc = Minecraft.getMinecraft();
        ItemStack stack = mc.thePlayer.getHeldItemMainhand();
        if (stack != null && stack.getItem() == SCMItems.component_tray && event.getType() == ElementType.HOTBAR) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null) {
                stack.setTagCompound(tag = new NBTTagCompound());
            }
            int slot = tag.getInteger("slot");

            GuiIngame igg = mc.ingameGUI;
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);

            mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/widgets.png"));
            int i = event.getResolution().getScaledWidth() / 2;

            GlStateManager.pushMatrix();
            GlStateManager.translate(i - 91,
                    event.getResolution().getScaledHeight() - 23 - (mc.thePlayer.capabilities.isCreativeMode ? 21 : 48), 0);
            GlStateManager.translate(182 / 2D, 22 / 2D, 0);
            GlStateManager.scale(0.75, 0.75, 1);
            GlStateManager.translate(-182 / 2D, -22 / 2D, 0);

            igg.drawTexturedModalRect(0, 0, 0, 0, 182, 22);
            igg.drawTexturedModalRect(-1 + slot * 20, -1, 0, 22, 24, 24);

            GlStateManager.popMatrix();

            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableBlend();
        }
        stack = mc.thePlayer.getHeldItemMainhand();
        if (stack != null && stack.getItem() == SCMItems.palette_and_brush && event.getType() == ElementType.ALL) {
            renderPalette(mc.thePlayer.getPrimaryHand(), stack, event);
        } else {
            stack = mc.thePlayer.getHeldItemOffhand();
            if (stack != null && stack.getItem() == SCMItems.palette_and_brush && event.getType() == ElementType.ALL) {
                renderPalette(mc.thePlayer.getPrimaryHand().opposite(), stack, event);
            }
        }
        // MinecraftForge.EVENT_BUS.unregister(this);
        // MinecraftForge.EVENT_BUS.register(this);
    }

    private void renderPalette(EnumHandSide hand, ItemStack stack, RenderGameOverlayEvent.Post event) {

        if (Minecraft.getMinecraft().currentScreen != null) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            stack.setTagCompound(tag = new NBTTagCompound());
        }
        int active = tag.getInteger("color");

        int size = 62;
        int off = 17;
        int b = 1;
        int cOff = 3;
        int cSize = (size - b * 2 - cOff * 5) / 4;

        int selInactive = 0xA010100F;

        int x = hand == EnumHandSide.LEFT ? off : event.getResolution().getScaledWidth() - size - off;
        int y = event.getResolution().getScaledHeight() - size - off;
        GuiUtils.drawGradientRect(0, x, y, x + size, y + size, 0x7010100F, 0x7010100F);
        GuiUtils.drawGradientRect(0, x, y, x + size, y + b, 0xA010100F, 0xA010100F);
        GuiUtils.drawGradientRect(0, x, y + size - b, x + size, y + size, 0xA010100F, 0xA010100F);
        GuiUtils.drawGradientRect(0, x, y + b, x + b, y + size - b, 0xA010100F, 0xA010100F);
        GuiUtils.drawGradientRect(0, x + size - b, y + b, x + size, y + size - b, 0xA010100F, 0xA010100F);

        for (int x_ = 0; x_ < 4; x_++) {
            int xOff = x + b + cOff * (x_ + 1) + cSize * x_;
            for (int y_ = 0; y_ < 4; y_++) {
                int yOff = y + b + cOff * (y_ + 1) + cSize * y_;
                EnumDyeColor dye = EnumDyeColor.byMetadata(y_ * 4 + x_);
                int color = dye.getMapColor().colorValue | 0xFF000000;
                if (active == (y_ * 4 + x_)) {
                    GuiUtils.drawGradientRect(0, xOff, yOff, xOff + cSize, yOff + cSize, color, color);

                    GuiUtils.drawGradientRect(0, xOff - b, yOff - b, xOff + cSize + b, yOff, selInactive, selInactive);
                    GuiUtils.drawGradientRect(0, xOff - b, yOff + cSize, xOff + cSize + b, yOff + cSize + b, selInactive, selInactive);
                    GuiUtils.drawGradientRect(0, xOff - b, yOff, xOff, yOff + cSize, selInactive, selInactive);
                    GuiUtils.drawGradientRect(0, xOff + cSize, yOff, xOff + cSize + b, yOff + cSize, selInactive, selInactive);
                } else {
                    GuiUtils.drawGradientRect(0, xOff + b, yOff + b, xOff + cSize - b, yOff + cSize - b, color, color);

                    GuiUtils.drawGradientRect(0, xOff, yOff, xOff + cSize, yOff + b, selInactive, selInactive);
                    GuiUtils.drawGradientRect(0, xOff, yOff + cSize - b, xOff + cSize, yOff + cSize, selInactive, selInactive);
                    GuiUtils.drawGradientRect(0, xOff, yOff + b, xOff + b, yOff + cSize - b, selInactive, selInactive);
                    GuiUtils.drawGradientRect(0, xOff + cSize - b, yOff + b, xOff + cSize, yOff + cSize - b, selInactive, selInactive);
                }
            }
        }
    }

    @SubscribeEvent
    public void wheelEvent(MouseEvent event) {

        int dwheel = event.getDwheel();
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        ItemStack stack = player.getHeldItemMainhand();
        if (dwheel != 0 && stack != null && stack.getItem() instanceof IScrollableItem) {
            event.setCanceled(((IScrollableItem) stack.getItem()).scroll(player, stack, dwheel));
        }
    }

}
