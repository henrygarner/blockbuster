# Blockbuster

Movies in Minecraft. Taking the current obsession for 3D film to its
illogical conclusion.

## Setup

To run Blockbuster you will need to have either a Raspberry Pi running
Minecraft: Pi Edition or a CraftBukkit server with the RaspberryJuice
plugin.


Comprehensive instructions for setting up a Craftbukkit server with
RaspberryJuice are
[available here](http://blog.lostbearlabs.com/2013/04/25/using-the-minecraft-api-without-a-raspberry-pi-craftbukkit-and-raspberryjuice/).

## Usage

See
[cli-options](https://github.com/henrygarner/blockbuster/blob/master/src/blockbuster/core.clj#L121)
for a complete list of arguments.

After compiling blockbuster (`lein compile`), the following command
will render the simpsons.mp4 on a screen 50 voxels wide at coordinates
0,5,0.

```sh
bin/blockbuster -f simpsons.mp4 -p 0,5,0 -w 50 -c
```

## See Also

[Redstone](https://github.com/henrygarner/redstone) is the library
Blockbuster uses to send instructions to Minecraft from Clojure.

## License

Copyright © 2014 Henry Garner

Distributed under the Eclipse Public License either version 1.0.
