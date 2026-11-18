/**
 * README
 * This extension is being used to update the CO header
 *
 * Name: EXT003MI.UpdCOHead
 * Description: Update the CO header details  - MODL,TEDL,PYCD,ZSTX,PYCD,TECD,INRC,OPRI
 * Date       Changed By                   Description
 * 2021   mayuransi@fortude.co        update the CO header information
 * 2024   mayuransi@fortude.co        OPRI field has been included for CO update
 * 2024   mayuransi@fortude.co        TECD field "?" validation has been included for CO update
 */

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

public class UpdateCOHead extends ExtendM3Transaction {
  private final MIAPI mi
  private final DatabaseAPI database
  private final ProgramAPI program
  private final MICallerAPI miCaller
  private final LoggerAPI logger

  public UpdateCOHead(MIAPI mi, DatabaseAPI database, ProgramAPI program, MICallerAPI miCaller, LoggerAPI logger) {
    this.mi = mi
    this.database = database
    this.program = program
    this.miCaller = miCaller
    this.logger = logger
  }

  
  int iCONO, iCUTP, iOT35, iHOCD,iOPRI
  String iORNO, iORST, iTEPY, iMODL, iTEDL, iZSTX, iPYCD, iTECD, iINRC, iCUNO, iLNCD, iSTAT, iPARM, iFECN, iORTP, VARPAYP, VARCRDC, DSTEPY, DSTECD, DSPYCD
  char iORTK
  boolean MODLUpdate = false, TEDLUpdate = false, TEPYUpdate = false
  int pageSize = 10000

