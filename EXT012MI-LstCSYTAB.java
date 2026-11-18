/**
 * README
 * This API is developed to retrieve the CSYTAB table for the specif input values
 *
 * Name: EXT012MI.LstCSYTAB
 * Description: Get CSYTAB information.
 * Date	      Changed By                   Description
 * 2021	  SwatiY@fortude.co        CSYTAB table for the specif input values
 */

public class LstCSYTAB extends ExtendM3Transaction {
  private final MIAPI mi
  private final ProgramAPI program
  private final DatabaseAPI database
  private final LoggerAPI logger

  // declare String 
  public String iCONO
  public String iDIVI
  public String iSTCO
  public String iFSTKY
  public String iTSTKY

  public LstCSYTAB(MIAPI mi, ProgramAPI program, DatabaseAPI database, LoggerAPI logger) {
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
    if (mi.inData.get("STCO").toString() == '') {
      mi.error("STCO Iis Mandatory field")
      return
    }
    iCONO = mi.inData.get("CONO").toString()
    iDIVI = mi.inData.get("DIVI").toString()
    iSTCO = mi.inData.get("STCO").toString()
    iFSTKY = mi.inData.get("FSKY").toString()
    iTSTKY = mi.inData.get("TSKY").toString()

    // Query on CSYTAB with Selection 
    ExpressionFactory expression = database.getExpressionFactory("CSYTAB")
    // to value blank
    if (iFSTKY.trim() != '' && iTSTKY.trim().isBlank()) {
      expression = expression.ge("CTSTKY", iFSTKY.trim())
    }
    // from value blank 
    if (iTSTKY.trim() != '' && iFSTKY.trim().isBlank()) {
      expression = expression.le("CTSTKY", iTSTKY.trim())
    }
    // from & to not blank
    if (iFSTKY.trim() != '' && iTSTKY.trim() != '') {
      expression = expression.ge("CTSTKY", iFSTKY.trim()).and(expression.le("CTSTKY", iTSTKY.trim()))
    }

    DBAction query = database.table("CSYTAB").index("00").matching(expression).selection("CTCONO", "CTDIVI", "CTSTCO", "CTSTKY", "CTTX40").build()
    DBContainer container = query.createContainer()
    container.set("CTCONO", Integer.valueOf(iCONO.trim()))
    container.set("CTSTCO", iSTCO)
    container.set("CTDIVI", iDIVI)

    query.readAll(container, 3, listCSYTAB)

  }

  /**
   *Closuer block for retrieiving records & send it to MI outout
   * @params 
   * @return 
   */

  Closure < ? > listCSYTAB = {
    DBContainer record ->
    mi.outData.put("CONO", String.valueOf(record.get("CTCONO")))
    mi.outData.put("DIVI", String.valueOf(record.get("CTDIVI")))
    mi.outData.put("STCO", String.valueOf(record.get("CTSTCO")))
    mi.outData.put("STKY", String.valueOf(record.get("CTSTKY")))
    mi.outData.put("TX40", String.valueOf(record.get("CTTX40")))
    // MI write
    mi.write()
  }
}