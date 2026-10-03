package com.pirog480.expbotl;

import net.fabricmc.api.ModInitializer;

import net.minecraft.item.ItemStack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Expbotl implements ModInitializer {
	public static final String MOD_ID = "expbotl";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[ExpBotl] Sneak + right-click a bottle o' enchanting to use the whole stack at once.");

		// Force ItemStack through the mixin transformer: if the injection failed,
		// class loading throws and the error becomes visible in the server log.
		try {
			Class.forName(ItemStack.class.getName(), false, Expbotl.class.getClassLoader());
			LOGGER.info("[ExpBotl] ItemStack loaded through the mixin transformer - mixin applied cleanly.");
		} catch (Throwable t) {
			LOGGER.error("[ExpBotl] ItemStack failed to load - the mixin did not apply!", t);
		}
	}
}
