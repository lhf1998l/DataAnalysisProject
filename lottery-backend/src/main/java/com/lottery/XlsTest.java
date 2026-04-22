package com.lottery;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import java.io.FileInputStream;
import java.io.InputStream;

public class XlsTest {
    public static void main(String[] args) {
        String filePath = "C:/Environment/ClaudeCode/Project2/DF1PK10-2026-04-02.xls";
        try (InputStream is = new FileInputStream(filePath);
             Workbook workbook = new HSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            System.out.println("Sheet Name: " + sheet.getSheetName());
            System.out.println("Last Row Num: " + sheet.getLastRowNum());

            for (int i = 0; i <= Math.min(10, sheet.getLastRowNum()); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    System.out.println("Row " + i + " is null");
                    continue;
                }
                System.out.print("Row " + i + " | ");
                for (int j = 0; j < Math.min(10, row.getLastCellNum()); j++) {
                    System.out.print("Col " + j + ": [" + getCellStringValue(row.getCell(j)) + "] | ");
                }
                System.out.println();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return cell.toString();
    }
}
