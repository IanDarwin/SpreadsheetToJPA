package spreadsheet;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import model.Member;
import model.MemberType;
import model.NewsletterMode;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRow;

public class ImportFromSpreadsheet {

	static EntityManagerFactory entityMgrFactory;
	static EntityManager entityManager;
	static EntityTransaction tx;
	static int saved = 0;
	
	public static void main(String[] args) throws Exception {
		final String oldFile = "../../UCFN/UCFN-Membership-2014-2015.xlsx";
		openJpa();
		try {
			convertFile(oldFile);
			closeJpa();
		} catch (Exception e) {
			System.err.println("Error: " + e);
		}
		System.out.println("Saved " + saved + " souls");
	}
	
	public static void openJpa() {

		entityMgrFactory = Persistence.createEntityManagerFactory("ucfn");
	}
	
	private static void closeJpa() {
		// Nothing left
	}
	
	/** Process one (the only one) input File
	 * @param fileName The name of the Spreadsheet file
	 * @throws Exception If it doesn't end well
	 */
	static void convertFile(String fileName) throws Exception {
		File file = new File(fileName);
        try (
        	InputStream is = new FileInputStream(file);
        	XSSFWorkbook workbook = new XSSFWorkbook(is);) {
            System.out.println("Reading workbook " + file.getName());
            
            for (int sheetNum = 0; sheetNum < workbook.getNumberOfSheets(); sheetNum++) {
            	final XSSFSheet sheet = workbook.getSheetAt(sheetNum);
            	System.out.println("Sheet " + sheet.getSheetName() + " has " + sheet.getPhysicalNumberOfRows() + " rows,");
            	System.out.println("... numbered from " + sheet.getFirstRowNum() + " to " + sheet.getLastRowNum());
            	for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
            		XSSFRow r = sheet.getRow(i);
            		convertRow(r);
            	}
            }
        }
	}

	/**
	 * Convert the current row to a Member object, persist it.
	 * @param row A Row from the Worksheet
	 */
	public static void convertRow(final XSSFRow row) {
		System.out.println("Converting row " + row.getRowNum() + " of " + row.getSheet().getPhysicalNumberOfRows());
		if (row.getRowNum() == 0) {
			return;					// Toss header row
		}
		final String firstName = row.getCell(4).getStringCellValue();
		final String lastName = row.getCell(3).getStringCellValue();
		if (firstName == null || firstName.length() == 0 ||
			lastName  == null || lastName.length()  == 0) {
			return;
		}
		final Member member = new Member();
		member.setFirstName(firstName);
		member.setLastName(lastName);
		String type = row.getCell(1).getStringCellValue();
		switch(type.charAt(0)) {
		case 'A':
			member.setMemberType(MemberType.Individual);
			break;
		case 'S':
			member.setMemberType(MemberType.Student);
			break;
		case 'F':
			member.setMemberType(MemberType.Individual);
			break;
		default:
			System.err.println("Warning: Member " + member + ": invalid member type " + type);
			break;
		}
		final String address = row.getCell(6).getStringCellValue();
		member.setAddress(address);
		final String city = row.getCell(7).getStringCellValue();
		member.setCity(city);
		final String province = row.getCell(8).getStringCellValue();
		member.setProvince(province);
		final String postCode = row.getCell(9).getStringCellValue();
		member.setPostCode(postCode);
		final String phone = row.getCell(10).getStringCellValue();
		member.setHomePhone(phone);
		final String newsletterMode = row.getCell(11).getStringCellValue();
		member.setNewsletterMode(newsletterMode.toLowerCase().startsWith("p")?NewsletterMode.POSTAL:NewsletterMode.EMAIL);
		final String email = row.getCell(12).getStringCellValue();
		member.setEmail(email);
		System.out.println(member);
		final XSSFCell yearJoinedCell = row.getCell(15);
		if (yearJoinedCell != null) {
			try {
				int year = (int)yearJoinedCell.getNumericCellValue();
				member.setYearJoined(year);
			} catch (Exception nfe) {
				System.err.println("Warning: member " + member + ": bad yearJoined: " + yearJoinedCell);
			}
		}

		// Now save the Entity
		entityManager = entityMgrFactory.createEntityManager();
		tx = entityManager.getTransaction();
		tx.begin();
		entityManager.persist(member);
		tx.commit();
		entityManager.close();
		++saved;
	}
}
