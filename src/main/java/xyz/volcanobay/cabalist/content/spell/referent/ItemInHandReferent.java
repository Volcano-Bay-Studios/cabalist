package xyz.volcanobay.cabalist.content.spell.referent;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellResolver;
import xyz.volcanobay.cabalist.system.spell.SpellRole;
import xyz.volcanobay.cabalist.system.spell.Word;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.ItemSubject;

public class ItemInHandReferent extends SpellComponent {
    private final InteractionHand hand;

    public ItemInHandReferent(InteractionHand hand) {
        this.hand = hand;
    }
    @Override
    public SpellRole getRole() {
        return SpellRole.REFERENT;
    }

    @Override
    public void resolve(Word word, SpellResolver resolver) {
        if (resolver.getCaster() instanceof EntitySubject subject && subject.getEntity() instanceof LivingEntity entity) {
            ItemStack itemInHand = entity.getItemInHand(hand);
            if (!itemInHand.isEmpty()) {
                word.setSubject(new ItemSubject(itemInHand,subject));
            }
        }
    }
}
