package eu.j3t.gps;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class GPSPlugin extends JavaPlugin implements Listener {
        
    /* Hash to store the path for each player */
    private HashMap<UUID, GPSSearchPathInfo> playerPaths;
    private boolean debugLog;
    
    @Override
    public void onEnable()
    {
        playerPaths = new HashMap<UUID, GPSSearchPathInfo>();
        this.getServer().getPluginManager().registerEvents(this, this);
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        String commandString = command.getName().toLowerCase();
        
        if (commandString.equals("gps")) {
            if (sender instanceof Player) {
                Player player = (Player)sender;
                GPSSearchPathInfo pathInfo = this.playerPaths.get(player.getUniqueId());
                
                if (args.length == 0) {
                    player.sendMessage("Il faut mettre le nom d'un joueur");
                } else if (args.length == 1 && args[0].equals("debuglog")) {
                    this.debugLog = !this.debugLog;
                    player.sendMessage("log debug : " + this.debugLog);
                } else if (args.length == 1 && args[0].equals("debug")) {
                    if (pathInfo != null) {
                        pathInfo.setSnowDebug(!pathInfo.snowDebug());
                        player.sendMessage("debug : " + pathInfo.snowDebug());
                        this.recomputePathForPlayer(player, pathInfo);
                    } else {
                        player.sendMessage("pas de gps en cours");
                    }
                } else {
                    Player playerTarget = null;
                    GPSSearch search = new GPSSearch(this.debugLog);
                    int distance = 0;
                    
                    if (pathInfo == null) {
                        pathInfo = new GPSSearchPathInfo();
                        pathInfo.setPreviousCompassTarget(player.getCompassTarget());
                    } else {
                        pathInfo.setPath(null);
                        pathInfo.setPlayerUUIDTarget(null);
                        pathInfo.setLocationTarget(null);
                    }
                    search.setFrom(player.getLocation());
                    
                    playerTarget = Bukkit.getServer().getPlayer(args[0]);
                    try {
                        distance = Integer.parseInt(args[0]);
                    } catch (Exception e) {
                    }
                    if (args.length >= 2) {
                        pathInfo.setSnowDebug(args[1].equals("debug"));
                    }
                    if (playerTarget != null) {
                        pathInfo.setPlayerUUIDTarget(playerTarget.getUniqueId());
                        search.setTo(playerTarget.getLocation());
                        this.getLogger().info("gps to " + playerTarget.getName());
                    } else if (distance > 0) {
                        Location locationTarget = player.getLocation();
                        double yaw = (locationTarget.getYaw() + 360) % 360;
                        
                        if (yaw > 45 && yaw <= 135) {
                            locationTarget.add(-distance, 0, 0);
                        } else if (yaw > 135 && yaw <= 225) {
                            locationTarget.add(0, 0, -distance);
                        } else if (yaw > 225 && yaw <= 315) {
                            locationTarget.add(distance, 0, 0);
                        } else {
                            locationTarget.add(0, 0, distance);
                        }
                        pathInfo.setLocationTarget(locationTarget);
                        search.setTo(locationTarget);
                        this.getLogger().info("gps to " + pathInfo.locationTarget().toString());
                    } else {
                        player.sendMessage("Joueur inconnu");
                        search = null;
                    }
                    
                    if (search != null) {
                        pathInfo.setPath(search.search());
                        if (pathInfo.path() == null) {
                            player.sendMessage("Pas de chemin");
                        } else {
                            this.playerPaths.put(player.getUniqueId(), pathInfo);
                            player.sendMessage("C'est parti, distance : " + (int)search.getPathDistance());
                            this.updateCompassForPlayer(player);
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }
    
    private void removeSearchPathForPlayer(Player player, GPSSearchPathInfo pathInfo)
    {
        player.setCompassTarget(pathInfo.previousCompassTarget());
        this.playerPaths.remove(player.getUniqueId());
    }
    
    private void updateCompassForPlayer(Player player)
    {
        GPSSearchPathInfo pathInfo = this.playerPaths.get(player.getUniqueId());
        Location playerLocation = player.getLocation();
        
        if (pathInfo != null && pathInfo.path() != null && playerLocation != null) {
            if (pathInfo.path().get(0).distance(playerLocation) > 15) {
                player.sendMessage("Vous êtes trop loin");
                pathInfo.setPath(null);
                this.removeSearchPathForPlayer(player, pathInfo);
            } else {
                while ((pathInfo.path().size() > 0 && pathInfo.path().get(0).distance(player.getLocation()) < 2)
                        || (pathInfo.path().size() > 1 && pathInfo.path().get(1).distance(player.getLocation()) < 2)
                        || (pathInfo.path().size() > 2 && pathInfo.path().get(2).distance(player.getLocation()) < 2)) {
                    /* if we are not too far from the first, the second or third location, remove them to go to the next one */
                    pathInfo.removeFirstLocation();
                }
                /* if the player when walked 20 blocks, let's recompute the path, case the player target moved */
                if (pathInfo.stepRemoved() >= 20) {
                    double pathDistance = this.recomputePathForPlayer(player, pathInfo);
                    if (pathInfo.path() != null) {
                        player.sendMessage("Distance : " + (int)pathDistance);
                    }
                }
            }
            /* update the compass if we still have a path */
            if (pathInfo.path() == null) {
                this.removeSearchPathForPlayer(player, pathInfo);
            } else if (pathInfo.path().size() == 0) {
                player.sendMessage("Vous êtes arrivé");
                this.removeSearchPathForPlayer(player, pathInfo);
            } else {
                Location location = pathInfo.path().get(0);
                
                player.setCompassTarget(location);
            }
        }
    }

    private double recomputePathForPlayer(Player player, GPSSearchPathInfo pathInfo)
    {
        GPSSearch search = new GPSSearch();
        Player playerTarget = Bukkit.getPlayer(pathInfo.playerUUIDTarget());
        double pathDistance = 0;
        
        /* recompute the path */
        if (playerTarget != null) {
            search.setFrom(player.getLocation());
            search.setTo(playerTarget.getLocation());
            pathInfo.setPath(search.search());
            pathDistance = search.getPathDistance();
            Bukkit.getServer().getLogger().info("recompute " + playerTarget.toString());
            if (pathInfo.path() == null) {
                player.sendMessage("Il n'y a pas de chemin");
            }
        } else if (pathInfo.playerUUIDTarget() == null) {
            search.setFrom(player.getLocation());
            search.setTo(pathInfo.locationTarget());
            pathInfo.setPath(search.search());
            pathDistance = search.getPathDistance();
            Bukkit.getServer().getLogger().info("recompute " + pathInfo.locationTarget().toString());
            if (pathInfo.path() == null) {
                player.sendMessage("Il n'y a pas de chemin");
            }
        } else {
            player.sendMessage("Joueur inconnu");
            pathInfo.setPath(null);
        }
        return pathDistance;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        updateCompassForPlayer(event.getPlayer());
    }
}
