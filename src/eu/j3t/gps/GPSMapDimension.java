package eu.j3t.gps;

/*
 * This class can store elements in one dimension
 * The goal is to create avoid a huge array with 3 dimensions
 * 
 * The first instance of GPSMapDimension is for the x coordinate.
 * It contains instances of GPSMapDimension for the y coordinate
 * Which contains instances of GPSMapDimension for the z coordinate
 * Which contains GPSNode instances
 * 
 */
public class GPSMapDimension implements GPSMapElement {
    private GPSMapElement[] elements;
    private int offset;
    
    public GPSMapDimension()
    {
    }
    
    private void increaseSizeBefore(int elementCountBefore, int increaseBy)
    {
        assert increaseBy >= elementCountBefore;
        GPSMapElement[] newElements = new GPSMapElement[elements.length + increaseBy];
        
        System.arraycopy(elements, 0, newElements, elements.length, elementCountBefore);
        elements = newElements;
        offset -= elementCountBefore;
    }

    private void addElement(GPSMapElement element, int[] position, int dimension)
    {
        if (elements == null) {
            elements = new GPSMapElement[11];
            offset = position[dimension] - 11 / 2;
        }
        while (true) {
            if (position[dimension] < offset) {
                this.increaseSizeBefore(elements.length, elements.length);
            } else if (position[dimension] >= offset + elements.length) {
                this.increaseSizeBefore(0, elements.length);
            } else {
                break;
            }
        }
        if (dimension == 0) {
            this.elements[position[dimension] - offset] = element;
        } else {
            if (this.elements[position[dimension] - offset] == null) {
                this.elements[position[dimension] - offset] = new GPSMapDimension();
            }
            ((GPSMapDimension)this.elements[position[dimension] - offset]).addElement(element, position, dimension - 1);
        }
    }

    protected void addElement(GPSMapElement element, int[] position)
    {
        assert position.length == 3;
        this.addElement(element, position, 2);
    }

    private GPSNode getElement(int[] position, int dimension)
    {
        if (this.elements == null || position[dimension] < offset || position[dimension] >= offset + this.elements.length) {
            return null;
        } else if (dimension == 0) {
            return (GPSNode)this.elements[position[dimension] - offset];
        } else if (this.elements[position[dimension] - offset] == null) {
            return null;
        } else {
            return ((GPSMapDimension)this.elements[position[dimension] - offset]).getElement(position, dimension - 1);
        }
    }

    protected GPSNode getElement(int[] position)
    {
        assert position.length == 3;
        return this.getElement(position, 2);
    }
}
