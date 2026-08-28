package xyz.volcanobay.cabalist.system.spell;

import edu.uniba.di.lacam.kdde.ws4j.WS4J;
import xyz.volcanobay.cabalist.core.CabalistDomains;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SpellEngine {
    public static final SpellEngine INSTANCE = new SpellEngine();
    private static final List<CompletableFuture<Spell>> spellFutures = new ArrayList<>();

    public void parseSpellFuture(String spell) {
        CompletableFuture<Spell> spellFuture = new CompletableFuture<>();
        spellFuture.completeAsync(() -> parseSpell(spell));
        spellFutures.add(spellFuture);
    }

    public Spell parseSpell(String spell) {
        // tokenize
        spell = spell.toLowerCase();
        spell = spell.replaceAll("[^a-zA-Z0-9 ]", "");
        String[] tokens = spell.split(" ");

        // find domain candidates


        // find subject candidates


        // find source candidates


        // find modifier candidates


        return new Spell();
    }
}
