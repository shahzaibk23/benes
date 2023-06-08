NUM_PES=8
LEVELS=7

Switches = 0
for i in range(NUM_PES):
    print(f"i:{i}")
    for j in range(1,LEVELS-1):
        Switches += 1
        
        print(f"ij:{i}{j}")
        print(f"SWITCH: {Switches}")
        print(f"imm_switch.in0: {(2 * i * (LEVELS - 1) + 2 * (j - 1))}")
        if j <= (LEVELS-1)/2:
            print(f"j <= (LEVELS-1)/2: {j <= (LEVELS-1)/2}")
            if i%2**j < 2**(j-1):
                print(f"i%2**j < 2**(j-1):{i%2**j < 2**(j-1)}")
                print(f"imm_switch.in1: {2*(i+2**(j-1))*(LEVELS-1)+2*(j-1)+1}")
            else:
                print(f"i%2**j < 2**(j-1):{i%2**j < 2**(j-1)}")
                print(f"imm_switch.in1: {2*(i-2**(j-1))*(LEVELS-1)+2*(j-1)+1}")
        else:
            print(f"j <= (LEVELS-1)/2: {j <= (LEVELS-1)/2}")
            if i%2**(LEVELS-j) < 2**(LEVELS-j-1):
                print(f"i%2**(LEVELS-j) < 2**(LEVELS-j-1): {i%2**(LEVELS-j) < 2**(LEVELS-j-1)}")
                print(f"imm_switch.in1: {2*(i+2**(LEVELS-j-1))*(LEVELS-1)+2*(j-1)+1}")
            else:
                print(f"i%2**(LEVELS-j) < 2**(LEVELS-j-1): {i%2**(LEVELS-j) < 2**(LEVELS-j-1)}")
                print(f"imm_switch.in1: {2*(i-2**(LEVELS-j-1))*(LEVELS-1)+2*(j-1)+1}")

        print(f"imm_switch.sel0: {2 * (LEVELS - 2) * i + 2 * (j - 1)}")
        print(f"imm_switch.sel1: {2 * (LEVELS - 2) * i + 2 * (j - 1) + 1}")

        print(f"imm_switch.y > {2 * i * (LEVELS - 1) + 2 * j}")
        print(f"imm_switch.z > {2 * i * (LEVELS - 1) + 2 * j + 1}")
        
        print(f"{2*i*(LEVELS-1)+2*j}")
        print(f"{2*i*(LEVELS-1)+2*j+1}")
            

# for i in range(NUM_PES):
#     print(f"{2*i*(LEVELS-1)}")
#     print(f"{2*i*(LEVELS-1)+1}")