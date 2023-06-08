import chisel3._
import chisel3.util._

class Benes(DATA_TYPE: Int, NUM_PES: Int, LEVELS: Int) extends Module {
  val io = IO(new Bundle {
    // val clk = Input(Bool())
    // val rst = Input(Bool())
    val i_data_bus = Input(Vec(NUM_PES, UInt(DATA_TYPE.W)))
    val i_mux_bus = Input(Vec(2 * (LEVELS - 2) * NUM_PES + NUM_PES, Bool()))
    val o_dist_bus = Output(Vec(NUM_PES, UInt(DATA_TYPE.W)))
  })

  val clk = clock
  val rst = reset.asBool

  val r_data_bus_ff = RegInit(VecInit(Seq.fill(NUM_PES)(0.U(DATA_TYPE.W))))
  val r_mux_bus_ff = RegInit(VecInit(Seq.fill(2 * (LEVELS - 2) * NUM_PES + NUM_PES)(false.B)))
  val w_dist_bus = Wire(Vec(NUM_PES, UInt(DATA_TYPE.W)))
  val w_internal = Wire(Vec(2 * NUM_PES * (LEVELS - 1), UInt(DATA_TYPE.W))) // 8 x 2 = 16

  dontTouch(w_internal)

  when(rst) {
    r_data_bus_ff := VecInit(Seq.fill(NUM_PES)(0.U(DATA_TYPE.W)))
  } .otherwise {
    r_data_bus_ff := io.i_data_bus
  }

  withClock(clk) {
    when(rst) {
      r_mux_bus_ff := VecInit(Seq.fill(2 * (LEVELS - 2) * NUM_PES + NUM_PES)(false.B))
      io.o_dist_bus := VecInit(Seq.fill(NUM_PES)(0.U(DATA_TYPE.W)))
    } .otherwise {
      r_mux_bus_ff := io.i_mux_bus
      io.o_dist_bus := w_dist_bus
    }
  }

  for (i <- 0 until NUM_PES) { // -- 4
    val in_switch = Module(new InputSwitch(DATA_TYPE))
    in_switch.io.in := r_data_bus_ff(i)
    w_internal(2 * i * (LEVELS - 1)) := in_switch.io.y      // 0-0 | 1-8 | 2-16 | 3-24 (col-1 -> horizontal)
    w_internal(2 * i * (LEVELS - 1) + 1) := in_switch.io.z  // 0-1 | 1-9 | 2-17 | 3-25 (col-1 -> diagonal)
  }

  for (i <- 0 until NUM_PES) { // 8
    val out_switch = Module(new OutputSwitch(DATA_TYPE))
    out_switch.io.in0 := w_internal(2 * i * (LEVELS - 1) + (2 * (LEVELS - 2))) // 0-10 | 1-22 | 2-34 | 3-46 | 4-58 | 5-70 | 6-82 | 7-94 (col-6 --> diagonal)
    if (i % 2 == 0) {
      out_switch.io.in1 := w_internal(2 * (i + 1) * (LEVELS - 1) + (2 * (LEVELS - 2)) + 1) // 0-22 | 2-46 | 4-70 | 6-94
    } else {
      out_switch.io.in1 := w_internal(2 * (i - 1) * (LEVELS - 1) + (2 * (LEVELS - 2)) + 1) // 1-10 | 3-34 | 5-58 | 7-82
    }
    out_switch.io.sel := r_mux_bus_ff(2 * NUM_PES * (LEVELS - 2) + i) 
    w_dist_bus(i) := out_switch.io.y
  }

  for (i <- 0 until NUM_PES) {              // i=0-3
    for (j <- 1 until (LEVELS - 1)) {       // j=1-3
      val imm_switch = Module(new Switch(DATA_TYPE))
      imm_switch.io.in0 := w_internal(2 * i * (LEVELS - 1) + 2 * (j - 1)) // 01-0 | 02-2
      if (j <= (LEVELS - 1) / 2) { // 01-1<4 - yes | 02-2<4 yes
        if (i % math.pow(2, j).toInt < math.pow(2, j - 1).toInt) { // 01-0<1 yes | 02-0<2 yes
          imm_switch.io.in1 := w_internal(2 * (i + math.pow(2, j - 1).toInt) * (LEVELS - 1) + 2 * (j - 1) + 1) // 01-9 | 02-19
        } else { // 
          imm_switch.io.in1 := w_internal(2 * (i - math.pow(2, j - 1).toInt) * (LEVELS - 1) + 2 * (j - 1) + 1) // 
        }
      } else { // 04-yes | 14-yes | 24-yes
        if (i % math.pow(2, LEVELS - j).toInt < math.pow(2, LEVELS - j - 1).toInt) { // 04-yes | 14-yes | 24-yes
          imm_switch.io.in1 := w_internal(2 * (i + math.pow(2, LEVELS - j - 1).toInt) * (LEVELS - 1) + 2 * (j - 1) + 1) // 04-55 | 14-67 | 24-79
        } else {
          imm_switch.io.in1 := w_internal(2 * (i - math.pow(2, LEVELS - j - 1).toInt) * (LEVELS - 1) + 2 * (j - 1) + 1)
        }
      }
      imm_switch.io.sel0 := r_mux_bus_ff(2 * (LEVELS - 2) * i + 2 * (j - 1))
      imm_switch.io.sel1 := r_mux_bus_ff(2 * (LEVELS - 2) * i + 2 * (j - 1) + 1)
      w_internal(2 * i * (LEVELS - 1) + 2 * j) := imm_switch.io.y       // 01-2 
      w_internal(2 * i * (LEVELS - 1) + 2 * j + 1) := imm_switch.io.z   // 01-3 
    }
  }
}
