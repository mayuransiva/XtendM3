/**
 * README
 * This extension is being used to Update the Last delivery date LDED in OOHEAD and OOLINE table. 
 *
 * Name: EXT020MI.UpdateDate
 * Description: Update LDED ield in OOHEAD and OOLINE table. 
 * Date	          Changed By                      Description
 * 2024-08-28    Mayuransi@fortude.co           Update last delivery date in OOHEADE and OOLINE table
 *
 */

import java.time.LocalDateTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.DateTimeException


public class UpdateDate extends ExtendM3Transaction {
    private final MIAPI mi
    private final DatabaseAPI database
    private final ProgramAPI program
    private final MICallerAPI miCaller
    private final UtilityAPI utility
    private final LoggerAPI logger

    private String iORNO, lowestStatus, iLastDelDate, reqDelDate
    private int iCONO, orderLocked
    private boolean flag
    int maxPageSize

    public UpdateDate(MIAPI mi, DatabaseAPI database, ProgramAPI program, MICallerAPI miCaller, UtilityAPI utility, LoggerAPI logger) {
        this.mi = mi
        this.database = database
        this.program = program
        this.miCaller = miCaller
        this.utility = utility
        this.logger = logger 
    }


    /**
     ** Main function
     * @param
     * @return
     */
    public void main() {
        iCONO = (mi.inData.get("CONO") == null || mi.inData.get("CONO").trim().isEmpty()) ? program.LDAZD.CONO as Integer : mi.inData.get("CONO") as Integer
        iORNO = (mi.inData.get("ORNO") == null || mi.inData.get("ORNO").trim().isEmpty()) ? "" : mi.inData.get("ORNO")
        iLastDelDate = (mi.inData.get("LDED") == null || mi.inData.get("LDED").trim().isEmpty()) ? "0" : mi.inData.get("LDED")
        flag = false
        maxPageSize = 100000
        
        boolean validInput = validateInput()
        if (validInput) 
        {
            validateLastDeliveryDateOOHEAD()
            validateLastDeliveryDateOOLINE()
            if(flag == true)
            {
              updateLastDelDate_OOHEAD()
              updateLastDelDate_OOLINE()
            }

        }
    }

    /**
     *Validate inputs
     * @params 
     * @return boolean
     */
    private boolean validateInput() {
        //Validate Company Number
        Map<String, String> params = ["CONO": iCONO.toString().trim()]
        Closure<?> callback = {
            Map < String,
            String > response ->
            if (response.CONO == null) {
                mi.error("Invalid Company Number " + iCONO)
                return false
            }
        }
        miCaller.call("MNS095MI", "Get", params, callback)

        //Validate Order Number
        params = ["ORNO": iORNO.toString().trim()]
        callback = {
            Map < String,
            String > response ->
            if (response.ORNO == null) {
                mi.error("Invalid Order Number " + iORNO)
                return false
            }
        }
        miCaller.call("OIS100MI", "GetOrderHead", params, callback)

        return true

    }
    
    
    
    /**
     * Validate Last Delivery Date against Request Delivery Date in OOHEAD
     * @params 
     * @return 
     */
    
    public validateLastDeliveryDateOOHEAD() {
        DBAction query = database.table("OOHEAD").index("00").selection("OARLDT","OAORSL","OAHOCD").build()
        DBContainer container = query.getContainer()
        container.set("OACONO", iCONO)
        container.set("OAORNO", iORNO)
        if (query.read(container)) 
        {
          reqDelDate = container.get("OARLDT")
          lowestStatus = container.get("OAORSL")
          orderLocked =  container.get("OAHOCD")
          if(Integer.parseInt(lowestStatus) >=44 )
          {
            flag = false
            mi.error("Last delivery date cannot be updated. Order lowest status should be less than 44")
            return
          }else if(orderLocked != 0)
          {
            flag = false
            mi.error("The order is locked by another program and Last Delivery date cannot be updated")
            return
          }
          else
          {
            if (iLastDelDate != "0") {
              String sourceFormat = utility.call("DateUtil", "getFullFormat", mi.getDateFormat())
              flag = utility.call("DateUtil", "isDateValid", iLastDelDate, sourceFormat.toString())
              if (!flag) {
                flag = false
                mi.error("Last Delivery Date not in " + sourceFormat + " format. Please Check " + iLastDelDate)
                return
              } else {
                if (!sourceFormat.trim().equals("yyyyMMdd")) {
                  iLastDelDate = utility.call("DateUtil", "convertDate", sourceFormat, "yyyyMMdd", iLastDelDate) //Maintain date in YMD8 format in the table
                }
                //Validate that  last delivery date must cover the requested delivery date
                if (Integer.parseInt(reqDelDate) > Integer.parseInt(iLastDelDate)) {
                  mi.error("Last delivery date cannot be updated. Last delivery date should be greater than the Request delivery date")
                  flag = false
                  return
                }
              }
            }  

          }
            
        } 
        else
        {
            mi.error("Record does not Exist for Order No - "+iORNO)
            return
        }
    }



