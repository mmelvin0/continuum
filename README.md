Continuum
=========

Goal
----

My goal with Continuum is to create a Bukkit plugin that:

- Provides inter- and intra- world travel via "gates"
- Is entirely driven by game mechanics (no /commands)
- Feels right in vanilla Minecraft

Design
------

A gate needs to be a 2 dimensional enclosed shape with air in the middle. Any shape other than a the standard nether gate is acceptable. The gate can be any material as long as a sign can be attached to at least one of the blocks that makes up the gate. An attached sign is required to enable departures/arrivals, but the shape retains its identify as a gate until broken.

Creating a gate works the same as creating a nether gate. Use a flint and steel to ignite the top side of the any block in the bottom row of the desired gate. Upon this action, the plugin performs a search to determine if a valid gate shape is present. For large gates, the operation will be computationally expensive, so there needs to be an upper bound from the starting position as to how large the gate can be.

Initially, gates will only be able to be created standing straight up. Eventually I would like to be able to have gates that can face up or down, as well as translate momentum through gates. Thus it is good design to ensure that gates have a "front". The front determines, at a minimum, which side to enter/exit when departing/arriving. The front will be set to the side the player is standing on when they ignite the gate with their flint and steel.

Upon recognition of a valid gate, an instance is created and stored in a database. Gates will be inoperable until a sign is attached. Line 1 determines the gate's name and line 2 specifies the destination of the gate. Either line can be blank. If the gate has no name, you can't connect to it from another gate. If the gate has no destination, it will pick a random one!

Once a sign is attached, further actions are possible by supplying redstone current to the gate.

The gate will be controllable by redstone "pulses". The most basic and essential pulse will be the longest pulse, and will activate the gate, connecting it to its destination.

Later on, combinations of pulses with shorter duration than the "activation" pulse will allow you to cycle forward and backwards through possible destinations.
