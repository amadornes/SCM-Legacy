package com.amadornes.rscircuits.item;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.circuit.Circuit;
import com.amadornes.rscircuits.part.PartCircuit;
import com.amadornes.rscircuits.util.ItemPool;

import mcmultipart.MCMultiPartMod;
import mcmultipart.raytrace.PartMOP;
import mcmultipart.raytrace.RayTraceUtils;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class ItemBlueprint extends Item implements ICircuitStorage {

    private static int id = 0;
    public static final List<NBTTagCompound> BLUEPRINTS = Arrays//
            .asList(new String[] { //
                    "H4sIAAAAAAAAAL3Vu07DMBQG4N/YSVpX4rLyKEzMDDCAWKPItVpLiY9lu43YeB6egjeDXIamkahANPEWK8efj470WwJLSEWVI6ttDBLApUCS544C+sUZsk1JtbEbcCSKSvLNNlsgzfP45jSufFDGq52J4a42XnOIUFJsixkSR7X2X80he+2DIcs5Voqs1So2X63ygQF5Pz/5PgcpjsjPvmxaUsoxyeYnLyYnH5rfls7r/XMsomYjnx+g6wEUyavtsRS6coa0KkLUno07EZN3cjsmk/nnlc5A/jgv8Y95YdAJOxFc/E+d4Lcpwk4E11TkieA6H9kE1wqZoYrWur2BwGgtIGxRafDHpxeOm8Nr8tqf2IaARLZz62ZeobukL8K2q/0GKXkHo4EGAAA=",
                    "H4sIAAAAAAAAAL2VsW7CMBCGz9hJwAy0YusjdEKAmtKJSp06dKnUNUqDBZFILrINqFvnPgpPwZu1DkgQIhIVCFiKFDvy/9n/Xe44QAN4gFGCsYi14gDQZmB5XoIKNoMScMZTXITxGChYAU5RmmVSB9vz9FcioCVVEMpgFmr1tAiloMDUFHW6mYCV4EJI8+LMhVQhxpRCM8A4FoE2M0NhS/O1kUgxf9e+FpDhD1P+DnSTAWmUwWSfpNbbCdiRr7SQZE+WZGXZ0bJkKwvZA36bp3bYoNZRBv2WGGTf55EFMakOyXgeyS5+y58scrXZdghJK8s8fpdHkosjeWFWpvzaqVlZnOyr4/+h/8qy02VzStYVfM8h7YsjX4vNY2eEGkplz6hrZbInhzpbLklJP6nOd1hmkcPrI0sqdHVI0zWb4IQY4UikJ2CQG3VgsR8JoM9vLxRud03+Y6OYVhsOziwZmXiZzu/0Bv2e2+lyaHfd/qPrDjoPg77rfaLWGJmy1dm22HUYzaWkrybryR8/8db0SAgAAA==",
                    "H4sIAAAAAAAAAL2VO04DMRCGf2PvJnEKHiVH4AhUIFFRpEGiXa0cK1kp61nZTiI6ao6SU3Az2E2KbCywRIgzkgvbmv+bhzQjgRGkoroho413EsCNQFYUDTnsjDMMZgtaV2YGjkzRgmz7zIbIi8K/NRqX1qnKqmXl3f26sppDuAX5zpkha2it7VcrstLWVWQ4x1iRMVr59tZRNughH86PfG/PRWpkfhcik2cpZIgUybP86CM/d24/IfmfkIgg5W2IZMmRsv0dNVavXnzpNQv4F3vQVQ/kyar5Iclt3RnyunReWxaV5WlkxfGygVJ2hroHyDw58vn34ol/tBpR2aNbHZc9utXo1Z1FlsLp6n4woVlkKaRCRib06ZBigzEGFdU01V0EAoENIUxZa4jJ4+SJ43q/ql93kt24kRgsm2nbMLeN0pZuvnX+Bt7Y/cDeBwAA",
                    "H4sIAAAAAAAAAL3VMW7DIBQG4J+A45Soirr2KJ06d4nUoUMXyyIoRbJ5CEisbj1PT9GjFddDE6v1ZMMESI8PhPiRwA2kotaR1TYGCeBWoKgqRwFD4wzlsaHO2CM4CkUN+TTNNlhXVXx3GjsflPHqZGJ46IzXHCI0FPtihsJRp33qlGftgyHLObaKrNUqplGvfOKCfMxPfuQgxRX5NZT9RfLZSHk/JtnipByTq/zkP3e5m+8uX8ekyH/KIj+5Xpx8uiDZRPrMR15FAZtIn6XIifSZj0zps0VpqKWD7ncgMGobCFu3Gqv9M8fd74/wMizYP2SJ8uQOdUwL9Hv0dXj7Kf0GI6Cge0UGAAA=",
                    "H4sIAAAAAAAAAL3VzUrEMBQF4BOTtjMdUGfro7hy7UJBwW0pmTATaHNDkpnizufxKXwz7c/CtmhBnDa7Bm6+3F44SYE1UkmlJaNM8CmAS4Eoyyx5dIszJPuCKm324IgkFeTqbbZCnGXh1SpcOS+1k0cd/G2lneIQvqDQFDNElirlPutDTsp5TYZzbCQZo2SovxrlHT3ybnnybQlSDMiPruwnkv+JxASZ3oxJNjuZjsmLBUiGtXXq9BzyoNjI70HXPSiQk4eh5NtyhrjMfVCOjTsRy/+8aHkynp28/31e4h/zQq8TNhFc5+tkkCJsIrjmIieC63xkHVwbJJpK2qnmBgKjtYIweanAHx6fOLbfr8lLd2ITAimSo93V8/LtJV3uD23tF6OAfY2BBgAA",
                    "H4sIAAAAAAAAAL2XTU7DMBCFx7XThCDxIy7AuivUVLSw4gZIICF2UUgtCDRxZLutYMWaI3AETsE5OAobcFKJJm5dUYibVZx4/OV55tmOD7AFfszSnGU0k8IHgEMCThjmTMDswgjc2xGbJtktYHBiNmJcPUYetMNQPuYUdrmIEx6PEylOpwmnGIgYMVkEI3ByNqVc3bgTykXCMoxhO2ZZRmOpWgXlDSrIsyLKNrLV0ZEt20jf15HWJ9ZbQBLbyO0FpGM9ly9V5PMmyoe86UjrKuvI91nYMiRubmI/1NutnNPJpYwk1fkVbXsVkGQ8vquTRBmOoJ1GQlKO9JEM3ttdS8nXCiU7vo40eK85ZGsBafBeoypX5ctpLF9t65X3VFOCqnyyduWhHyWwctg1J6g67HwkBJuwZm3LLJGGNW+96lq5mdwX3ZIsH8sCMKSj6LEI1b6jpd7JJH5Q7Ul3+YSWocsqTtdk3aTk81eaSEXT+T81GTaKBvP0qiOt27VejWebMIDX0ZEGAzSHrB3gSqRh32pQpW9asEo+/uuChXQlhr2pUSUa0mAEm/mybgTvxZwv8o98qXM1uAlL2ZAWdiOgXR6QLEop4OvzCwz78/+5q9mHQnkeccf5UA2rfvLc4KQX9HsDHw4G3d5xEPS7R4OgH94wKVmqiu5ovn5BvVOwvBOUE8ojcVc2vgGg9rOzWA4AAA==",
                    "H4sIAAAAAAAAAL2VzUoDMRDHJ/vd9ODXzUcoCKVbbPWkeBHxZMXrsm5Du9DNLEna4s2zj+JT+GaabsFug65UdhvIYRLm/yMz4T8UoAU0wSxHzriSFACOHXCjKEcJ62UT8CczXKZ8Aja4Cc5Q6GMSgBdF6iVncCBkkopknip5uUwFs8GRM1SrZAJujksmPrXIggmZIrdtaCfIOUuUjlaUdyghr/aPfNXbahppdUxk468MqIl0mkZ6b2XkxzrtJ6S9ExIqkPTURJLGkVTftnLBFiMVK2byrQ3osARSKJLpNkkW6QS8LJaKCbIlSwxZZzdZUpI1lNw9FMhAeo0j734vnrNzTzbFg0rZf/cESgUiUJcBVRWo1TGRtRhQFdKiJrIWA6pCBmUDIrUNkyrk1jAhtTn7H8g2+ClmOGar0AFjBeDwOGMQPIzO7q8fb25tONqM+ae1LBQd8uf5WH9NPfv98KIfDgYhhZNhr38ehoNedxj2o2dUCjPted1vyyq+j36ZiOW0CL4AZjD6YkoIAAA=",
                    "H4sIAAAAAAAAAL3VsU7DMBAA0DN2mtQVAlb+gR9gArEAYqxYo8o9FUuJz7KdRmx8D1/Bn4GTDE0i1C5pPMXW3b1LJF8kwBKkotKSQRO8BIBLAUmeW/LQLc4g3RVUa7MDDomiglw8Zhks8jx8WoQr55V2qtLB39faIQfhCwpNMoPEUo0uPqR7dF6T4RxWioxBFeKuUb6hRz7MT37NQYoB+dOl/UfyyUh5OybZ2Uk5Ji9mIGOYNrYKYzvGZo7Cpons3n5pHe5f2thDA9e9BgqMzrADHwsgG9cW83/KZH5ycXbytUeyyebN7xGS9W8im2zeHCMH84ZNNm9OkCtINZW0xWYrYLQyEGZTIqTru7fH9dMzh5vDj+C9q9rcXwlpZbfxCvi2UbfxH23+HytzJQU8BgAA",
                    "H4sIAAAAAAAAAL2UTW7CMBCFnxungVCJVr0IECChq26666ISardRGixIReLINqDuOEePwCm4WeuABAY1SJEAr/wzft/42WMXqMONeZrzjGVKugDuKOwwzLnEtlkEznjKF0k2hgU75lMu9DSp4TYM1XfO0BQyTkQ8S5R8WiSCWaByylWxmcDO+YIJ3XHmTMiEZ5aFRsyzjMVKjwrKCgby+RpIutKr9Vyw+VBFipn8ZcHfg+4NkOIinhyS5GY7wW0aScUEOZAlpiytLEt2sjATXJcb1Kxk0O8pg75KDSr49PwGrWllWdOgElmC6tdJjGyPlOilfa/9lJ/kXC+InCgx6zJVTZbXQOqqbsBJeMpHrMiA4qjVQLMoZbDf3l+HLxYe9j/fx1ZTB924cGb5SNunv0PHG3Q932+5eAw63b7n+Z1W4PXDT64UT12Q1u6NAzqo1x34vV7Q9oJB+/8gbM4uIjnZDP4A5x0Sc4IFAAA=",
                    "H4sIAAAAAAAAAL2WTW7bMBCFh5b8EyVAWqQHyDorNzLcpCsXRRYtAnSRIItuBJUibDYSRyCpGNll3SP0CD1FzpNLtKSEJDRhGXBhW4AAURq+j0O+oRgB7EFEsShRMKFVBABHIXSTpEQFzRUQ6E9znHMxhQC6FHOU5jUZQC9J9H3J4FAqyiWtuFYf51yyAEKVo7adCXRLnDNpHvp3TCqOIghgn6IQjGrTspQ/4CAnu0AOTnxkuG1k55eLfDB3ZznycC3k31XInzaMi7LSPtvEDiTq1EY2cnulZHdf6tjXAbxxBpAzw1kcgTICjPjaW59K+O0iH5tu20VGkY8kW0d+tboZy9N70+Odh++Yb5rT27pKly+Y5oVdsF7OxFTPmj7OCHo50luWmTwO1AyrPPtWabv6fqItxbieTVclevD92X5X1k8+P2zJDiWdLbGjzaxIlWaSLMgSVzZcW5a8yLoTRKC9joPN+f3JR7aU2Oa2jtDdk8lkF1ku7MlkxW9gc8hO1GaRmv/fFml1HplszHkPu5ggY4N96HMsMGPWiCF41wBCkRYMoqvrT9cXx58vLi8DePt6oLhphKGe635VZiYhc8og7yM4OjsdjeP4w+nwLB4lP1BrLMyXoYk1ofH5KB4PzxfDxk7YS73XZxSTvEzVrG78A2d/P73VCAAA",
                    "H4sIAAAAAAAAAL1WzW6cMBAeLz8LJFJaNYeqj9DTkna7uzn10kN7iaJKuaItWLtOwINsk1VuPfcR+gh5ipz7Uq0NhwBhkSoBSEjAePzNN575mADAhyDGLEdOuZIBALy1wYmiHCVUl0VgvkvxwPgOLHBiTFHoz8QDN4rUQ07hTMiYibhgSl4emKAW2DJFZZwJODkeqNAP83sqJENuWXASI+c0VvpNo9iPUIP8rO9ZN+TZYJDue7OM8bxQbWy91hOotmZlxd7PBb3/Wq59DuBVLYCUapxmBFJvQKG9tz06r191yJ/Ga2zI2a3ZN6Hp9kF7nLfgZ9qmWHxXFlN39hTLTPbclPKd2lc+tQjcFOM7mmgep3KPRZpcFcocRZvokTL9P6J/e4j6f/qI2pMRdUY/0d91yKfKbVwJCL519qPBJs1+hKH68WkonemrGetFKgeRgD7I09ujqXSaqZwNmUp39BJ5V4MkMIW0ecekrYSfRNpIzx94uJpxL/uITiJtJdLo0ubV+5FMMmr4naMG6Rg1yED9SCYZNfz6qEF6/sDDSQB5hBOYM8wwoaYYbWhdHth8m1Gwvn+5tuD181x7U+1ozjuAeZEnOmV62CVhAOfhYrNZhuGni9VqtY5+oFKYadNCS07DuF60jG+WHzer5XIdflhvLnpsTT8ocyO2cl+G/A/tMR9/ggsAAA==",
                    "H4sIAAAAAAAAAL2XzW7aQBDHx/gzpmpSpe2h195ywsTYOKdEPRWpSpVWvVqOsRJL4LV2F1B74syj9BF6yrmPkEfII/RC1yYJy8IiRdj4ZNa78/PM/GcG2wAHYMdomKMsySixAeCjBnoY5ojA4lIUMG8GaJJmN6CCHqMBwsWyBUYY0p95AoeYxCmORyklZ5MUJypoZIBoeRj0HE0SzG7McYJJijJVhWaMsiyJKfvFKI0TEJCNupGGLSLVupGvZuypgUY0H1FFgBtLyhFHYecJ5TD8O5/vIzPwICIbJjTSjN3oc4A5o1oY0ajYXsRwCX7HgcnomuIopgiv4FUWizWXJGk4fJFL8y0umTMRaVSBVLYgld88crr/xE3lJVVdYLUzEak+a6WQilwrvOSjfj9Zl8lcNK1V4c22AKq2iNTrRr7uiUhjGUAFxAA2diu2u8X6JpfU6jr7vYisXfnaP/b0IMfJ+BsLVaII/MZm2bFwxberJFIeZy17GBHKClz0pJJOtXVg9ESkxhXUmh52bL53colXpwf1r4iUNN/qkNYvuR60HfTAB0+BfRSTdSIiFX4YV9sfSvuSmaFXNjOavWJbmrG/RCJbXfXmKYmfy72bkzZIxsLseEyaaFsyPV7m19ZU3YvI2ksL/vDIc3mrrRD5ICL37OV0H16ult1UXhbVISVlMV2FvOcgaTYOSRwxG1g8IRF7hW/7QUTWLgNrBk0wUzRE/aRQhQbCZYGWRcME7E+XX75eXF18v7xS4c3yO/fHwnCRTBvMUd5nbYJ9/JqnQdf3Wx0bjjtu4Hc6Xee0GzjhNaIUDcGGt47nBm3Pd13H8/zHdRuU1nO+oNzVCoKO43htn13L08dt32X2g5YXuB63zLHcp2UoI4cjcls69B9rhjqzqQ8AAA==",
                    "H4sIAAAAAAAAAL2WvU7DMBDHz9hpUnfgQ2JhYmRmZEJCbBEbrFHkWsVS4rNsl4iNkWfpUyAei4l8SDRKpA4Vtifb0v1/d7o7nznAErjA2qCW2jsOAJcMkqIw6GBYlEC6qbBRegMUEoEV2vY6y2BRFP7dSDi1Tigrtsq7u0ZZSYG5Cn1nTCAx2EjbbtI3aZ1CTSmsBGothW9PHWUHI+R9fORHfOTXYBYWya+mSBIcyafIk+DI7xlyr3020jZV6Ufi5IDZxdgMnS+s+3NllliYCoWvpZ8Z8riQWfDk3M6QR3r6X8lJ4rfAIjKSxei6mykyeNXPowxfvp8jJIk/DUn8aUiiTEO2gxWkCmtcS9c36WRlwHRZS1g+Pef59cNjnlM4339UXgbd7tXmkG7Nun09XO+qLd1rr/AL2rG0mNwIAAA=",
                    "H4sIAAAAAAAAAL1W3U7CMBQ+tR0bI/En3BgfwSuE4ZhXXpN4ZTTGmwVHA0vYurQF4h3P4SP4FDyHL6MdJDAmWwSKTZasW/t95zvn9PTYAFWwAxYlLKaxFDYAXBIwfD9hApYDIzAHIzYN4wFgMAI2Ylx9RhZUfF++JxTOuAhCHoxDKe6mIacYiBgxmW5GYCRsSrl6MSeUi5DFGEMtYHFMA6lmioV8QobyXj0n2ynPtFFWrvOUBSotbZSWnackx1Z52lV/qwmnk0fZkzTPb6yJzjNEkvFguMkkFtsRVKKekJSjDViUhSU7w6IVLBRaO0tzQo+1eViyP2wOydARzu+yc/JR6PcZ2dlBRX7Pw+7ooCxsBmm+XLPNQVhbvttXhUrmmxVrNyUor0RLfSoLtfVaqgRrU6KlBpUq+SpVsnex+KWkcvTs6hYrIQdkV/HxS2H3DnU57AFFeo2EStoDfX6HbHuASu7qY1HO/oNSNUE1MEMWsT5NLSCQGxaQuBdRwA9PLxgu1j3b8xIxLUs2mOOkr+KlGjmz5Tlus+PYUG+6Tsd1vcat53j+G5OSRTagxupOU7kN9bbjue1256bV8Rp/WdTcvggW7uE9MVxMfgDD8B9LYQoAAA=="
            //
            }).stream()//
            .map(Base64.getDecoder()::decode)//
            .map(ByteArrayInputStream::new)//
            .map(ItemBlueprint::safeReadCompressed)//
            .map(t -> {
                t.setInteger("blueprintID", id++);
                return t;
            }).collect(Collectors.toList());

    private static NBTTagCompound safeReadCompressed(InputStream is) {

        try {
            return CompressedStreamTools.readCompressed(is);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public ItemBlueprint() {

        setUnlocalizedName(SCM.MODID + ":blueprint");
        setMaxStackSize(1);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {

        return "item." + SCM.MODID + (stack.getItemDamage() < 2 || stack.getItemDamage() == 4 ? ":blueprint" : ":redprint");
    }

    @Override
    public boolean getHasSubtypes() {

        return true;
    }

    @Override
    public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> subItems) {

        subItems.add(new ItemStack(item, 1, 0));
        subItems.add(new ItemStack(item, 1, 2));
        for (NBTTagCompound tag : BLUEPRINTS) {
            ItemStack is = new ItemStack(item, 1, 4);
            is.setTagCompound(tag.copy());
            subItems.add(is);
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {

        if (stack.getItemDamage() == 4 && stack.hasTagCompound() && stack.getTagCompound().hasKey("name")) {
            String name = stack.getTagCompound().getString("name");
            if (!name.isEmpty()) {
                return TextFormatting.RESET + super.getItemStackDisplayName(stack) + ": " + name;
            }
        }
        return super.getItemStackDisplayName(stack);
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, IBlockAccess world, BlockPos pos, EntityPlayer player) {

        return true;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand) {

        Vec3d start = RayTraceUtils.getStart(player);
        Vec3d end = RayTraceUtils.getEnd(player);
        RayTraceResult result = world.rayTraceBlocks(start, end);
        if (result != null && result instanceof PartMOP && ((PartMOP) result).partHit instanceof PartCircuit) {
            PartCircuit circuit = ((PartCircuit) ((PartMOP) result).partHit);
            if (stack.getItemDamage() % 2 == 0 && stack.getItemDamage() < 4) {
                if (player.isSneaking()) {
                    if (!world.isRemote) {
                        ItemStack finalStack = new ItemStack(stack.getItem(), 1, stack.getItemDamage() + 1);
                        NBTTagCompound tag = new NBTTagCompound();

                        if (stack.getItemDamage() == 2) {
                            circuit.onRemoved();
                            circuit.circuit.writeToNBT(tag);
                            circuit.circuit.clear();
                            circuit.sendUpdatePacket();
                            world.notifyNeighborsOfStateChange(circuit.getPos(), MCMultiPartMod.multipart);
                        } else {
                            circuit.circuit.writeToNBT(tag);
                        }

                        finalStack.setTagCompound(tag.copy());
                        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, finalStack);
                    }
                    return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
                }
            } else {
                if (circuit.circuit.isEmpty()) {
                    if ((stack.getItemDamage() == 1 || stack.getItemDamage() == 4) && !player.capabilities.isCreativeMode) {
                        Circuit circuit2 = new Circuit(null);
                        circuit2.readFromNBT(getTag(stack));
                        ItemPool requiredItems = new ItemPool();
                        circuit2.forEach(c -> c.getPlacementItems().forEach(requiredItems::add));
                        ItemPool playerInventory = new ItemPool();
                        Arrays.stream(player.inventory.mainInventory).filter(Predicate.isEqual(null).negate())
                                .forEach(playerInventory::add);
                        Pair<ItemStack, Integer> missing = playerInventory.getFirstMissing(requiredItems);
                        if (missing != null) {
                            if (!world.isRemote) {
                                player.addChatMessage(new TextComponentString(
                                        TextFormatting.RED + "You need: " + missing.getValue() + "x " + missing.getKey().getDisplayName()));
                            }
                            return new ActionResult<ItemStack>(EnumActionResult.FAIL, stack);
                        }
                        requiredItems.getRawItems().forEach((d, s) -> {
                            player.inventory.clearMatchingItems(d.getStack().getItem(), d.getStack().getItemDamage(), s,
                                    d.getStack().getTagCompound());
                        });
                    }
                    if (!world.isRemote) {
                        try {
                            circuit.circuit.readFromNBT(getTag(stack), false);
                            circuit.circuit.forEach(IComponent::onLoaded);
                            circuit.circuit.forEach(IComponent::onCircuitAdded);
                            world.notifyNeighborsOfStateChange(circuit.getPos(), MCMultiPartMod.multipart);
                            circuit.sendUpdatePacket();
                        } catch (Throwable t) {
                            // t.printStackTrace();
                            circuit.circuit.clear();
                            circuit.circuit.onCrash(t);
                            return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
                        }
                    }
                    return new ActionResult<ItemStack>(EnumActionResult.SUCCESS,
                            stack.getItemDamage() == 3 ? new ItemStack(stack.getItem(), 1, stack.getItemDamage() - 1) : stack);
                }
            }
        } else {
            if (player.isSneaking() && (stack.getItemDamage() % 2 == 1 || stack.getItemDamage() == 4)) {
                NBTTagCompound tag = getTag(stack);
                boolean alreadyClicked = tag.hasKey("deleting") && world.getTotalWorldTime() - tag.getLong("deleting") < 40;
                if (alreadyClicked) {
                    if (stack.getItemDamage() == 3) {
                        Circuit circuit = new Circuit(null);
                        circuit.readFromNBT(tag);
                        ItemPool pool = new ItemPool();
                        circuit.forEach(c -> c.getDrops().forEach(pool::add));
                        pool.getItems().forEach(s -> {
                            if (!player.inventory.addItemStackToInventory(s)) {
                                InventoryHelper.spawnItemStack(world, player.posX, player.posY, player.posZ, s);
                            }
                        });
                    }
                    return new ActionResult<ItemStack>(EnumActionResult.SUCCESS,
                            new ItemStack(stack.getItem(), 1, stack.getItemDamage() == 4 ? 0 : stack.getItemDamage() - 1));
                } else {
                    stack.getTagCompound().setLong("deleting", world.getTotalWorldTime());
                    return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
                }
            }
        }
        return super.onItemRightClick(stack, world, player, hand);
    }

    private NBTTagCompound getTag(ItemStack stack) {

        return stack.getItemDamage() == 4 ? BLUEPRINTS.get(stack.getTagCompound().getInteger("blueprintID")).copy()
                : stack.getTagCompound();
    }

    @Override
    public NBTTagCompound getCircuitData(EntityPlayer player, ItemStack stack) {

        if (!stack.hasTagCompound()) {
            return null;
        }
        NBTTagCompound stackTag = stack.getTagCompound();
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("components", stackTag.getTag("components").copy());
        tag.setTag("componentsVersion", stackTag.getTag("componentsVersion").copy());
        tag.setTag("updates", stackTag.getTag("updates").copy());
        tag.setTag("name", stackTag.getTag("name").copy());
        tag.setTag("iomodes", stackTag.getTag("iomodes").copy());
        return tag;
    }

    @Override
    public boolean canOverrideCircuitData(EntityPlayer player, ItemStack stack) {

        return stack.getItemDamage() != 4 && (stack.getItemDamage() == 0 || player.capabilities.isCreativeMode);
    }

    @Override
    public ActionResult<ItemStack> overrideCircuitData(EntityPlayer player, ItemStack stack, NBTTagCompound tag) {

        stack = stack.copy();
        stack.setItemDamage(((stack.stackSize >> 1) << 1) + 1);
        stack.setTagCompound(tag.copy());
        return ActionResult.newResult(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public NBTTagCompound getNBTShareTag(ItemStack stack) {

        if (stack.hasTagCompound()) {
            NBTTagCompound tag = stack.getTagCompound().copy();
            tag.removeTag("components");
            tag.removeTag("updates");
            tag.removeTag("components");
            tag.removeTag("componentsVersion");
            tag.removeTag("updates");
            tag.removeTag("iomodes");
            return tag;
        } else {
            return super.getNBTShareTag(stack);
        }
    }

}
