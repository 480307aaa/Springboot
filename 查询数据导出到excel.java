 @RequestMapping(value = "{company_id}/statics", method = RequestMethod.GET)
    public void staticsHonourByCompanyName(@PathVariable("company_id") Long companyId,
                                           @RequestParam(value = "beginTime") Long beginTime,
                                           @RequestParam(value = "endTime") Long endTime,
                                           HttpServletResponse response) throws IOException {

        if (Objects.isNull(companyId)) {
            throw new BusinessException(ErrorType.SYS_VALIDATE_ERROR, "没有输入企业id");
        }

        List<CompanyMemberHonourStatisticDTO> honourResult = honourAppService.getStatisticHonourByCompanyId(companyId, beginTime, endTime);
        String[] headers = {"id", "姓名", "邮箱", "手机", "荣耀数量"};

        //将查询到的数据转成list形式存储
        List<Object[]> res = honourResult.stream().map(honourItem -> new Object[]{honourItem.getOwnerId(), honourItem.getUserName(),
                honourItem.getEmail(), honourItem.getMobile(), honourItem.getCount()}).collect(Collectors.toList());

        Map<String, List<Object[]>> resMap = new HashMap<>(1);
        resMap.put(companyId.toString(), res);

        ExcelUtil.generateResponseForMultiSheet(response, headers, resMap, "honour.xls");
    }
}

//具体的export方法
package com.yonyoucloud.ec.sns.confidential.meeting.support;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;


/**
 * 数据导出
 *
 * @author yaoshw
 */
@Slf4j
public class ExcelUtil {

    public static void generateResponseForMultiSheet(HttpServletResponse response, String[] headArray,
                                                     Map<String, List<Object[]>> contentListMap, String fileName) throws IOException {
        // 读到流中
        @Cleanup InputStream inStream = exportExcel(headArray, contentListMap);
        // 设置输出的格式
        response.reset();
         response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.addHeader("Content-Disposition", "attachment; filename=\""
                + fileName + "\"");
        // 循环取出流中的数据
        byte[] b = new byte[100];
        int len;
        try {
            while ((len = Objects.requireNonNull(inStream).read(b)) > 0) {
                response.getOutputStream().write(b, 0, len);
            }
        } catch (IOException e) {
            log.error("error is : {} ", e);
        }
    }
// POI操作 Export excle

    public static InputStream exportExcel(String[] headers, Map<String, List<Object[]>> dataset) throws IOException {

        if (Objects.isNull(dataset)) {

            return null;
        }

        // 声明一个工作薄
        HSSFWorkbook workbook = new HSSFWorkbook();
        @Cleanup ByteArrayOutputStream os = new ByteArrayOutputStream();
        for (String key : dataset.keySet()) {
            workbook = exportInner(workbook, key, headers, dataset.get(key), os);
        }
        try {
            workbook.write(os);
        } catch (IOException e) {
            log.error("error is : {} ", e);
        }
        @Cleanup InputStream is = new ByteArrayInputStream(os.toByteArray());
        return is;
    }
//exportInner方法

