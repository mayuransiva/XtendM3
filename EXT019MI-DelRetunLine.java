/**
 * README
 * This extension is being used to delete the return order line
 *
 * Name: EXT019MI.DelReturnLine
 * Description: Customer requested a functionality to modify return order lines (change qtys / remove lines) via the SDK. 
 *              Since there is no API for such functionality a new API is required to delete the return order lines. 
 *              In case they the users need to make a change to an existing line they need to remove the line and add again.
 * Date	          Changed By                      Description
 *2023-06-27    mayuransi@fortude.co           Delete the return order line
 *
 */

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

public class DelReturnLine extends ExtendM3Transaction {
  private final MIAPI mi
  private final DatabaseAPI database
  private final ProgramAPI program
  private final MICallerAPI miCaller
  private final LoggerAPI logger

  private String iWHLO, iE0PA, iFRE1
  private int iCONO, iREPN, iRELI

  public DelReturnLine(MIAPI mi, DatabaseAPI database, ProgramAPI program, MICallerAPI miCaller, LoggerAPI logger) {
    this.mi = mi
    this.database = database
    this.program = program
    this.miCaller = miCaller
    this.logger = logger
  }

  /**
   ** Main function
   * @param
   * @return
   */
  public void main() {
    iCONO = (mi.inData.get("CONO") == null || mi.inData.get("CONO").trim().isEmpty()) ? program.LDAZD.CONO as Integer : mi.inData.get("CONO") as Integer
    iWHLO = (mi.inData.get("WHLO") == null || mi.inData.get("WHLO").trim().isEmpty()) ? "" : mi.inData.get("WHLO")
    iREPN = (mi.inData.get("REPN") == null || mi.inData.get("REPN").trim().isEmpty()) ? 0 : mi.inData.get("REPN") as Integer
    iRELI = (mi.inData.get("RELI") == null || mi.inData.get("RELI").trim().isEmpty()) ? 0 : mi.inData.get("RELI") as Integer
    iE0PA = (mi.inData.get("E0PA") == null || mi.inData.get("E0PA").trim().isEmpty()) ? "" : mi.inData.get("E0PA")
    iFRE1 = (mi.inData.get("FRE1") == null || mi.inData.get("FRE1").trim().isEmpty()) ? "" : mi.inData.get("FRE1")

    boolean validInput = validateInput()
    if (validInput) {
      deleteRetrunLine()
      updateMHEXRD()
      mi.outData.put("STAT", "Retrun number: " + iREPN + ", Line number: " + iRELI + " has been Sucesfully Deleted")
    }

  }

  /**
   *Validate inputs and check the return line number existence in m3
   * @params 
   * @return 
   */
  private boolean validateInput() {
    //Validate Company Number
    def params = ["CONO": iCONO.toString().trim()]
    def callback = {
      Map < String,
      String > response ->
      if (response.CONO == null) {
        mi.error("Invalid Company Number " + iCONO)
        return false
      }

    }
    miCaller.call("MNS095MI", "Get", params, callback)

    //Validate return Order Number
    params = ["WHLO": iWHLO.toString().trim(), "REPN": iREPN.toString().trim(), "RELI": iRELI.toString().trim()]
    def callback2 = {
      Map < String,
      String > response ->

      if (response.RELI == null) {
        mi.error("Invalid Return Line " + iRELI)
        return false
      }
      if (Integer.parseInt(response.REST) > 11) {
        mi.error("Return line " + iRELI + " status " + response.REST + " invalid ")
        return false
      }

    }

    miCaller.call("OIS390MI", "GetLine", params, callback2)
    return true

  }

  //
  /**
   *Delete the retun line from OCLINE
   * @params 
   * @return 
   */
  public deleteRetrunLine() {
    DBAction query = database.table("OCLINE").index("00").build()
    DBContainer container = query.getContainer()
    container.set("ODCONO", iCONO)
    container.set("ODWHLO", iWHLO)
    container.set("ODREPN", iREPN)
    container.set("ODRELI", iRELI)

    if (!query.readLock(container, deleteCallBack)) {
      mi.error("Record does not Exist.");
      return;
    }

  }

  Closure < ? > deleteCallBack = {
    LockedResult lockedResult ->

    lockedResult.delete()

  }

  /**
   *Update MHEXRD table
   * @params 
   * @return 
   */
  public updateMHEXRD() {
    DBAction query = database.table("MHEXRD").index("41").build()
    DBContainer container = query.getContainer()
    container.set("H2CONO", iCONO)
    container.set("H2E0PA", iE0PA)
    container.set("H2WHLO", iWHLO)
    container.set("H2REPN", iREPN)
    container.set("H2RELI", iRELI)
    container.set("H2TTYP", 30)
    container.set("H2RIDN", iFRE1)
    
     if (!query.readLock(container, updateCallBack)) {
      mi.error("Record does not Exist in MHEXRD");
      return;
    }
    
  }

  Closure < ? > updateCallBack = {
    LockedResult lockedResult ->

    lockedResult.set("H2CMND", "*DLT")
    lockedResult.set("H2DOWN", 0)
    lockedResult.set("H2PRBE", 0)
    lockedResult.set("H2TRQT", 0)
    lockedResult.set("H2STAT", "99")

    lockedResult.set("H2LMDT", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger())
    lockedResult.set("H2LMTS", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")).toInteger())
    lockedResult.set("H2CHID", program.getUser())
    lockedResult.set("H2CHNO", lockedResult.getInt("H2CHNO") + 1)
    lockedResult.update()
  }

}