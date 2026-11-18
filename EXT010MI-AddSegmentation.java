/**
 * README
 * This API is developed in a manner that it will uadd segmentation
 *
 * Name: EXT010MI.AddSegmentation
 * Date	      Changed By                   Description
 * 2021	  SwatiY@fortude.co       Add segmentation to CPNMAS
 */

import java.time.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter


public class AddSegmentation extends ExtendM3Transaction {
  private final MIAPI mi
  private final ProgramAPI program
  private final DatabaseAPI database
  public String iPNOD
  public String iTX15
  public String iTX40
  public String iCONO
  public String iCHID

  public AddSegmentation(MIAPI mi, ProgramAPI program, DatabaseAPI database) {
    this.mi = mi
    this.program = program
    this.database = database
  }

  public void main() {
    iPNOD = mi.inData.get("PNOD").toString()
    iTX15 = mi.inData.get("TX15").toString()
    iTX40 = mi.inData.get("TX40").toString()
    iCONO = mi.inData.get("CONO").toString()
    iCHID  = program.getUser()

    //Add segmentation code
    addSegmentationCode()

  }

  Optional < DBContainer > addSegmentationCode() {

    DBAction newSegmentation = database.table("CPNMAS").index("00").build()
    DBContainer cPNMAS = newSegmentation.createContainer()
    cPNMAS.set("PNPNOD", iPNOD)
    cPNMAS.set("PNTX15", iTX15)
    cPNMAS.set("PNTX40", iTX40)
    cPNMAS.set("PNCONO", Integer.valueOf(iCONO))
    cPNMAS.set("PNCHID", iCHID)
    cPNMAS.set("PNCHNO", Integer.valueOf("1"))
    cPNMAS.set("PNRGDT", Integer.valueOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))))
    cPNMAS.set("PNRGTM", Integer.valueOf(LocalTime.now().format(DateTimeFormatter.ofPattern("hhmmss"))))
    cPNMAS.set("PNLMDT", Integer.valueOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))))


    // execute add query
    newSegmentation.insert(cPNMAS)
    return Optional.of(cPNMAS)

  }

}