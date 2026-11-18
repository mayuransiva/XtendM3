/**
 * README
 * This extension is being used to get the inventory source.
 *
 * Name: EXT017MI.GetInventSource
 * Description: Get inventory source.
 * Date	      Changed By                   Description
 * 20230221	  MayuranSi@fortude.co        Get inventory source To identify the best fulfillment warehouse
 */


import java.math.RoundingMode
import java.text.DecimalFormat

public class GetInventSource extends ExtendM3Transaction {
  private final MIAPI mi
  private final DatabaseAPI database
  private final ProgramAPI program
  private final MICallerAPI miCaller
  

  //Input Variables
  private String iCONO
  private String iRLDT
  private String iITNO
  private String iSPLM
  private String iOQTY
  private String iFLDT

  //Global Variables
  HashMap < String, HashMap < String, String >> iSuppleModelLines = new HashMap < String, HashMap < String, String >> ()
  HashMap < String, HashMap < String, String >> iDOQtyLines = new HashMap < String, HashMap < String, String >> ()
  HashMap < String, HashMap < String, String >> iPOQtyLines = new HashMap < String, HashMap < String, String >> ()
  HashMap < String, HashMap < String, String >> iCOQtyLines = new HashMap < String, HashMap < String, String >> ()
  HashMap < String, String > values = new HashMap < String, String > ()
  HashMap < String, String > iTempValues = new HashMap < String, String > ()
  HashMap < String, String > inventoryQty = new HashMap < String, String > ()
  HashMap < String, String > iDOQty = new HashMap < String, String > ()
  HashMap < String, String > iPOQty = new HashMap < String, String > ()
  HashMap < String, String > iCOQty = new HashMap < String, String > ()
  HashMap < String, String > iDOQtyPreAllocated = new HashMap < String, String > ()
  HashMap < String, String > iPOQtyPreAllocated = new HashMap < String, String > ()
  HashMap < String, String > iCOQtyPreAllocated = new HashMap < String, String > ()
  ArrayList < String > iSupplyModels = new ArrayList < String > ()
  HashMap < String, Double > iFlrtWhloMap = new HashMap < String, Double > ()
  HashMap < String, String > iWhloQtyMap = new LinkedHashMap < String, String > ()
  HashMap < String, String > iWhloInvsMap = new HashMap < String, String > ()
  ArrayList < String > iWareHouseList = new ArrayList < String > ()
  Double iDOQtyTotal = 0
  Double iDOPreAloocatedTotal = 0
  Double iPOQtyTotal = 0
  Double iPOPreAllocatedQtyTotal = 0
  Double iCOPreAllocatedQtyTotal = 0
  Double iCOQtyTotal = 0
  String iDemandTimeFence
  boolean miWrite = false
  String oINVSOutVariable
  String oWHLOOutVariable
  Double oFLRTOutVariable
  String oAQTYOutVariable
  String oWHFROutVariable
  String firstWareHouseMMS059
  Double totalPOAndDO = 0
  String flag = "0"
  int maxPageSize = 100000

  public GetInventSource(MIAPI mi, DatabaseAPI database,  ProgramAPI program, MICallerAPI miCaller) {
    this.mi = mi
    this.database = database
    this.program = program
    this.miCaller = miCaller
    
  }

  public void main() {
    iCONO = (mi.inData.get("CONO") == null || mi.inData.get("CONO").trim().isEmpty()) ? "" : mi.inData.get("CONO").trim()
    iRLDT = (mi.inData.get("RLDT") == null || mi.inData.get("RLDT").trim().isEmpty()) ? "" : mi.inData.get("RLDT").trim()
    iITNO = (mi.inData.get("ITNO") == null || mi.inData.get("ITNO").trim().isEmpty()) ? "" : mi.inData.get("ITNO").trim()
    iSPLM = (mi.inData.get("SPLM") == null || mi.inData.get("SPLM").trim().isEmpty()) ? "" : mi.inData.get("SPLM").trim()
    iOQTY = (mi.inData.get("OQTY") == null || mi.inData.get("OQTY").trim().isEmpty()) ? "" : mi.inData.get("OQTY").trim()
    flag = (mi.inData.get("FLAG") == null || mi.inData.get("FLAG").trim().isEmpty()) ? "" : mi.inData.get("FLAG").trim()
    iFLDT = (mi.inData.get("FLDT") == null || mi.inData.get("FLDT").trim().isEmpty()) ? "" : mi.inData.get("FLDT").trim()

    if(flag.trim().isEmpty())
    {
      flag = "0"
    }



    getAvailSupplyModels()
    getAvailableInventory()
    setOutput()
  }

