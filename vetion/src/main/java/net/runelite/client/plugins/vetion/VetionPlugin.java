/*
 * Copyright (c) 2019, Frosty Fridge <https://github.com/frostyfridge>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.vetion;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.events.AnimationChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;

@Extension
@PluginDescriptor(
	name = "Vetion Helper",
	enabledByDefault = false,
	description = "Tracks Vet'ion's special attacks",
	tags = {"bosses", "combat", "pve", "overlay"}
)
public class VetionPlugin extends Plugin
{

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private VetionOverlay overlay;

	@Getter(AccessLevel.PACKAGE)
	private Map<Actor, Instant> vetions;

	public static final int VETION_EARTHQUAKE = 5507;

	@Override
	protected void startUp()
	{

		vetions = new HashMap<>();
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		vetions = null;
	}

	private boolean isVetion(NPC npc)
	{
		switch (npc.getId())
		{
			case NpcID.VETION:
			case NpcID.VETION_REBORN:
				return true;
		}
		return false;
	}

	@Subscribe
	private void onAnimationChanged(AnimationChanged event)
	{
		Actor actor = event.getActor();
		if (actor instanceof NPC
				&& isVetion((NPC) actor)
				&& actor.getAnimation() == VETION_EARTHQUAKE)
		{
			vetions.remove(actor, Instant.now());
			vetions.put(actor, Instant.now());
		}
	}
}