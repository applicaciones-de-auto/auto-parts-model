/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.auto.model.parts;

import java.lang.reflect.Method;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import javax.sql.rowset.CachedRowSet;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.iface.GEntity;
import org.json.simple.JSONObject;

/**
 *
 * @author Arsiela
 */
public class Model_Inventory_Information implements GEntity {

    final String XML = "Model_Inventory_Master.xml";

    GRider poGRider;                //application driver
    CachedRowSet poEntity;          //rowset
    JSONObject poJSON;              //json container
    int pnEditMode;                 //edit mode

    /**
     * Entity constructor
     *
     * @param foValue - GhostRider Application Driver
     */
    public Model_Inventory_Information(GRider foValue) {
        if (foValue == null) {
            System.err.println("Application Driver is not set.");
            System.exit(1);
        }

        poGRider = foValue;

        initialize();
    }
    
    private void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);
            poEntity.updateString("cRecdStat", RecordStatus.ACTIVE);

            poEntity.insertRow();
            poEntity.moveToCurrentRow();

            poEntity.absolute(1);

            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Gets the column index name.
     *
     * @param fnValue - column index number
     * @return column index name
     */
    @Override
    public String getColumn(int fnValue) {
        try {
            return poEntity.getMetaData().getColumnLabel(fnValue);
        } catch (SQLException e) {
        }
        return "";
    }

    /**
     * Gets the column index number.
     *
     * @param fsValue - column index name
     * @return column index number
     */
    @Override
    public int getColumn(String fsValue) {
        try {
            return MiscUtil.getColumnIndex(poEntity, fsValue);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Gets the total number of column.
     *
     * @return total number of column
     */
    @Override
    public int getColumnCount() {
        try {
            return poEntity.getMetaData().getColumnCount();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }

    @Override
    public int getEditMode() {
        return pnEditMode;
    }

    @Override
    public String getTable() {
        return "inventory";
    }

    /**
     * Gets the value of a column index number.
     *
     * @param fnColumn - column index number
     * @return object value
     */
    @Override
    public Object getValue(int fnColumn) {
        try {
            return poEntity.getObject(fnColumn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets the value of a column index name.
     *
     * @param fsColumn - column index name
     * @return object value
     */
    @Override
    public Object getValue(String fsColumn) {
        try {
            return poEntity.getObject(MiscUtil.getColumnIndex(poEntity, fsColumn));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Sets column value.
     *
     * @param fnColumn - column index number
     * @param foValue - value
     * @return result as success/failed
     */
    @Override
    public JSONObject setValue(int fnColumn, Object foValue) {
        try {
            poJSON = MiscUtil.validateColumnValue(System.getProperty("sys.default.path.metadata") + XML, MiscUtil.getColumnLabel(poEntity, fnColumn), foValue);
            if ("error".equals((String) poJSON.get("result"))) {
                return poJSON;
            }

            poEntity.updateObject(fnColumn, foValue);
            poEntity.updateRow();

            poJSON = new JSONObject();
            poJSON.put("result", "success");
            poJSON.put("value", getValue(fnColumn));
        } catch (SQLException e) {
            e.printStackTrace();
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        }

        return poJSON;
    }

    /**
     * Sets column value.
     *
     * @param fsColumn - column index name
     * @param foValue - value
     * @return result as success/failed
     */
    @Override
    public JSONObject setValue(String fsColumn, Object foValue) {
        poJSON = new JSONObject();

        try {
            return setValue(MiscUtil.getColumnIndex(poEntity, fsColumn), foValue);
        } catch (SQLException e) {
            e.printStackTrace();
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        }
        return poJSON;
    }

    /**
     * Set the edit mode of the entity to new.
     *
     * @return result as success/failed
     */
    @Override
    public JSONObject newRecord() {
        pnEditMode = EditMode.ADDNEW;

        //replace with the primary key column info
        setStockID(MiscUtil.getNextCode(getTable(), "sStockIDx", true, poGRider.getConnection(), poGRider.getBranchCode()));

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        return poJSON;
    }

    /**
     * Opens a record.
     *
     * @param fsCondition - filter values
     * @return result as success/failed
     */
    @Override
    public JSONObject openRecord(String fsCondition) {
        poJSON = new JSONObject();

        String lsSQL = MiscUtil.makeSelect(this);

        //replace the condition based on the primary key column of the record
        lsSQL = MiscUtil.addCondition(lsSQL, " sStockIDx = " + SQLUtil.toSQL(fsCondition));

        ResultSet loRS = poGRider.executeQuery(lsSQL);

        try {
            if (loRS.next()) {
                for (int lnCtr = 1; lnCtr <= loRS.getMetaData().getColumnCount(); lnCtr++) {
                    setValue(lnCtr, loRS.getObject(lnCtr));
                }

                pnEditMode = EditMode.UPDATE;

                poJSON.put("result", "success");
                poJSON.put("message", "Record loaded successfully.");
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "No record to load.");
            }
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        }

        return poJSON;
    }

    /**
     * Save the entity.
     *
     * @return result as success/failed
     */
    @Override
    public JSONObject saveRecord() {
        poJSON = new JSONObject();

        if (pnEditMode == EditMode.ADDNEW || pnEditMode == EditMode.UPDATE) {
            String lsSQL;
            if (pnEditMode == EditMode.ADDNEW) {
                //replace with the primary key column info
                setStockID(MiscUtil.getNextCode(getTable(), "sStockIDx", true, poGRider.getConnection(), poGRider.getBranchCode()));

                lsSQL = makeSQL();

                if (!lsSQL.isEmpty()) {
                    if (poGRider.executeQuery(lsSQL, getTable(), poGRider.getBranchCode(), "") > 0) {
                        poJSON.put("result", "success");
                        poJSON.put("message", "Record saved successfully.");
                    } else {
                        poJSON.put("result", "error");
                        poJSON.put("message", poGRider.getErrMsg());
                    }
                } else {
                    poJSON.put("result", "error");
                    poJSON.put("message", "No record to save.");
                }
            } else {
                Model_Inventory_Information loOldEntity = new Model_Inventory_Information(poGRider);

                //replace with the primary key column info
                JSONObject loJSON = loOldEntity.openRecord(this.getStockID());

                if ("success".equals((String) loJSON.get("result"))) {
                    //replace the condition based on the primary key column of the record
                    lsSQL = MiscUtil.makeSQL(this, loOldEntity, "sStockIDx = " + SQLUtil.toSQL(this.getStockID()));

                    if (!lsSQL.isEmpty()) {
                        if (poGRider.executeQuery(lsSQL, getTable(), poGRider.getBranchCode(), "") > 0) {
                            poJSON.put("result", "success");
                            poJSON.put("message", "Record saved successfully.");
                        } else {
                            poJSON.put("result", "error");
                            poJSON.put("message", poGRider.getErrMsg());
                        }
                    } else {
                        poJSON.put("result", "success");
                        poJSON.put("message", "No updates has been made.");
                    }
                } else {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Record discrepancy. Unable to save record.");
                }
            }
        } else {
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid update mode. Unable to save record.");
            return poJSON;
        }

        return poJSON;
    }

    /**
     * Prints all the public methods used<br>
     * and prints the column names of this entity.
     */
    @Override
    public void list() {
        Method[] methods = this.getClass().getMethods();

        System.out.println("--------------------------------------------------------------------");
        System.out.println("LIST OF PUBLIC METHODS FOR " + this.getClass().getName() + ":");
        System.out.println("--------------------------------------------------------------------");
        for (Method method : methods) {
            System.out.println(method.getName());
        }

        try {
            int lnRow = poEntity.getMetaData().getColumnCount();

            System.out.println("--------------------------------------------------------------------");
            System.out.println("ENTITY COLUMN INFO");
            System.out.println("--------------------------------------------------------------------");
            System.out.println("Total number of columns: " + lnRow);
            System.out.println("--------------------------------------------------------------------");

            for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++) {
                System.out.println("Column index: " + (lnCtr) + " --> Label: " + poEntity.getMetaData().getColumnLabel(lnCtr));
                if (poEntity.getMetaData().getColumnType(lnCtr) == Types.CHAR
                        || poEntity.getMetaData().getColumnType(lnCtr) == Types.VARCHAR) {

                    System.out.println("Column index: " + (lnCtr) + " --> Size: " + poEntity.getMetaData().getColumnDisplaySize(lnCtr));
                }
            }
        } catch (SQLException e) {
        }

    }
    
    /**
     * Gets the SQL statement for this entity.
     *
     * @return SQL Statement
     */
    public String makeSQL() {
        return MiscUtil.makeSQL(this, "");
    }
    
    /**
     * Gets the SQL Select statement for this entity.
     *
     * @return SQL Select Statement
     */
    public String makeSelectSQL() {
        return MiscUtil.makeSelect(this);
    }
    
    private String getSQL() {                                                              
        return " SELECT "                                                              
                    + " a.sStockIDx "                                                  
                    + " ,a.sBarCodex "                                                 
                    + " ,a.sDescript "                                                 
                    + " , IFNULL(a.sBriefDsc, '') sBriefDsc "                          
                    + " , IFNULL(a.sCategCd1, '') sCategCd1 "                          
                    + " , IFNULL(a.sCategCd2, '') sCategCd2 "                          
                    + " , IFNULL(a.sCategCd3, '') sCategCd3 "                          
                    + " , IFNULL(a.sCategCd4, '') sCategCd4 "                          
                    + " , IFNULL(a.sModelCde, '') sModelCde "                          
                    + " , IFNULL(a.sMeasurID, '') sMeasurID "                          
                    + " , IFNULL(a.sInvTypCd, '') sInvTypCd "                          
                    + " ,a.nUnitPrce "                                                 
                    + " ,a.nSelPrice "                                                 
                    + " ,a.nDiscLev1 "                                                 
                    + " ,a.nDiscLev2 "                                                 
                    + " ,a.nDiscLev3 "                                                 
                    + " ,a.nDealrDsc "                                                 
                    + " , IFNULL(a.cComboInv, '') cComboInv "                          
                    + " , IFNULL(a.cWthPromo, '') cWthPromo "                          
                    + " , IFNULL(a.cUnitType, '') cUnitType "                          
                    + " , IFNULL(a.cInvStatx, '') cInvStatx "                          
                    + " , IFNULL(a.cGenuinex, '') cGenuinex "                          
                    + " , IFNULL(a.cReplacex, '') cReplacex "                          
                    + " , IFNULL(a.sSupersed, '') sSupersed "                          
                    + " , IFNULL(a.sFileName, '') sFileName "                          
                    + " , IFNULL(a.sTrimBCde, '') sTrimBCde "                          
                    + " , IFNULL(a.cRecdStat, '') cRecdStat "                          
                    + " , IFNULL(a.sModified, '') sModified "                          
                    + " ,a.dModified "                                             
                    + " , IFNULL(b.sDescript, '') sBrandNme "                          
                    + " , IFNULL(c.sDescript, '') sCategNme "                          
                    + " , IFNULL(d.sMeasurNm, '') sMeasurNm "                          
                    + " , IFNULL(e.sDescript, '') sInvTypNm "                          
                    + " , IFNULL(f.sLocatnID, '') sLocatnID "                          
                    + " , IFNULL(g.sLocatnDs, '') sLocatnDs "                          
                    + " FROM inventory a "                                             
                    + " LEFT JOIN brand b ON b.sStockIDx = a.sStockIDx  "              
                    + " LEFT JOIN inventory_category c ON c.sCategrCd = a.sCategCd1 "  
                    + " LEFT JOIN measure d ON d.sMeasurID = a.sMeasurID   "           
                    + " LEFT JOIN inv_type e ON e.sInvTypCd = a.sInvTypCd  "           
                    + " LEFT JOIN inv_master f on f.sStockIDx = a.sStockIDx  "         
                    + " LEFT JOIN item_location g on g.sLocatnID = f.sLocatnID  ";		 
    }                                                                                  

    /**
     * Description: Sets the ID of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setStockID(String fsValue) {
        return setValue("sStockIDx", fsValue);
    }

    /**
     * @return The ID of this record.
     */
    public String getStockID() {
        return (String) getValue("sStockIDx");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setBarCode(String fsValue) {
        return setValue("sBarCodex", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getBarCode() {
        return (String) getValue("sBarCodex");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setDescript(String fsValue) {
        return setValue("sDescript", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getDescript() {
        return (String) getValue("sDescript");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setBriefDsc(String fsValue) {
        return setValue("sBriefDsc", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getBriefDsc() {
        return (String) getValue("sBriefDsc");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setCategCd1(String fsValue) {
        return setValue("sCategCd1", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getCategCd1() {
        return (String) getValue("sCategCd1");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setCategCd2(String fsValue) {
        return setValue("sCategCd2", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getCategCd2() {
        return (String) getValue("sCategCd2");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setCategCd3(String fsValue) {
        return setValue("sCategCd3", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getCategCd3() {
        return (String) getValue("sCategCd3");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setCategCd4(String fsValue) {
        return setValue("sCategCd4", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getCategCd4() {
        return (String) getValue("sCategCd4");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setModelCde(String fsValue) {
        return setValue("sModelCde", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getModelCde() {
        return (String) getValue("sModelCde");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setMeasurID(String fsValue) {
        return setValue("sMeasurID", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getMeasurID() {
        return (String) getValue("sMeasurID");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setInvTypCd(String fsValue) {
        return setValue("sInvTypCd", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getInvTypCd() {
        return (String) getValue("sInvTypCd");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fnValue
     * @return result as success/failed
     */
    public JSONObject setUnitPrce(Double fnValue) {
        return setValue("nUnitPrce", fnValue);
    }

    /**
     * @return The Value of this record.
     */
    public Double getUnitPrce() {
        return (Double) getValue("nUnitPrce");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fnValue
     * @return result as success/failed
     */
    public JSONObject setSelPrice(Double fnValue) {
        return setValue("nSelPrice", fnValue);
    }

    /**
     * @return The Value of this record.
     */
    public Double getSelPrice() {
        return (Double) getValue("nSelPrice");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fnValue
     * @return result as success/failed
     */
    public JSONObject setDiscLev1(Double fnValue) {
        return setValue("nDiscLev1", fnValue);
    }

    /**
     * @return The Value of this record.
     */
    public Double getDiscLev1() {
        return (Double) getValue("nDiscLev1");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fnValue
     * @return result as success/failed
     */
    public JSONObject setDiscLev2(Double fnValue) {
        return setValue("nDiscLev2", fnValue);
    }

    /**
     * @return The Value of this record.
     */
    public Double getDiscLev2() {
        return (Double) getValue("nDiscLev2");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fnValue
     * @return result as success/failed
     */
    public JSONObject setDiscLev3(Double fnValue) {
        return setValue("nDiscLev3", fnValue);
    }

    /**
     * @return The Value of this record.
     */
    public Double getDiscLev3() {
        return (Double) getValue("nDiscLev3");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fnValue
     * @return result as success/failed
     */
    public JSONObject setDealrDsc(Double fnValue) {
        return setValue("nDealrDsc", fnValue);
    }

    /**
     * @return The Value of this record.
     */
    public Double getDealrDsc() {
        return (Double) getValue("nDealrDsc");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setComboInv(String fsValue) {
        return setValue("cComboInv", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getComboInv() {
        return (String) getValue("cComboInv");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setWthPromo(String fsValue) {
        return setValue("cWthPromo", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getWthPromo() {
        return (String) getValue("cWthPromo");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setUnitType(String fsValue) {
        return setValue("cUnitType", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getUnitType() {
        return (String) getValue("cUnitType");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setInvStatx(String fsValue) {
        return setValue("cInvStatx", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getInvStatx() {
        return (String) getValue("cInvStatx");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setGenuinex(String fsValue) {
        return setValue("cGenuinex", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getGenuinex() {
        return (String) getValue("cGenuinex");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setReplacex(String fsValue) {
        return setValue("cReplacex", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getReplacex() {
        return (String) getValue("cReplacex");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setSupersed(String fsValue) {
        return setValue("sSupersed", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getSupersed() {
        return (String) getValue("sSupersed");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setFileName(String fsValue) {
        return setValue("sFileName", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getFileName() {
        return (String) getValue("sFileName");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setTrimBCde(String fsValue) {
        return setValue("sTrimBCde", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getTrimBCde() {
        return (String) getValue("sTrimBCde");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setRecdStat(String fsValue) {
        return setValue("cRecdStat", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String gsetRecdStat() {
        return (String) getValue("cRecdStat");
    }
    
    /**
     * Sets record as active.
     *
     * @param fbValue
     * @return result as success/failed
     */
    public JSONObject setActive(boolean fbValue) {
        return setValue("cRecdStat", fbValue ? "1" : "0");
    }

    /**
     * @return If record is active.
     */
    public boolean isActive() {
        return ((String) getValue("cRecdStat")).equals("1");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setModified(String fsValue) {
        return setValue("sModified", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getModified() {
        return (String) getValue("sModified");
    }
    
    /**
     * Sets the date and time the record was modified.
     *
     * @param fdValue
     * @return result as success/failed
     */
    public JSONObject setModifiedDte(Date fdValue) {
        return setValue("dModified", fdValue);
    }

    /**
     * @return The date and time the record was modified.
     */
    public Date getModifiedDte() {
        return (Date) getValue("dModified");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setBrandNme(String fsValue) {
        return setValue("sBrandNme", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getBrandNme() {
        return (String) getValue("sBrandNme");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setCategNme(String fsValue) {
        return setValue("sCategNme", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getCategNme() {
        return (String) getValue("sCategNme");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setMeasurNm(String fsValue) {
        return setValue("sMeasurNm", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getMeasurNm() {
        return (String) getValue("sMeasurNm");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setInvTypNm(String fsValue) {
        return setValue("sInvTypNm", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getInvTypNm() {
        return (String) getValue("sInvTypNm");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setLocatnID(String fsValue) {
        return setValue("sLocatnID", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getLocatnID() {
        return (String) getValue("sLocatnID");
    }
    
    /**
     * Description: Sets the Value of this record.
     *
     * @param fsValue
     * @return result as success/failed
     */
    public JSONObject setLocatnDs(String fsValue) {
        return setValue("sLocatnDs", fsValue);
    }

    /**
     * @return The Value of this record.
     */
    public String getLocatnDs() {
        return (String) getValue("sLocatnDs");
    }
}