  /**
   * List all the available supplier models
   *
   */

  private getAvailSupplyModels() {

    String iSRCC
    String iFWHL
    String iORTY
    String iSUNO
    String recordNo = "0"

    Map<String, String> params = ["CONO": iCONO, "SPLM": iSPLM]

    Closure<?> callback = {
      Map < String,
        String > APIResponse ->

        String response = ("${APIResponse}")

        values = new HashMap < String,
          String > ()

        if (APIResponse.FWHL != null) {
          iFWHL = APIResponse.FWHL
        }
        if (APIResponse.SRCC != null) {
          iSRCC = APIResponse.SRCC
        }
        if (APIResponse.ORTY != null) {
          if(Double.parseDouble(APIResponse.QQTY) < 0.0)
          {
            iORTY = 0.0
          }else
          {
            iORTY = APIResponse.QQTY
          }

        }
        if (APIResponse.SUNO != null) {
          iSUNO = APIResponse.SUNO
        }

        this.values.put("FWHL", iFWHL)
        this.values.put("SRCC", iSRCC)
        this.values.put("ORTY", iORTY)
        this.values.put("SUNO", iSUNO)
        

        

        iSuppleModelLines.put(recordNo, this.values)
        iWareHouseList.add(iFWHL)

        if (recordNo == "0") {
          firstWareHouseMMS059 = iFWHL
        }



        int newRecordNo = recordNo.toInteger() + 1
        recordNo = newRecordNo.toString()

    }

    miCaller.call("MMS059MI", "List", params, callback)

  }