    private static HSSFWorkbook exportInner(HSSFWorkbook workbook, String title,
                                            String[] headers, List<Object[]> dataset,
                                            ByteArrayOutputStream os) {

        // 生成一个表格
        HSSFSheet sheet = workbook.createSheet(title);
        // 生成一个样式
        HSSFCellStyle style = workbook.createCellStyle();
        // 设置这些样式
        style.setFillForegroundColor(HSSFColor.HSSFColorPredefined.SKY_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.GENERAL);
        // 生成一个字体
        HSSFFont font = workbook.createFont();
        font.setColor(HSSFColor.HSSFColorPredefined.VIOLET.getIndex());

        font.setFontHeightInPoints((short) 12);
        font.setBold(true);
        // 把字体应用到当前的样式
        style.setFont(font);
        // 生成并设置另一个样式
        HSSFCellStyle style2 = workbook.createCellStyle();
        style2.setFillForegroundColor(HSSFColor.HSSFColorPredefined.LIGHT_YELLOW.getIndex());
        style2.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style2.setBorderBottom(BorderStyle.THIN);
        style2.setBorderLeft(BorderStyle.THIN);
        style2.setBorderRight(BorderStyle.THIN);
        style2.setBorderTop(BorderStyle.THIN);
        style2.setAlignment(HorizontalAlignment.GENERAL);
        style2.setVerticalAlignment(VerticalAlignment.CENTER);
        // 生成另一个字体
        HSSFFont font2 = workbook.createFont();
        font2.setBold(true);
        // 把字体应用到当前的样式
        style2.setFont(font2);
        // 声明一个画图的顶级管理器
        HSSFPatriarch patriarch = sheet.createDrawingPatriarch();
        // 定义注释的大小和位置,详见文档
        HSSFComment comment = patriarch.createComment(new HSSFClientAnchor(0,
                0, 0, 0, (short) 4, 2, (short) 6, 5));
        // 设置注释内容
        comment.setString(new HSSFRichTextString("会议签到"));
        // 设置注释作者，当鼠标移动到单元格上是可以在状态栏中看到该内容.
        comment.setAuthor("yaoshw@yonyou.com");
        // 产生表格标题行
        HSSFRow row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            HSSFCell cell = row.createCell(i);
            cell.setCellStyle(style);
            HSSFRichTextString text = new HSSFRichTextString(headers[i]);
            cell.setCellValue(text);
        }
        // 遍历集合数据，产生数据行
        int index = 0;
        for (Object[] o : dataset) {
            index++;
            row = sheet.createRow(index);
            for (int i = 0; i < o.length; i++) {
                HSSFCell cell = row.createCell(i);
                cell.setCellStyle(style2);
                try {
                    Object value = o[i];
                    if (value == null) {
                        value = "";
                    }
                    // 判断值的类型后进行强制类型转换
                    String textValue = null;
                    if (value instanceof Boolean) {
                        boolean bValue = (Boolean) value;
                        textValue = "已签到";
                        if (!bValue) {
                            textValue = "未签到";
                        }
                    } else if (value instanceof Long) {
                        Date date = new Date((Long) value);
                        SimpleDateFormat sdf = new SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss");
                        textValue = sdf.format(date);
                    } else if (value instanceof byte[]) {
                        // 有图片时，设置行高为60px;
                        row.setHeightInPoints(60);
                        // 设置图片所在列宽度为80px,注意这里单位的一个换算
                        sheet.setColumnWidth(i, (short) (35.7 * 80));
                        // sheet.autoSizeColumn(i);
                        byte[] bsValue = (byte[]) value;
                        HSSFClientAnchor anchor = new HSSFClientAnchor(0, 0,
                                1023, 255, (short) 7, index, (short) 7, index);
                        anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);
                        patriarch.createPicture(anchor, workbook.addPicture(
                                bsValue, HSSFWorkbook.PICTURE_TYPE_JPEG));
                    } else {
                        // 其它数据类型都当作字符串简单处理
                        textValue = value.toString();
                    }
                    // 如果不是图片数据，就利用正则表达式判断textValue是否全部由数字组成
                    if (textValue != null) {
                        Pattern p = compile("^//d+(//.//d+)?$");
                        Matcher matcher = p.matcher(textValue);
                        if (matcher.matches()) {
                            // 是数字当作double处理
                            cell.setCellValue(Double.parseDouble(textValue));
                        } else {
                            HSSFRichTextString richString = new HSSFRichTextString(
                                    textValue);
                            HSSFFont font3 = workbook.createFont();
                            font3.setColor(HSSFColor.HSSFColorPredefined.BLUE.getIndex());
                            richString.applyFont(font3);
                            cell.setCellValue(richString);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return workbook;
    }
}