  /**
   ** Main function
   * @param
   * @return
   */
  public void main() {
    iCONO = (mi.inData.get("CONO") == null || mi.inData.get("CONO").trim().isEmpty()) ? program.LDAZD.CONO as Integer : mi.inData.get("CONO") as Integer
    iORNO = (mi.inData.get("ORNO") == null || mi.inData.get("ORNO").trim().isEmpty()) ? "" : mi.inData.get("ORNO")
    iTEPY = (mi.inData.get("TEPY") == null || mi.inData.get("TEPY").trim().isEmpty()) ? "" : mi.inData.get("TEPY")
    iMODL = (mi.inData.get("MODL") == null || mi.inData.get("MODL").trim().isEmpty()) ? "" : mi.inData.get("MODL")
    iTEDL = (mi.inData.get("TEDL") == null || mi.inData.get("TEDL").trim().isEmpty()) ? "" : mi.inData.get("TEDL")
    iZSTX = (mi.inData.get("ZSTX") == null || mi.inData.get("ZSTX").trim().isEmpty()) ? "" : mi.inData.get("ZSTX")
    iPYCD = (mi.inData.get("PYCD") == null || mi.inData.get("PYCD").trim().isEmpty()) ? "" : mi.inData.get("PYCD")
    iTECD = (mi.inData.get("TECD") == null || mi.inData.get("TECD").trim().isEmpty()) ? "" : mi.inData.get("TECD")
    iINRC = (mi.inData.get("INRC") == null || mi.inData.get("INRC").trim().isEmpty()) ? "" : mi.inData.get("INRC")
    iOPRI = (mi.inData.get("OPRI") == null || mi.inData.get("OPRI").trim().isEmpty() || mi.inData.get("OPRI").equals("?")) ? 100 : mi.inData.get("OPRI") as Integer
    

    boolean validInput = validateInput()
    if (validInput) {
      updateOOHEAD()
      updateOOLINE()
      updateMHDISH()
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
    logger.debug("Company is" + iCONO)
    miCaller.call("MNS095MI", "Get", params, callback)

    //Validate Order Number
    params = ["ORNO": iORNO.toString().trim()]
    callback = {
      Map < String,
      String > response ->
      if (response.ORNO == null) {
        mi.error("Invalid Order Number " + iORNO)
        return false
      } else {
        // If Order number is valid, fetch Customer number,future agreement rate, language code and order type
        iCUNO = response.CUNO
        logger.debug("Customer Number is  " + iCUNO)
        iFECN = response.FECN
        logger.debug("Future agreement Number is  " + iFECN)
        iLNCD = response.LNCD
        logger.debug("Language code is  " + iLNCD)
        iORTP = response.ORTP
        logger.debug("Order Type is  " + iORTP)
        iHOCD = Integer.parseInt(response.HOCD)
        logger.debug("Hold Code is  " + iHOCD)
        iORST = response.ORST
        logger.debug("Highest Order Status is  " + iORST)

      }
    }

    miCaller.call("OIS100MI", "GetOrderHead", params, callback)

    if (iHOCD != 0) {
      mi.error("Order " + iORNO + "is locked by hold code " + iHOCD)
      return false
    }

    if (iORST > "44") {
      mi.error("Order status should not be greater than 44")
      return false
    }

    //Call CRS610MI.GetBasicData to get Status and customer type to be used later
    params = ["CONO": iCONO.toString().trim(), "CUNO": iCUNO.toString().trim()]
    callback = {
      Map < String,
      String > response ->
      if (response.CUNO == null) {
        mi.error("Invalid Customer " + iCUNO)
        return false
      } else {
        // If Order number is valid, fetch status and customer type
        iSTAT = response.STAT
        logger.debug("Status is  " + iSTAT)
        iCUTP = Integer.parseInt(response.CUTP)
        logger.debug("Customer Type is  " + iCUTP)
      }
    }
    miCaller.call("CRS610MI", "GetBasicData", params, callback)

    //Validate Payment Terms
    if (!iTEPY.trim().isEmpty()) {
		  if (!iTEPY.trim().equals("?")) {

		  DBAction query = database.table("CSYTAB").index("00").selection("CTPARM").build()
		  DBContainer container = query.getContainer()
		  container.set("CTCONO", iCONO)
		  container.set("CTDIVI", "")
		  container.set("CTSTCO", "TEPY")
		  container.set("CTSTKY", iTEPY.toString().trim())
		  container.set("CTLNCD", iLNCD.toString().trim())

		  if (query.read(container)) {
			iPARM = container.getString("CTPARM").trim()
			logger.debug("iPARM for TEPY value is " + iPARM)
		  } else {
			mi.error("Invalid Payment terms " + iTEPY)
			return false
		  }

		  // Move the content of iPARM to the DS for Payment Terms [DSTEPY]

		  DSTEPY = ""
		  if (iPARM != null) {
			DSTEPY = iPARM
			logger.debug("iPARM TEPY is  " + iPARM)
			logger.debug("DSTEPY is  " + DSTEPY)
		  }

		  // Extracting substrings based on positions same as MAK DS DSTEPY
		  String YLTEL1 = DSTEPY.substring(0, 35)
		  logger.debug("YLTEL1 is  " + YLTEL1)
		  String YLTEL2 = DSTEPY.substring(36, 71)
		  logger.debug("YLTEL2 is  " + YLTEL2)
		  String YLSDAY = DSTEPY.substring(72, 73)
		  logger.debug("YLSDAY is  " + YLSDAY)
		  String YLTEFM = DSTEPY.substring(74, 75)
		  logger.debug("YLTEFM is  " + YLTEFM)
		  String YLTEPD = DSTEPY.substring(76, 78)
		  logger.debug("YLTEPD is  " + YLTEPD)
		  String YLPDAY = DSTEPY.substring(79, 80)
		  logger.debug("YLPDAY is  " + YLPDAY)
		  String YLCAPY = DSTEPY.substring(81, 81)
		  logger.debug("YLCAPY is  " + YLCAPY)
		  String YLEFYN = DSTEPY.substring(82, 82)
		  logger.debug("YLEFYN is  " + YLEFYN)
		  String YLCCOD = DSTEPY.substring(83, 83)
		  logger.debug("YLCCOD is  " + YLCCOD)
		  String YLUOWR = DSTEPY.substring(84, 84)
		  logger.debug("YLUOWR is  " + YLUOWR)
		  String YLPAYP = DSTEPY.substring(85, 85)
		  logger.debug("YLPAYP is  " + YLPAYP)
		  String YLDTIR = DSTEPY.substring(86, 95)
		  logger.debug("YLDTIR is  " + YLDTIR)
		  String YLEICY = DSTEPY.substring(96, 96)
		  logger.debug("YLEICY is  " + YLEICY)
		  String YLCSPC = DSTEPY.substring(97, 97)
		  logger.debug("YLCSPC is  " + YLCSPC)

		  VARPAYP = YLPAYP

		  // Future rate agreement is not allowed if paymentproposal is activatet on payment terms
		  if (YLPAYP == "1" && !iFECN.isBlank()) { // [CRS075/E - PAYMENT PROPOSAL]
			mi.error("Future rate agreement number is not allowed when payment proposal is activated on payment term")
			return false
		  }

		  DBAction queryOOTYPE = database.table("OOTYPE").index("00").selection("OOORTK", "OOOT35").build()
		  DBContainer containerOOTYPE = queryOOTYPE.getContainer()
		  containerOOTYPE.set("OOCONO", iCONO)
		  containerOOTYPE.set("OOORTP", iORTP.toString().trim())

		  if (queryOOTYPE.read(containerOOTYPE)) {
			iORTK = containerOOTYPE.getChar("OOORTK")
			iOT35 = containerOOTYPE.getInt("OOOT35")
			logger.debug("iORTP value is" + iORTK + "iOT35 values is " + iOT35)
		  } else {
			mi.error("Invalid Order Type " + iORTP)
			return false
		  }

		  if (YLCAPY != "1") { // [CRS075/E - CASH PAYMENT]
			if (iORTK == 5 || iCUTP == 7) {
			  mi.error("Cash payment" + YLCAPY + "is invalid")
			  return false
			}
		  }

		  if (!iPYCD.trim().isEmpty()) {
			  if (!iPYCD.trim().equals("?")) {
				DBAction queryPYCD = database.table("CSYTAB").index("00").selection("CTPARM").build()
				DBContainer containerPYCD = queryPYCD.getContainer()
				containerPYCD.set("CTCONO", iCONO)
				containerPYCD.set("CTDIVI", "")
				containerPYCD.set("CTSTCO", "PYCD")
				containerPYCD.set("CTSTKY", iPYCD.toString().trim())
				containerPYCD.set("CTLNCD", "")
				if (queryPYCD.read(containerPYCD)) {
				  iPARM = ''
				  iPARM = containerPYCD.get("CTPARM")
				  logger.debug("iPARM for PYCD value is " + iPARM)
				} else {
				  mi.error("Invalid Payment method")
				  return false
				}

				// Move the content of iPARM to the DS for Payment method [DSPYCD]

				String DSPYCD = ""
				DSPYCD = iPARM

				String FPPYTP = DSPYCD.substring(0, 1)
				String FPQACE = DSPYCD.substring(2, 2)
				String FPQCTA = DSPYCD.substring(3, 3)
				String FPQCTP = DSPYCD.substring(4, 4)
				String FPQTAP = DSPYCD.substring(5, 5)
				String FPNBID = DSPYCD.substring(6, 6)
				String FPDTVP = DSPYCD.substring(7, 7)
				String FPDRRF = DSPYCD.substring(8, 8)
				String FPBOPC = DSPYCD.substring(9, 10)
				String FPLECP = DSPYCD.substring(11, 11)
				String FPBICT = DSPYCD.substring(12, 12)
				String FPFNCN = DSPYCD.substring(13, 15)
				String FPPDDY = DSPYCD.substring(16, 17)
				String FPPRDO = DSPYCD.substring(18, 18)
				String FPELET = DSPYCD.substring(19, 28)
				String FPDOCU = DSPYCD.substring(29, 38)
				String FPCRDC = DSPYCD.substring(39, 39)
				String FPCNVM = DSPYCD.substring(40, 40)
				String FPTEPY = DSPYCD.substring(41, 43)
				String FPTECD = DSPYCD.substring(44, 46)
				String FPACNB = DSPYCD.substring(47, 52)
				String FPPADV = DSPYCD.substring(53, 53)
				String FPPBSB = DSPYCD.substring(54, 54)
				String FPGIRT = DSPYCD.substring(55, 55)
				String FPBBIN = DSPYCD.substring(56, 56)
				String FPADRT = DSPYCD.substring(57, 58)
				String FPADID = DSPYCD.substring(59, 64)
				String FPEGCD = DSPYCD.substring(65, 65)
				String FPSBAG = DSPYCD.substring(66, 66)
				String FPCAOP = DSPYCD.substring(67, 67)
				String FPCNET = DSPYCD.substring(68, 68)

				String VARCRDC = FPCRDC

				if (FPCRDC == "1") { // Credit card
				  if (YLSDAY != "0" ||
					YLTEFM != "0" ||
					YLTEPD != "0" ||
					YLPDAY != "0" ||
					YLEFYN != "0" ||
					YLCAPY == "2") {

					mi.error("Payment terms" + iTEPY + "is invalid if credit card used as payment")
					return false
				  }
				}
			}
		  }
		}

    }

    //Validate Cash discount term
    if (!iTECD.trim().isEmpty()) {
		  if (!iTECD.trim().equals("?")) {
		  DBAction queryTECD = database.table("CSYTAB").index("00").selection("CTPARM").build()
		  DBContainer containerTECD = queryTECD.getContainer()
		  containerTECD.set("CTCONO", iCONO)
		  containerTECD.set("CTDIVI", "")
		  containerTECD.set("CTSTCO", "TECD")
		  containerTECD.set("CTSTKY", iTECD.toString().trim())
		  containerTECD.set("CTLNCD", iLNCD.toString().trim())
		  if (queryTECD.read(containerTECD)) {
			iPARM = ''
			iPARM = containerTECD.get("CTPARM")
			logger.debug("iPARM for TECD value is " + iPARM)
		  } else {
			mi.error("Invalid Cash discount term")
			return false
		  }

		  //Validate cash discount term
		  if ((iOT35 == 3 || iOT35 == 4 || iOT35 == 5) &&
			VARPAYP == "1" && !iTECD.isBlank()) {
			mi.error("Cash discount may not be used when the payment proposal is activated.")
			return false
		  }

		  // Move the content of iPARM to the DS for Cash discount term [DSTECD]

		  String DSTECD = ""
		  DSTECD = iPARM

		  String CDTEL1 = DSTECD.substring(0, 35)
		  logger.debug("CDTEL1 is  " + CDTEL1)
		  String CDTEL2 = DSTECD.substring(36, 71)
		  logger.debug("CDTEL2 is  " + CDTEL2)
		  String CDCDDY = DSTECD.substring(72, 74)
		  logger.debug("CDCDDY is  " + CDCDDY)
		  String CDCDPC = DSTECD.substring(75, 78)
		  logger.debug("CDCDPC is  " + CDCDPC)
		  String CDCDD2 = DSTECD.substring(79, 81)
		  logger.debug("CDCDD2 is  " + CDCDD2)
		  String CDCDP2 = DSTECD.substring(82, 85)
		  logger.debug("CDCDP2 is  " + CDCDP2)
		  String CDCDD3 = DSTECD.substring(86, 88)
		  logger.debug("CDCDD3 is  " + CDCDD3)
		  String CDCDP3 = DSTECD.substring(89, 92)
		  logger.debug("CDCDP3 is  " + CDCDP3)
		  String CDCDTI = DSTECD.substring(93, 95)
		  String CDCDTO = DSTECD.substring(96, 98)
		  String CDTEF1 = DSTECD.substring(99, 100)
		  String CDSDA1 = DSTECD.substring(101, 102)
		  String CDPDA1 = DSTECD.substring(103, 104)
		  String CDTEF2 = DSTECD.substring(105, 106)
		  String CDSDA2 = DSTECD.substring(107, 108)
		  String CDPDA2 = DSTECD.substring(109, 110)
		  String CDTEF3 = DSTECD.substring(111, 112)
		  String CDSDA3 = DSTECD.substring(113, 114)
		  String CDPDA3 = DSTECD.substring(115, 116)
		  String CDCDAY = DSTECD.substring(117, 119)
		  String CDDTIR = DSTECD.substring(120, 129)

		  if (VARCRDC == "1") { // Credit card
			if (CDCDDY != "0" ||
			  CDCDPC != "0" ||
			  CDCDD2 != "0" ||
			  CDCDP2 != "0" ||
			  CDCDD3 != "0" ||
			  CDCDP3 != "0") {
			  mi.error("Cash discount term" + iTECD + "is invalid if credit card used as payment")
			  return false
			}
		  }
		}

    }
    //Validate Delivery Method
    if (!iMODL.trim().isEmpty()) {
	  if (!iMODL.trim().equals("?")) {
      DBAction queryMODL = database.table("CSYTAB").index("00").selection("CTPARM").build()
      DBContainer containerMODL = queryMODL.getContainer()
      containerMODL.set("CTCONO", iCONO)
      containerMODL.set("CTDIVI", "")
      containerMODL.set("CTSTCO", "MODL")
      containerMODL.set("CTSTKY", iMODL.toString().trim())
      containerMODL.set("CTLNCD", iLNCD.toString().trim())
      if (queryMODL.read(containerMODL)) {
        iPARM = ''
        iPARM = containerMODL.get("CTPARM")
        logger.debug("iPARM for MODL value is " + iPARM)
      } else {
        mi.error("Invalid Delivery method")
        return false
      }
	  }
    }

    //Validate Delivery Terms
    if (!iTEDL.trim().isEmpty()) {
	  if (!iTEDL.trim().equals("?")) {
      DBAction queryTEDL = database.table("CSYTAB").index("00").selection("CTPARM").build()
      DBContainer containerTEDL = queryTEDL.getContainer()
      containerTEDL.set("CTCONO", iCONO)
      containerTEDL.set("CTDIVI", "")
      containerTEDL.set("CTSTCO", "TEDL")
      containerTEDL.set("CTSTKY", iTEDL.toString().trim())
      containerTEDL.set("CTLNCD", iLNCD.toString().trim())
      if (queryTEDL.read(containerTEDL)) {
        iPARM = ''
        iPARM = containerTEDL.get("CTPARM")
        logger.debug("iPARM for TEDL value is " + iPARM)
      } else {
        mi.error("Invalid Delivery Terms")
        return false
      }
	  }

    }

    if (!iINRC.isBlank()) {
	  if (!iINRC.trim().equals("?")) {
      //Validate Invoice recipient
      params = ["CONO": iCONO.toString().trim(), "CUNO": iINRC.toString().trim()]
      callback = {
        Map < String,
        String > response ->
        if (response.CUNO == null) {
          mi.error("Invalid Invoice Recipient " + iINRC)
          return false
        } else {
          // If Order number is valid, fetch status and customer type
          iSTAT = response.STAT
          logger.debug("Status is  " + iSTAT)
          iCUTP = Integer.parseInt(response.CUTP)
          logger.debug("Customer Type is  " + iCUTP)
        }
      }
      miCaller.call("CRS610MI", "GetBasicData", params, callback)
	  
	  }
    }

    //Other validations of INRC from OOHEADPI
    if (!iINRC.trim().isEmpty()) {
	  if (!iINRC.trim().equals("?")) {
      DBAction queryINRC = database.table("OOHEAD").index("00").build()
      DBContainer containerINRC = queryINRC.createContainer()
      containerINRC.set("OACONO", iCONO)
      containerINRC.set("OAORNO", iORNO)

      if (queryINRC.read(containerINRC)) {
        if (containerINRC.get("OAINRC") != containerINRC.get("OAPYNO") &&
          containerINRC.get("OAINRC") != containerINRC.get("OACUNO") &&
          iSTAT != "20") {
          mi.error("Invoice Recipient not permitted for status" + iSTAT)
          return false
        }

        if (containerINRC.get("OAINRC") != containerINRC.get("OAPYNO") &&
          containerINRC.get("OAINRC") != containerINRC.get("OACUNO") &&
          iCUTP != 0 && iCUTP != 2) {
          mi.error("Invoice Recipient not permitted for customer type" + iCUTP)
          return false
        }
      }
	  }
    }
    

    

    return true

  }

  /**
   * Updates the OOHEAD table record in the database 
   * <p>
   * Important fields for updating include: OACONO, OAORNO, OATEPY, OAMODL, 
   * OATEDL, OADSTX, OAPYCD, OATECD, OAINRC, OALMDT, OACHID, and OACHNO.
   * </p>
   * If the record doesn't exist, an error message will be shown.
   */
  public updateOOHEAD() {
    DBAction query = database.table("OOHEAD").index("00").build()
    DBContainer container = query.getContainer()
    container.set("OACONO", iCONO)
    container.set("OAORNO", iORNO)
    if (!query.readLock(container, updateCallBack)) {
      mi.error("Record does not Exist.")
      return
    }
  }

  /**
   * Callback closure to handle the field update logic for an OOHEAD record.
   * <p>
   * The closure checks each predefined field. If the field has a certain
   * value, such as "?", it's cleared. Otherwise, the field is set to the 
   * corresponding value.
   * </p>
   * Some fields, like OALMDT, OACHID, and OACHNO, have special update logic 
   * regardless of the field values.
   *
   * @param lockedResult The database record to be updated. This record is 
   *                     expected to be locked for update.
   */

  Closure < ? > updateCallBack = {
    LockedResult lockedResult ->

    if (!iTEPY.trim().isEmpty()) {
      if (iTEPY.trim().equals("?")) {
        lockedResult.set("OATEPY", "")
      } else {
        lockedResult.set("OATEPY", iTEPY)
        TEPYUpdate = true
      }
    }

    if (!iMODL.trim().isEmpty()) {
      if (iMODL.trim().equals("?")) {
        lockedResult.set("OAMODL", "")
      } else {
        lockedResult.set("OAMODL", iMODL)
        MODLUpdate = true
      }
    }

    if (!iTEDL.trim().isEmpty()) {
      if (iTEDL.trim().equals("?")) {
        lockedResult.set("OATEDL", "")
      } else {
        lockedResult.set("OATEDL", iTEDL)
        TEDLUpdate = true
      }
    }

    if (!iZSTX.trim().isEmpty()) {
      if (iZSTX.trim().equals("?")) {
        lockedResult.set("OADSTX", "")
      } else {
        lockedResult.set("OADSTX", iZSTX)
      }
    }

    if (!iPYCD.trim().isEmpty()) {
      if (iPYCD.trim().equals("?")) {
        lockedResult.set("OAPYCD", "")
      } else {
        lockedResult.set("OAPYCD", iPYCD)
      }
    }

    if (!iTECD.trim().isEmpty()) {
      if (iTECD.trim().equals("?")) {
        lockedResult.set("OATECD", "")
      } else {
        lockedResult.set("OATECD", iTECD)
      }
    }

    if (!iINRC.trim().isEmpty()) {
      if (iINRC.trim().equals("?")) {
        lockedResult.set("OAINRC", "")
      } else {
        lockedResult.set("OAINRC", iINRC)
      }
    }
    
    if (!iOPRI.equals(100)) {
        lockedResult.set("OAOPRI", iOPRI)
    }

    lockedResult.set("OALMDT", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger())
    lockedResult.set("OACHID", program.getUser())
    lockedResult.set("OACHNO", lockedResult.getInt("OACHNO") + 1)
    lockedResult.update()
  }

  /**
   * Queries and updates records from the OOLINE table based on the provided 
   * selection criteria. This method specifically targets records matching the 
   * OBCONO and OBORNO fields. If matching records are found, they are processed 
   * using the {@code getOOLINE} closure.
   * 
   * Fields of interest for the selection include: OBCONO and OBORNO.
   */

  public updateOOLINE() {
    ExpressionFactory expression = database.getExpressionFactory("OOLINE")

    DBAction query = database.table("OOLINE").selection("OBCONO", "OBORNO").build()
    DBContainer container = query.getContainer()
    container.set("OBCONO", iCONO)
    container.set("OBORNO", iORNO)
    query.readAll(container, 2, pageSize, getOOLINE)

  }

  /** 
   * Closure to process records retrieved from the OOLINE table. The closure
   * extracts the OBPONR and OBPOSX fields from each record. If the OBPONR field
   * is non-null, additional processing and updates are performed on the record
   * using the {@code updateMODL_TEDL_TEPY} method.
   *
   * @param container The database record container that provides access to
   *                  fields of the current record being processed.
   */

  Closure < ? > getOOLINE = {

    DBContainer container ->
    String PONR = container.get("OBPONR").toString()
    String POSX = container.get("OBPOSX").toString()

    logger.debug("POSX" + POSX)
    logger.debug("PONR" + PONR)

    if (PONR != null) {
      updateMODL_TEDL_TEPY(PONR, POSX)

    }

  }

  /**
   * Updates specific fields of an OOLINE table record based on provided
   * order line number and order sub-line number. The fields to be updated 
   * are determined by global flags such as MODLUpdate, TEDLUpdate, and TEPYUpdate.
   * If the respective flags are set, the related fields in the record will be
   * updated accordingly.
   * 
   * @param PONR The order line number. Expected to be a string representation
   *             of an integer.
   * @param POSX The order sub-line number. Expected to be a string representation
   *             of an integer.
   */

  public updateMODL_TEDL_TEPY(String PONR, String POSX) {
    DBAction query = database.table("OOLINE").index("00").build()
    DBContainer container = query.getContainer()
    container.set("OBCONO", iCONO)
    container.set("OBORNO", iORNO)
    container.set("OBPONR", Integer.parseInt(PONR))
    container.set("OBPOSX", Integer.parseInt(POSX))

    query.readLock(container, updateLineDetails)

  }

  /**
   * Closure to handle the update logic for fields in an OOLINE record.
   * <p>
   * The closure checks certain global flags (MODLUpdate, TEDLUpdate, TEPYUpdate)
   * to determine if the associated fields should be updated. If the related flag
   * is set and the field has a particular value, like "?", it's cleared. 
   * Otherwise, the field is updated with the corresponding global value.
   * </p>
   * Additional fields such as OBLMDT, OBCHID, and OBCHNO are always updated 
   * with specific logic.
   *
   * @param lockedResult The database record to be updated. This record is 
   *                     expected to be locked for update.
   */

  Closure < ? > updateLineDetails = {
    LockedResult lockedResult ->

    if (MODLUpdate) {
      if (!iMODL.trim().isEmpty()) {
        if (iMODL.trim().equals("?")) {
          lockedResult.set("OBMODL", "")
        } else {
          lockedResult.set("OBMODL", iMODL)
        }
      }
    }

    if (TEDLUpdate) {
      if (!iTEDL.trim().isEmpty()) {
        if (iTEDL.trim().equals("?")) {
          lockedResult.set("OBTEDL", "")
        } else {
          lockedResult.set("OBTEDL", iTEDL)

        }
      }
    }

    if (TEPYUpdate) {
      if (!iTEPY.trim().isEmpty()) {
        if (iTEPY.trim().equals("?")) {
          lockedResult.set("OBTEPY", "")
        } else {
          lockedResult.set("OBTEPY", iTEPY)

        }
      }
    }
    
    //Update priority in OOLINE
    if (!iOPRI.equals(100)) {
        lockedResult.set("OBPRIO", iOPRI)
    }
    

    lockedResult.set("OBLMDT", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger())
    lockedResult.set("OBCHID", program.getUser())
    lockedResult.set("OBCHNO", lockedResult.getInt("OBCHNO") + 1)
    lockedResult.update()

  }

  /**
   * Queries and processes records from the MHDISH table based on the 
   * provided order number (iORNO). This method specifically targets records 
   * matching the OQRIDN field. If matching records are found, they are 
   * processed using the {@code getMHDISH} closure.
   */

  public updateMHDISH() {
    ExpressionFactory expression = database.getExpressionFactory("MHDISH")
    expression = expression.eq("OQRIDN", iORNO.toString().trim())
    DBAction query = database.table("MHDISH").matching(expression).build()
    DBContainer container = query.getContainer()
    container.set("OQCONO", iCONO)
    query.readAll(container, 1,pageSize, getMHDISH)

  }

  /**
   * Closure to process records retrieved from the MHDISH table. The closure
   * extracts the OQINOU and OQDLIX fields from each record. If the OQDLIX field
   * is non-null, additional processing and updates are performed on the record
   * using the {@code updateMODL_TEDL} method. If OQPGRS > 50 then error
   *
   * @param container The database record container that provides access to
   *                  fields of the current record being processed.
   */

  Closure < ? > getMHDISH = {

    DBContainer container ->

    String oqpgrsValue = container.get("OQPGRS").toString().trim()
    logger.debug("OQPGRS value is " +  oqpgrsValue)

    // Compare the string values directly
    if (oqpgrsValue.compareTo("50") >= 0) {
      mi.error("There are records with OQPGRS value greater than 50. Update is not allowed.")
      return false
    }

    String INOU = container.get("OQINOU").toString()
    String DLIX = container.get("OQDLIX").toString()

    logger.debug("INOU" + INOU)
    logger.debug("DLIX" + DLIX)

    if (DLIX != null) {
      updateMODL_TEDL(INOU, DLIX)

    }

  }

  /**
   * Updates specific fields of an MHDISH table record based on provided 
   * internal order number (INOU) and delivery index (DLIX). The fields to 
   * be updated are determined by global flags such as MODLUpdate and TEDLUpdate.
   * If the respective flags are set, the related fields in the record will be
   * updated accordingly.
   * 
   * @param INOU The internal order number. Expected to be a string representation
   *             of an integer.
   * @param DLIX The delivery index. Expected to be a string representation
   *             of an integer.
   */

  public updateMODL_TEDL(String INOU, String DLIX) {
    DBAction query = database.table("MHDISH").index("00").build()
    DBContainer container = query.getContainer()
    container.set("OQCONO", iCONO)
    container.set("OQINOU", Integer.parseInt(INOU))
    container.set("OQDLIX", Long.parseLong(DLIX))

    query.readLock(container, updateDlixDetails)

  }

  /**
   * Closure to handle the update logic for fields in an MHDISH record.
   * <p>
   * The closure checks certain global flags (MODLUpdate and TEDLUpdate)
   * to determine if the associated fields should be updated. If the related flag
   * is set and the field has a particular value, like "?", both the field and
   * its corresponding flag counterpart are cleared. Otherwise, the field is 
   * updated with the corresponding global value.
   * </p>
   * Additional fields like OQTRDT, OQLMDT, OQTRTM, OQCHID, and OQCHNO are 
   * always updated with specific logic.
   *
   * @param lockedResult The database record to be updated. This record is 
   *                     expected to be locked for update.
   */
  Closure < ? > updateDlixDetails = {
    LockedResult lockedResult ->

    if (MODLUpdate) {

      if (!iMODL.trim().isEmpty()) {
        if (iMODL.trim().equals("?")) {
          lockedResult.set("OQMODL", "")
          lockedResult.set("OQMODF", "")
        } else {
          lockedResult.set("OQMODL", iMODL)
          lockedResult.set("OQMODF", iMODL)

        }
      }

    }

    if (TEDLUpdate) {
      if (!iTEDL.trim().isEmpty()) {
        if (iTEDL.trim().equals("?")) {
          lockedResult.set("OQTEDL", "")
          lockedResult.set("OQTEDF", "")
        } else {
          lockedResult.set("OQTEDL", iTEDL)
          lockedResult.set("OQTEDF", iTEDL)

        }
      }
    }
    
    //Update priority in MHDISH
    if (!iOPRI.equals(100)) {
        lockedResult.set("OQPRIO", iOPRI)
    }
    

    lockedResult.set("OQTRDT", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger())
    lockedResult.set("OQLMDT", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInteger())
    lockedResult.set("OQTRTM", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")).toInteger())
    lockedResult.set("OQCHID", program.getUser())
    lockedResult.set("OQCHNO", lockedResult.getInt("OQCHNO") + 1)
    lockedResult.update()

  }

}