  /**
   * List all the available inventory
   *
   */
  private getAvailableInventory() {

    String iFWHL
    String iSRCC
    String iORTY
    String iSUNO
    Double avilableQty



    Iterator iSuppleModelLinesIterator = this.iSuppleModelLines.entrySet().iterator()
    while (iSuppleModelLinesIterator.hasNext()) {
      iCOQtyTotal = 0
      iCOPreAllocatedQtyTotal = 0
      iCOQtyLines.clear()
      iCOQty.clear()

      Map.Entry suplierModelDetailEntry = (Map.Entry) iSuppleModelLinesIterator.next()
      iTempValues = (HashMap) suplierModelDetailEntry.getValue()


      iFWHL = iTempValues.get("FWHL")
      iSRCC = iTempValues.get("SRCC")
      iORTY = iTempValues.get("ORTY")
      iSUNO = iTempValues.get("SUNO")



      /**
       * Get the sum of open CO quantities
       *
       */

      ExpressionFactory expression = database.getExpressionFactory("OOLINE")

      expression = ((expression.lt("OBDWDT", iRLDT)).or(expression.eq("OBDWDT", iRLDT)))
        .and(expression.ne("OBORST", "99"))
        .and(expression.ne("OBORST", "10"))
        .and(expression.eq("OBLTYP", "0"))

      DBAction query = database.table("OOLINE").index("21").matching(expression).selection("OBORNO", "OBPONR", "OBPOSX", "OBORQT", "OBALQT", "OBPLQT", "OBDLQT", "OBIVQT").build()
      DBContainer container = query.getContainer()
      container.set("OBCONO", Integer.parseInt(iCONO))
      container.set("OBITNO", iITNO)
      container.set("OBWHLO", iFWHL)

      query.readAll(container, 3,maxPageSize, resultsetQtyCO)

      // Get all the pre allocated CO qty  from MITPOL


      Iterator COPreAllocatedLinesIterator = this.iCOQtyLines.entrySet().iterator()
      while (COPreAllocatedLinesIterator.hasNext()) {
        String tempORNO
        String tempPONR
        String tempPOSX

        Map.Entry cOPreAllocatedLineDetails = (Map.Entry) COPreAllocatedLinesIterator.next()
        iTempValues = (HashMap) cOPreAllocatedLineDetails.getValue()

        tempORNO = iTempValues.get("ORNO")
        tempPONR = iTempValues.get("PONR")
        tempPOSX = iTempValues.get("POSX")


        query = database.table("MITPLO").index("60").selection("MOPQTT").build()
        container = query.getContainer()
        container.set("MOCONO", Integer.parseInt(iCONO))
        container.set("MOWHLO", iFWHL)
        container.set("MOITNO", iITNO)
        container.set("MORIDN", tempORNO)
        container.set("MOORCA", "311")
        container.set("MORIDL", Integer.parseInt(tempPONR))
        container.set("MORIDX", Integer.parseInt(tempPOSX))
        
        query.readAll(container, 7,maxPageSize, resultsetCOQtyPreAllocated)

        iCOQtyTotal = iCOQtyTotal - iCOPreAllocatedQtyTotal
        


      }

      /**
       * Get the qty for the each warehouse
       *
       */


      query = database.table("MITBAL").index("00").selection("MBSTQT", "MBALQT").build()
      container = query.getContainer()
      container.set("MBCONO", Integer.parseInt(iCONO))
      container.set("MBITNO", iITNO)
      container.set("MBWHLO", iFWHL)
      
      String tempSTQT
      String tempALQT
      
      if (query.read(container)) {
      
        tempSTQT = container.get("MBSTQT");
        tempALQT = container.get("MBALQT");
      }

      if (tempSTQT == null) {
        tempSTQT = 0
      }
      if (tempALQT == null) {
        tempALQT = 0
      }

      avilableQty = Double.parseDouble(tempSTQT) - Double.parseDouble(tempALQT) - iCOQtyTotal
      
      
      if(avilableQty<0)
      {
        avilableQty = 0
      }


      /**
       * Validate the avilable qty with the input
       *
       */

      if (avilableQty >= Double.parseDouble(iOQTY) && iSRCC == "0" && miWrite == false) {

        oINVSOutVariable = "1"
        oWHLOOutVariable = iFWHL
        oFLRTOutVariable = 1
        oAQTYOutVariable = Double.parseDouble(iOQTY)
        iFlrtWhloMap.put(iFWHL, oFLRTOutVariable)

        if(flag == "0")
        {

          iWhloQtyMap.put(iFWHL,iOQTY)
          oWHFROutVariable = iWhloQtyMap.toString()
          miWrite = true
        }else{
          miWrite = false
          iWhloQtyMap.put(iFWHL,iOQTY)
          iWhloInvsMap.put(iFWHL,oINVSOutVariable)
        }



      } else if (avilableQty < Double.parseDouble(iOQTY) && iSRCC == "0" && miWrite == false) {

        // Set the hashmaps to empty that are used for each PO and DO level
        iDOQtyTotal = 0
        iDOPreAloocatedTotal = 0
        iPOQtyTotal = 0
        iPOPreAllocatedQtyTotal = 0
        iDOQtyLines.clear()
        iPOQtyLines.clear()
        iDOQty.clear()
        iPOQty.clear()
        iDOQtyPreAllocated.clear()
        iPOQtyPreAllocated.clear()

        /**
         * If the qty is not available, then Check the DOs First
         *
         */

        /**
         * get the pre allocated qty for open DOs
         *
         */
        expression = database.getExpressionFactory("MITPLO")

        expression = ((expression.lt("MOPLDT", iRLDT)).or(expression.eq("MOPLDT", iRLDT)))
          .and((expression.gt("MOPLDT", iFLDT)).or(expression.eq("MOPLDT", iFLDT)))
          .and(expression.eq("MORFTX", "B0T=>" + iFWHL))

        query = database.table("MITPLO").index("20").matching(expression).selection("MORIDN", "MORIDL", "MOPQTT").build()
        container = query.getContainer()

        container.set("MOCONO", Integer.parseInt(iCONO))
        container.set("MOITNO", iITNO)
        container.set("MOORCA", "511")
        container.set("MOWHLO", "B0T")
        
        // Get all the pre allocated DO lines from MITPOL
        query.readAll(container, 3, maxPageSize,resultsetQtyDO_PreAloocatedQty)



        Iterator dOPreAllocatedLinesIterator = this.iDOQtyLines.entrySet().iterator()
        while (dOPreAllocatedLinesIterator.hasNext()) {
          String tempRIDN
          String tempRIDL

          Map.Entry DoPreAllocatedLineDetails = (Map.Entry) dOPreAllocatedLinesIterator.next()
          iTempValues = (HashMap) DoPreAllocatedLineDetails.getValue()

          tempRIDN = iTempValues.get("RIDN")
          tempRIDL = iTempValues.get("RIDL")
          
          expression = database.getExpressionFactory("MGLINE")
          expression = ((expression.lt("MRTRSH", "66")).or(expression.eq("MRTRSH", "66")))


          query = database.table("MGLINE").matching(expression).index("00").selection("MRTRQT", "MRALQT", "MRDPQT", "MRRPQT").build()
          container = query.getContainer()
          container.set("MRCONO", Integer.parseInt(iCONO))
          container.set("MRPONR", Integer.parseInt(tempRIDL))
          container.set("MRTRNR", tempRIDN)
          //container.set("MRTRSH", "99")
          query.readAll(container, 3,maxPageSize, resultsetQtyDO)

        }
          
        iDOQtyTotal = iDOQtyTotal - iDOPreAloocatedTotal + avilableQty


        if (iDOQtyTotal >= Double.parseDouble(iOQTY) && miWrite == false) {
          oINVSOutVariable = "2"
          oWHLOOutVariable = iFWHL
          oFLRTOutVariable = 1
          oAQTYOutVariable = Double.parseDouble(iOQTY)
          iFlrtWhloMap.put(iFWHL, oFLRTOutVariable)

          if(flag == "0")
          {
            miWrite = true
            iWhloQtyMap.put(iFWHL,iOQTY)
            oWHFROutVariable = iWhloQtyMap.toString()
          }else{
            miWrite = false
            iWhloQtyMap.put(iFWHL,iOQTY)
            iWhloInvsMap.put(iFWHL,oINVSOutVariable)
          }


        } else {
          /**
           * If the qty is not available from DOs, then Check the POs
           *
           */



          expression = database.getExpressionFactory("MPLINE")

          expression = expression.ne("IBRORC", "3")
            .and(expression.eq("IBRORN", ""))
            .and((expression.lt("IBPLDT", iRLDT)).or(expression.eq("IBPLDT", iRLDT)))
            .and(expression.ne("IBPUSL", "99"))
            .and(expression.ne("IBORTY", "R13"))
            .and((expression.gt("IBPLDT", iFLDT)).or(expression.eq("IBPLDT", iFLDT)))

          query = database.table("MPLINE").index("10").matching(expression).selection("IBORQA", "IBRVQA", "IBIVQA", "IBPUNO", "IBPNLI", "IBPNLS").build()
          container = query.getContainer()
          container.set("IBCONO", Integer.parseInt(iCONO))
          container.set("IBWHLO", iFWHL)
           container.set("IBITNO", iITNO)

          query.readAll(container, 3,maxPageSize, resultsetQtyPO)

          // Get all the pre allocated PO lines from MITPOL



          Iterator POPreAllocatedLinesIterator = this.iPOQtyLines.entrySet().iterator()
          while (POPreAllocatedLinesIterator.hasNext()) {
            String tempPUNO
            String tempPNLI
            String tempPNLS

            Map.Entry POPreAllocatedLineDetails = (Map.Entry) POPreAllocatedLinesIterator.next()
            iTempValues = (HashMap) POPreAllocatedLineDetails.getValue()


            tempPUNO = iTempValues.get("PUNO")

            tempPNLI = iTempValues.get("PNLI")


            tempPNLS = iTempValues.get("PNLS")



            query = database.table("MITPLO").index("60").selection("MOPQTT").build()
            container = query.getContainer()
            container.set("MOCONO", Integer.parseInt(iCONO))
            container.set("MOWHLO", iFWHL)
            container.set("MOITNO", iITNO)
            container.set("MORIDN", tempPUNO)
            container.set("MOORCA", "251")
            container.set("MORIDL", Integer.parseInt(tempPNLI))
            container.set("MORIDX", Integer.parseInt(tempPNLS))
            query.readAll(container, 7,maxPageSize, resultsetPOQtyPreAllocated)
            
            
          }

          iPOQtyTotal = iPOQtyTotal - iPOPreAllocatedQtyTotal
          

          if ((iPOQtyTotal + iDOQtyTotal) >= Double.parseDouble(iOQTY) && miWrite == false) {
            oINVSOutVariable = "2"
            oWHLOOutVariable = iFWHL
            oFLRTOutVariable = 1
            oAQTYOutVariable = Double.parseDouble(iOQTY)
            iFlrtWhloMap.put(iFWHL, oFLRTOutVariable)

            if(flag == "0")
            {
              miWrite = true
              iWhloQtyMap.put(iFWHL,iOQTY)
              oWHFROutVariable = iWhloQtyMap.toString()

            }else{
              miWrite = false
              iWhloQtyMap.put(iFWHL,iOQTY)
              iWhloInvsMap.put(iFWHL,oINVSOutVariable)

            }

          } else {

            totalPOAndDO = iPOQtyTotal + iDOQtyTotal
            oFLRTOutVariable = totalPOAndDO / Double.parseDouble(iOQTY)

            DecimalFormat df = new DecimalFormat("0.00")
            df.setRoundingMode(RoundingMode.DOWN)
            oFLRTOutVariable = Double.parseDouble(df.format(oFLRTOutVariable))



            oAQTYOutVariable = totalPOAndDO.toString()
            iFlrtWhloMap.put(iFWHL, oFLRTOutVariable)
            iWhloQtyMap.put(iFWHL,totalPOAndDO.toString())

            double temp_FLRT = 4
            iFlrtWhloMap.put(iFWHL,temp_FLRT)
            iFlrtWhloMap.put(iFWHL, oFLRTOutVariable)


          }

        }

      }





    }



    // Finally No POs or DOs quantities are not matching
    if (iSRCC == "0" && miWrite == false) {
      String warehouse
      Double iFLRT
      Double iFLRT_Highest


      for (int i = 0; i < iWareHouseList.size(); i++) {

        warehouse = iWareHouseList.get(i)


        if (i == 0) {

          iFLRT_Highest = iFlrtWhloMap.get(warehouse)


          oINVSOutVariable = 4
          oWHLOOutVariable = warehouse
          oAQTYOutVariable = iWhloQtyMap.get(warehouse)
          oFLRTOutVariable = iFLRT_Highest
          oWHFROutVariable = iWhloQtyMap.toString()




          if(oFLRTOutVariable == 1)
          {
            oINVSOutVariable = iWhloInvsMap.get(warehouse)

          }

        } else {
          iFLRT = iFlrtWhloMap.get(warehouse)


          if (iFLRT > iFLRT_Highest) {
            iFLRT_Highest = iFLRT

            oINVSOutVariable = 4
            oWHLOOutVariable = warehouse
            oAQTYOutVariable = iWhloQtyMap.get(warehouse)
            oFLRTOutVariable = iFLRT_Highest
            oWHFROutVariable = iWhloQtyMap.toString()

            if(oFLRTOutVariable == 1)
            {
              oINVSOutVariable = iWhloInvsMap.get(warehouse)
            }
          }
        }

      }


    }

    if (iSRCC == "0" && miWrite == true && flag == "1")
    {
      oWHFROutVariable = iWhloQtyMap.toString()
    }


  }

