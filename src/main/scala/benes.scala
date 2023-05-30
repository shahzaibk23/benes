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

  for (i <- 0 until NUM_PES) { // -- 8
    val in_switch = Module(new InputSwitch(DATA_TYPE))
    in_switch.io.in := r_data_bus_ff(i)
    w_internal(2 * i * (LEVELS - 1)) := in_switch.io.y      // 0-0 | 1-12 | 2-24 | 3-36 | 4-48 | 5-60 | 6-72 | 7-84 (col-1 -> horizontal)
    w_internal(2 * i * (LEVELS - 1) + 1) := in_switch.io.z  // 0-1 | 1-13 | 2-25 | 3-37 | 4-49 | 5-61 | 6-73 | 7-85 (col-1 -> diagonal)
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

  for (i <- 0 until NUM_PES) {              // i=0-7
    for (j <- 1 until (LEVELS - 1)) {       // j=1-4
      val imm_switch = Module(new Switch(DATA_TYPE))
      imm_switch.io.in0 := w_internal(2 * i * (LEVELS - 1) + 2 * (j - 1)) // 01-0 | 02-2 | 03-4 | 04-6 | 11-12 | 12-14 | 13-16 | 14-18 | 21-24 | 22-26 | 23-28 | 24-30
      if (j <= (LEVELS - 1) / 2) { // 01-1<3 yes | 02-2<3 yes | 03-yes | 11-yes | 12-yes | 13-yes | 21-yes | 22-yes | 23-yes
        if (i % math.pow(2, j).toInt < math.pow(2, j - 1).toInt) { // 01-0<1 yes | 02- 0<2 yes | 12-yes | 13-yes | 21-yes | 23-yes
          imm_switch.io.in1 := w_internal(2 * (i + math.pow(2, j - 1).toInt) * (LEVELS - 1) + 2 * (j - 1) + 1) // 01-13 | 02-27 | 03-53 | 12-39 | 13-65 | 21-37 | 23-77
        } else { // 11-yes | 22-yes
          imm_switch.io.in1 := w_internal(2 * (i - math.pow(2, j - 1).toInt) * (LEVELS - 1) + 2 * (j - 1) + 1) // 11-1 | 22-3
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
      w_internal(2 * i * (LEVELS - 1) + 2 * j) := imm_switch.io.y       // 01-2 | 02-4 | 03-6 | 04-8 | 11-14 | 12-16 | 13-18 | 14-20 | 21-26 | 22-28 | 23-30 | 24-32
      w_internal(2 * i * (LEVELS - 1) + 2 * j + 1) := imm_switch.io.z   // 01-3 | 02-5 | 03-7 | 04-9 | 11-15 | 12-17 | 13-19 | 14-21 | 21-27 | 22-29 | 23-31 | 24-33
    }
  }
}
