/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.soc.rv32im;

import java.math.BigInteger;

import com.cburch.logisim.soc.file.ElfHeader;

public class RV32im_M_ExtensionInstructions implements RV32imExecutionUnitInterface {

  private static final int OP = 0x33;
  
  private static final int INSTR_MUL = 0;
  private static final int INSTR_MULH = 1;
  private static final int INSTR_MULHSU = 2;
  private static final int INSTR_MULHU = 3;
  private static final int INSTR_DIV = 4;
  private static final int INSTR_DIVU = 5;
  private static final int INSTR_REM = 6;
  private static final int INSTR_REMU = 7;
  
  private static final String[] AsmOpcodes = {"MUL","MULH","MULHSU","MULHU","DIV","DIVU","REM","REMU"};

  private int instruction;
  private boolean valid;
  private int operation;
  private int destination;
  private int source1;
  private int source2;
  
  public boolean execute(RV32im_state state) {
    if (!valid) return false;
    int val1 = state.getRegisterValue(source1);
    int val2 = state.getRegisterValue(source2);
    BigInteger opp1,opp2,res;
    BigInteger mask = BigInteger.valueOf(1).shiftLeft(32).subtract(BigInteger.valueOf(1));
    int result = 0;
    switch (operation) {
      case INSTR_MULH   : 
      case INSTR_MUL    : opp1 = BigInteger.valueOf(val1); 
                          opp2 = BigInteger.valueOf(val2);
                          res = opp1.multiply(opp2);
                          result = (operation == INSTR_MUL) ? res.and(mask).intValue() : res.shiftRight(32).and(mask).intValue();
                          break;
      case INSTR_MULHSU : opp1 = BigInteger.valueOf(val1);
                          opp2 = BigInteger.valueOf(ElfHeader.getLongValue((Integer)val2));
                          res = opp1.multiply(opp2);
                          result = res.shiftRight(32).and(mask).intValue();
                          break;
      case INSTR_MULHU  : opp1 = BigInteger.valueOf(ElfHeader.getLongValue((Integer)val1)); 
                          opp2 = BigInteger.valueOf(ElfHeader.getLongValue((Integer)val2));
                          res = opp1.multiply(opp2);
                          result = res.shiftRight(32).and(mask).intValue();
                          break;
      case INSTR_DIV    :
      case INSTR_REM    : opp1 = BigInteger.valueOf(val1);
                          opp2 = BigInteger.valueOf(val2);
                          res = (operation == INSTR_REM) ? opp1.remainder(opp2) : opp1.divide(opp2);
                          result = res.and(mask).intValue();
                          break;
      case INSTR_DIVU   :
      case INSTR_REMU   : opp1 = BigInteger.valueOf(ElfHeader.getLongValue((Integer)val1));
                          opp2 = BigInteger.valueOf(ElfHeader.getLongValue((Integer)val2));
                          res = (operation == INSTR_REMU) ? opp1.remainder(opp2) : opp1.divide(opp2);
                          result = res.and(mask).intValue();
                          break;
    }
    state.writeRegister(destination, result);
    return true;
  }

  public String getAsmInstruction() {
    if (!valid) return null;
    StringBuffer s = new StringBuffer();
    s.append(AsmOpcodes[operation].toLowerCase());
    while (s.length()<RV32imSupport.ASM_FIELD_SIZE)
      s.append(" ");
    s.append(RV32im_state.registerABINames[destination]+","+RV32im_state.registerABINames[source1]+","+
            RV32im_state.registerABINames[source2]);
    return s.toString();
  }

  public int getBinInstruction() {
    return instruction;
  }

  public boolean setAsmInstruction(String instr) {
    valid = false;
    return valid;
  }

  public boolean setBinInstruction(int instr) {
    instruction = instr;
    valid = decodeBin();
    return valid;
  }

  public boolean performedJump() { return false; }

  public boolean isValid() { return valid; }
  
  private boolean decodeBin() {
    if (RV32imSupport.getOpcode(instruction)==OP) {
      if (RV32imSupport.getFunct7(instruction) != 1)
        return false;
      operation = RV32imSupport.getFunct3(instruction);
      destination = RV32imSupport.getDestinationRegisterIndex(instruction);
      source1 = RV32imSupport.getSourceRegister1Index(instruction);
      source2 = RV32imSupport.getSourceRegister2Index(instruction);
      return true;
    }
    return false;
  }

  public String getErrorMessage() { return null; }

}
