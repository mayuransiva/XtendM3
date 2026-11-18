/**
 * README
 * This extension is being used to remove schedule number from MO
 *
 * Name: EXT360MI.RemSchNumFrmMO
 * Description: Remove schedule number from MO
 * Date	      Changed By            Description
 * 20230106	  RA             Remove schedule number from MO
 * 20230330 Mayuran - Adding audit trails
 */
 
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

public class RemSchNumFrmMO extends ExtendM3Transaction {
  private final MIAPI mi;
	private final DatabaseAPI database;
	private final LoggerAPI logger;
	private final ProgramAPI program;
	private final UtilityAPI utility;
	private final MICallerAPI miCaller;
  
  
  //Primary key fields
	private int iCONO;
  private String iFACI;
  private String iPRNO;
  private String iMFNO;
  
  
  public RemSchNumFrmMO(MIAPI mi, DatabaseAPI database, LoggerAPI logger, ProgramAPI program, UtilityAPI utility, MICallerAPI miCaller) {
    this.mi = mi;
		this.database = database;
		this.logger = logger;
		this.program = program;
		this.utility = utility
		this.miCaller = miCaller;
  }
  
  public void main() {
    iCONO = (Integer)program.getLDAZD().CONO 
		iFACI = mi.inData.get("FACI") == null ? "" : mi.inData.get("FACI").trim()
		iPRNO = mi.inData.get("PRNO") == null ? "" : mi.inData.get("PRNO").trim()
	  iMFNO = mi.inData.get("MFNO") == null ? "" : mi.inData.get("MFNO").trim()
	
	
	
	  if (validateInput()){
	    updateRecordMWOHED();
	    
	    updateRecordMWOOPE();
	    
	    updateRecordMWOMAT();
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
        String WHST = container.get("VHWHST")
        
        if (WHST.trim().equals("")){
           return true
        }
        else{
           mi.error("MO is not closed!")
           return false
        }
        
       
    }
    else{
      
      mi.error("MO does not exist " + iMFNO+" for the given facility and product")
      return false
    }
		
		
  }
  
  
  /**
	 * Update SCHN in table MWOMAT
	**/
	public void updateRecordMWOMAT()
  {
    DBAction action = database.table("MWOMAT").index("00").selection("VMCONO","VMFACI","VMPRNO","VMMFNO","VMMSEQ").build()
		DBContainer container = action.getContainer()
    container.set("VMCONO", iCONO)
	  container.set("VMFACI", iFACI)
		container.set("VMPRNO", iPRNO)
		container.set("VMMFNO", iMFNO)
	
	  action.readAllLock(container, 4, updateCallBackMWOMAT)
		
  }
  /**
	 * Closure for MWOMAT Update
	 */
		Closure<?> updateCallBackMWOMAT = { LockedResult lockedResult ->

    lockedResult.set("VMSCHN", 0)
    lockedResult.set("VMLMDT", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger())
    lockedResult.set("VMCHID", program.getUser())
    lockedResult.set("VMCHNO", lockedResult.getInt("VMCHNO") + 1)
    
		lockedResult.update()
  }
  
  
  
  /**
	 * Update SCHN in table MWOOPE
	**/
	public void updateRecordMWOOPE()
  {
    DBAction action = database.table("MWOOPE").index("00").selection("VOCONO","VOFACI","VOPRNO","VOMFNO","VOOPNO").build()
		DBContainer container = action.getContainer()
    container.set("VOCONO", iCONO)
	  container.set("VOFACI", iFACI)
		container.set("VOPRNO", iPRNO)
		container.set("VOMFNO", iMFNO)
	
	  action.readAllLock(container, 4, updateCallBackMWOOPE)
		
  }
  /**
	 * Closure for MWOOPE Update
	 */
		Closure<?> updateCallBackMWOOPE = { LockedResult lockedResult ->

    lockedResult.set("VOSCHN", 0)
    lockedResult.set("VOLMDT", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger())
    lockedResult.set("VOCHID", program.getUser())
    lockedResult.set("VOCHNO", lockedResult.getInt("VOCHNO") + 1)
    
		lockedResult.update()
  }
 

	
	/**
	 * Update SCHN in table MWOHED
	**/
	public void updateRecordMWOHED()
  {
    DBAction action = database.table("MWOHED").index("00").selection("VHCONO","VHFACI","VHPRNO","VHMFNO").build()
		DBContainer container = action.getContainer()
    container.set("VHCONO", iCONO)
	  container.set("VHFACI", iFACI)
		container.set("VHPRNO", iPRNO)
		container.set("VHMFNO", iMFNO)
	
	  action.readLock(container, updateCallBackMWOHED)
		
  }
  /**
	 * Closure for MWOHED Update
	 */
		Closure<?> updateCallBackMWOHED = { LockedResult lockedResult ->

    lockedResult.set("VHSCHN", 0)
    lockedResult.set("VHLMDT", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger())
    lockedResult.set("VHCHID", program.getUser())
    lockedResult.set("VHCHNO", lockedResult.getInt("VHCHNO") + 1)
    
		 lockedResult.update()
  }
  

	
	

	
}