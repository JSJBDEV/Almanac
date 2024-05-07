package ace.actually.almanac;

import ace.actually.almanac.items.AlmanacItem;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nettakrim.spyglass_astronomy.*;
import com.nettakrim.spyglass_astronomy.commands.admin_subcommands.ConstellationsCommand;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Almanac implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("almanac");
	public static final Identifier ASTRA_PACKET = new Identifier("almanac","astra_packet");
	public static final Identifier ASTRA_UPDATE_CLIENT_PACKET = new Identifier("almanac","astra_update_client_packet");

	public static final AlmanacItem ALMANAC_ITEM  = new AlmanacItem(new Item.Settings());
	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		Registry.register(Registries.ITEM,new Identifier("almanac","almanac"),ALMANAC_ITEM);
		LOGGER.info("It's time to map some stars");

		ServerPlayNetworking.registerGlobalReceiver(ASTRA_PACKET,((server, player, handler, buf, responseSender) ->
		{
			NbtCompound newAstra = buf.readNbt();
			server.execute(()->
			{
				NbtCompound compound;
				if(player.getMainHandStack().hasNbt())
				{
					compound = player.getMainHandStack().getNbt();
					compound.put("astra",newAstra.get("astra"));
				}
				else
				{
					compound = newAstra;
				}

				if(compound.contains("version"))
				{
					compound.putInt("version",compound.getInt("version")+1);
				}
				else
				{
					compound.putInt("version",1);
				}

				if(compound.contains("authors"))
				{
					NbtList authors = (NbtList) compound.get("authors");
					boolean canAdd = true;
					for (int i = 0; i < authors.size(); i++) {
						if(authors.getString(i).equals(player.getName().getString()))
						{
							canAdd=false;
							break;
						}
					}
					if(canAdd)
					{
						authors.add(NbtString.of(player.getName().getString()));
						compound.put("authors",authors);
					}

				}
				else
				{
					NbtList authors = new NbtList();
					authors.add(NbtString.of(player.getName().getString()));
					compound.put("authors",authors);
				}


				player.sendMessage(Text.translatable("almanac.wrote"));
			});
		}));

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