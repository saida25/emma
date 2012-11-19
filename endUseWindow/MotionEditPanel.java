package endUseWindow;


import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class MotionEditPanel extends JPanel implements ActionListener{
	private static final long serialVersionUID = 2465730538321066498L;
	
	//Appliance Edit Panel
	JLabel siteIDLabel = new JLabel("Site ID:");
	JLabel sourceIDLabel = new JLabel("Source ID:");
	JLabel sourceNameLabel = new JLabel("Source Name:");
	JLabel roomIDLabel = new JLabel("Room ID:");
	JLabel sourceTypeLabel = new JLabel("Source Type:");
	JLabel measurementTypeLabel = new JLabel("MeasurementType:");
	JLabel notesLabel = new JLabel("Notes:");
	JTextField siteIDInput = new JTextField(20); //site id
	JTextField sourceIDInput = new JTextField(20); //source id
	JTextField sourceNameInput = new JTextField(20); //source name
	JComboBox roomIDInput = new JComboBox(); //room id
	JComboBox sourceTypeInput = new JComboBox(Source.getSourceTypeList()); //source type
	JComboBox measurementTypeInput = new JComboBox(Source.getMeasurementList()); //measurement type
	JTextField notesInput = new JTextField(20); //notes
	
	Connection dbConn;
	
	public MotionEditPanel(Connection dbConn){
		this.dbConn = dbConn;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buildGUI();
			}
		});
	}
	
	private void buildGUI(){
		this.setLayout(new GridLayout(16,2));
		
		this.add(siteIDLabel);
		this.add(siteIDInput);
		this.add(sourceIDLabel);
		this.add(sourceIDInput);
		this.add(sourceNameLabel);
		this.add(sourceNameInput);
		this.add(roomIDLabel);
		this.add(roomIDInput);
		this.add(sourceTypeLabel);
		this.add(sourceTypeInput);
		this.add(measurementTypeLabel);
		this.add(measurementTypeInput);
		this.add(notesLabel);
		this.add(notesInput);
		
		
		siteIDInput.setEnabled(false);
		sourceIDInput.setEnabled(false);
		
		roomIDInput.addActionListener(this);
		sourceTypeInput.addActionListener(this);
		
		sourceTypeInput.setSelectedItem("Motion");
		sourceTypeInput.setEnabled(false);
	}

	private void resetMotionEditForm(){
		siteIDInput.setText("");
		sourceIDInput.setText("");
		sourceNameInput.setText("");
		roomIDInput.removeAllItems();
		sourceTypeInput.setSelectedItem("Motion");
		measurementTypeInput.setSelectedIndex(0);
		notesInput.setText("");
	}
	
	private void addMotion(Site site){

		//PREPARE NEW SOURCE
		String newSourceID = null;
		if (siteIDInput.getText().equals("") && sourceIDInput.getText().equals("")){
			//set sites
			siteIDInput.setText(site.siteID+": "+site.siteName);
			
			try{
				//get new source
				Statement MySQL_Statement = dbConn.createStatement();
				MySQL_Statement.executeUpdate("INSERT INTO sources (site_id,source_name,source_type,measurement_type) VALUES("+site.siteID+",NULL,'Motion','OnTime')");//creates new row in sources
				ResultSet new_id_result = MySQL_Statement.executeQuery("SELECT LAST_INSERT_ID()"); //returns new id 
	
				if (new_id_result.next()){ //if retrieved a new id
					newSourceID = new_id_result.getString(1);
				}
				sourceIDInput.setText(newSourceID);
	
				//create new motion
				//MySQL_Statement.executeUpdate("INSERT INTO appliances (site_id,source_id) VALUES("+siteID+","+newSourceID+")");//creates new row in sources
				try{
					//get room types
					ResultSet roomIDsSQL = MySQL_Statement.executeQuery("SELECT room_id,room_type FROM rooms WHERE site_id ="+site.siteID);//gets relevant rooms
					if (roomIDsSQL.next()){
						roomIDsSQL.beforeFirst();
						while (roomIDsSQL.next()){ //if got some rooms
							roomIDInput.addItem(roomIDsSQL.getString("room_id")+": "+roomIDsSQL.getString("room_type"));
						}
					}
					else{
						roomIDInput.addItem("No Rooms");
						roomIDInput.addItem("Add New Room");
					}
		
					//if (!roomIDInput.getItemAt(0).equals("No Rooms")){
					//	fillGPOs(siteID);
					//}else{
					//	gpoInput.addItem("No Rooms");
					//}
				} catch(SQLException sE){
					sE.printStackTrace();
					removeMotion(site,newSourceID); //incomplete source
				}
			} catch (SQLException sE){
				sE.printStackTrace();
				JOptionPane.showMessageDialog(null,"Error occured when creating new source.","Error",JOptionPane.ERROR_MESSAGE);
			} 
		}
		else{ //this happens when an error has occurred with the previous source add attempt
			newSourceID = sourceIDInput.getText();
		}

		//GET USER RESPONSE
		if (newSourceID != null){
			sourceNameInput.requestFocus();

			int response = JOptionPane.showOptionDialog(null,this,"Adding new motion.",JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,null,new String[] {"Done","Add Another","Cancel"},null);
			if (response == JOptionPane.YES_OPTION || response==JOptionPane.NO_OPTION){
				Source newSource = new Source(site,newSourceID,sourceNameInput.getText(),sourceTypeInput.getSelectedItem().toString(),measurementTypeInput.getSelectedItem().toString());
				if (newSource.isValid()){
					Motion newMotion = new Motion(site,newSourceID,sourceNameInput.getText(),roomIDInput.getSelectedItem().toString().split(": ")[0],notesInput.getText());
					if (newMotion.isValid()){
						try{
							Statement MySQL_Statement = dbConn.createStatement();
							try{
								String updSourceSQL = "UPDATE sources SET source_name='"+sourceNameInput.getText()+"',source_type='"+sourceTypeInput.getSelectedItem().toString()+"',measurement_type='"+measurementTypeInput.getSelectedItem().toString()+"' WHERE site_id = "+site.siteID+" AND source_id = "+newSourceID; //adds specified information into the database
								MySQL_Statement.executeUpdate(updSourceSQL);
								try{
									String updMotionSQL = "INSERT INTO motion (site_id,source_id,room_id,notes) VALUES("+site.siteID+","+newSourceID+","+(roomIDInput.getSelectedItem().toString().split(": ")[0]+"")+","+(notesInput.getText().equals("")?"NULL":"'"+notesInput.getText()+"'")+")"; //adds specified information into the database
									MySQL_Statement.executeUpdate(updMotionSQL);
	
									resetMotionEditForm();
	
									//restart, because user clicked "Add another"
									if (response==JOptionPane.NO_OPTION){
										addMotion(site);
									}
								}catch(SQLException sE){
									sE.printStackTrace();
									JOptionPane.showMessageDialog(null,"Error occured when writing motion information.","Error",JOptionPane.ERROR_MESSAGE);
									addMotion(site);
								}
							} catch(SQLException sE){
								if (sE.getErrorCode()==1062){
									JOptionPane.showMessageDialog(null,"Information entered conflicts with an existing source for this site.","Error",JOptionPane.ERROR_MESSAGE);
								}
								else{
									sE.printStackTrace();
									JOptionPane.showMessageDialog(null,"Error occured when writing source information.","Error",JOptionPane.ERROR_MESSAGE);
								}
								addMotion(site);
							}
						} catch (SQLException sE){
							sE.printStackTrace();
						} 
					}
					else{
						addMotion(site);
					}
				}
				else{
					addMotion(site);
				}
			}
			else{
				removeMotion(site,newSourceID); //incomplete source
			}
			resetMotionEditForm();
		}
		else{
			JOptionPane.showMessageDialog(this,"An error occured when creating new source ID.","Fatal Error",JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void editMotion(Source source){	
		try{
			Statement MySQL_Statement = dbConn.createStatement();
			
			if(siteIDInput.getText().equals("") || sourceIDInput.getText().equals("") || roomIDInput.getItemCount()==0){
				System.out.println("lala");
				//get room types
				ResultSet roomIDsSQL = MySQL_Statement.executeQuery("SELECT room_id,room_type FROM rooms WHERE site_id ="+source.site.siteID);//gets relevant rooms
				if (roomIDsSQL.next()){
					roomIDsSQL.beforeFirst();
					while (roomIDsSQL.next()){ //if got some rooms
						roomIDInput.addItem(roomIDsSQL.getString("room_id")+": "+roomIDsSQL.getString("room_type"));
					}
				}
				else{
					roomIDInput.addItem("No Rooms");
					roomIDInput.addItem("Add New Room");
				}

				siteIDInput.setText(source.site.siteID+": "+source.site.siteName);
				sourceIDInput.setText(source.sourceID);
				ResultSet motionData = MySQL_Statement.executeQuery("SELECT sources.source_name,sources.source_type,sources.measurement_type,rooms.room_type,motion.* FROM motion LEFT JOIN sources ON sources.source_id = motion.source_id LEFT JOIN rooms ON motion.room_id = rooms.room_id WHERE motion.site_id ="+source.site.siteID+" AND motion.source_id = "+source.sourceID); //retrieves data pertaining to selected site
				if (motionData.next()){
					sourceNameInput.setText(motionData.getString("source_name"));
					roomIDInput.setSelectedItem(motionData.getString("room_id")+": "+motionData.getString("room_type"));
					System.out.println(motionData.getString("room_id")+": "+motionData.getString("room_type"));
					sourceTypeInput.setSelectedItem(motionData.getString("source_type"));
					measurementTypeInput.setSelectedItem(motionData.getString("measurement_type"));
					notesInput.setText(motionData.getString("notes"));
				}
			}
						
			if (!siteIDInput.getText().equals("") && !sourceIDInput.getText().equals("") && roomIDInput.getItemCount()>0){ //if found relevant record
				
	
				int response = JOptionPane.showConfirmDialog(null,this,"Editing motion (Source ID: "+source.sourceID+")",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
				if (response == JOptionPane.OK_OPTION){
					Source newSource = new Source(source.site,source.sourceID,sourceNameInput.getText(),sourceTypeInput.getSelectedItem().toString(),measurementTypeInput.getSelectedItem().toString());
					if (newSource.isValid()){
						Motion newMotion = new Motion(source.site,source.sourceID,sourceNameInput.getText(),roomIDInput.getSelectedItem().toString().split(": ")[0],notesInput.getText());
						if (newMotion.isValid()){
							try{
								String updSourceSQL = "UPDATE sources SET source_name='"+sourceNameInput.getText()+"',source_type='"+sourceTypeInput.getSelectedItem().toString()+"',measurement_type='"+measurementTypeInput.getSelectedItem().toString()+"' WHERE site_id = "+source.site.siteID+" AND source_id = "+source.sourceID; //adds specified information into the database
								MySQL_Statement.executeUpdate(updSourceSQL);
								try{
									String updMotionSQL = "UPDATE motion SET room_id="+(roomIDInput.getSelectedItem().toString().split(": ")[0]+"")+",notes="+(notesInput.getText().equals("")?"NULL":"'"+notesInput.getText()+"'")+" WHERE site_id = "+source.site.siteID+" AND source_id = "+source.sourceID; //adds specified information into the database
									MySQL_Statement.executeUpdate(updMotionSQL);
								}catch(SQLException sE){
									JOptionPane.showMessageDialog(null,"Error occured when writing motion information.","Error",JOptionPane.ERROR_MESSAGE);
									editMotion(source);
								}
							} catch(SQLException sE){
								if (sE.getErrorCode()==1062){
									JOptionPane.showMessageDialog(null,"Information entered conflicts with an existing source for this site.","Error",JOptionPane.ERROR_MESSAGE);
								}
								else{
									sE.printStackTrace();
									JOptionPane.showMessageDialog(null,"Error occured when writing source information.","Error",JOptionPane.ERROR_MESSAGE);
								}
								editMotion(source);
							}
						}
						else{
							editMotion(source);
						}
					}
					else{
						editMotion(source);
					}
				}
				resetMotionEditForm();
			}
			else{
				JOptionPane.showMessageDialog(this,"An error occured when writing data for motion (Source ID: "+source.sourceID+".","Fatal Error",JOptionPane.ERROR_MESSAGE);
			}
		} catch(SQLException sE){
			sE.printStackTrace();
		}
	}
	
	private void questionRemoveMotion(Site site,String sourceID){
		int response = JOptionPane.showConfirmDialog(this,"Are you sure you wish to permanently remove the selected source ("+sourceID+") and all associated files and data?","Removing Source...",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
		if (response == JOptionPane.YES_OPTION){
			removeMotion(site,sourceID);
		}
	}
	
	private void removeMotion(Site site,String sourceID){
		try{
			Statement MySQL_Statement = dbConn.createStatement();
			MySQL_Statement.executeUpdate("DELETE FROM sources WHERE site_id = "+site.siteID+" AND source_id = "+sourceID); //removes data pertaining to selected source
		} catch (SQLException sE){
			JOptionPane.showMessageDialog(this,"An error occured when removing data for the specified source (Source ID: "+sourceID+").","Retrieval Error",JOptionPane.ERROR_MESSAGE);
			sE.printStackTrace();
		}

	}
	
	/*void fillGPOs(String siteID){
		gpoInput.removeAllItems();
		try{
			//fill gpos
			Statement MySQL_Statement = dbConn.createStatement();
			ResultSet gpoIDsSQL = MySQL_Statement.executeQuery("SELECT gpo_id,gpo_name FROM gpos WHERE site_id ="+siteID+" AND room_id = "+roomIDInput.getSelectedItem().toString().split(": ")[0]);//gets relevant gpos
			if (gpoIDsSQL.next()){
				gpoIDsSQL.beforeFirst();
				while (gpoIDsSQL.next()){ //if got some rooms
					gpoInput.addItem(gpoIDsSQL.getString("gpo_id")+": "+gpoIDsSQL.getString("gpo_name"));
				}
			}
			else{
				gpoInput.addItem("No GPOs");
				gpoInput.addItem("Add New GPO");
			}
		}catch(SQLException sE){
			gpoInput.addItem("DB Error");
		}
	}*/

	@Override
	public void actionPerformed(ActionEvent aE) {
		if (aE.getSource().equals(sourceTypeInput)){
			measurementTypeInput.removeAllItems();
			if(sourceTypeInput.getSelectedItem().toString().equals("Appliance")){
				measurementTypeInput.addItem("AppEnergy");
				measurementTypeInput.addItem("AppPower");
				measurementTypeInput.addItem("ActEnergy");
				measurementTypeInput.addItem("ActPower");
				measurementTypeInput.addItem("Volts");
				measurementTypeInput.addItem("Amps");
			} else if(sourceTypeInput.getSelectedItem().toString().equals("Circuit")){
				measurementTypeInput.addItem("ActPower");
			} else if(sourceTypeInput.getSelectedItem().toString().equals("Gas")){
				measurementTypeInput.addItem("Pulse");
			} else if(sourceTypeInput.getSelectedItem().toString().equals("Humidity")){
				measurementTypeInput.addItem("Humidity");
			} else if(sourceTypeInput.getSelectedItem().toString().equals("Light")){
				measurementTypeInput.addItem("OnTime");
				measurementTypeInput.addItem("LightLevel");
				measurementTypeInput.addItem("AppEnergy");
				measurementTypeInput.addItem("AppPower");
				measurementTypeInput.addItem("ActEnergy");
				measurementTypeInput.addItem("ActPower");
				measurementTypeInput.addItem("Volts");
				measurementTypeInput.addItem("Amps");
			} else if(sourceTypeInput.getSelectedItem().toString().equals("Motion")){
				measurementTypeInput.addItem("OnTime");
			} else if(sourceTypeInput.getSelectedItem().toString().equals("Temperature")){
				measurementTypeInput.addItem("Temp");
				measurementTypeInput.addItem("AvgTemp");
			} else if(sourceTypeInput.getSelectedItem().toString().equals("Water")){
				measurementTypeInput.addItem("Pulse");
			}
		}
		else if (aE.getSource().equals(roomIDInput) && roomIDInput.getItemCount()>0 && roomIDInput.getSelectedItem().toString().equals("Add New Room") && siteIDInput.getText().toString().split(": ")[0].matches("^\\d{1,10}$")){
			RoomEditPanel roomEditPanel = new RoomEditPanel(dbConn);
			String[] split = siteIDInput.getText().toString().split(": ");
			roomEditPanel.addRoom(split[0],split[1]);
		}
		//else if (aE.getSource().equals(roomIDInput) && roomIDInput.getItemCount()>0 && !roomIDInput.getItemAt(0).equals("No Rooms")){
		//	fillGPOs(siteIDInput.getText().split(": ")[0]);
		//}
	}
	
	public JPanel getMotionButtonPanel(SiteTable siteTable,MotionTable motionTable){
		return new MotionButtonPanel(siteTable,motionTable);
	}
	
	//Button Panel
	public class MotionButtonPanel extends JPanel implements ActionListener{
		private static final long serialVersionUID = -2869230293945872400L;
		
		//Buttons
		JButton motionAddB = new JButton("Add");
		JButton motionEditB = new JButton("Edit");
		JButton motionRemB = new JButton("Remove");

		SiteTable siteTable;
		MotionTable motionTable;
		
		MotionButtonPanel(SiteTable siteTable,MotionTable motionTable){
			this.siteTable = siteTable;
			this.motionTable = motionTable;
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					buildGUI();
				}
			});
		}
		
		private void buildGUI(){
			this.setLayout(new FlowLayout());
			this.add(motionAddB);
			this.add(motionEditB);
			this.add(motionRemB);
			
			motionAddB.addActionListener(this);
			motionEditB.addActionListener(this);
			motionRemB.addActionListener(this);
		}
		
		public void setEnabled(boolean enabled){
			motionAddB.setEnabled(enabled);
			motionEditB.setEnabled(enabled);
			motionRemB.setEnabled(enabled);
			super.setEnabled(enabled);
		}
		
		public void actionPerformed(ActionEvent aE) {
			if (siteTable.siteListModel.isSelectionEmpty()==false){
				Site site = siteTable.siteList.get(siteTable.getSelectedRow());

				
				if (aE.getSource().equals(motionAddB)){
					addMotion(site);
				}
				else if (aE.getSource().equals(motionEditB)){
					if (motionTable.motionListModel.isSelectionEmpty()==false){
						Source source = motionTable.motionList.get(motionTable.getSelectedRow());
						editMotion(source);
					}
					else{
						JOptionPane.showMessageDialog(this,"No source selected.","Retrieval Error",JOptionPane.WARNING_MESSAGE);
					}
				}
				else if (aE.getSource().equals(motionRemB)){
					if (motionTable.motionListModel.isSelectionEmpty()==false){
						Source source = motionTable.motionList.get(motionTable.getSelectedRow());
						questionRemoveMotion(site,source.sourceID);
					}
					else{
						JOptionPane.showMessageDialog(this,"No source selected.","Retrieval Error",JOptionPane.WARNING_MESSAGE);
					}
				}
				motionTable.update(dbConn, site);
			}
		}
	}
}
