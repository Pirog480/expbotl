package com.pirog480.expbotl.mixin;

import com.pirog480.expbotl.ExperienceRoller;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts {@link ItemStack#use} (the entry point of every "use this item"
 * interaction, invoked on both the logical client and server - it is what
 * vanilla's own {@code ExperienceBottleItem#use} is called through).
 *
 * <p>When the held stack is a bottle o' enchanting and the player is sneaking,
 * the whole stack is consumed instantly and the combined experience of every
 * bottle is granted directly to the player. A plain right-click keeps the
 * vanilla behaviour of throwing a single bottle.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void expbotl$useWholeStack(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
		ItemStack self = (ItemStack) (Object) this;

		// Only intercept sneak + use with a bottle o' enchanting; everything else stays vanilla.
		if (!self.isOf(Items.EXPERIENCE_BOTTLE) || !user.isSneaking()) {
			return;
		}

		int bottles = self.getCount();
		if (bottles <= 0) {
			return;
		}

		// Mirror vanilla: keep the "item used" statistic ticking (once per use, like a single throw).
		user.incrementStat(Stats.USED.getOrCreateStat(Items.EXPERIENCE_BOTTLE));

		// Predict the consumption locally on both sides; the server is authoritative for the XP.
		self.decrementUnlessCreative(bottles, user);

		if (!world.isClient()) {
			// Server: roll every bottle exactly like vanilla orbs would
			// (3 + two uniform 0..4 dice per bottle) and grant the sum directly.
			// No orb entities, no pickup delay - friendly to large stacks.
			int amount = ExperienceRoller.roll(world.getRandom(), bottles);
			user.addExperience(amount);

			// Feedback: the bottle's own splash event (glass smash + green droplets),
			// the orb pickup "pling" and a burst of green sparks around the player.
			world.syncWorldEvent(WorldEvents.SPLASH_POTION_SPLASHED, user.getBlockPos(), -13083194);
			world.playSound(null, user.getX(), user.getY(), user.getZ(),
					SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.3F, 0.65F);

			if (world instanceof ServerWorld serverWorld) {
				serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
						user.getX(), user.getBodyY(0.5D), user.getZ(),
						Math.min(24, 4 + bottles / 8), 0.4D, 0.5D, 0.4D, 0.05D);
			}

			// Make sure every client (also vanilla ones, when only the server runs the mod)
			// sees the emptied hand slot immediately.
			user.currentScreenHandler.sendContentUpdates();
		}

		// Same result the vanilla bottle throw produces (ItemStack.use wraps the item's
		// SUCCESS with the mutated hand stack as the new hand stack): swings the hand,
		// and hands the caller the already-decremented stack instance.
		cir.setReturnValue(ActionResult.SUCCESS.withNewHandStack(self));
	}
}
