package eu.j3t.gps;

import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

/*
 * A* algo
 * 
 * fromLocation: where the player is
 * toLocation: where the player wants to go
 * fromNode: block node where the player is
 */

public class GPSSearch {
    private Location fromLocation;
    private Location toLocation;
    private boolean logging;
    private GPSNode fromNode;
    
    protected static boolean canWalkOnBlock(Location location)
    {
        Location standOn = new Location(location.getWorld(), location.getX(), location.getY() - 1, location.getZ());
        Location above = new Location(location.getWorld(), location.getX(), location.getY() + 1, location.getZ());
        
        return GPSMap.canWalkOn(standOn.getBlock().getType(), location.getBlock().getType(), above.getBlock().getType());
    }
    
    public GPSSearch()
    {
    }
    
    public GPSSearch(boolean newLogging)
    {
        this.logging = newLogging;
    }
    
    public void setFrom(Location newFrom)
    {
        this.fromLocation = newFrom.getBlock().getLocation().clone();
        if (this.fromLocation.getBlock().getType() == Material.AIR) {
            while (this.fromLocation.getBlock().getType() == Material.AIR) {
                this.fromLocation.add(0, -1, 0);
            }
            this.fromLocation.add(0, 1, 0);
        }
    }
    
    public void setTo(Location newTo)
    {
        this.toLocation = newTo.getBlock().getLocation().clone();
        if (this.toLocation.getBlock().getType() == Material.AIR) {
            while (this.toLocation.getBlock().getType() == Material.AIR) {
                this.toLocation.add(0, -1, 0);
            }
            this.toLocation.add(0, 1, 0);
        }
    }
    
    public List<Location> search()
    {
        GPSNode node = null;
        GPSMap map = new GPSMap(this.toLocation.getWorld());
        PriorityQueue<GPSNode> queue = new PriorityQueue<GPSNode>();
        
        if (this.toLocation.getWorld() != this.fromLocation.getWorld() || !GPSSearch.canWalkOnBlock(this.toLocation) || !GPSSearch.canWalkOnBlock(this.fromLocation)) {
            Bukkit.getServer().getLogger().info("from: " + this.fromLocation.toString());
            Bukkit.getServer().getLogger().info("to: " + this.toLocation.toString());
            Bukkit.getServer().getLogger().info("impossible");
            return null;
        }
        
        node = map.getOrCreateNode(null, this.toLocation.getBlockX(), this.toLocation.getBlockY(), this.toLocation.getBlockZ());
        node.setWeight(this.fromLocation.distance(this.fromLocation));
        queue.add(node);
        while (queue.size() > 0 && this.fromNode == null) {
            node = queue.poll();
            
            for (GPSNode nextNode : map.childNodes(node)) {
                if (nextNode.getWeight() == Double.MAX_VALUE) {
                    double fromDistance = map.distanceNodeFromLocation(nextNode, this.fromLocation);
                    
                    nextNode.setWeight(fromDistance);
                    if (logging) {
                        Bukkit.getServer().getLogger().info("add node: " + nextNode.toString());
                    }
                    queue.offer(nextNode);
                    if (fromDistance < 1.0) {
                        this.fromNode = nextNode;
                    }
                }
            }
            if (queue.size() > 1000) {
                Bukkit.getServer().getLogger().info("trop");
                return null;
            }
        }
        if (this.fromNode != null) {
            LinkedList<Location> result = new LinkedList<Location>();
            GPSNode currentNode = this.fromNode;
            while (currentNode != null) {
                Location location = map.getLocation(currentNode);
                
                result.push(location);
                currentNode = currentNode.getParent();
            }
            return result;
        }
        return null;
    }
}