  Closure < ? > resultsetQtyDO_PreAloocatedQty = {
    DBContainer container ->
      iDOQtyPreAllocated = new HashMap < String,
        String > ()
      iDOQtyPreAllocated.put("PQTT", container.get("MOPQTT").toString())
      iDOQtyPreAllocated.put("RIDN", container.get("MORIDN").toString())
      iDOQtyPreAllocated.put("RIDL", container.get("MORIDL").toString())

      String tempPQTT = container.get("MOPQTT").toString()

      iDOPreAloocatedTotal = iDOPreAloocatedTotal + Double.parseDouble(tempPQTT)

      this.iDOQtyLines.put(container.get("MORIDN").toString(), iDOQtyPreAllocated)

  }

  Closure < ? > resultsetQtyDO = {
    DBContainer container ->
      iDOQty = new HashMap < String,
        String > ()
      iDOQty.put("TRQT", container.get("MRTRQT").toString())
      iDOQty.put("ALQT", container.get("MRALQT").toString())
      iDOQty.put("DPQT", container.get("MRDPQT").toString())
      iDOQty.put("PRQT", container.get("MRRPQT").toString())

      String tempTRQT = container.get("MRTRQT").toString()
      String tempALQT = container.get("MRALQT").toString()
      String tempDPQT = container.get("MRDPQT").toString()
      String tempPRQT = container.get("MRRPQT").toString()

      
      iDOQtyTotal = iDOQtyTotal + Double.parseDouble(tempTRQT)

      
  }



