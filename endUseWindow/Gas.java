package endUseWindow;

import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JOptionPane;


public class Gas extends Source{
	String sourceID;
	String sourceName;
	String notes;

	
	
	public Gas(Site site,String sourceID,String sourceName,String notes){
		super(site,sourceID,sourceName,"Gas","");
		this.sourceID = (sourceID==null?"":sourceID);
		this.sourceName = (sourceName==null?"":sourceName);
		this.notes = (notes==null?"":notes);
	}
	
	public boolean isValid(){
		boolean isValid = false;
		if (sourceID.matches("^\\d{1,10}$")){
			if (sourceName.matches("^[\\w\\s\\-\\.\\(\\)/]{0,20}$")){
				if (notes.matches("^[\\w\\s]{0,255}$") || notes.equals("")){
					isValid = true;
				}
				else{
					JOptionPane.showMessageDialog(null,"The notes provided are invalid.\r\n Max 255 Alphnumeric characters.","Appliance Information Invalid",JOptionPane.WARNING_MESSAGE);
				}
			}
			else{
				JOptionPane.showMessageDialog(null,"The Source Name provided is invalid.\r\nMax 16 Alphnumeric characters.","Appliance Information Invalid",JOptionPane.WARNING_MESSAGE);
			}
		}
		else{
			JOptionPane.showMessageDialog(null,"The Source ID provided is invalid.\r\nNumeric characters.","Appliance Information Invalid",JOptionPane.WARNING_MESSAGE);
		}
		return isValid;
	}
	
	public static boolean addGas(Statement MySQL_Statement,LogWindow logWindow,String siteID,Gas gas) throws SQLException{
		if (gas.isValid()){
			try{
				String updGasSQL = "INSERT INTO gas (site_id,source_id,notes) VALUES("+siteID+","+gas.getSourceID()+","+(gas.getNotes().equals("")?"NULL":"'"+gas.getNotes()+"'")+")"; //adds specified information into the database
				MySQL_Statement.executeUpdate(updGasSQL);
				
				return true;
			}catch(SQLException sE){
				Source.removeSource(MySQL_Statement,siteID,gas.sourceID);
				sE.printStackTrace();
				logWindow.println("Error occured when writing gas information.");
				throw new SQLException();
			}
		}
		else{
			Source.removeSource(MySQL_Statement,siteID,gas.sourceID);
			logWindow.println("Error occured when writing gas information.");
			throw new SQLException();
		}
	}
	
	public String getSourceID(){
		return sourceID;
	}
	
	public String getSourceName(){
		return sourceName;
	}
	
	public String getNotes(){
		return notes;
	}
}
