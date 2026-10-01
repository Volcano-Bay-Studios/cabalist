package xyz.volcanobay.cabalist.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.UsernameCache;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractAction;
import xyz.volcanobay.cabalist.system.contract.ContractSystem;
import xyz.volcanobay.cabalist.system.contract.Contractee;
import xyz.volcanobay.cabalist.system.contract.Term;
import xyz.volcanobay.cabalist.system.contract.WorldContractee;
import xyz.volcanobay.cabalist.system.energy.EnergyStack;
import xyz.volcanobay.cabalist.system.energy.Lifeforce;
import xyz.volcanobay.cabalist.system.entropy.EntropyNetwork;
import xyz.volcanobay.cabalist.system.entropy.EntropyNetworkContract;
import xyz.volcanobay.cabalist.system.focus.Focus;
import xyz.volcanobay.cabalist.system.focus.Gathering;
import xyz.volcanobay.cabalist.system.focus.Imbuement;
import xyz.volcanobay.cabalist.system.form.FormDimension;
import xyz.volcanobay.cabalist.system.request.AmendCooldownSystem;
import xyz.volcanobay.cabalist.system.request.PartyStatus;
import xyz.volcanobay.cabalist.system.request.Request;
import xyz.volcanobay.cabalist.system.request.RequestSystem;
import xyz.volcanobay.cabalist.system.request.WorldConsent;
import xyz.volcanobay.cabalist.system.rift.RiftHelper;
import xyz.volcanobay.cabalist.system.spell.Devotion;
import xyz.volcanobay.cabalist.system.spell.Domain;
import xyz.volcanobay.cabalist.system.spell.EnergyLedger;
import xyz.volcanobay.cabalist.system.spell.HangingSpellSystem;
import xyz.volcanobay.cabalist.system.spell.PendingSpell;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellDictionary;
import xyz.volcanobay.cabalist.system.spell.SpellEngine;
import xyz.volcanobay.cabalist.system.spell.SpellExecutor;
import xyz.volcanobay.cabalist.system.spell.SpellParse;
import xyz.volcanobay.cabalist.system.spell.SpellPayment;
import xyz.volcanobay.cabalist.system.spell.SpellRole;
import xyz.volcanobay.cabalist.system.spell.Word;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.PositionSubject;
import xyz.volcanobay.cabalist.system.subject.Subject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Cabalist.MODID)
public class CabalistCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(registerSuperpositionCommand());
    }

    public static LiteralArgumentBuilder<CommandSourceStack> registerSuperpositionCommand() {

        return Commands.literal("cabalist").requires(stack -> stack.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("requests")
                        .then(Commands.literal("cooldown")
                                .then(Commands.literal("clear")
                                        .executes(context -> clearCooldowns(context.getSource(), context.getSource().getPlayerOrException()))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> clearCooldowns(context.getSource(), EntityArgument.getPlayer(context, "player"))))))
                        .then(Commands.literal("world")
                                .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(context -> {
                                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                    WorldConsent.set(context.getSource().getPlayerOrException(), enabled);
                                    context.getSource().sendSuccess(() -> Component.literal(enabled ? "Receiving the world's requests" : "No longer receiving the world's requests"), false);
                                    return 1;
                                }))))
                .then(Commands.literal("debug")
                        .then(Commands.literal("compare")
                                .then(Commands.argument("dictionary", ResourceLocationArgument.id()).suggests(CabalistSpellDictionary.spellDictionarysSuggestionProvider())
                                        .then(Commands.argument("string", StringArgumentType.greedyString()).executes(context -> {
                                                    ResourceLocation dictionary = ResourceLocationArgument.getId(context, "dictionary");
                                                    String[] tokens = SpellEngine.INSTANCE.tokenize(StringArgumentType.getString(context, "string"));
                                                    SpellDictionary spellDictionary = CabalistSpellDictionary.getSpellDictionary(dictionary);
                                                    if (spellDictionary == null) {
                                                        context.getSource().sendFailure(Component.literal("Unknown dictionary " + dictionary));
                                                        return 0;
                                                    }

                                                    List<SpellEngine.Candidate> matches = new ArrayList<>();
                                                    SpellEngine.INSTANCE.findAllMatches(tokens, spellDictionary, 0, matches);
                                                    MutableComponent result = describeMatches(tokens, matches);
                                                    context.getSource().sendSuccess(() -> result, false);
                                                    return 1;
                                                })
                                        )
                                ))
                        .then(Commands.literal("parse")
                                .then(Commands.argument("string", StringArgumentType.greedyString()).executes(context -> {
                                            CommandSourceStack source = context.getSource();
                                            SpellParse parse = SpellEngine.INSTANCE.parse(StringArgumentType.getString(context, "string"));
                                            Spell spell = SpellEngine.INSTANCE.resolve(parse, getCaster(source));
                                            Gathering gathering = Gathering.simulate(Focus.standard(), spell, StringArgumentType.getString(context, "string"));
                                            MutableComponent result = describeUtterance(parse, parse.getWords()).append(describeClauses(spell))
                                                    .append(Component.literal(String.format("\ncosts %.1f per firing, standard wand gathers %.1f",
                                                            SpellPayment.INSTANCE.estimateCost(spell), gathering.getGathered())).withStyle(ChatFormatting.AQUA));
                                            source.sendSuccess(() -> result, false);
                                            return 1;
                                        })
                                ))
                        .then(Commands.literal("cast")
                                .then(Commands.argument("string", StringArgumentType.greedyString()).executes(context -> {
                                            CommandSourceStack source = context.getSource();
                                            SpellParse parse = SpellEngine.INSTANCE.parse(StringArgumentType.getString(context, "string"));
                                            Spell spell = SpellEngine.INSTANCE.resolve(parse, getCaster(source));
                                            Gathering gathering = Gathering.simulate(Focus.standard(), spell, StringArgumentType.getString(context, "string"));
                                            gathering.applyTo(spell, source.getEntity() instanceof LivingEntity living ? living : null);
                                            int affected = SpellExecutor.INSTANCE.cast(spell);
                                            String hanging = spell.isRunning() ? ", running" : HangingSpellSystem.INSTANCE.find(spell) != null ? ", hanging" : "";
                                            MutableComponent result = describeUtterance(parse, parse.getWords())
                                                    .append(withHover(Component.literal(" (" + affected + " affected" + hanging + ")").withStyle(ChatFormatting.GRAY),
                                                            describeGathering(gathering).append(describePayment(spell)).append(describeEnergy(spell.getEnergyStack()))));
                                            source.sendSuccess(() -> result, false);
                                            return 1;
                                        })
                                ))
                        .then(Commands.literal("hanging").executes(context -> {
                            EntitySubject subject = EntitySubject.of(context.getSource().getEntityOrException());
                            MutableComponent result = describeHanging(subject);
                            context.getSource().sendSuccess(() -> result, false);
                            return 1;
                        }))
                        .then(Commands.literal("energy").executes(context -> {
                            EntitySubject subject = EntitySubject.of(context.getSource().getEntityOrException());
                            MutableComponent result = describeEnergyFlow(subject);
                            context.getSource().sendSuccess(() -> result, false);
                            return 1;
                        }))
                        .then(Commands.literal("dismiss").executes(context -> {
                            EntitySubject subject = EntitySubject.of(context.getSource().getEntityOrException());
                            List<PendingSpell> pendingSpells = new ArrayList<>(HangingSpellSystem.INSTANCE.get(subject));
                            for (PendingSpell pending : pendingSpells) {
                                HangingSpellSystem.INSTANCE.removeSpell(pending.getSpell());
                            }
                            context.getSource().sendSuccess(() -> Component.literal("dismissed " + pendingSpells.size()).withStyle(ChatFormatting.GRAY), false);
                            return pendingSpells.size();
                        }))
                        .then(Commands.literal("network")
                                .then(Commands.literal("info").executes(context -> runNetworkCommand(context.getSource(), 0)))
                                .then(Commands.literal("join").executes(context -> runNetworkCommand(context.getSource(), 1)))
                                .then(Commands.literal("leave").executes(context -> runNetworkCommand(context.getSource(), -1))))
                        .then(Commands.literal("contract")
                                .then(Commands.literal("list").executes(context -> {
                                    MutableComponent result = describeContracts();
                                    context.getSource().sendSuccess(() -> result, false);
                                    return 1;
                                }))
                                .then(Commands.literal("create")
                                        .then(Commands.argument("name", StringArgumentType.greedyString()).executes(context -> {
                                            String name = StringArgumentType.getString(context, "name").trim();
                                            if (ContractSystem.INSTANCE.getContract(name) != null) {
                                                context.getSource().sendFailure(Component.literal("Contract " + name + " already exists"));
                                                return 0;
                                            }
                                            ContractSystem.INSTANCE.addContract(new Contract(name));
                                            context.getSource().sendSuccess(() -> Component.literal("Created ").append(Component.literal(name).withStyle(ChatFormatting.LIGHT_PURPLE)), false);
                                            return 1;
                                        })))
                                .then(Commands.literal("join")
                                        .then(Commands.argument("name", StringArgumentType.greedyString()).suggests(CabalistCommands::suggestContracts).executes(context -> {
                                            Contract contract = getNamedContract(context);
                                            if (contract == null) {
                                                return 0;
                                            }
                                            contract.addMember(EntitySubject.of(context.getSource().getEntityOrException()));
                                            context.getSource().sendSuccess(() -> Component.literal("Joined ").append(Component.literal(contract.getName()).withStyle(ChatFormatting.LIGHT_PURPLE)), false);
                                            return 1;
                                        })))
                                .then(Commands.literal("leave")
                                        .then(Commands.argument("name", StringArgumentType.greedyString()).suggests(CabalistCommands::suggestContracts).executes(context -> {
                                            Contract contract = getNamedContract(context);
                                            if (contract == null) {
                                                return 0;
                                            }
                                            contract.removeMember(EntitySubject.of(context.getSource().getEntityOrException()));
                                            context.getSource().sendSuccess(() -> Component.literal("Left ").append(Component.literal(contract.getName()).withStyle(ChatFormatting.LIGHT_PURPLE)), false);
                                            return 1;
                                        })))
                                .then(Commands.literal("destroy")
                                        .then(Commands.argument("name", StringArgumentType.greedyString()).suggests(CabalistCommands::suggestContracts).executes(context -> {
                                            Contract contract = getNamedContract(context);
                                            if (contract == null) {
                                                return 0;
                                            }
                                            ContractSystem.INSTANCE.destroyContract(contract.getUUID(), "destroyed by command");
                                            context.getSource().sendSuccess(() -> Component.literal("Destroyed ").append(Component.literal(contract.getName()).withStyle(ChatFormatting.LIGHT_PURPLE)), false);
                                            return 1;
                                        })))
                        )
                );
    }

    private static int runNetworkCommand(CommandSourceStack source, int membershipChange) throws CommandSyntaxException {
        EntitySubject subject = EntitySubject.of(source.getEntityOrException());
        EntropyNetwork network = EntropyNetworkContract.findTouchingNetwork(subject);
        if (network == null) {
            source.sendFailure(Component.literal("Not touching an entropy network"));
            return 0;
        }
        Contract contract = EntropyNetworkContract.getOrCreate(source.getLevel(), network.getId());
        if (membershipChange > 0) {
            contract.addMember(subject);
        } else if (membershipChange < 0) {
            contract.removeMember(subject);
        }
        MutableComponent hover = Component.literal(String.format("free entropy %.2f\nstored entropy %.2f\nmembers %d",
                network.getFreeEntropy(), network.getStoredEntropy(), contract.getMemberIds().size())).withStyle(ChatFormatting.DARK_PURPLE);
        boolean isMember = contract.hasMember(subject.getUUID());
        MutableComponent result = withHover(contract.getDisplayName().copy().withStyle(ChatFormatting.LIGHT_PURPLE), hover)
                .append(Component.literal(isMember ? " (member)" : " (not a member)").withStyle(ChatFormatting.GRAY));
        source.sendSuccess(() -> result, false);
        return 1;
    }

    private static @Nullable Contract getNamedContract(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name").trim();
        Contract contract = ContractSystem.INSTANCE.getContract(name);
        if (contract == null) {
            context.getSource().sendFailure(Component.literal("Unknown contract " + name));
        }
        return contract;
    }

    private static CompletableFuture<Suggestions> suggestContracts(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<String> names = new ArrayList<>();
        for (Contract contract : ContractSystem.INSTANCE.getContracts()) {
            names.add(contract.getName());
        }
        return SharedSuggestionProvider.suggest(names, builder);
    }

    private static MutableComponent describeHanging(Subject host) {
        List<PendingSpell> pendingSpells = HangingSpellSystem.INSTANCE.get(host);
        MutableComponent result = Component.literal(pendingSpells.size() + " hanging: ").withStyle(ChatFormatting.GRAY);
        for (PendingSpell pending : pendingSpells) {
            MutableComponent hover = Component.literal("\"" + pending.getIncantation() + "\"").withStyle(ChatFormatting.GOLD)
                    .append(describeClauseStates(pending));
            result.append(withHover(Component.literal("[spell] ").withStyle(ChatFormatting.LIGHT_PURPLE), hover));
        }
        return result;
    }

    private static MutableComponent describeClauseStates(PendingSpell pending) {
        MutableComponent result = Component.empty();
        List<SpellClause> clauses = pending.getSpell().getClauses();
        for (int i = 0; i < pending.getClauseCount(); i++) {
            boolean isRunning = pending.isRunning(i);
            String state = isRunning ? "running" : pending.getFireCount(i) > 0 ? "fired " + pending.getFireCount(i) + "x" : "waiting";
            String requirements = clauses.get(i).hasRequirements() ? " when " + describeRequirements(clauses.get(i)) : "";
            result.append(Component.literal("\n clause " + (i + 1) + ": " + state + requirements)
                    .withStyle(isRunning ? ChatFormatting.GOLD : pending.getFireCount(i) > 0 ? ChatFormatting.DARK_GRAY : ChatFormatting.BLUE));
        }
        return result;
    }

    private static MutableComponent describeEnergyFlow(Subject host) {
        EnergyStack stack = host.getEnergyStack();
        double stackEntropy = stack == null ? 0 : stack.get(CabalistEnergyTypes.ENTROPY.get());
        MutableComponent result = Component.literal("energy ").withStyle(ChatFormatting.GRAY)
                .append(describeFocus(host))
                .append(withHover(Component.literal(String.format("stack %.1f ", stackEntropy)).withStyle(ChatFormatting.DARK_PURPLE),
                        Component.literal("Your stored energy, used by a spell being cast when its own runs short").withStyle(ChatFormatting.GRAY)))
                .append(withHover(Component.literal(String.format("life %.1f", host.getLifeforcePool())).withStyle(ChatFormatting.RED),
                        describeLifeforce(host)));

        for (PendingSpell pending : HangingSpellSystem.INSTANCE.get(host)) {
            String name = pending.getIncantation().isBlank() ? "spell" : pending.getIncantation();
            if (name.length() > 24) {
                name = name.substring(0, 23) + "…";
            }
            List<Spell> running = pending.getRunningSpells();
            if (running.isEmpty()) {
                double left = pending.getSpell().getRemainingAllotment();
                double upkeep = CabalistConfig.HANGING_UPKEEP_PER_SECOND.get();
                result.append(withHover(Component.literal("\n[" + name + "] waiting ").withStyle(ChatFormatting.BLUE)
                                .append(Component.literal(String.format("%.1f left ", left)).withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(upkeep > 0 ? String.format("~%.0fs", left / upkeep) : "").withStyle(ChatFormatting.GRAY)),
                        Component.literal(String.format("\"%s\"\nPays %.2f/s upkeep from its own energy until it fires.", pending.getIncantation(), upkeep))
                                .withStyle(ChatFormatting.GRAY)));
                continue;
            }
            for (Spell spell : running) {
                result.append(describeRunningSpell(name, pending.getIncantation(), spell));
            }
        }
        return result;
    }

    private static MutableComponent describeFocus(Subject host) {
        Focus focus = host instanceof EntitySubject entitySubject && entitySubject.getEntity() instanceof LivingEntity living ? Focus.of(living) : null;
        if (focus == null) {
            return withHover(Component.literal("no focus ").withStyle(ChatFormatting.DARK_GRAY),
                    Component.literal("Hold an item imbued with lifeforce to cast with it").withStyle(ChatFormatting.GRAY));
        }
        Imbuement combined = focus.getCombined();
        MutableComponent hover = Component.literal(String.format("safe maximum %.1f\nthroughput %.1f/s",
                combined.getTotalLifeforce(), focus.getThroughput(null))).withStyle(ChatFormatting.GRAY);
        combined.lifeforce().forEach((type, amount) -> hover.append(Component.literal(String.format("\n %s %.1f", type.getPath(), amount)).withStyle(ChatFormatting.LIGHT_PURPLE)));
        return withHover(Component.literal(String.format("focus %.0f ", combined.getTotalLifeforce())).withStyle(ChatFormatting.AQUA), hover);
    }

    private static MutableComponent describeLifeforce(Subject host) {
        MutableComponent result = Component.literal("Entropy your lifeforce can still pay, used last").withStyle(ChatFormatting.GRAY);
        if (host instanceof EntitySubject entitySubject && entitySubject.getEntity() instanceof LivingEntity living) {
            result.append(Component.literal(String.format("\nlifeforce %.1f of %.1f health", Lifeforce.get(living), living.getHealth())).withStyle(ChatFormatting.RED));
        }
        return result;
    }

    private static MutableComponent describeRunningSpell(String name, String incantation, Spell spell) {
        EnergyLedger ledger = spell.getLedger();
        double left = spell.getRemainingAllotment();
        double rate = ledger.getTotalRate();
        String remaining = rate > 0.01 ? String.format("~%.0fs", left / rate) : rate < -0.01 ? "gaining" : "idle";
        MutableComponent hover = Component.literal("\"" + incantation + "\"").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(String.format("\nleft %.2f of %.2f gathered", left, spell.getSource().getTotal())).withStyle(ChatFormatting.AQUA));
        for (Object2DoubleMap.Entry<String> entry : ledger.getTotals().object2DoubleEntrySet()) {
            double entryRate = ledger.getRate(entry.getKey());
            boolean isGain = entry.getDoubleValue() < 0;
            hover.append(Component.literal(String.format("\n%s %s %.2f/s, %.2f total", entry.getKey(), isGain ? "gained" : "spent",
                            Math.abs(entryRate), Math.abs(entry.getDoubleValue())))
                    .withStyle(isGain ? ChatFormatting.GREEN : entry.getKey().equals(EnergyLedger.UPKEEP) ? ChatFormatting.DARK_GRAY : ChatFormatting.LIGHT_PURPLE));
        }
        return withHover(Component.literal("\n[" + name + "] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(String.format("%.1f left ", left)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(String.format("-%.2f/s ", rate)).withStyle(ChatFormatting.RED))
                .append(Component.literal(remaining).withStyle(ChatFormatting.GRAY)), hover);
    }

    private static MutableComponent describeDevotion(SpellClause clause) {
        Devotion devotion = clause.getDevotion();
        MutableComponent result = Component.literal("\ndevotional words " + devotion.getGenericWords()).withStyle(ChatFormatting.DARK_GRAY);
        for (Map.Entry<Domain, Double> entry : devotion.getPhrases().entrySet()) {
            result.append(Component.literal(String.format("\ndevotion to %s %.2f", CabalistSpellComponents.PART_REGISTRY.getKey(entry.getKey()), entry.getValue()))
                    .withStyle(ChatFormatting.GOLD));
        }
        for (Map.Entry<Domain, Double> entry : devotion.getDomainWords().entrySet()) {
            result.append(Component.literal(String.format("\ninvoked %s %.2f", CabalistSpellComponents.PART_REGISTRY.getKey(entry.getKey()), entry.getValue()))
                    .withStyle(ChatFormatting.GOLD));
        }
        Domain primary = clause.getPrimaryDomain();
        result.append(Component.literal(String.format("\nstrength x%.2f  cost x%.2f  size x%.2f  capacity x%.2f",
                devotion.getStrengthMultiplier(primary), devotion.getCostMultiplier(primary),
                devotion.getSizeMultiplier(primary), devotion.getCapacityMultiplier(primary))).withStyle(ChatFormatting.LIGHT_PURPLE));
        return result;
    }

    private static String describeRequirements(SpellClause clause) {
        StringBuilder text = new StringBuilder();
        List<List<Term>> groups = clause.getRequirementGroups();
        for (int group = 0; group < groups.size(); group++) {
            if (group > 0) {
                text.append(" or ");
            }
            List<Term> requirements = groups.get(group);
            for (int i = 0; i < requirements.size(); i++) {
                if (i > 0) {
                    text.append(" and ");
                }
                text.append(requirements.get(i).getResourceLocation().getPath());
            }
        }
        return text.toString();
    }

    private static String describeSpoken(double value) {
        return Double.isNaN(value) ? "default" : String.valueOf(value);
    }

    private static MutableComponent describeGathering(Gathering gathering) {
        MutableComponent result = Component.literal(String.format("gathered %.1f of %.1f capacity in %.1fs with the standard wand\n",
                gathering.getGathered(), gathering.getCapacity(), gathering.getSeconds())).withStyle(ChatFormatting.AQUA);
        if (gathering.getOverflow() > 0) {
            result.append(Component.literal(String.format("backlash %.1f\n", gathering.getOverflow())).withStyle(ChatFormatting.DARK_RED));
        }
        return result;
    }

    private static MutableComponent describePayment(Spell spell) {
        SpellPayment payment = SpellPayment.INSTANCE;
        MutableComponent result = Component.literal(String.format("cast %.1fs, pressure %.2f", spell.getCastSeconds(), payment.getPressure())).withStyle(ChatFormatting.WHITE);
        if (payment.getRiftCost() > 0) {
            result.append(Component.literal(String.format("\nincluding rift %.2f", payment.getRiftCost())).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        result.append(Component.literal(String.format("\nfrom its energy %.2f, had %.2f", payment.getFromSpell(), payment.getAvailable())).withStyle(ChatFormatting.AQUA));
        result.append(Component.literal(String.format("\nbearer stacks %.2f", payment.getFromBearerStacks())).withStyle(ChatFormatting.DARK_PURPLE));
        result.append(Component.literal(String.format("\nlifeforce (harm) %.2f", payment.getFromLifeforce())).withStyle(ChatFormatting.RED));
        if (payment.getUnpaid() > 0) {
            result.append(Component.literal(String.format("\nunpaid %.2f, spell weakened", payment.getUnpaid())).withStyle(ChatFormatting.DARK_RED));
        }
        return result;
    }

    private static MutableComponent describeEnergy(EnergyStack stack) {
        return Component.literal(String.format("\nentropy %.2f  lifeforce %.2f  pressure %.2f",
                stack.get(CabalistEnergyTypes.ENTROPY.get()),
                stack.get(CabalistEnergyTypes.LIFEFORCE.get()),
                stack.getWorldPressure())).withStyle(ChatFormatting.DARK_PURPLE);
    }

    private static MutableComponent describeContracts() {
        MutableComponent result = Component.literal(ContractSystem.INSTANCE.getContracts().size() + " contracts: ").withStyle(ChatFormatting.GRAY);
        for (Contract contract : ContractSystem.INSTANCE.getContracts()) {
            MutableComponent hover = Component.literal(contract.getUUID().toString()).withStyle(ChatFormatting.DARK_GRAY);
            hover.append(Component.literal("\nmembers " + contract.getLoadedMembers().size() + "/" + contract.getMemberIds().size() + " loaded").withStyle(ChatFormatting.GRAY));
            for (Contractee member : contract.getLoadedMembers()) {
                hover.append("\n ").append(member.getDisplayName().copy().withStyle(ChatFormatting.AQUA));
            }
            for (Set<Term> terms : contract.getTerms().values()) {
                for (Term term : terms) {
                    hover.append(Component.literal("\nterm " + term.getResourceLocation()).withStyle(ChatFormatting.BLUE));
                }
            }
            hover.append(describeEnergy(contract.getEnergyStack()));
            if (contract.getCreatorId() != null) {
                hover.append(Component.literal("\ncreator " + contract.getCreatorId()).withStyle(ChatFormatting.DARK_GRAY));
            }
            hover.append(Component.literal(contract.getHost() == null ? "\nnot bound" : "\nbound to ").withStyle(ChatFormatting.GRAY));
            if (contract.getHost() != null) {
                hover.append(contract.getHost().getDisplayName());
            }
            if (contract.getPendingSpells().isEmpty()) {
                for (String incantation : contract.getIncantations()) {
                    hover.append(Component.literal("\n\"" + incantation + "\"").withStyle(ChatFormatting.GOLD));
                }
            }
            for (PendingSpell pending : contract.getPendingSpells()) {
                hover.append(Component.literal(String.format("\n\"%s\" costs %.2f", pending.getIncantation(), SpellPayment.INSTANCE.estimateCost(pending.getSpell())))
                        .withStyle(ChatFormatting.GOLD)).append(describeClauseStates(pending));
            }
            for (Request request : RequestSystem.INSTANCE.getRequests()) {
                if (request.getContractId().equals(contract.getUUID())) {
                    hover.append(Component.literal("\nrequest " + request.getKind().name().toLowerCase() + " " + String.join("; ", request.getChanges())
                            + " (" + request.getOutcome().name().toLowerCase() + ")").withStyle(ChatFormatting.YELLOW));
                    for (Map.Entry<UUID, PartyStatus> party : request.getParties().entrySet()) {
                        hover.append(Component.literal("\n  " + describeParty(party.getKey()) + " " + party.getValue().name().toLowerCase()).withStyle(ChatFormatting.GRAY));
                    }
                }
            }
            List<ContractAction> log = contract.getLog();
            for (int i = Math.max(0, log.size() - 5); i < log.size(); i++) {
                ContractAction action = log.get(i);
                hover.append(Component.literal("\n" + action.action() + " " + action.detail() + " - " + action.outcome()).withStyle(ChatFormatting.DARK_GRAY));
            }
            result.append(withHover(contract.getDisplayName().copy().withStyle(ChatFormatting.LIGHT_PURPLE), hover)).append(" ");
        }
        return result;
    }

    private static Subject getCaster(CommandSourceStack source) {
        if (source.getEntity() != null) {
            return EntitySubject.of(source.getEntity());
        }
        Vec3 position = source.getPosition();
        Vec3 facing = Vec3.directionFromRotation(source.getRotation());
        return new PositionSubject(source.getLevel(), new Vector3d(position.x, position.y, position.z), new Vector3d(facing.x, facing.y, facing.z));
    }

    private static ChatFormatting getRoleColor(SpellRole role) {
        return switch (role) {
            case INVOCATION -> ChatFormatting.GOLD;
            case PETITION -> ChatFormatting.RED;
            case REFERENT -> ChatFormatting.AQUA;
            case ANCHOR -> ChatFormatting.LIGHT_PURPLE;
            case MANNER -> ChatFormatting.GREEN;
            case DEVOTIONAL -> ChatFormatting.DARK_GRAY;
            case NUMBER -> ChatFormatting.YELLOW;
            case CONDITION -> ChatFormatting.BLUE;
            case CONJUNCTION -> ChatFormatting.WHITE;
        };
    }

    private static MutableComponent withHover(MutableComponent text, Component hover) {
        return text.withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
    }

    private static MutableComponent describeUtterance(SpellParse parse, List<Word> words) {
        String[] tokens = parse.getTokens();
        MutableComponent line = Component.literal("");
        int token = 0;
        int wordIndex = 0;
        while (token < tokens.length) {
            if (parse.isDevotional(token)) {
                line.append(withHover(Component.literal(tokens[token] + " ").withStyle(ChatFormatting.DARK_GRAY),
                        Component.literal("Devotional").withStyle(ChatFormatting.DARK_GRAY)));
                token++;
                continue;
            }
            while (wordIndex < words.size() && words.get(wordIndex).getEnd() <= token) {
                wordIndex++;
            }
            if (wordIndex >= words.size() || words.get(wordIndex).getStart() > token) {
                token++;
                continue;
            }
            Word first = words.get(wordIndex);
            MutableComponent hover = Component.literal("");
            int spanWord = wordIndex;
            while (spanWord < words.size() && words.get(spanWord).getStart() == first.getStart()) {
                if (spanWord > wordIndex) {
                    hover.append("\n\n");
                }
                hover.append(describeWord(tokens, words.get(spanWord)));
                spanWord++;
            }
            MutableComponent span = Component.literal(joinTokens(tokens, first.getStart(), first.getEnd()));
            span.withStyle(getRoleColor(first.getRole()));
            if (first.isClaimed()) {
                span.withStyle(ChatFormatting.UNDERLINE);
            }
            line.append(withHover(span, hover)).append(" ");
            token = first.getEnd();
        }
        return line;
    }

    private static MutableComponent describeWord(String[] tokens, Word word) {
        SpellComponent component = word.getComponent();
        MutableComponent result = Component.literal(String.valueOf(CabalistSpellComponents.PART_REGISTRY.getKey(component)))
                .withStyle(getRoleColor(word.getRole()));
        result.append(Component.literal("\n" + word.getRole() + " priority " + component.getPriority()).withStyle(ChatFormatting.GRAY));
        result.append(Component.literal(String.format("\nscore %.2f", word.getScore())).withStyle(ChatFormatting.GRAY));
        if (word.hasNumber()) {
            result.append(Component.literal("\nvalue " + word.getNumber()).withStyle(ChatFormatting.YELLOW));
        }
        SpellRole[] needs = component.getNeeds();
        for (int i = 0; i < needs.length; i++) {
            Word claimed = word.getClaimed(i);
            result.append(Component.literal("\nneeds " + needs[i] + ": ").withStyle(ChatFormatting.GRAY));
            if (claimed == null) {
                result.append(Component.literal("default").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                result.append(Component.literal(joinTokens(tokens, claimed.getStart(), claimed.getEnd())).withStyle(getRoleColor(claimed.getRole())));
            }
        }
        Word claimedBy = word.getClaimedBy();
        if (claimedBy != null) {
            result.append(Component.literal("\nclaimed by ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(joinTokens(tokens, claimedBy.getStart(), claimedBy.getEnd())).withStyle(getRoleColor(claimedBy.getRole())));
        }
        Subject subject = word.getSubject();
        if (subject != null) {
            result.append(Component.literal("\nsubject ").withStyle(ChatFormatting.GRAY)).append(subject.getDisplayName());
        }
        return result;
    }

    private static MutableComponent describeClauses(Spell spell) {
        MutableComponent result = Component.literal("");
        List<SpellClause> clauses = spell.getClauses();
        for (int i = 0; i < clauses.size(); i++) {
            SpellClause clause = clauses.get(i);
            result.append("\n");
            MutableComponent clauseHover = Component.literal("caster ").withStyle(ChatFormatting.GRAY).append(clause.getCaster().getDisplayName())
                    .append(Component.literal("\nbearer ").withStyle(ChatFormatting.GRAY)).append(clause.getBearer().getDisplayName());
            if (clause.getSpareNumber() != 0) {
                clauseHover.append(Component.literal("\nspare number " + clause.getSpareNumber()).withStyle(ChatFormatting.YELLOW));
            }
            clauseHover.append(describeDevotion(clause));
            String label = i == 0 ? "Clause 1" : (clause.runsAfterPrevious() ? "then " : "and ") + "clause " + (i + 1);
            result.append(withHover(Component.literal(label + ": ").withStyle(ChatFormatting.WHITE), clauseHover));
            if (clause.hasRequirements()) {
                result.append(Component.literal("[when " + describeRequirements(clause) + "] ").withStyle(ChatFormatting.BLUE));
            }

            Subject target = clause.getTarget();
            if (target == null) {
                result.append(Component.literal("[wander] ").withStyle(ChatFormatting.DARK_RED));
            } else {
                result.append(Component.literal("[target ").withStyle(ChatFormatting.AQUA)).append(target.getDisplayName()).append(Component.literal("] ").withStyle(ChatFormatting.AQUA));
            }
            if (target != null && RiftHelper.opensRift(clause)) {
                result.append(Component.literal(String.format("[rift %.1fm] ", RiftHelper.getEffectiveDistance(clause.getCaster(), target))).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            Subject origin = clause.getOrigin();
            if (origin != null) {
                result.append(Component.literal("[from ").withStyle(ChatFormatting.DARK_AQUA)).append(origin.getDisplayName()).append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA));
            }
            if (clause.getDomain() != null) {
                result.append(Component.literal("[" + CabalistSpellComponents.PART_REGISTRY.getKey(clause.getDomain()) + "] ").withStyle(ChatFormatting.GOLD));
            }
            if (clause.getForm() != null) {
                MutableComponent formHover = Component.literal(String.format("size %s x%.2f\nbreadth %s x%.2f\ncost x%.2f",
                        describeSpoken(clause.getFormDimension(FormDimension.SIZE)), clause.getFormFactor(FormDimension.SIZE),
                        describeSpoken(clause.getFormDimension(FormDimension.BREADTH)), clause.getFormFactor(FormDimension.BREADTH),
                        clause.getForm().getCostFactor(clause))).withStyle(ChatFormatting.GREEN);
                result.append(withHover(Component.literal("[" + CabalistForms.FORM_REGISTRY.getKey(clause.getForm()) + "] ").withStyle(ChatFormatting.GREEN), formHover));
            }
            for (Aspect aspect : clause.getAspects()) {
                result.append(Component.literal("[" + CabalistAspects.ASPECT_REGISTRY.getKey(aspect) + "] ").withStyle(ChatFormatting.RED));
            }
            result.append(Component.literal(String.format("power %.2f", clause.getPower())).withStyle(ChatFormatting.DARK_GRAY));
        }
        return result;
    }

    private static MutableComponent describeMatches(String[] tokens, List<SpellEngine.Candidate> matches) {
        MutableComponent result = Component.literal(matches.size() + " windows: ").withStyle(ChatFormatting.GRAY);
        for (SpellEngine.Candidate match : matches) {
            ChatFormatting color = ChatFormatting.RED;
            if (match.similarity() >= 0.85) {
                color = ChatFormatting.GREEN;
            } else if (match.similarity() >= SpellEngine.MIN_CANDIDATE_SIMILARITY) {
                color = ChatFormatting.YELLOW;
            }
            MutableComponent hover = Component.literal("~ \"" + match.phrase() + "\"")
                    .append(String.format("\nsimilarity %.2f\nscore %.2f", match.similarity(), match.score()));
            result.append(withHover(Component.literal("[" + joinTokens(tokens, match.start(), match.end()) + "]").withStyle(color), hover)).append(" ");
        }
        return result;
    }

    private static String joinTokens(String[] tokens, int start, int end) {
        return String.join(" ", Arrays.copyOfRange(tokens, start, end));
    }

    private static String describeParty(UUID id) {
        if (id.equals(WorldContractee.WORLD_ID)) {
            return "the world";
        }
        String name = UsernameCache.getLastKnownUsername(id);
        return name != null ? name : id.toString();
    }

    private static int clearCooldowns(CommandSourceStack source, ServerPlayer player) {
        int cleared = AmendCooldownSystem.INSTANCE.clearFor(player.getUUID());
        source.sendSuccess(() -> Component.literal("Cleared " + cleared + " amendment cooldowns for " + player.getGameProfile().getName()), false);
        return cleared;
    }
}
