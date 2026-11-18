/**
 * README
 * This extension is being used to update TXT1 and TXT2 of MO 
 *
 * Name: EXT360MI.UpdMOTxt
 * Description: Update TXT1 and TXT2 of MO 
 * Date	      Changed By            Description
 * 20230106	  RA             Update TXT1 and TXT2 of MO  
 *
 */

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

public class UpdMOTxt extends ExtendM3Transaction {
  private final MIAPI mi
	private final DatabaseAPI database
	private final LoggerAPI logger
	private final ProgramAPI program
	private final UtilityAPI utility
	private final MICallerAPI miCaller
  
  
  //Primary key fields
	private int iCONO
  private String iFACI
  private String iPRNO
  private String iMFNO
  
  private String iTXT1
  private String iTXT2
  
  private int iCHNO
  
  
  public UpdMOTxt(MIAPI mi, DatabaseAPI database, LoggerAPI logger, ProgramAPI program, UtilityAPI utility, MICallerAPI miCaller) {
    this.mi = mi
		this.database = database
		this.logger = logger
		this.program = program
		this.utility = utility
		this.miCaller = miCaller
  }
  
  public void main() {
    iCONO = (Integer)program.getLDAZD().CONO 
		iFACI = mi.inData.get("FACI") == null ? "" : mi.inData.get("FACI").trim()
		iPRNO = mi.inData.get("PRNO") == null ? "" : mi.inData.get("PRNO").trim()
	  iMFNO = mi.inData.get("MFNO") == null ? "" : mi.inData.get("MFNO").trim()
		iTXT1 = mi.inData.get("TXT1") == null ? "" : mi.inData.get("TXT1").trim()
		iTXT2 = mi.inData.get("TXT2") == null ? "" : mi.inData.get("TXT2").trim()
	
	
	  if (validateInput()){
	    updateRecord();
	  }
	 
    
  }
	
  
  /**
	 * Update Text 1 and Text 2 fields
	**/
	public boolean validateInput()
  {
    DBAction action = database.table("MWOHED").index("00").selection("VHCONO","VHFACI","VHPRNO","VHMFNO").build()
		DBContainer container = action.getContainer()
    container.set("VHCONO", iCONO)
	  container.set("VHFACI", iFACI)
		container.set("VHPRNO", iPRNO)
		container.set("VHMFNO", iMFNO)
		
		if (action.read(container)) {
        String FACI = container.get("VHFACI")
        String PRNO = container.get("VHPRNO")
        iCHNO = container.get("VHCHNO")
        iCHNO++
        
        return true;
    }
    else{
      
      mi.error("MO does not exist " + iMFNO+" for the given facility and product")
      return false
    }
		
		
  }
 

	
	/**
	 * Update Text 1 and Text 2 fields
	**/
	public void updateRecord()
  {
    DBAction action = database.table("MWOHED").index("00").build()
		DBContainer container = action.getContainer()
    container.set("VHCONO", iCONO)
	  container.set("VHFACI", iFACI)
		container.set("VHPRNO", iPRNO)
		container.set("VHMFNO", iMFNO)
	
	  action.readLock(container, updateCallBack)
		
  }
  /**
	 * Closure for MWOHED Update
	 */
		Closure<?> updateCallBack = { LockedResult lockedResult ->

    if (!iTXT2.trim().isEmpty()) {
      if (iTXT2 == "?") {
        lockedResult.set("VHTXT2", "")
        } else {
        lockedResult.set("VHTXT2", iTXT2)
        }
      }
      
      if (!iTXT1.trim().isEmpty()) {
      if (iTXT1 == "?") {
        lockedResult.set("VHTXT1", "")
        } else {
        lockedResult.set("VHTXT1", iTXT1)
        }
      }
      
      lockedResult.set("VHCHNO", iCHNO)
      lockedResult.set("VHLMDT", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger())
      lockedResult.set("VHCHID", program.getUser())
		  lockedResult.update()
  }
  

	
	

	
}