  Closure < ? > resultsetQtyPO = {
    DBContainer container ->
      iPOQty = new HashMap < String,
        String > ()
      iPOQty.put("ORQA", container.get("IBORQA").toString())
      iPOQty.put("RVQA", container.get("IBRVQA").toString())
      iPOQty.put("IVQA", container.get("IBIVQA").toString())
      iPOQty.put("PUNO", container.get("IBPUNO").toString())
      iPOQty.put("PNLI", container.get("IBPNLI").toString())
      iPOQty.put("PNLS", container.get("IBPNLS").toString())

      String tempORQA = container.get("IBORQA").toString()
      String tempRVQA = container.get("IBRVQA").toString()
      String tempIVQA = container.get("IBIVQA").toString()



      iPOQtyTotal = iPOQtyTotal + Double.parseDouble(tempORQA) - Double.parseDouble(tempRVQA) - Double.parseDouble(tempIVQA)
      
      this.iPOQtyLines.put(container.get("IBPNLI").toString() + container.get("IBPNLI").toString(), iPOQty)
  }

  Closure < ? > resultsetPOQtyPreAllocated = {
    DBContainer container ->
      iPOQtyPreAllocated = new HashMap < String,
        String > ()
      iPOQtyPreAllocated.put("PQTT", container.get("MOPQTT").toString())
      String PQTT = container.get("MOPQTT").toString()

      iPOPreAllocatedQtyTotal = iPOPreAllocatedQtyTotal + Double.parseDouble(PQTT)



  }

