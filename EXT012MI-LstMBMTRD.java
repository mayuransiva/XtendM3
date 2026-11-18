/**
 * README
 * This API is developed to retrieve the MBMTRD table for the specif input values
 *
 * Name: EXT012MI.LstMBMTRD
 * Description: Get MBMTRD information.
 * Date	      Changed By                   Description
 * 2021	  SwatiY@fortude.co        Retrieve values from MBMTRD table for the specif input values
 */

public class LstMBMTRD extends ExtendM3Transaction {
  private final MIAPI mi

  private final ProgramAPI program
  private final DatabaseAPI database
  private final LoggerAPI logger

  // declare String 
  public String iCONO
  public String iIDTR

  public LstMBMTRD(MIAPI mi, ProgramAPI program, DatabaseAPI database, LoggerAPI logger) {
    this.mi = mi
    this.program = program
    this.database = database
    this.logger = logger
  }

  public void main() {
    // validation for mandatory fields
    if (mi.inData.get("CONO").toString() == '') {
      mi.error("Company number is Mandatory field")
      return
    }
    if (mi.inData.get("IDTR").toString() == '') {
      mi.error("IDTR Iis Mandatory field")
      return
    }

    iCONO = mi.inData.get("CONO").toString()
    iIDTR = mi.inData.get("IDTR").toString()

    // Query on MBMTRD with IDTR, CONO Input 
    DBAction query = database.table("MBMTRD").index("40").selection("TDCONO", "TDIDTR", "TDMVXD", "TDMBMD").build()
    DBContainer container = query.createContainer()
    if (iIDTR.trim() == '0') {
      container.set("TDIDTR", 0)
    } else {
      container.set("TDIDTR", Integer.valueOf(iIDTR.trim()))
    }
    container.set("TDCONO", Integer.valueOf(iCONO))
    query.readAll(container, 2, listTranslation)

  }

  /**
   *Closuer block for retrieiving records & send it to MI outout
   * @params 
   * @return 
   */

  Closure < ? > listTranslation = {
    DBContainer record ->
    mi.outData.put("CONO", String.valueOf(record.get("TDCONO")))
    mi.outData.put("IDTR", String.valueOf(record.get("TDIDTR")))
    mi.outData.put("MVXD", String.valueOf(record.get("TDMVXD")))
    mi.outData.put("MBMD", String.valueOf(record.get("TDMBMD")))
    // MI write
    mi.write()
  }
}