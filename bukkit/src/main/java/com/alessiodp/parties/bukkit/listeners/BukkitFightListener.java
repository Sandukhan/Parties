package com.alessiodp.parties.bukkit.listeners;

import com.alessiodp.parties.bukkit.configuration.data.BukkitConfigParties;
import com.alessiodp.parties.bukkit.configuration.data.BukkitMessages;
import com.alessiodp.parties.bukkit.events.BukkitEventManager;
import com.alessiodp.parties.bukkit.parties.objects.BukkitPartyImpl;
import com.alessiodp.parties.common.PartiesPlugin;
import com.alessiodp.parties.common.configuration.Constants;
import com.alessiodp.parties.common.logging.LogLevel;
import com.alessiodp.parties.common.logging.LoggerManager;
import com.alessiodp.parties.common.parties.objects.PartyImpl;
import com.alessiodp.parties.common.players.PartiesPermission;
import com.alessiodp.parties.common.players.objects.PartyPlayerImpl;
import com.alessiodp.parties.api.events.bukkit.unique.BukkitPartiesCombustFriendlyFireBlockedEvent;
import com.alessiodp.parties.api.events.bukkit.unique.BukkitPartiesFriendlyFireBlockedEvent;
import com.alessiodp.parties.api.events.bukkit.unique.BukkitPartiesPotionsFriendlyFireBlockedEvent;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.projectiles.ProjectileSource;

public class BukkitFightListener implements Listener {
	private PartiesPlugin plugin;
	
	public BukkitFightListener(PartiesPlugin instance) {
		plugin = instance;
	}
	
