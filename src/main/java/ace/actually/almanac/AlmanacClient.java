package ace.actually.almanac;

import com.nettakrim.spyglass_astronomy.*;
import com.nettakrim.spyglass_astronomy.commands.admin_subcommands.ConstellationsCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class AlmanacClient implements ClientModInitializer {
    public static final Identifier ASTRA_UPDATE_CLIENT_PACKET = new Identifier("almanac","astra_update_client_packet");

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ASTRA_UPDATE_CLIENT_PACKET,((client, handler, buf, responseSender) ->
        {
            NbtCompound compound = buf.readNbt();
            client.execute(()->
            {
                NbtList astra = (NbtList) compound.get("astra");
                for (int i = 0; i < astra.size(); i++) {
                    if(astra.getString(i).contains("constellations"))
                    {
                        //"/sga:admin constellations add constellationData constellationName,
                        String[] v = astra.getString(i).split(" ");
                        Constellation constellation = SpaceDataManager.decodeConstellation(null, v[4], v[3]);
                        ConstellationsCommand.addConstellation(constellation,true,true);
                    }
                    if(astra.getString(i).contains("star"))
                    {

                        //"/sga:admin rename star starIndex starName;
                        String[] v = astra.getString(i).split(" ");
                        Star star = SpyglassAstronomyClient.stars.get(Integer.parseInt(v[3]));
                        String name = v[4];
                        if (star.isUnnamed()) {
                            SpyglassAstronomyClient.say("commands.name.star", name);
                        } else {
                            SpyglassAstronomyClient.say("commands.name.star.rename", star.name,name);
                        }

                        star.name=name;
                        star.select();
                        SpaceDataManager.makeChange();
                    }
                    if(astra.getString(i).contains("planet"))
                    {
                        //"/sga:admin rename planet  orbitingBodyIndex orbitingBodyName;
                        String[] v = astra.getString(i).split(" ");
                        OrbitingBody orbitingBody = SpyglassAstronomyClient.orbitingBodies.get(Integer.parseInt(v[3]));
                        String name = v[4];
                        if (orbitingBody.isUnnamed()) {
                            SpyglassAstronomyClient.say("commands.name."+(orbitingBody.isPlanet ? "planet" : "comet"), name);
                        } else {
                            SpyglassAstronomyClient.say("commands.name."+(orbitingBody.isPlanet ? "planet" : "comet")+".rename", orbitingBody.name, name);
                        }
                        orbitingBody.name = name;
                        orbitingBody.select();
                        SpaceDataManager.makeChange();
                    }

                }
                client.player.sendMessage(Text.translatable("almanac.learnt"));
            });
        }));
    }
}
