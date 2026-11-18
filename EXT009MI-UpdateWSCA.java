/**
 * README
 * This extension is being used to  update costing model
 *
 * Name: EXT009MI.UpdateWSCA
 * Date	      Changed By                   Description
 * 2023	  Mayuransi@fortude.co        Update WSCA field in MPCOVE table
 */

import java.time.*
import java.time.format.DateTimeFormatter
import java.text.ParseException

public class UpdateWSCA extends ExtendM3Transaction {
  private final MIAPI mi
  private final ProgramAPI program
  private final DatabaseAPI database
  private final MICallerAPI miCaller

  
  /**
   * Global Variables declaration
   *  
   */
 
  public String iCONO
  public String iCEID
  public String iOVK1
  public String iOVK2
  public String iOVK3
  public String iOVK4
  public String iOVK5
  public String iVFDT
  public String iOVHE
  public String iWSCA
  public String iWSOP
  
  


  String Message_response
  Optional < String > TransactionDetails = Optional.empty()

  public UpdateWSCA(MIAPI mi, ProgramAPI program, MICallerAPI miCaller, DatabaseAPI database) {
    this.mi = mi
    this.program = program
    this.miCaller = miCaller
    this.database = database
  }


  /**
   * Main() function
   *  
   */
   
  public void main() {

    
    iCONO = mi.inData.get("CONO").trim()
    iCEID = mi.inData.get("CEID").trim()
    iOVK1 = mi.inData.get("OVK1").trim()
    iOVK2 = mi.inData.get("OVK2").trim()
    iOVK3 = mi.inData.get("OVK3").trim()
    iOVK4 = mi.inData.get("OVK2").trim()
    iOVK5 = mi.inData.get("OVK3").trim()
    iVFDT = mi.inData.get("VFDT").trim()
    iWSCA = mi.inData.get("WSCA").trim()


    // validaate WSCA & WSOP
    if (iWSOP == "16" && (iWSCA.isBlank() || iWSCA.contains("?"))) {
      mi.error("NOK-WSCA is Mandatory when WSOP is 16" + iWSCA)
      return 
    }

    // Validate for Date format for VFDT
    try {
      String Date = LocalDate.parse(iVFDT, DateTimeFormatter.ofPattern("yyyyMMdd"))
    } catch (ParseException e) {
      mi.error("Invalid Date VFDT -" + iVFDT)
      return 
    }


    //Update MPCOVE table
    boolean validInput = validateInput()
    if (validInput) {
      //Update record in MPCOVE table
      update_MPCOVE()
    }

  }
  
  /**
   *Validate Userinputs 
   * @params 
   * @return 
   */
     
  public boolean validateInput() {

    //Validate CONO
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

    //Validate CEID and other input fi
    params = ["OVK1": iOVK1, "OVK2": iOVK2, "OVK3": iOVK3, "OVK4": iOVK4, "OVK5": iOVK1, "VFDT": iVFDT]
    callback = {
      Map < String,
      String > response ->
      if (response.CEID == null) {
        mi.error("No Records Exists For given input of CONO- " + iCONO + " CEID- " + iCEID + " OVK1- " + iOVK1 + " OVK2- " + iOVK2 + " VFDT- " + iVFDT + " OVHE- " + iOVHE)
        return false
      }
    }
    miCaller.call("PPS280MI", "GetElement", params, callback)

    return true

  }


  /**
   * update records in MPCOVE table
   * @params 
   * @return 
   */
 
  public update_MPCOVE() {

    DBAction query = database.table("MPCOVE").index("00").build()
    DBContainer container = query.createContainer()

    container.set("IJCONO", Integer.valueOf(iCONO))
    container.set("IJCEID", iCEID)

    if (!mi.inData.get("OVK1").isBlank()) {
      container.set("IJOVK1", iOVK1)
    }
    if (!mi.inData.get("OVK2").isBlank()) {
      container.set("IJOVK2", iOVK2)
    }
    if (!mi.inData.get("OVK3").isBlank()) {
      container.set("IJOVK3", iOVK3)
    }
    if (!mi.inData.get("OVK4").isBlank()) {
      container.set("IJOVK4", iOVK4)
    }
    if (!mi.inData.get("OVK5").isBlank()) {
      container.set("IJOVK5", iOVK5)
    }
    container.set("IJVFDT", Integer.valueOf(iVFDT))


    if (!query.readLock(container, updateCallBack)) {
      mi.error("Record does not Exist.");
      return;
    }

  }

  Closure < ? > updateCallBack = {
    LockedResult record ->

    if (!mi.inData.get("WSCA").isBlank()) {
      if (mi.inData.get("WSCA") == "?") {
        record.set("IJWSCA", "")
      } else {
        record.set("IJWSCA", mi.inData.get("WSCA").trim())
      }

    }

    record.set("IJLMDT", Integer.valueOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))))
    record.set("IJCHNO", record.getInt("IJCHNO") + 1)
    record.set("IJCHID", program.getUser())
    record.update()

  }

}