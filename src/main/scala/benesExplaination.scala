/*

Sample Indexing Diagram with 4 PES example

Table:
 O   --> switch (regular)
 $	 --> input switch
 #   --> output switch

----------------------------------------------------------------------------------------------- 
Data IO Diagram: 

r_data_bus_ff[Element 0] -->  $     O     0

----------------------------------------------------------------------------------------------- 
Horizontal Internal Wires Diagram: (between each switch) 

$ w_internal[0]  w_internal[2]

----------------------------------------------------------------------------------------------- 
Diagonal Internal Wires Diagram: (between each switch)

$ w_internal[1]  

$ w_internal[9] 

----------------------------------------------------------------------------------------------- 
Mux Select Signals Diagram (inputs to each switch)
	* input switch does not require any control signals --> value will go to both horizontal and diagonal
	* output switch only requires one control bit

NA  r_mux_bus_ff[0,1]      r_mux_bus_ff[2,3]      r_mux_bus_ff[4,5]     r_mux_bus_ff[24] 
NA  r_mux_bus_ff[6,7]      r_mux_bus_ff[8,9]      r_mux_bus_ff[10,11]   r_mux_bus_ff[25]
NA  r_mux_bus_ff[12,13]    r_mux_bus_ff[14,15]    r_mux_bus_ff[16,17]	r_mux_bus_ff[26]
NA  r_mux_bus_ff[18,19]	   r_mux_bus_ff[20,21]	  r_mux_bus_ff[22,23]   r_mux_bus_ff[27]

----------------------------------------------------------------------------------------------- 
*/