package ace.actually.almanac.items;

import ace.actually.almanac.Almanac;
import com.nettakrim.spyglass_astronomy.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AlmanacItem extends Item {
    public AlmanacItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        if(!world.isClient && !user.isSneaking())
        {
            NbtCompound compound = user.getStackInHand(hand).getNbt();
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeNbt(compound);
            ServerPlayNetworking.send((ServerPlayerEntity) user,Almanac.ASTRA_UPDATE_CLIENT_PACKET,buf);
        }
        if(world.isClient && user.isSneaking())
        {
            user.sendMessage(Text.translatable("almanac.wrote.help"));
        }


        return super.use(world, user, hand);
    }


    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if(context.getWorld().isClient && context.getPlayer().isSneaking() && context.getWorld().getBlockState(context.getBlockPos()).isOf(Blocks.CARTOGRAPHY_TABLE))
        {
            NbtCompound compound = new NbtCompound();
            NbtList astra = new NbtList();

            for(Star star: SpyglassAstronomyClient.stars)
            {
                if(!star.isUnnamed())
                {
                    astra.add(NbtString.of(reprocessor(share(star))));
                }
            }

            for(OrbitingBody body: SpyglassAstronomyClient.orbitingBodies)
            {
                if(!body.isUnnamed())
                {
                    astra.add(NbtString.of(reprocessor(share(body))));
                }
            }

            for(Constellation constellation: SpyglassAstronomyClient.constellations)
            {
                if(!constellation.isUnnamed())
                {
                    astra.add(NbtString.of(reprocessor(share(constellation))));
                }
            }
            compound.put("astra",astra);
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeNbt(compound);
            ClientPlayNetworking.send(Almanac.ASTRA_PACKET,buf);

        }
        return super.useOnBlock(context);
    }

    private static String share(Constellation constellation) {
        return "sga:c_"+(SpaceDataManager.encodeConstellation(null, constellation).replace(" | ", "|"))+"|";

    }

    private static String share(Star star) {
        String starName = (star.isUnnamed() ? "Unnamed" : star.name);
        return "sga:s_"+starName+"|"+ star.index +"|";

    }

    private static String share(OrbitingBody orbitingBody) {
        String orbitingBodyName = (orbitingBody.isUnnamed() ? "Unnamed" : orbitingBody.name);
        return "sga:p_"+orbitingBodyName+"|"+ SpyglassAstronomyClient.orbitingBodies.indexOf(orbitingBody) +"|";

    }

    private static String reprocessor(String message)
    {
        int sgaIndex = message.indexOf("sga:");
        if (sgaIndex == -1) return null;

        String data = message.substring(sgaIndex+4);
        int firstIndex = data.indexOf("|");
        if (firstIndex == -1) return null;
        int secondIndex = data.indexOf("|", firstIndex+1);
        data = data.substring(0, secondIndex == -1 ? firstIndex : secondIndex);
        if (data.charAt(1) != '_') return null;



        switch (data.charAt(0)) {
            case 'c' -> {
                //constellation shared with sga:c_Name|AAAA|
                String constellationName = data.substring(2, firstIndex);
                String constellationData = data.substring(firstIndex + 1, secondIndex);
                return "/sga:admin constellations add " + constellationData + " " + constellationName;

            }
            case 's' -> {
                //star shared with sga:s_Name|index|
                String starName = data.substring(2, firstIndex);
                int starIndex;
                int index = Integer.parseInt(data.substring(firstIndex + 1, secondIndex));
                try {
                    starIndex = index;
                } catch (Exception e) {
                    break;
                }
                return "/sga:admin rename star " + starIndex + " " + starName;

            }
            case 'p' -> {
                //planets shared with sga:p_Name|index|
                String orbitingBodyName = data.substring(2, firstIndex);
                int orbitingBodyIndex;
                int index = Integer.parseInt(data.substring(firstIndex + 1, secondIndex));
                try {
                    orbitingBodyIndex = index;
                } catch (Exception e) {
                    break;
                }
                if (orbitingBodyIndex >= SpyglassAstronomyClient.orbitingBodies.size()) break;


                return "/sga:admin rename planet " + orbitingBodyIndex + " " + orbitingBodyName;

            }
        }
        return null;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        if(stack.hasNbt())
        {
            tooltip.add(Text.of("Version "+stack.getNbt().getInt("version")));
            NbtList authors = (NbtList) stack.getNbt().get("authors");

            StringBuilder v = new StringBuilder("Authors: ");
            for (int i = 0; i < authors.size(); i++) {
                v.append(authors.getString(i)).append(", ");
            }
            String pcomma = v.toString();
            tooltip.add(Text.of(pcomma.substring(0,pcomma.length()-2)));
        }
    }
}
