package ace.actually.almanac.items;

import ace.actually.almanac.Almanac;
import com.nettakrim.spyglass_astronomy.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class AlmanacItem extends Item {
    public AlmanacItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if(world.isClient && user.isSneaking())
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
        if(!world.isClient && !user.isSneaking())
        {
            NbtCompound compound = user.getStackInHand(hand).getNbt();
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeNbt(compound);
            ServerPlayNetworking.send((ServerPlayerEntity) user,Almanac.ASTRA_UPDATE_CLIENT_PACKET,buf);
        }


        return super.use(world, user, hand);
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
                if (secondIndex == -1) return null;
                String constellationName = data.substring(2, firstIndex);
                String constellationData = data.substring(firstIndex + 1, secondIndex);
                return "/sga:admin constellations add " + constellationData + " " + constellationName;

            }
            case 's' -> {
                //star shared with sga:s_Name|index|
                if (secondIndex == -1) return null;
                String starName = data.substring(2, firstIndex);
                int starIndex;
                try {
                    starIndex = Integer.parseInt(data.substring(firstIndex + 1, secondIndex));
                } catch (Exception e) {
                    break;
                }
                return "/sga:admin rename star " + starIndex + " " + starName;

            }
            case 'p' -> {
                //planets shared with sga:p_Name|index|
                if (secondIndex == -1) return null;
                String orbitingBodyName = data.substring(2, firstIndex);
                int orbitingBodyIndex;
                try {
                    orbitingBodyIndex = Integer.parseInt(data.substring(firstIndex + 1, secondIndex));
                } catch (Exception e) {
                    break;
                }
                if (orbitingBodyIndex >= SpyglassAstronomyClient.orbitingBodies.size()) break;


                return "/sga:admin rename planet " + orbitingBodyIndex + " " + orbitingBodyName;

            }
        }
        return null;
    }
}
