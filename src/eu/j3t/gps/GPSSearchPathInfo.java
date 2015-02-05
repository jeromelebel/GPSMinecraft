package eu.j3t.gps;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;

/*
 * internal class to keep track of the path for each player
 * 
 * previousCompassTarget: since we mess up with the compass, the goal is to
 *                          the previous value back, when the GPS stops
 * path: list of location to follow to reach the destination
 * playerUUIDTarget: UUID for the destination, useful to recompute the path
 * locationTarget: used if the GPS is set to a location instead of a player
 * stepRemoved: number of location been reached by the player since the last
 *              recomputation
 * snowDebug: if true, the path is recovered by snow
 * 
 * */

public class GPSSearchPathInfo {
    private Location previousCompassTarget;
    private List<Location> path;
    private UUID playerUUIDTarget;
    private Location locationTarget;
    private int stepRemoved;
    private boolean snowDebug;
    
    protected void removeFirstLocation()
    {
        Location location = this.path.get(0);
        
        if (this.snowDebug && location.getBlock().getType() == Material.SNOW) {
            location.getBlock().setType(Material.AIR);
        }
        this.path.remove(0);
        this.stepRemoved += 1;
    }

    protected void setPreviousCompassTarget(Location newLocation)
    {
        this.previousCompassTarget = newLocation;
    }
    
    protected Location previousCompassTarget()
    {
        return this.previousCompassTarget;
    }
    
    protected void setPath(List<Location> newPath)
    {
        if (this.snowDebug) {
            this.removeSnow();
        }
        this.path = newPath;
        this.stepRemoved = 0;
        if (this.snowDebug) {
            this.addSnow();
        }
    }
    
    protected List<Location> path()
    {
        return this.path;
    }
    
    protected void setSnowDebug(boolean newDebug)
    {
        if (this.snowDebug != newDebug) {
            if (this.snowDebug) {
                this.removeSnow();
            }
            this.snowDebug = newDebug;
            if (this.snowDebug) {
                this.addSnow();
            }
        }
    }
    
    protected boolean snowDebug()
    {
        return this.snowDebug;
    }
    
    private void addSnow()
    {
        if (this.path != null) {
            for (Location location : this.path) {
                Location underLocation = location.clone();
                
                underLocation.add(0, -1, 0);
                if (underLocation.getBlock().getType().isSolid() && location.getBlock().getType() == Material.AIR) {
                    location.getBlock().setType(Material.SNOW);
                }
            }
        }
    }
    
    private void removeSnow()
    {
        if (this.path != null) {
            for (Location location : this.path) {
                if (location.getBlock().getType() == Material.SNOW) {
                    location.getBlock().setType(Material.AIR);
                }
            }
        }
    }
    
    protected void setPlayerUUIDTarget(UUID target)
    {
        this.playerUUIDTarget = target;
    }
    
    protected UUID playerUUIDTarget()
    {
        return this.playerUUIDTarget;
    }
    
    protected int stepRemoved()
    {
        return this.stepRemoved;
    }
    
    protected void setLocationTarget(Location newTarget)
    {
        this.locationTarget = newTarget;
    }
    
    protected Location locationTarget()
    {
        return this.locationTarget;
    }
}
