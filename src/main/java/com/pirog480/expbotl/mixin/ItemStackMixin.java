package com.pirog480.expbotl.mixin;

import com.pirog480.expbotl.ExperienceRoller;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts {@link ItemStack#use} (the entry point of every "use this item"
 * interaction, invoked on both the logical client and server).
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

		if (world.isClient()) {
			// Client: accept the interaction so the use packet is sent to the server.
			// The server performs the real work and syncs the emptied stack and the XP.
			cir.setReturnValue(ActionResult.SUCCESS);
			return;
		}

		// Server: roll every bottle (3..11 XP each, uniform - same as vanilla orbs),
		// sum it up and grant the experience directly. No orb entities, no pickup delay.
		int amount = ExperienceRoller.roll(world.getRandom(), bottles);
		user.addExperience(amount);
		self.decrementUnlessCreative(bottles, user);

		// Feedback: the orb pickup "pling" plus a burst of green sparks around the player.
		world.playSound(null, user.getX(), user.getY(), user.getZ(),
				SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.3F, 0.65F);

		if (world instanceof ServerWorld serverWorld) {
			serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
					user.getX(), user.getBodyY(0.5D), user.getZ(),
					Math.min(24, 4 + bottles / 8), 0.4D, 0.5D, 0.4D, 0.05D);
		}

		cir.setReturnValue(ActionResult.SUCCESS);
	}
}