	@EventHandler
	public void onPlayerHit(EntityDamageByEntityEvent event) {
		if (BukkitConfigParties.FRIENDLYFIRE_ENABLE && event.getEntity() instanceof Player) {
			Player victim = (Player) event.getEntity();
			Player attacker = null;
			DamageType type = DamageType.UNSUPPORTED;
			if (event.getDamager() instanceof Player)
				type = DamageType.PLAYER;
			else if (event.getDamager() instanceof Arrow)
				type = DamageType.ARROW;
			else if (event.getDamager() instanceof EnderPearl)
				type = DamageType.ENDERPEARL;
			else if (event.getDamager() instanceof Snowball)
				type = DamageType.SNOWBALL;
			
			if (!type.equals(DamageType.UNSUPPORTED)) {
				ProjectileSource shooterSource;
				switch (type) {
				case PLAYER:
					attacker = (Player) event.getDamager();
					break;
				case ARROW:
					shooterSource = ((Arrow)event.getDamager()).getShooter();
					if (shooterSource instanceof Player)
						attacker = (Player) shooterSource;
					break;
				case ENDERPEARL:
					shooterSource = ((EnderPearl)event.getDamager()).getShooter();
					if (shooterSource instanceof Player)
						attacker = (Player) shooterSource;
					break;
				case SNOWBALL:
					shooterSource = ((Snowball)event.getDamager()).getShooter();
					if (shooterSource instanceof Player)
						attacker = (Player) shooterSource;
					break;
				case UNSUPPORTED:
				}
				if (attacker != null) {
					// Found right attacker
					if (!victim.getUniqueId().equals(attacker.getUniqueId())) {
						// Friendly fire not allowed here
						PartyPlayerImpl ppVictim = plugin.getPlayerManager().getPlayer(victim.getUniqueId());
						PartyPlayerImpl ppAttacker = plugin.getPlayerManager().getPlayer(attacker.getUniqueId());
						BukkitPartyImpl party = (BukkitPartyImpl) plugin.getPartyManager().getParty(ppAttacker.getPartyName());
						
						if (party != null && ppVictim.getPartyName().equalsIgnoreCase(ppAttacker.getPartyName())) {
							if (party.isFriendlyFireProtected() && !attacker.hasPermission(PartiesPermission.ADMIN_PROTECTION_BYPASS.toString())) {
								// Calling API event
								BukkitPartiesFriendlyFireBlockedEvent partiesFriendlyFireEvent = ((BukkitEventManager) plugin.getEventManager()).preparePartiesFriendlyFireBlockedEvent(ppVictim, ppAttacker, event);
								plugin.getEventManager().callEvent(partiesFriendlyFireEvent);
								
								if (!partiesFriendlyFireEvent.isCancelled()) {
									// Friendly fire confirmed
									ppAttacker.sendMessage(BukkitMessages.ADDCMD_PROTECTION_PROTECTED);
									party.sendFriendlyFireWarn(ppVictim, ppAttacker);
									
									event.setCancelled(true);
									LoggerManager.log(LogLevel.DEBUG, Constants.DEBUG_FRIENDLYFIRE_DENIED
											.replace("{type}", type.name())
											.replace("{player}", attacker.getName())
											.replace("{victim}", victim.getName()), true);
								} else
									LoggerManager.log(LogLevel.DEBUG, Constants.DEBUG_API_FRIENDLYFIREEVENT_DENY
											.replace("{type}", type.name())
											.replace("{player}", attacker.getName())
											.replace("{victim}", victim.getName()), true);
							}
						}
					}
				}
			}
		}
	}
	@EventHandler
	public void onPotionSplash(PotionSplashEvent event) {
		if (BukkitConfigParties.FRIENDLYFIRE_ENABLE
				&& event.getEntity() instanceof Player
				&& event.getPotion().getShooter() instanceof Player) {
			Player attacker = (Player) event.getPotion().getShooter();
			PartyPlayerImpl ppAttacker = plugin.getPlayerManager().getPlayer(attacker.getUniqueId());
			BukkitPartyImpl party = (BukkitPartyImpl) plugin.getPartyManager().getParty(ppAttacker.getPartyName());
			
			if (party != null && party.isFriendlyFireProtected() && !attacker.hasPermission(PartiesPermission.ADMIN_PROTECTION_BYPASS.toString())) {
				boolean cancel = false;
				for (PotionEffect pe : event.getEntity().getEffects()) {
					switch (pe.getType().getName().toLowerCase()) {
					case "harm":
					case "blindness":
					case "confusion":
					case "poison":
					case "slow":
					case "slow_digging":
					case "weakness":
					case "unluck":
						cancel = true;
					}
					if (cancel)
						break;
				}
				if (cancel) {
					// Friendly fire not allowed here
					for (LivingEntity e : event.getAffectedEntities()) {
						if (e instanceof Player) {
							Player victim = (Player) e;
							if (!attacker.equals(victim)) {
								PartyPlayerImpl ppVictim = plugin.getPlayerManager().getPlayer(victim.getUniqueId());
								if (ppVictim.getPartyName().equalsIgnoreCase(ppAttacker.getPartyName())) {
									// Calling API event
									BukkitPartiesPotionsFriendlyFireBlockedEvent partiesFriendlyFireEvent = ((BukkitEventManager) plugin.getEventManager()).preparePartiesPotionsFriendlyFireBlockedEvent(ppVictim, ppAttacker, event);
									plugin.getEventManager().callEvent(partiesFriendlyFireEvent);
									
									if (!partiesFriendlyFireEvent.isCancelled()) {
										// Friendly fire confirmed
										ppAttacker.sendMessage(BukkitMessages.ADDCMD_PROTECTION_PROTECTED);
										party.sendFriendlyFireWarn(ppVictim, ppAttacker);
										
										event.setIntensity(e, 0);
										LoggerManager.log(LogLevel.DEBUG, Constants.DEBUG_FRIENDLYFIRE_DENIED
												.replace("{type}", "potion splash")
												.replace("{player}", attacker.getName())
												.replace("{victim}", victim.getName()), true);
									} else
										LoggerManager.log(LogLevel.DEBUG, Constants.DEBUG_API_FRIENDLYFIREEVENT_DENY
												.replace("{type}", "potion splash")
												.replace("{player}", attacker.getName())
												.replace("{victim}", victim.getName()), true);
								}
							}
						}
					}
				}
			}
		}
	}
	@EventHandler
	public void onEntityCombustByEntity(EntityCombustByEntityEvent event) {
		if (BukkitConfigParties.FRIENDLYFIRE_ENABLE
				&& event.getEntity() instanceof Player
				&& event.getCombuster() instanceof Projectile
				&& ((Projectile) event.getCombuster()).getShooter() instanceof Player) {
			Player victim = (Player) event.getEntity();
			Player attacker = (Player)((Projectile) event.getCombuster()).getShooter();
			
			// Found right attacker
			if (!victim.getUniqueId().equals(attacker.getUniqueId())) {
				// Friendly fire not allowed here
				PartyPlayerImpl ppVictim = plugin.getPlayerManager().getPlayer(victim.getUniqueId());
				PartyPlayerImpl ppAttacker = plugin.getPlayerManager().getPlayer(attacker.getUniqueId());
				BukkitPartyImpl party = (BukkitPartyImpl) plugin.getPartyManager().getParty(ppAttacker.getPartyName());
				
				if (party != null && ppVictim.getPartyName().equalsIgnoreCase(ppAttacker.getPartyName())) {
					if (party.isFriendlyFireProtected() && !attacker.hasPermission(PartiesPermission.ADMIN_PROTECTION_BYPASS.toString())) {
						// Calling API event
						BukkitPartiesCombustFriendlyFireBlockedEvent partiesFriendlyFireEvent = ((BukkitEventManager) plugin.getEventManager()).prepareCombustFriendlyFireBlockedEvent(ppVictim, ppAttacker, event);
						plugin.getEventManager().callEvent(partiesFriendlyFireEvent);
						
						if (!partiesFriendlyFireEvent.isCancelled()) {
							// Friendly fire confirmed
							ppAttacker.sendMessage(BukkitMessages.ADDCMD_PROTECTION_PROTECTED);
							party.sendFriendlyFireWarn(ppVictim, ppAttacker);
							
							event.setCancelled(true);
							LoggerManager.log(LogLevel.DEBUG, Constants.DEBUG_FRIENDLYFIRE_DENIED
									.replace("{type}", "entity combust")
									.replace("{player}", attacker.getName())
									.replace("{victim}", victim.getName()), true);
						} else
							LoggerManager.log(LogLevel.DEBUG, Constants.DEBUG_API_FRIENDLYFIREEVENT_DENY
									.replace("{type}", "entity combust")
									.replace("{player}", attacker.getName())
									.replace("{victim}", victim.getName()), true);
					}
				}
			}
			
		}
	}
	
