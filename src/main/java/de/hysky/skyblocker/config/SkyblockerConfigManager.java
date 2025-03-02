package de.hysky.skyblocker.config;

import java.lang.StackWalker.Option;
import java.nio.file.Path;

import com.google.gson.FieldNamingPolicy;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import de.hysky.skyblocker.SkyblockerMod;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import de.hysky.skyblocker.config.categories.DiscordRPCCategory;
import de.hysky.skyblocker.config.categories.DungeonsCategory;
import de.hysky.skyblocker.config.categories.DwarvenMinesCategory;
import de.hysky.skyblocker.config.categories.GeneralCategory;
import de.hysky.skyblocker.config.categories.LocationsCategory;
import de.hysky.skyblocker.config.categories.MessageFilterCategory;
import de.hysky.skyblocker.config.categories.QuickNavigationCategory;
import de.hysky.skyblocker.config.categories.SlayersCategory;
import de.hysky.skyblocker.utils.scheduler.Scheduler;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SkyblockerConfigManager {
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("skyblocker.json");
	private static final ConfigClassHandler<SkyblockerConfig> HANDLER = ConfigClassHandler.createBuilder(SkyblockerConfig.class)
			.serializer(config -> GsonConfigSerializerBuilder.create(config)
					.setPath(PATH)
					.setJson5(false)
					.appendGsonBuilder(builder -> builder
						.setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
						.registerTypeHierarchyAdapter(Identifier.class, new Identifier.Serializer()))
					.build())
			.build();
	
	public static SkyblockerConfig get() {
		return HANDLER.instance();
	}
	
	/**
	 * This method is caller sensitive and can only be called by the mod initializer,
	 * this is enforced.
	 */
	public static void init() {
		if (StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE).getCallerClass() != SkyblockerMod.class) {
			throw new RuntimeException("Skyblocker: Called config init from an illegal place!");
		}
		
		HANDLER.load();
		ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal(SkyblockerMod.NAMESPACE).then(optionsLiteral("config")).then(optionsLiteral("options")))));
	}
	
	public static void save() {
		HANDLER.save();
	}
	
	public static Screen createGUI(Screen parent) {
		return YetAnotherConfigLib.create(HANDLER, (defaults, config, builder) -> builder
				.title(Text.translatable("text.autoconfig.skyblocker.title"))
				.category(GeneralCategory.create(defaults, config))
				.category(DungeonsCategory.create(defaults, config))
				.category(DwarvenMinesCategory.create(defaults, config))
				.category(LocationsCategory.create(defaults, config))
				.category(SlayersCategory.create(defaults, config))
				.category(QuickNavigationCategory.create(defaults, config))
				.category(MessageFilterCategory.create(defaults, config))
				.category(DiscordRPCCategory.create(defaults, config))).generateScreen(parent);
	}
	
	/**
	 * Registers an options command with the given name. Used for registering both options and config as valid commands.
	 *
	 * @param name the name of the command node
	 * @return the command builder
	 */
	private static LiteralArgumentBuilder<FabricClientCommandSource> optionsLiteral(String name) {
		// Don't immediately open the next screen as it will be closed by ChatScreen right after this command is executed
		return ClientCommandManager.literal(name).executes(Scheduler.queueOpenScreenCommand(() -> createGUI(null)));
	}
}
