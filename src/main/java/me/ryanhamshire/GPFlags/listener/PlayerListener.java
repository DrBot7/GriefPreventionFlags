package me.ryanhamshire.GPFlags.listener;

import me.ryanhamshire.GPFlags.event.PlayerClaimBorderEvent;
import me.ryanhamshire.GPFlags.util.Util;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.events.ClaimDeletedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimModifiedEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;

public class PlayerListener implements Listener {

	private HashMap<Player, Boolean> fallingPlayers = new HashMap<>();
	private DataStore dataStore = GriefPrevention.instance.dataStore;

	@EventHandler
	private void onFall(EntityDamageEvent e) {
		if (!(e.getEntity() instanceof Player)) return;
		Player p = ((Player) e.getEntity());
		EntityDamageEvent.DamageCause cause = e.getCause();
		if (cause != EntityDamageEvent.DamageCause.FALL) return;
		Boolean val = fallingPlayers.get(p);
		if (val != null && val) {
			e.setCancelled(true);
			fallingPlayers.remove(p);
		}
	}

	/** Add a player to prevent fall damage under certain conditions
	 * @param player Player to add
	 */
	public void addFallingPlayer(Player player) {
		this.fallingPlayers.put(player, true);
	}

	@EventHandler
    private void onMove(PlayerMoveEvent event) {
        if (event.isCancelled()) return;
        if (event.getTo() == null) return;
        Location locTo = event.getTo();
        Location locFrom = event.getFrom();
        Player player = event.getPlayer();
        processMovement(locTo, locFrom, player, event);
    }

    @EventHandler
    private void onTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled()) return;
        if (event.getTo() == null) return;
        Location locTo = event.getTo();
        Location locFrom = event.getFrom();
        Player player = event.getPlayer();
        processMovement(locTo, locFrom, player, event);
    }

    private void processMovement(Location locTo, Location locFrom, Player player, Cancellable event) {
        if (locTo.getBlockX() == locFrom.getBlockX() && locTo.getBlockY() == locFrom.getBlockY() && locTo.getBlockZ() == locFrom.getBlockZ()) return;

        Claim claimTo = dataStore.getClaimAt(locTo, false, null);
        Claim claimFrom = dataStore.getClaimAt(locFrom, false, null);
        if (claimTo == null && claimFrom == null) return;
        if (claimTo == claimFrom) return;
        PlayerClaimBorderEvent playerClaimBorderEvent = new PlayerClaimBorderEvent(player, claimFrom, claimTo, locFrom, locTo);
        Bukkit.getPluginManager().callEvent(playerClaimBorderEvent);
        event.setCancelled(playerClaimBorderEvent.isCancelled());
    }

    @EventHandler
    // Disable flight when a player deletes their claim
    private void onDeleteClaim(ClaimDeletedEvent event) {
	    Claim claim = event.getClaim();
        World world = claim.getGreaterBoundaryCorner().getWorld();
        assert world != null;
	    for (Player player : world.getPlayers()) {
	        if (claim.contains(player.getLocation(), false, true)) {
                Util.disableFlight(player);
            }
        }
    }

    @EventHandler
    // Call the claim border event when a player resizes a claim and they are now outside of the claim
    private void onChangeClaim(ClaimModifiedEvent event) {
	    Claim claim = event.getClaim();
        CommandSender modifier = event.getModifier();
	    if (modifier instanceof Player) {
	        Player player = ((Player) modifier);
	        Location loc = player.getLocation();
	        Claim claimAtLoc = GriefPrevention.instance.dataStore.getClaimAt(loc, false, null);
	        if (claimAtLoc == null) {
	            PlayerClaimBorderEvent borderEvent = new PlayerClaimBorderEvent(player, claim, null, claim.getLesserBoundaryCorner(), loc);
	            Bukkit.getPluginManager().callEvent(borderEvent);
            }
        }
    }

}
