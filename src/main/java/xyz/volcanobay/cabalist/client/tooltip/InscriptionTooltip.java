package xyz.volcanobay.cabalist.client.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import xyz.volcanobay.cabalist.system.contract.Inscription;

public record InscriptionTooltip(Inscription inscription) implements TooltipComponent {
}
