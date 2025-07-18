package com.wanderersoftherift.wotr.modifier;

import com.wanderersoftherift.wotr.init.WotrDataComponentType;
import com.wanderersoftherift.wotr.item.implicit.GearImplicits;
import com.wanderersoftherift.wotr.item.socket.GearSocket;
import com.wanderersoftherift.wotr.item.socket.GearSockets;
import com.wanderersoftherift.wotr.modifier.source.GearImplicitModifierSource;
import com.wanderersoftherift.wotr.modifier.source.GearSocketModifierSource;
import com.wanderersoftherift.wotr.modifier.source.ModifierSource;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;

public class ModifierHelper {

    public static void runIterationOnItem(
            ItemStack stack,
            WotrEquipmentSlot slot,
            LivingEntity entity,
            ModifierInSlotVisitor visitor) {
        if (!stack.isEmpty() && slot.canAccept(stack)) {
            runOnImplicits(stack, slot, entity, visitor);
            runOnGearSockets(stack, slot, entity, visitor);
        }
    }

    private static void runOnGearSockets(
            ItemStack stack,
            WotrEquipmentSlot slot,
            LivingEntity entity,
            ModifierInSlotVisitor visitor) {
        GearSockets gearSockets = stack.get(WotrDataComponentType.GEAR_SOCKETS);
        if (gearSockets != null && !gearSockets.isEmpty()) {
            for (GearSocket socket : gearSockets.sockets()) {
                if (socket.isEmpty()) {
                    continue;
                }
                ModifierInstance modifierInstance = socket.modifier().get();
                Holder<Modifier> modifier = modifierInstance.modifier();
                if (modifier != null) {
                    ModifierSource source = new GearSocketModifierSource(socket, gearSockets, slot, entity);
                    visitor.accept(modifier, modifierInstance.tier(), modifierInstance.roll(), source);
                }
            }
        }
    }

    private static void runOnImplicits(
            ItemStack stack,
            WotrEquipmentSlot slot,
            LivingEntity entity,
            ModifierInSlotVisitor visitor) {
        GearImplicits implicits = stack.get(WotrDataComponentType.GEAR_IMPLICITS);
        if (implicits != null) {
            List<ModifierInstance> modifierInstances = implicits.modifierInstances(stack, entity.level());
            for (ModifierInstance modifier : modifierInstances) {
                ModifierSource source = new GearImplicitModifierSource(implicits, slot, entity);
                visitor.accept(modifier.modifier(), modifier.tier(), modifier.roll(), source);
            }
        }
    }

    public static void runIterationOnEquipment(LivingEntity entity, ModifierInSlotVisitor visitor) {
        var slots = NeoForge.EVENT_BUS.post(new CollectEquipmentSlotsEvent(new ArrayList<>(), entity)).getSlots();
        for (var wotrSlot : slots) {
            runIterationOnItem(wotrSlot.getContent(entity), wotrSlot, entity, visitor);
        }
    }

    public static void enableModifier(LivingEntity entity) {
        runIterationOnEquipment(entity, (modifierHolder, tier, roll, source) -> modifierHolder.value()
                .enableModifier(roll, entity, source, tier));
    }

    public static void enableModifier(ItemStack stack, LivingEntity entity, EquipmentSlot slot) {
        enableModifier(stack, entity, WotrEquipmentSlotFromMC.fromVanillaSlot(slot));
    }

    public static void enableModifier(ItemStack stack, LivingEntity entity, WotrEquipmentSlot slot) {
        runIterationOnItem(stack, slot, entity, (modifierHolder, tier, roll, source) -> modifierHolder.value()
                .enableModifier(roll, entity, source, tier));
    }

    public static void disableModifier(LivingEntity entity) {
        runIterationOnEquipment(entity, (modifierHolder, tier, roll, source) -> modifierHolder.value()
                .disableModifier(roll, entity, source, tier));
    }

    public static void disableModifier(ItemStack stack, LivingEntity entity, EquipmentSlot slot) {
        disableModifier(stack, entity, WotrEquipmentSlotFromMC.fromVanillaSlot(slot));
    }

    public static void disableModifier(ItemStack stack, LivingEntity entity, WotrEquipmentSlot slot) {
        runIterationOnItem(stack, slot, entity, (modifierHolder, tier, roll, source) -> modifierHolder.value()
                .disableModifier(roll, entity, source, tier));
    }

    @FunctionalInterface
    public interface ModifierInSlotVisitor {
        void accept(Holder<Modifier> modifierHolder, int tier, float roll, ModifierSource item);
    }

    public static class CollectEquipmentSlotsEvent extends Event {
        private final List<WotrEquipmentSlot> slots;
        private final LivingEntity entity;

        public CollectEquipmentSlotsEvent(List<WotrEquipmentSlot> slots, LivingEntity entity) {
            this.slots = slots;
            this.entity = entity;
        }

        public List<WotrEquipmentSlot> getSlots() {
            return slots;
        }

        public LivingEntity getEntity() {
            return entity;
        }
    }
}
