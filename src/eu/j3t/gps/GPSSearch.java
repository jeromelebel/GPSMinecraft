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
    private Location startLocation;
    private Location destinationLocation;
    private boolean debugLog;
    private GPSMapNode destinationNode;
    
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
        this.debugLog = newLogging;
    }
    
    public void setFrom(Location newFrom)
    {
        this.startLocation = newFrom.getBlock().getLocation().clone();
        if (this.startLocation.getBlock().getType() == Material.AIR) {
            while (this.startLocation.getBlock().getType() == Material.AIR) {
                this.startLocation.add(0, -1, 0);
            }
            this.startLocation.add(0, 1, 0);
        }
    }
    
    public void setTo(Location newTo)
    {
        this.destinationLocation = newTo.getBlock().getLocation().clone();
        if (this.destinationLocation.getBlock().getType() == Material.AIR) {
            while (this.destinationLocation.getBlock().getType() == Material.AIR) {
                this.destinationLocation.add(0, -1, 0);
            }
            this.destinationLocation.add(0, 1, 0);
        }
    }
    
    public List<Location> search()
    {
        GPSMapNode node = null;
        GPSMap map = new GPSMap(this.destinationLocation.getWorld(), this.debugLog);
        PriorityQueue<GPSMapNode> queue = new PriorityQueue<GPSMapNode>();
        
        if (this.destinationLocation.getWorld() != this.startLocation.getWorld() || !GPSSearch.canWalkOnBlock(this.destinationLocation) || !GPSSearch.canWalkOnBlock(this.startLocation)) {
            Bukkit.getServer().getLogger().info("from: " + this.startLocation.toString());
            Bukkit.getServer().getLogger().info("to: " + this.destinationLocation.toString());
            Bukkit.getServer().getLogger().info("impossible");
            return null;
        }
        
        node = map.getOrCreateNode(null, this.destinationLocation.getBlockX(), this.destinationLocation.getBlockY(), this.destinationLocation.getBlockZ());
        node.setWeight(this.startLocation.distance(this.destinationLocation));
        queue.add(node);
        while (queue.size() > 0 && this.destinationNode == null) {
            node = queue.poll();
            
            if (this.debugLog) {
                Bukkit.getServer().getLogger().info("unqueue node: " + node.toString());
            }
            for (GPSMapNode nextNode : map.childNodes(node)) {
                if (nextNode.getWeight() == Double.MAX_VALUE) {
                    double startDistance = GPSMap.distanceNodeFromLocation(nextNode, this.startLocation);
                    
                    nextNode.setWeight(startDistance);
                    if (this.debugLog) {
                        Bukkit.getServer().getLogger().info("queue node: " + nextNode.toString());
                    }
                    queue.offer(nextNode);
                    if (startDistance < 1.0) {
                        this.destinationNode = nextNode;
                    }
                }
            }
            if (queue.size() > 1000) {
                Bukkit.getServer().getLogger().info("trop");
                return null;
            }
        }
        if (this.destinationNode != null) {
            LinkedList<Location> result = new LinkedList<Location>();
            GPSMapNode currentNode = this.destinationNode;
            while (currentNode != null) {
                Location location = map.getLocation(currentNode);
                
                result.addLast(location);
                currentNode = currentNode.getParent();
            }
            if (this.debugLog) {
                Bukkit.getServer().getLogger().info("result array:");
                for (Location location : result) {
                    Bukkit.getServer().getLogger().info("    " + location.toString());
                }
                Bukkit.getServer().getLogger().info("distance " + this.destinationNode.getOriginDistance());
            }
            return result;
        }
        return null;
    }
    
    protected double getPathDistance()
    {
        return this.destinationNode.getOriginDistance();
    }
}
