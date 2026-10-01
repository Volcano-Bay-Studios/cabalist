package xyz.volcanobay.cabalist.core.data;

import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.LanguageProvider;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.core.CabalistBlocks;
import xyz.volcanobay.cabalist.core.CabalistItems;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;


public class CabalistLanguageProvider extends LanguageProvider {
    private ResourceManager resourceManager;

    public CabalistLanguageProvider(PackOutput output, String locale, ResourceManager resourceManager) {
        super(output, Cabalist.MODID, locale);
        this.resourceManager = resourceManager;
    }


    @Override
    protected void addTranslations() {
        add("itemGroup.cabalist","Cabalist");
        add("casting.cabalist.abandon_spell", "abandon this spell?");
        add("casting.cabalist.yes_or_no", "type yes or no");
        add("request.cabalist.join", "%1$s asks to join %2$s");
        add("request.cabalist.invite", "%1$s invites %3$s into %2$s");
        add("request.cabalist.leave", "%1$s asks to leave %2$s");
        add("request.cabalist.expel", "%1$s asks to expel %3$s from %2$s");
        add("request.cabalist.amend", "%1$s proposes amending %2$s");
        add("request.cabalist.cooldown", "you may amend %1$s again in %2$s");
        add("request.cabalist.draft_add", "inscribe a new line here");
        add("request.cabalist.appraisal", "appraisal of %s");
        add("request.cabalist.approved", "approved: %s");
        add("request.cabalist.denied", "denied: %s");
        add("request.cabalist.answer_hint", "type i consent or i don't");
        add("request.cabalist.status.unanswered", "has not answered");
        add("request.cabalist.status.yes", "consents");
        add("request.cabalist.status.no", "refuses");
        add("request.cabalist.status.burned", "burned it");
        add("request.cabalist.status.dispelled", "dispelled it");
        autoLang();
    }

    // --- Helper methods ---

    private static String toTitleCase(String id) {
        return Arrays.stream(id.split("_"))
                .map(s -> s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1))
                .collect(Collectors.joining(" "));
    }

    private void add(Block key) {
        try {
            this.add(key.getDescriptionId(), toTitleCase(BuiltInRegistries.BLOCK.getKey(key).getPath()));
        } catch (Exception ignored) {
        }
    }

    private void addExisting(Block block, String name) {
        try {
            add(block, name);
        } catch (Exception ignored) {
        }
    }

    private void addExisting(Item item, String name) {
        try {
            add(item, name);
        } catch (Exception ignored) {
        }
    }

    private void add(Item key) {
        try {
            this.add(key.getDescriptionId(), toTitleCase(BuiltInRegistries.ITEM.getKey(key).getPath()));
        } catch (Exception ignored) {
        }
    }

    private void add(SoundEvent key, String name) {
        this.add("sounds." + key.getLocation().toLanguageKey(), name);
    }

    private void addTab(String key, String name) {
        this.add("itemGroup." + key, name);
    }

    private void autoLang() {
        for (RegistryObject<Block> blockRegistryObject : CabalistBlocks.getBlocks()) {
            add(blockRegistryObject.get());
        }
        for (RegistryObject<Item> blockRegistryObject : CabalistItems.getItems()) {
            add(blockRegistryObject.get());
        }
    }
}