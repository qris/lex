/*
 * Created on 21-Dec-2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.lex;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.qwirx.db.Change;
import com.qwirx.db.sql.SqlChange;
import com.qwirx.db.sql.SqlDatabase;


/**
 * @author chris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class LexicalClassImporter {

	public static void main(String[] args) throws Exception
    {
		SqlDatabase db = Lex.getSqlDatabase("chris");
		
		if (args.length != 1) {
			System.err.println("Usage: java LexicalClassImporter <file>");
			System.exit(2);
		}
		
		FileInputStream fis = null;
		
		try {
			fis = new FileInputStream(args[0]);
		} catch (FileNotFoundException e) {
			System.err.println("File not found: "+args[0]);
			System.exit(1);
		}
		
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		
		String line = null;
		String[] prev_code = new String[]{"0"};
		
		while ( (line = br.readLine()) != null) {
			if (line.equals("")) continue;
			// if (line.equals("\n")) continue;
			// if (line.equals("\r\n")) continue;
			
			String[] code_desc = line.split(" ", 2);
			if (code_desc.length != 2) {
				System.err.println("Aborted on strange line: '"+line+"'");
				System.exit(1);
			}
			
			String code = code_desc[0];
			String[] code_parts = code.split("\\.");
			// System.out.println(code);
			
			if (code_parts.length > prev_code.length + 1) {
				System.err.println("Code is not a proper subcode " +
						"of previous line: "+code);
				System.exit(1);
			} else if (code_parts.length == prev_code.length + 1) {
				// when the new code is 1 entry longer than the previous,
				// they should match in every part, and the last entry
				// should be "1".
				for (int i = 0; i < prev_code.length; i++) {
					if (! code_parts[i].equals(prev_code[i]) ) {
						System.err.println("Code is not a proper subcode " +
								"of previous line: " + code);
						System.exit(1);
					}
				}
				if (! code_parts[prev_code.length].equals("1") ) {
					System.err.println("Code is not a proper subcode " +
							"of previous line: " + code);
					System.exit(1);
				}
			} else {
				// Otherwise, new code is shorter than previous. They should
				// match in every part except the last, which should be one
				// greater.
				for (int i = 0; i < code_parts.length - 1; i++) {
					if (! code_parts[i].equals(prev_code[i]) ) {
						System.err.println("Code is not a proper subcode " +
								"of previous line: " + code);
						System.exit(1);
					}
				}

				int newCode = -1, oldCode = -1;
				
				try {
					newCode = Integer.parseInt(
							code_parts[code_parts.length - 1]);
				} catch (NumberFormatException e) {
					System.err.println("Invalid number: '"
							+ code_parts[code_parts.length - 1]);
					System.exit(1);
				}

				try {
					oldCode = Integer.parseInt(
							prev_code[code_parts.length - 1]);
				} catch (NumberFormatException e) {
					System.err.println("Invalid number: '"
							+ prev_code[code_parts.length - 1]);
					System.exit(1);
				}
				
				if (newCode != oldCode + 1) {
					System.err.println("Code is not a proper subcode " +
							"of previous line: " + code);
					System.exit(1);
				}
			}
			
			prev_code = code_parts;
			
			int myId = -1, parentId = 0;
			String oldDesc = null;
			
			for (int i = 0; i < code_parts.length; i++) {
				PreparedStatement stmt = db.prepareSelect(
						"SELECT ID,Domain_Desc FROM lexicon_entries "+
						"WHERE Domain_Label = ? AND Domain_Parent_ID = ?");
				stmt.setString(1, code_parts[i]);
				stmt.setInt   (2, parentId);
				ResultSet rs = db.select();
				
				if (rs.next()) {
					myId    = rs.getInt(1);
					if (i == code_parts.length - 1)
						oldDesc = rs.getString(2);
					db.finish();
				} else if (i != code_parts.length - 1) {
					db.finish();
					System.err.println("Missing label "+i+
							" ("+code_parts[i]+") "+
							" under ID "+parentId+" for code "+code);
					System.exit(1);
				} else {
					db.finish();

					Change ch = db.createChange(
							SqlChange.INSERT, "lexicon_entries", null);
					ch.setInt   ("Domain_Parent_ID", parentId);
					ch.setString("Domain_Label", code_parts[i]);
					ch.setString("Domain_Desc",  "x");
					ch.execute();
					myId = ((SqlChange)ch).getInsertedRowId();
					if (i == code_parts.length - 1)
						oldDesc = "x";
					System.out.println("Created entry "+myId+" for "+code);
				}

				parentId = myId;
			}
			
			if (! oldDesc.equals(code_desc[1])) {
				System.out.println("Updating entry "+myId+" with new "+
						"description: '" + code_desc[1] + "'");
				
				Change ch = db.createChange(SqlChange.UPDATE, 
						"lexicon_entries", "ID = "+myId);
				ch.setString("Domain_Desc", code_desc[1]);
				ch.execute();
			}
		}
		
		System.out.println("Finished parsing file successfully");
	}
}
