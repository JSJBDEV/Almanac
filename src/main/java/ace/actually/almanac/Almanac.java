package ace.actually.almanac;

import ace.actually.almanac.items.AlmanacItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.recipe.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Almanac implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("almanac");
	public static final Identifier ASTRA_PACKET = new Identifier("almanac","astra_packet");

	public static final AlmanacItem ALMANAC_ITEM  = new AlmanacItem(new Item.Settings());

	public static RecipeSerializer<AlmanacCloningRecipe> ALMANAC_CLONING = new SpecialRecipeSerializer<>(AlmanacCloningRecipe::new);
	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		Registry.register(Registries.ITEM,new Identifier("almanac","almanac"),ALMANAC_ITEM);
		LOGGER.info("It's time to map some stars");

		//fun fact: to actually make cloning work you also need to create a (recipe).json. see data/almanac/recipes/almanac_cloning.json
		Registry.register(Registries.RECIPE_SERIALIZER,new Identifier("almanac","almanac_cloning"),ALMANAC_CLONING);

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


	}
}