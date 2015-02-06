## Description

This project is a test to implement [A* algorithm](http://en.wikipedia.org/wiki/A*_search_algorithm) using Bukkit in minecraft. The direction to take will be shown with the compass. Once the player reach the destination, the compass will be set back to its previous target. The path is recomputed every 20 blocks.

The algorithm take care of ladders, avoid lava and water. The distance jump limit is 2 blocks (and the algorithm checks if the ceiling is high enough to jump). The fall down limit is 2 blocks down.

## Known Bugs

The path doesn't try to go in diagonal.

## Commands

/gps &lt;distance&gt;

The plugin will add the distance to the current player (in x or z axes according to player direction), and the path will be computed to reach that block.

/gps &lt;nickname&gt;

The gps will compute the path to go a player.

/gps debug

The plugin will show the path some snow. The snow will be remove when going through the path. 

## Classes

**GPSPlugin**

Main class to get the plugin working in minecraft Bukkit. 

**GPSMap**

Class used to compute which blocks can be reached from a specific location. This class also stores all the GPSMapNode. This is done using GPSMapDimension.

**GPSMapDimension**

Each instance deals with a specific axe (x, y or z). The first instance stored by GPSMap is in x. All instances stored by this first instance, deal with y axe. All y instances contain instances for the z axe. All z instances contain GPSMapNode instances.

**GPSMapNode**

For each visited block, an instance of GPSMapNode is created to save all the data. 

**GPSSearch**

This class contains the A* algorithm.
