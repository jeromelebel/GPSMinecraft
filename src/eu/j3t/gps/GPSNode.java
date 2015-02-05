package eu.j3t.gps;

import java.util.List;

/*
 * The one GPSNode is created for each block visited
 * 
 * originDistance: distance in block need to go from the origin to this block
 * weight: distance that should be needed to reach the destination
 * parent: block where we come from to get to this block
 * nextNodes: block that the player can walk/jump to, from this block
 * 
 */

public class GPSNode implements GPSMapElement, Comparable<GPSNode> {
    private int x, y, z;
    private double originDistance;
    private double weight;
    private GPSNode parent;
    private List<GPSNode> nextNodes;
    
    protected GPSNode(GPSNode newParent, int newX, int newY, int newZ)
    {
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        this.parent = newParent;
        this.weight = Double.MAX_VALUE;
        if (this.parent == null) {
            this.originDistance = 0;
        } else {
            this.originDistance = this.parent.getOriginDistance() + 1;
        }
    }
    
    protected int getX()
    {
        return this.x;
    }
    
    protected int getY()
    {
        return this.y;
    }
    
    protected int getZ()
    {
        return this.z;
    }
    
    protected GPSNode getParent()
    {
        return this.parent;
    }
    
    protected double getWeight()
    {
        return this.weight;
    }
    
    protected void setWeight(double newWeight)
    {
        this.weight = newWeight;
    }
    
    protected double getOriginDistance()
    {
        return this.originDistance;
    }
    
    protected void setOriginDistance(double newDistance)
    {
        this.originDistance = newDistance;
    }
    
    protected List<GPSNode> getNextNodes()
    {
        return nextNodes;
    }
    
    protected void setNextNodes(List<GPSNode> nodes)
    {
        this.nextNodes = nodes;
    }
    
    protected String coordinateToString()
    {
        StringBuilder builder = new StringBuilder();
        
        builder.append("x: ");
        builder.append(x);
        builder.append(", y: ");
        builder.append(y);
        builder.append(", z: ");
        builder.append(z);
        return builder.toString();
    }
    
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        
        builder.append("Node: ");
        builder.append(this.coordinateToString());
        builder.append(", distance: ");
        builder.append(this.originDistance);
        builder.append(", weight: ");
        builder.append(this.weight);
        if (this.parent != null) {
            builder.append(", parent from: ");
            builder.append(this.parent.coordinateToString());
        } else {
            builder.append(", has no parent");
        }
        return builder.toString();
    }
    
    @Override
    public int compareTo(GPSNode other)
    {
        double difference = this.getOriginDistance() + this.getWeight() - (other.getOriginDistance() + other.getWeight());
        
        if (difference == 0) {
            return 0;
        } else if (difference > 0) {
            return 1;
        } else {
            return -1;
        }
    }
}
