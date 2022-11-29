<h2> Chord</h2>
<br>
<br>
<h3>Complie</h3>
<br>
Open the terminal and go into the Chord directory, use 'make' command to complie all .java files in /bin directory.
<br>
Now you should have a /bin directory contains all .class files.
<br>
<br>
<br>
<h3>Run</h3>
1. For running Chord, there are two types of command, one is to create a new chord ring and another is to join an existing chord ring. 
<br>
<br>
  - Create a new chord ring [java Chord (port number)]
		
	java Chord 1234
  
The command will create a chord ring and a node based on the device's ip address and the port number you want the node to listen to.
<br>
<br>
  - Join an existing chord ring [java Chord (port number) (target ip address) (target port number)]
		
		java Chord 2 172.31.173.206 1234
  
The command create a node based on the device's ip address and the port number 2 and joining the ring through 172.31.173.206:1234
<br>
<br>
2. After a node (a java program) connects to the ring, we can use 'table' command to check the finger table of the node and the 'quit' command to leave the ring.

<br>
<br>
<br>
3. For querying, first try to connect a node in the chord ring

	java Query 172.31.173.206 1234
  
If we successfully connected to the node, we can start to type the search key and find out where the key should store in.
<br>
Use 'quit' command to quit the query