	@EventHandler
	public void onEntityDieKill(EntityDeathEvent event) {
		if (BukkitConfigParties.KILLS_ENABLE) {
			if (event.getEntity().getKiller() != null) {
				Player killer = event.getEntity().getKiller();
				PartyPlayerImpl ppKiller = plugin.getPlayerManager().getPlayer(killer.getUniqueId());
				
				if (!ppKiller.getPartyName().isEmpty()) {
					PartyImpl party = plugin.getPartyManager().getParty(ppKiller.getPartyName());
					boolean gotKill = false;
					
					if (BukkitConfigParties.KILLS_MOB_HOSTILE
							&& event.getEntity() instanceof Monster)
						gotKill = true;
					else if (BukkitConfigParties.KILLS_MOB_NEUTRAL
							&& event.getEntity() instanceof Animals)
						gotKill = true;
					else if (BukkitConfigParties.KILLS_MOB_PLAYERS
							&& event.getEntity() instanceof Player)
						gotKill = true;
					
					if (gotKill) {
						party.setKills(party.getKills() + 1);
						party.updateParty();
						LoggerManager.log(LogLevel.MEDIUM, Constants.DEBUG_KILL_ADD
								.replace("{party}", party.getName())
								.replace("{player}", killer.getName()), true);
					}
				}
			}
		}
	}
	
	private enum DamageType {
		UNSUPPORTED, PLAYER, ARROW, ENDERPEARL, SNOWBALL
	}
}
