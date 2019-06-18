 @RequestMapping(value = "{company_id}/statics", method = RequestMethod.GET)
    public void staticsHonourByCompanyName(@PathVariable("company_id") Long companyId,
                                           @RequestParam(value = "beginTime") Long beginTime,
                                           @RequestParam(value = "endTime") Long endTime,
                                           HttpServletResponse response) throws IOException {

        if (Objects.isNull(companyId)) {
            throw new BusinessException(ErrorType.SYS_VALIDATE_ERROR, "û��������ҵid");
        }

        List<CompanyMemberHonourStatisticDTO> honourResult = honourAppService.getStatisticHonourByCompanyId(companyId, beginTime, endTime);
        String[] headers = {"id", "����", "����", "�ֻ�", "��ҫ����"};

        //����ѯ��������ת��list��ʽ�洢
        List<Object[]> res = honourResult.stream().map(honourItem -> new Object[]{honourItem.getOwnerId(), honourItem.getUserName(),
                honourItem.getEmail(), honourItem.getMobile(), honourItem.getCount()}).collect(Collectors.toList());

        Map<String, List<Object[]>> resMap = new HashMap<>(1);
        resMap.put(companyId.toString(), res);

        ExcelUtil.generateResponseForMultiSheet(response, headers, resMap, "honour.xls");
    }


//����ĵ�������

public static void generateResponseForMultiSheet(HttpServletResponse response, String[] headArray,
                                                     Map<String, List<Object[]>> contentListMap, String fileName) throws IOException {
        // ��������
        @Cleanup InputStream inStream = exportExcel(headArray, contentListMap);
        // ��������ĸ�ʽ
        response.reset();
        response.setContentType("bin");
        response.addHeader("Content-Disposition", "attachment; filename=\""
                + fileName + "\"");
        // ѭ��ȡ�����е�����
        byte[] b = new byte[100];
        int len;
        try {
            while ((len = inStream.read(b)) > 0)
                response.getOutputStream().write(b, 0, len);
        } catch (IOException e) {
            log.error("error is : {} ", e);
        }
    }
// POI���� Export excle

 public static InputStream exportExcel(String[] headers, Map<String, List<Object[]>> dataset) throws IOException {

        if (Objects.isNull(dataset)) {

            return null;
        }

        // ����һ��������
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
//exportInner����

    private static HSSFWorkbook exportInner(HSSFWorkbook workbook, String title,
                                            String[] headers, List<Object[]> dataset,
                                            ByteArrayOutputStream os) {

        // ����һ�����
        HSSFSheet sheet = workbook.createSheet(title);
        // ����һ����ʽ
        HSSFCellStyle style = workbook.createCellStyle();
        // ������Щ��ʽ
        style.setFillForegroundColor(HSSFColor.SKY_BLUE.index);
        style.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        style.setBorderBottom(HSSFCellStyle.BORDER_THIN);
        style.setBorderLeft(HSSFCellStyle.BORDER_THIN);
        style.setBorderRight(HSSFCellStyle.BORDER_THIN);
        style.setBorderTop(HSSFCellStyle.BORDER_THIN);
        style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
        // ����һ������
        HSSFFont font = workbook.createFont();
        font.setColor(HSSFColor.VIOLET.index);
        font.setFontHeightInPoints((short) 12);
        font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        // ������Ӧ�õ���ǰ����ʽ
        style.setFont(font);
        // ���ɲ�������һ����ʽ
        HSSFCellStyle style2 = workbook.createCellStyle();
        style2.setFillForegroundColor(HSSFColor.LIGHT_YELLOW.index);
        style2.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        style2.setBorderBottom(HSSFCellStyle.BORDER_THIN);
        style2.setBorderLeft(HSSFCellStyle.BORDER_THIN);
        style2.setBorderRight(HSSFCellStyle.BORDER_THIN);
        style2.setBorderTop(HSSFCellStyle.BORDER_THIN);
        style2.setAlignment(HSSFCellStyle.ALIGN_CENTER);
        style2.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
        // ������һ������
        HSSFFont font2 = workbook.createFont();
        font2.setBoldweight(HSSFFont.BOLDWEIGHT_NORMAL);
        // ������Ӧ�õ���ǰ����ʽ
        style2.setFont(font2);
        // ����һ����ͼ�Ķ���������
        HSSFPatriarch patriarch = sheet.createDrawingPatriarch();
        // ����ע�͵Ĵ�С��λ��,����ĵ�
        HSSFComment comment = patriarch.createComment(new HSSFClientAnchor(0,
                0, 0, 0, (short) 4, 2, (short) 6, 5));
        // ����ע������
        comment.setString(new HSSFRichTextString("������?��"));
        // ����ע�����ߣ�������ƶ�����Ԫ�����ǿ�����״̬���п���������.
        comment.setAuthor("braveliu");
        // ������������
        HSSFRow row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            HSSFCell cell = row.createCell(i);
            cell.setCellStyle(style);
            HSSFRichTextString text = new HSSFRichTextString(headers[i]);
            cell.setCellValue(text);
        }
        // �����������ݣ�����������
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
                    // �ж�ֵ�����ͺ����ǿ������ת��
                    String textValue = null;
                    if (value instanceof Boolean) {
                        boolean bValue = (Boolean) value;
                        textValue = "��";
                        if (!bValue) {
                            textValue = "Ů";
                        }
                    } else if (value instanceof Date) {
                        Date date = (Date) value;
                        SimpleDateFormat sdf = new SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss");
                        textValue = sdf.format(date);
                    } else if (value instanceof byte[]) {
                        // ��ͼƬʱ�������и�Ϊ60px;
                        row.setHeightInPoints(60);
                        // ����ͼƬ�����п��Ϊ80px,ע�����ﵥλ��һ������
                        sheet.setColumnWidth(i, (short) (35.7 * 80));
                        // sheet.autoSizeColumn(i);
                        byte[] bsValue = (byte[]) value;
                        HSSFClientAnchor anchor = new HSSFClientAnchor(0, 0,
                                1023, 255, (short) 7, index, (short) 7, index);
                        anchor.setAnchorType(2);
                        patriarch.createPicture(anchor, workbook.addPicture(
                                bsValue, HSSFWorkbook.PICTURE_TYPE_JPEG));
                    } else {
                        // �����������Ͷ������ַ����򵥴���
                        textValue = value.toString();
                    }
                    // �������ͼƬ���ݣ�������������ʽ�ж�textValue�Ƿ�ȫ�����������
                    if (textValue != null) {
                        Pattern p = Pattern.compile("^//d+(//.//d+)?$");
                        Matcher matcher = p.matcher(textValue);
                        if (matcher.matches()) {
                            // �����ֵ���double����
                            cell.setCellValue(Double.parseDouble(textValue));
                        } else {
                            HSSFRichTextString richString = new HSSFRichTextString(
                                    textValue);
                            HSSFFont font3 = workbook.createFont();
                            font3.setColor(HSSFColor.BLUE.index);
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