/*
 * MIT License
 *
 * Copyright (c) 2020 Azercoco & Technici4n
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package aztech.modern_industrialization.machines.blockentities.multiblocks;

import aztech.modern_industrialization.MIBlock;
import aztech.modern_industrialization.compat.rei.machines.ReiMachineRecipes;
import aztech.modern_industrialization.machines.components.CrafterComponent;
import aztech.modern_industrialization.machines.components.EnergyComponent;
import aztech.modern_industrialization.machines.components.OrientationComponent;
import aztech.modern_industrialization.machines.components.UpgradeComponent;
import aztech.modern_industrialization.machines.init.MIMachineRecipeTypes;
import aztech.modern_industrialization.machines.init.MachineTier;
import aztech.modern_industrialization.machines.models.MachineCasings;
import aztech.modern_industrialization.machines.multiblocks.*;
import aztech.modern_industrialization.machines.multiblocks.HatchType;
import aztech.modern_industrialization.machines.recipe.MachineRecipeType;
import aztech.modern_industrialization.util.Simulation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

// TODO: should the common part with ElectricCraftingMultiblockBlockEntity be refactored?
public class DistillationTowerBlockEntity extends AbstractCraftingMultiblockBlockEntity {
    private static final ShapeTemplate[] shapeTemplates;

    public DistillationTowerBlockEntity(BlockEntityType<?> type) {
        super(type, "distillation_tower", new OrientationComponent(new OrientationComponent.Params(false, false, false)), shapeTemplates);
        this.upgrades = new UpgradeComponent();
        this.registerComponents(upgrades);
    }

    @Override
    protected CrafterComponent.Behavior getBehavior() {
        return new Behavior();
    }

    private final List<EnergyComponent> energyInputs = new ArrayList<>();
    private final UpgradeComponent upgrades;

    @Override
    protected void onSuccessfulMatch(ShapeMatcher shapeMatcher) {
        energyInputs.clear();
        for (HatchBlockEntity hatch : shapeMatcher.getMatchedHatches()) {
            hatch.appendEnergyInputs(energyInputs);
        }
    }

    protected ActionResult onUse(PlayerEntity player, Hand hand, Direction face) {
        ActionResult result = super.onUse(player, hand, face);
        if (!result.isAccepted()) {
            result = upgrades.onUse(this, player, hand);
        }
        return result;
    }

    @Override
    public List<ItemStack> dropExtra() {
        List<ItemStack> drops = super.dropExtra();
        drops.add(upgrades.getDrop());
        return drops;
    }

    private class Behavior implements CrafterComponent.Behavior {
        @Override
        public long consumeEu(long max, Simulation simulation) {
            long total = 0;

            for (EnergyComponent energyComponent : energyInputs) {
                total += energyComponent.consumeEu(max - total, simulation);
            }

            return total;
        }

        @Override
        public MachineRecipeType recipeType() {
            return MIMachineRecipeTypes.DISTILLATION_TOWER;
        }

        @Override
        public long getBaseRecipeEu() {
            return MachineTier.MULTIBLOCK.getBaseEu();
        }

        @Override
        public long getMaxRecipeEu() {
            return MachineTier.MULTIBLOCK.getMaxEu() + upgrades.getAddMaxEUPerTick();
        }

        @Override
        public World getWorld() {
            return world;
        }

        @Override
        public int getMaxFluidOutputs() {
            return activeShape.getActiveShapeIndex() + 1;
        }
    }

    public static void registerReiShapes() {
        for (ShapeTemplate shapeTemplate : shapeTemplates) {
            ReiMachineRecipes.registerMultiblockShape("distillation_tower", shapeTemplate);
        }
    }

    static {
        int maxHeight = 9;
        shapeTemplates = new ShapeTemplate[maxHeight];

        SimpleMember casing = SimpleMember.forBlock(MIBlock.blocks.get("clean_stainless_steel_machine_casing"));
        SimpleMember pipe = SimpleMember.forBlock(MIBlock.blocks.get("stainless_steel_machine_casing_pipe"));
        HatchFlags hatchFlags = new HatchFlags.Builder().with(HatchType.ENERGY_INPUT, HatchType.FLUID_INPUT, HatchType.FLUID_OUTPUT).build();
        for (int i = 0; i < maxHeight; ++i) {
            ShapeTemplate.Builder builder = new ShapeTemplate.Builder(MachineCasings.CLEAN_STAINLESS_STEEL);
            for (int y = 0; y <= i + 1; ++y) {
                builder.add3by3(y, casing, y != 0, hatchFlags);
                if (y != 0) {
                    builder.add(0, y, 1, pipe, null);

                }
            }
            shapeTemplates[i] = builder.build();
        }
    }
}