    /**
     * Validate Last Delivery Date against Request Delivery Date in OOLINE
     * @params 
     * @return 
     */
     
     
     public validateLastDeliveryDateOOLINE() {
       
       if(flag)
       {
         
        DBAction query = database.table("OOLINE").index("00").selection("OBDWDT").build()
        DBContainer container = query.getContainer()
        container.set("OBCONO", iCONO)
        container.set("OBORNO", iORNO)
        query.readAll(container, 2,maxPageSize, resultsetOOLINE)
       }
       else
       {
         return
       }
       
    }
    
    
    Closure < ? > resultsetOOLINE = {
      DBContainer container ->
      reqDelDate = container.get("OBDWDT")
      
      if (iLastDelDate != "0") {
        String sourceFormat = utility.call("DateUtil", "getFullFormat", mi.getDateFormat())
        flag = utility.call("DateUtil", "isDateValid", iLastDelDate, sourceFormat.toString())
        if (!flag) {
          flag = false
          mi.error("Last Delivery Date not in " + sourceFormat + " format. Please Check " + iLastDelDate)
          return
        } else {
          if (!sourceFormat.trim().equals("yyyyMMdd")) {
            iLastDelDate = utility.call("DateUtil", "convertDate", sourceFormat, "yyyyMMdd", iLastDelDate) //Maintain date in YMD8 format in the table
          }
          //Validate that  last delivery date must cover the requested delivery date
          if (Integer.parseInt(reqDelDate) > Integer.parseInt(iLastDelDate)) {
            mi.error("Last delivery date cannot be updated. Last delivery date in the order lines should be greater than the Request delivery date")
            flag = false
            return
          }
        }
      }  
      
    
    
    }




  
    /**
     * update OALDED in OOHEAD table
     * @params 
     * @return 
     */
   
   
    public updateLastDelDate_OOHEAD() {
        DBAction query = database.table("OOHEAD").index("00").build()
        DBContainer container = query.getContainer()
        container.set("OACONO", iCONO)
        container.set("OAORNO", iORNO)
        if (query.read(container)) {
            query.readLock(container, updateCallBack)
        } else {
            mi.error("Record does not Exist.")
            return
        }
    }

    Closure<?> updateCallBack = {
        LockedResult lockedResult ->

        lockedResult.set("OALDED", iLastDelDate.toInteger())
        lockedResult.set("OALMDT", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger())
        lockedResult.set("OALMTS", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")).toInteger())
        lockedResult.set("OACHID", program.getUser())
        lockedResult.set("OACHNO", lockedResult.getInt("OACHNO") + 1)
        lockedResult.update()
    }
    
    
    
    /**
     * update OALDED in OOLINE table
     * @params 
     * @return 
     */
   
    
    //Read all the lines before update
    public updateLastDelDate_OOLINE() {
        DBAction query = database.table("OOLINE").index("00").selection("OBORST").build()
        DBContainer container = query.getContainer()
        container.set("OBCONO", iCONO)
        container.set("OBORNO", iORNO)
        query.readAll(container, 2,maxPageSize, getOOLINE)
    }
      
      
    Closure < ? > getOOLINE = {
  
      DBContainer container ->
      String PONR = container.get("OBPONR").toString()
      String POSX = container.get("OBPOSX").toString()
      String status = container.get("OBORST").toString()
      if (PONR != null) 
      {
        if(Integer.parseInt(status) < 44 )
        {
          updateCallBackOOLINE(PONR, POSX)
        }
      }
  
    }
  
  
    //Update each OOLINE table record  
    public updateCallBackOOLINE(String PONR, String POSX) {
    DBAction query = database.table("OOLINE").index("00").build()
    DBContainer container = query.getContainer()
    container.set("OBCONO", iCONO)
    container.set("OBORNO", iORNO)
    container.set("OBPONR", Integer.parseInt(PONR))
    container.set("OBPOSX", Integer.parseInt(POSX))
  
    query.readLock(container, updateLineDetails)
  
    }
    
     Closure < ? > updateLineDetails = {
      LockedResult lockedResult ->
  
      lockedResult.set("OBLDED", iLastDelDate.toInteger())
      lockedResult.set("OBLMDT", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger())
      lockedResult.set("OBLMTS", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")).toInteger())
      lockedResult.set("OBCHID", program.getUser())
      lockedResult.set("OBCHNO", lockedResult.getInt("OBCHNO") + 1)
      lockedResult.update()
  
    }
    
    
}