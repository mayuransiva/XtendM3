/**
 * README
 * This extension is being used to update planning node in OCUSAD  
 *
 * Name: EXT003MI.UpdAddress
 * Description: Get inventory source.
 * Date	      Changed By                   Description
 * 2021	  SwatiY@fortude.co         update planning node in OCUSAD  
 */

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

public class UpdAddress extends ExtendM3Transaction {
  private final MIAPI mi
  private final ProgramAPI program
  private final DatabaseAPI database
  public String iADID
  public String iCUNO
  public String iPNOD
  public Integer iCONO
  boolean error
  public int iCHNO

  public UpdAddress(MIAPI mi, ProgramAPI program, DatabaseAPI database) {
    this.mi = mi
    this.program = program
    this.database = database
  }

  public void main() {

    // Get Inputs from the API
    iCONO = mi.inData.get("CONO").toInteger()
    iCUNO = mi.inData.get("CUNO").toString()
    iADID = mi.inData.get("ADID").toString()
    iPNOD = mi.inData.get("PNOD").toString()

    //validate Customer and Address
    if ((!iCUNO.isBlank() && !iCUNO.contains("?")) && (!iADID.isBlank() && !iADID.contains("?"))) {
      Optional < DBContainer > getADID = validateADID()
      if (getADID.isPresent()) {
        DBContainer record1 = getADID.get()
        //validate PlanningNode
        if (!iPNOD.trim().isBlank() && !iPNOD.contains("?")) {
          Optional < DBContainer > checkPNOD = validatePNOD()
          if (checkPNOD.isPresent()) {
            DBContainer record2 = checkPNOD.get()
          } else {
            mi.error("Invalid Planning Node")
            error = true
            return
          }
        }
      } else {
        mi.error("Invalid Address ID")
        error = true
        return
      }
    } else {
      mi.error("ERROR")
      return
    }
    //update PlanningNode
    if (!error) {
      updatePlanningNode()
    }

  }
  // Validate ADID
  Optional < DBContainer > validateADID() {
    DBAction query1 = database.table("OCUSAD").index("00").selection("OPCONO", "OPCUNO", "OPADRT", "OPADID", "OPCHNO").build()
    DBContainer container1 = query1.createContainer()
    container1.set("OPCONO", iCONO)
    container1.set("OPCUNO", iCUNO)
    container1.set("OPADRT", Integer.valueOf("1"))
    container1.set("OPADID", iADID)
    boolean found1 = query1.read(container1)
    if (found1) {
      iCHNO = container1.get("OPCHNO")
      return Optional.of(container1)
    }
    return Optional.empty()
  }

  // Validate PNOD
  Optional < DBContainer > validatePNOD() {
    DBAction query2 = database.table("CPNMAS").index("00").selection("PNCONO", "PNDIVI", "PNPNOD").build()
    DBContainer container2 = query2.createContainer()
    container2.set("PNCONO", iCONO)
    container2.set("PNDIVI", "")
    container2.set("PNPNOD", iPNOD)
    if (query2.read(container2)) {
      return Optional.of(container2)
    }
    return Optional.empty()
  }

  //Update PNOD 
  public updatePlanningNode() {
    DBAction planningNode = database.table("OCUSAD").index("00").build()
    DBContainer containerOCUSAD = planningNode.createContainer()
    containerOCUSAD.set("OPCONO", iCONO)
    containerOCUSAD.set("OPCUNO", iCUNO)
    containerOCUSAD.set("OPADRT", Integer.valueOf("1"))
    containerOCUSAD.set("OPADID", iADID)

    planningNode.readLock(containerOCUSAD, updateCallBack)

  }
  Closure < ? > updateCallBack = {
    LockedResult record ->
      //set PNOD "" if blank
      if (iPNOD.contains("?")) {
        iPNOD = ""
      }

      iCHNO++
      record.set("OPPNOD", iPNOD)
      record.set("OPCHNO", iCHNO)
      record.set("OPLMDT", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger())
      record.set("OPCHID", program.getUser())

      record.update()

  }

}