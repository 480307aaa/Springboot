package com.yonyoucloud.ec.sns.conference.util.excelutil;

import com.yonyoucloud.ec.sns.error.ECIOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yegk7
 * @create 2018/7/21 16:58
 */
@Slf4j
public class ImportExcel {

    private InputStream inputStream;

    private String fileName;

    public ImportExcel(InputStream inputStream, String fileName) {
        this.inputStream = inputStream;
        this.fileName = fileName;
    }


    /**
     * 2003�����°汾��excel
     */
    private final static String EXCEL_2003_L = ".xls";

    /**
     * 2007�����ϰ汾��excel
     */
    private final static String EXCEL_2007_U = ".xlsx";

    private final static String GENERAL = "General";

    private Workbook workbook;

    /**
     * ��ȡexcel
     *
     * @return �����б�
     * @throws Exception
     */
    public List<List<Object>> importExcel() {
        // ����excel
        Workbook workbook = getWorkBook(inputStream, fileName);
        if (null == workbook) {
            throw new ECIOException("excel is empty");
        }

        return importExcelData();
    }

    /**
     * �����ļ��ϴ�����׺���ж��ļ��汾
     *
     * @param inputStream
     * @param fileName
     * @return
     */
    private Workbook getWorkBook(InputStream inputStream, String fileName) {
        Workbook workbook = null;
        String fileType = fileName.substring(fileName.lastIndexOf("."));

        if (EXCEL_2003_L.equals(fileType)) {
            try {
                workbook = new HSSFWorkbook(inputStream);
            } catch (IOException e) {
                throw new ECIOException("excel create error", e);
            }
        } else if (EXCEL_2007_U.equals(fileType)) {
            try {
                workbook = new XSSFWorkbook(inputStream);
            } catch (IOException e) {
                throw new ECIOException("excel create error", e);
            }
        } else {
            throw new ECIOException("The parsed file format is incorrect");
        }
        return workbook;
    }


    /**
     * ��ȡ��Ԫ�����ݣ����Ҹ�ʽ��
     *
     * @param cell
     * @return
     */
    private Object getCellValue(Cell cell) {

        Object object = null;

        // ��ʽ���ַ����͵�����
        DecimalFormat decimalFormat = new DecimalFormat("0");

        // ���ڸ�ʽ��
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // ��ʽ������
        DecimalFormat decimalFormat1 = new DecimalFormat("0.00");
        switch (cell.getCellTypeEnum()) {
            case STRING:
                object = cell.getRichStringCellValue().toString();
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    object = simpleDateFormat.format(cell.getDateCellValue());
                } else if (GENERAL.equals(cell.getCellStyle().getDataFormatString())) {
                    object = decimalFormat.format(cell.getNumericCellValue());
                } else {
                    object = decimalFormat1.format(cell.getNumericCellValue());
                }
                break;
            case BLANK:
                object = "";
                break;
            case BOOLEAN:
                object = cell.getBooleanCellValue();
                break;
            default:
                break;
        }

        return object;
    }

    /**
     * ����excel����
     *
     * @return
     */
    private List<List<Object>> importExcelData() {

        List<List<Object>> dataList = new ArrayList<>();

        // ����excel���е�sheet
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet == null) {
                continue;
            }
            // ����sheet��������
            for (int j = sheet.getFirstRowNum(); j <= sheet.getLastRowNum(); j++) {
                Row row = sheet.getRow(j);
                // ȥ�����кͱ�ͷ����
                if (row == null || row.getFirstCellNum() == j) {
                    continue;
                }
                dataList.add(getSheetData(row));
            }
        }
        return dataList;
    }

    /**
     * ��ȡsheet������
     *
     * @param row
     * @return
     */
    private List<Object> getSheetData(Row row) {

        List<Object> objectList = new ArrayList<>();
        // ����������
        for (int k = row.getFirstCellNum(); k <= row.getLastCellNum(); k++) {
            Cell cell = row.getCell(k);
            objectList.add(getCellValue(cell));
        }
        return objectList;
    }
}
