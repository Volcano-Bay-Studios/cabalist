package xyz.volcanobay.cabalist.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.system.spell.SpellDictionary;
import xyz.volcanobay.cabalist.system.spell.SpellEngine;

import java.util.List;
import java.util.TreeMap;

@EventBusSubscriber(modid = Cabalist.MODID)
public class CabalistCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(registerSuperpositionCommand());
    }

    public static LiteralArgumentBuilder<CommandSourceStack> registerSuperpositionCommand() {

        return Commands.literal("cabalist").requires(stack -> stack.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("debug")
                        .then(Commands.literal("compare")
                                .then(Commands.argument("dictionary", ResourceLocationArgument.id()).suggests(CabalistSpellDictionary.spellDictionarysSuggestionProvider())
                                        .then(Commands.argument("string", StringArgumentType.string()).executes(context -> {
                                                    ResourceLocation dictionary = ResourceLocationArgument.getId(context, "dictionary");
                                                    String string = StringArgumentType.getString(context, "string");
                                                    string = string.toLowerCase();
                                                    string = string.replaceAll("[^a-zA-Z0-9 ]", "");
                                                    String[] tokens = string.split(" ");
                                                    SpellDictionary spellDictionary = CabalistSpellDictionary.getSpellDictionary(dictionary);

                                                    TreeMap<String,Double> allMatches = SpellEngine.INSTANCE.findAllMatches(tokens, spellDictionary);
                                                    MutableComponent literal = Component.literal("Results:\n");
                                                    for (String s : allMatches.keySet()) {
                                                        Double v = allMatches.get(s);
                                                        literal.append('"'+s+'"'+" - "+v+"\n");
                                                    }
                                                    context.getSource().sendSuccess(() -> literal, false);
                                                    return 0;
                                                })
                                        )
                                ))

                );
    }
}