  Closure < ? > resultsetDemandTimeFence = {
    DBContainer container ->

      iDemandTimeFence = container.get("MWDMF0").toString()



  }

  /**
   * Set the final values to the output variable 
   *
   */
  
  private setOutput() {
    mi.outData.put("INVS", oINVSOutVariable)
    mi.outData.put("WHLO", oWHLOOutVariable)
    mi.outData.put("FLRT", oFLRTOutVariable.toString())
    mi.outData.put("AQTY", oAQTYOutVariable)
    mi.outData.put("WHFR", oWHFROutVariable)
    mi.write()
  }

  Closure < ? > resultsetQtyCO = {
    DBContainer container ->
      iCOQty = new HashMap < String,
        String > ()
      iCOQty.put("ORNO", container.get("OBORNO").toString())
      iCOQty.put("PONR", container.get("OBPONR").toString())
      iCOQty.put("POSX", container.get("OBPOSX").toString())
      iCOQty.put("ORQT", container.get("OBORQT").toString())
      iCOQty.put("ALQT", container.get("OBALQT").toString())
      iCOQty.put("PLQT", container.get("OBPLQT").toString())
      iCOQty.put("DLQT", container.get("OBDLQT").toString())
      iCOQty.put("IVQT", container.get("OBIVQT").toString())

      String tempORQT = container.get("OBORQT").toString()
      String tempALQT = container.get("OBALQT").toString()
      String tempPLQT = container.get("OBPLQT").toString()
      String tempDLQT = container.get("OBDLQT").toString()
      String tempIVQT = container.get("OBIVQT").toString()



      iCOQtyTotal = iCOQtyTotal + Double.parseDouble(tempORQT) - Double.parseDouble(tempALQT) - Double.parseDouble(tempPLQT) - Double.parseDouble(tempDLQT) - Double.parseDouble(tempIVQT)


      this.iCOQtyLines.put(container.get("OBORNO").toString() + container.get("OBPONR").toString() + container.get("OBPOSX").toString(), iCOQty)
  }

  Closure < ? > resultsetCOQtyPreAllocated = {
    DBContainer container ->
      iCOQtyPreAllocated = new HashMap < String,
        String > ()
      iCOQtyPreAllocated.put("PQTT", container.get("MOPQTT").toString())
      String PQTT = container.get("MOPQTT").toString()

      iCOPreAllocatedQtyTotal = iCOPreAllocatedQtyTotal + Double.parseDouble(PQTT)



  }

}
