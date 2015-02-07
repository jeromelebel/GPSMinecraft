package eu.j3t.gps;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class GPSMap {
    private GPSMapDimension dimension;
    private World world;
    private boolean debugLog;
    
    /*
     * compute a distance between <node1> and <node2>
     */
    static protected double distanceNode(GPSMapNode node1, GPSMapNode node2)
    {
        int xDifference = node1.getX() - node2.getX();
        int yDifference = node1.getY() - node2.getY();
        int zDifference = node1.getZ() - node2.getZ();
        
        return Math.sqrt(xDifference * xDifference + yDifference * yDifference + zDifference * zDifference);
    }

    /*
     * compute a distance between <node> and <location>
     */
    static protected double distanceNodeFromLocation(GPSMapNode node, Location location)
    {
        return distanceNodeFromPosition(node, location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
    
    /*
     * compute a distance between <node> and x, y, z
     */
    static protected double distanceNodeFromPosition(GPSMapNode node, int x, int y, int z)
    {
        int xDifference = node.getX() - x;
        int yDifference = node.getY() - y;
        int zDifference = node.getZ() - z;
        
        return Math.sqrt(xDifference * xDifference + yDifference * yDifference + zDifference * zDifference);
    }
    
    /*
     * return true if the player can go through the material set at this position 
     */
    protected static boolean canPassThru(World world, int x, int y, int z)
    {
        Location above = new Location(world, x + 0.5, y, z + 0.5);
        
        return canPassThru(above.getBlock().getType());
    }

    /*
     * return true if the player can go through this material
     * I'm assuming we can open all doors
     *  
     */
    @SuppressWarnings("deprecation")
    protected static boolean canPassThru(Material material)
    {
        /* we can walk through all those types of material */
        return material == Material.AIR
                || material == Material.SIGN_POST
                || material == Material.CARPET
                || material == Material.LADDER
                || material == Material.DEAD_BUSH
                || material == Material.VINE
                || material == Material.DOUBLE_PLANT
                || material == Material.RED_ROSE
                || material == Material.SUGAR_CANE_BLOCK
                || material == Material.RED_MUSHROOM
                || material == Material.BROWN_MUSHROOM
                || material == Material.SAPLING
                || material == Material.STONE_BUTTON
                || material == Material.WOOD_BUTTON
                || material == Material.POWERED_RAIL
                || material == Material.DETECTOR_RAIL
                || material == Material.ACTIVATOR_RAIL
                || material == Material.RAILS
                || material == Material.TORCH
                || material == Material.TRAP_DOOR
                || material == Material.FENCE_GATE
                || material == Material.WOOD_DOOR
                || material == Material.IRON_DOOR_BLOCK
                || material == Material.REDSTONE_TORCH_OFF
                || material == Material.LEVER
                || material == Material.GOLD_PLATE
                || material == Material.WOOD_PLATE
                || material == Material.IRON_PLATE
                || material == Material.STONE_PLATE
                || material == Material.TRIPWIRE_HOOK
                || material == Material.TRIPWIRE
                || material == Material.REDSTONE_TORCH_ON
                || material == Material.REDSTONE_COMPARATOR_OFF
                || material == Material.REDSTONE_COMPARATOR_ON
                || material == Material.REDSTONE_WIRE
                || material.getId() == 36; /* Piston Extension, not really used anymore */
    }

    /* m0 is the block material where the player stand on
     * m1 and m2 are the block material where the player is 
     * 
     * One case not supported: while climbing on vine or standing on top of vine.
     * 
     * Specific case: the user can walk on a ladder
     * 
     * Case not well supported: I don't deal the height of a snow block. If a snow block
     * is on top of a solid block, and below a "pass through" block, the player will
     * be able to go through. If the snow is too high, he will have to remove the snow
     */
    protected static boolean canWalkOn(Material m0, Material m1, Material m2)
    {
        boolean canWalkByM1 = canPassThru(m1);
        boolean canWalkByM2 = canPassThru(m2);
        
        return (m0.isSolid() && canWalkByM1 && canWalkByM2)
                || (m0.isSolid() && m1 == Material.SNOW && canWalkByM2)
                || (m0 == Material.SNOW && m1 == Material.SNOW && canWalkByM2)
                || (m0 == Material.SNOW && m1 != Material.AIR && canWalkByM1 && canWalkByM2)
                || (m1 == Material.LADDER && m2 == Material.LADDER)
                || (m0 == Material.LADDER && canWalkByM1 && canWalkByM2);
    }
    
    protected static boolean canWalkOn(World world, int x, int y, int z)
    {
        Material m0 = new Location(world, x, y - 1, z).getBlock().getType();
        Material m1 = new Location(world, x, y, z).getBlock().getType();
        Material m2 = new Location(world, x, y + 2, z).getBlock().getType();
        
        return canWalkOn(m0, m1, m2);
    }
    
    protected GPSMap(World newWorld, boolean newLoggin)
    {
        this.world = newWorld;
        this.dimension = new GPSMapDimension();
        this.debugLog = newLoggin;
    }
    
    protected GPSMapNode getNode(int x, int y, int z)
    {
        int[] position = { x, y, z };
        
        return this.dimension.getElement(position);
    }
    
    protected GPSMapNode getOrCreateNode(GPSMapNode parent, int x, int y, int z)
    {
        GPSMapNode result;
        
        result = this.getNode(x, y, z);
        if (result == null) {
            int[] position = { x, y, z };
            
            result = new GPSMapNode(parent, x, y, z);
            this.dimension.addElement(result, position);
        }
        return result;
    }
    
    /*
     * This test if the player can be at:
     *      <x> <y> <z> position
     *      <x> <y - 1> <z> position
     *      <x> <y + 1> <z> position
     *      <x> <y + 2> <z> position
     * and arrive at <parent>
     *
     * We are going backward, and we want to test if we arrive from a block at the same level,
     * one below (this mean jumping) or if we can arrive one or two blocks above (this mean falling down)
     *
     * IN:
     * <parentFreeBlockAboveCount> is the number of "pass thru" blocks above the head player at position <parent>
     *      that can be "pass thru"
     * <jumpFreeBlockAboveCount> is the number of "pass thru" blocks above the head player while
     *      jumping between <parent> and <x>, <y>, <z>
     * <jumpLength> is the number of horizontal blocks to jump to go between <parent> and <x>, <y> and <z>
     * 
     * OUT:
     * <list> is the list of GPSMapNode where the player can be to reach <parent> 
     */
    private boolean addGPSNode(GPSMapNode parent,
                               int x,
                               int y,
                               int z,
                               int parentFreeBlockAboveCount,
                               int jumpFreeBlockAboveCount,
                               int jumpLength,
                               List<GPSMapNode> list)
    {
        GPSMapNode newNode = null;
        
        Material[] materials = {
                new Location(this.world, x + 0.5, y - 2, z + 0.5).getBlock().getType(),
                new Location(this.world, x + 0.5, y - 1, z + 0.5).getBlock().getType(),
                new Location(this.world, x + 0.5, y, z + 0.5).getBlock().getType(),
                new Location(this.world, x + 0.5, y + 1, z + 0.5).getBlock().getType(),
                new Location(this.world, x + 0.5, y + 2, z + 0.5).getBlock().getType(),
                new Location(this.world, x + 0.5, y + 3, z + 0.5).getBlock().getType(),
                new Location(this.world, x + 0.5, y + 4, z + 0.5).getBlock().getType()
        };
        
        if (this.debugLog) {
            Bukkit.getServer().getLogger().info("test x: " + x + " y: " + y + " z: " + z);
        }
        if (GPSMap.canWalkOn(materials[1], materials[2], materials[3])
                && (jumpLength == 0
                    || (jumpFreeBlockAboveCount >= 1) && GPSMap.canPassThru(materials[4]))) {
            // same level
            // if <jumpLength> is greater than 0, we have to jump
            if (this.debugLog) {
                Bukkit.getServer().getLogger().info("    ok for y: " + y);
            }
            newNode = this.getOrCreateNode(parent, x, y, z);
        } else if (GPSMap.canWalkOn(materials[0], materials[1], materials[2])
                && GPSMap.canPassThru(materials[3])
                && (jumpLength == 0 
                    || (jumpFreeBlockAboveCount >= 1))) {
            // jumping one block
            // Make sure we can jump
            if (this.debugLog) {
                Bukkit.getServer().getLogger().info("    ok for y: " + (y - 1));
            }
            newNode = this.getOrCreateNode(parent, x, y - 1, z);
        } else if (GPSMap.canWalkOn(materials[2], materials[3], materials[4])
                    && ((jumpFreeBlockAboveCount >= 2 && GPSMap.canPassThru(materials[5]))
                            || (jumpFreeBlockAboveCount >= 1 && jumpLength <= 1))) {
            // can fall down one block
            // Make sure we can jump if <jumpLength> is greater than 1 (otherwise, we can just fall)
            if (this.debugLog) {
                Bukkit.getServer().getLogger().info("    ok for y: " + (y + 1));
            }
            newNode = this.getOrCreateNode(parent, x, y + 1, z);
        } else if (GPSMap.canWalkOn(materials[3], materials[4], materials[5])
                    && ((jumpLength == 0 && parentFreeBlockAboveCount >= 2)
                        || (jumpLength >= 1
                            && parentFreeBlockAboveCount >= 1
                            && jumpFreeBlockAboveCount >= 2
                            && GPSMap.canPassThru(materials[6])))) {
            // can fall down 2 blocks
            // make sure we can 
            if (this.debugLog) {
                Bukkit.getServer().getLogger().info("    ok for y: " + (y + 2));
            }
            newNode = this.getOrCreateNode(parent, x, y + 2, z);
        }
        if (newNode != null) {
            list.add(newNode);
            return true;
        } else {
            return false;
        }
    }
    
    /*
     * Count the number of block that the player can pass thru, above its head
     * return 0, 1 or 2, not more
     */
    protected int countPassThruBlockAboveHead(int x, int y, int z)
    {
        int result = 0;
        
        if (canPassThru(this.world, x, y + 2, z)) {
            result = 1;
            if (canPassThru(this.world, x, y + 3, z)) {
                result = 2;
            }
        }
        return result;
    }
    
    protected void tryNextBlockInDirection(GPSMapNode node, int x, int y, int z, int freeBlockAboveCount, int xDirection, int zDirection, List<GPSMapNode> childNodes)
    {
        if (this.addGPSNode(node, x + xDirection, y, z + zDirection, freeBlockAboveCount, freeBlockAboveCount, 0, childNodes)) {
            // we can arrive from the next block
        } else if (canPassThru(this.world, x + xDirection, y, z + zDirection)
                    && canPassThru(this.world, x + xDirection, y + 1, z + zDirection)) {
            // if we can't arrive from the next block, maybe we can arrive from one block away
            // we don't really care about the ceiling of the arrival block
            // when the player has to jump above one blockat least  
            int jumpFreeBlockAboveCount = this.countPassThruBlockAboveHead(x + xDirection, y, z + zDirection);
            if (this.addGPSNode(node, x + xDirection * 2, y, z + zDirection * 2, freeBlockAboveCount, jumpFreeBlockAboveCount, 1, childNodes)) {
                // so we can arrive from 2 blocks away
            } else if (canPassThru(this.world, x + xDirection * 2, y, z + zDirection * 2)
                        && canPassThru(this.world, x + xDirection * 2, y + 1, z + zDirection * 2)) {
                // if we can't arrive from 2 blocks away, let's 3 blocks away
                jumpFreeBlockAboveCount = Math.min(jumpFreeBlockAboveCount, this.countPassThruBlockAboveHead(x + xDirection * 2, y, z + zDirection * 2));
                this.addGPSNode(node, x + xDirection * 3, y, z + zDirection * 3, freeBlockAboveCount, jumpFreeBlockAboveCount, 2, childNodes);
            }
        }
    }
    
    /*
     *  Get the list of nodes that a player can go to from a <node>
     *  
     *  If the list has been already computed, otherwise, we test all positions arround
     *  
     *  Let's all blocks around the current position (with going up, going down,
     *  jumping one block, jumping 2 blocks)
     */
    protected List<GPSMapNode> childNodes(GPSMapNode node)
    {
        List<GPSMapNode> childNodes;
        
        childNodes = node.getNextNodes();
        if (childNodes == null) {
            childNodes = new LinkedList<GPSMapNode>();
            int x = node.getX();
            int y = node.getY();
            int z = node.getZ();
            int passThruBlockAboveHeadCount = this.countPassThruBlockAboveHead(x, y, z);
            
            // let's try horizontally on all fourth direction
            this.tryNextBlockInDirection(node, x, y, z, passThruBlockAboveHeadCount, 1, 0, childNodes);
            this.tryNextBlockInDirection(node, x, y, z, passThruBlockAboveHeadCount, -1, 0, childNodes);
            this.tryNextBlockInDirection(node, x, y, z, passThruBlockAboveHeadCount, 0, 1, childNodes);
            this.tryNextBlockInDirection(node, x, y, z, passThruBlockAboveHeadCount, 0, -1, childNodes);
            
            // let's try to arrive to <node> vertically
            if (this.debugLog) {
                Bukkit.getServer().getLogger().info("test verical");
            }
            if (canWalkOn(this.world, x, y - 1, z)) {
                // this means we can climb up a ladder to arrive to node 
                childNodes.add(this.getOrCreateNode(node, x, y - 1, z));
                if (this.debugLog) {
                    Bukkit.getServer().getLogger().info("    ok for y: " + (y - 1));
                }
            }
            if (canWalkOn(this.world, x, y + 1, z)) {
                // we can fall down one block from a ladder to arrive to node 
                childNodes.add(this.getOrCreateNode(node, x, y + 1, z));
            } else if (canWalkOn(this.world, x, y + 2, z)) {
                // we can fall down two blocks from a ladder to arrive to node 
                childNodes.add(this.getOrCreateNode(node, x, y + 2, z));
            }
            node.setNextNodes(childNodes);
        } else if (this.debugLog) {
            Bukkit.getServer().getLogger().info("already computed for x: " + node.getX() + " y: " + node.getY() + " z: " + node.getZ() + " children: " + childNodes.size());
        }
        return childNodes;
    }

    protected Location getLocation(GPSMapNode node)
    {
        return new Location(this.world, node.getX() + 0.5, node.getY(), node.getZ() + 0.5);
    }
}
