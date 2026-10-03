package com.pirog480.expbotl;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Expbotl implements ModInitializer {
	public static final String MOD_ID = "expbotl";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[ExpBotl] Sneak + right-click a bottle o' enchanting to use the whole stack at once.");
	}
}
