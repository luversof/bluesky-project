package net.luversof.web.dynamiccrud2.menu.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/** 엑셀로 여러 행 넣기: 칸 값을 화면 입력과 같은 글자로, 빈 줄은 뺀다 */
class ExcelRowsTest {

  @Test
  void cellsBecomeInputText() throws IOException {
    byte[] bytes;
    try (var workbook = new XSSFWorkbook();
        var out = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet();
      var dateStyle = workbook.createCellStyle();
      dateStyle.setDataFormat(
          workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));
      var header = sheet.createRow(0);
      String[] names = {"n", "d", "t", "b", "f", "s"};
      for (int i = 0; i < names.length; i++) {
        header.createCell(i).setCellValue(names[i]);
      }
      var row = sheet.createRow(1);
      row.createCell(0).setCellValue(3.0);
      var date = row.createCell(1);
      date.setCellValue(LocalDate.of(2026, 11, 26));
      date.setCellStyle(dateStyle);
      var time = row.createCell(2);
      time.setCellValue(LocalDateTime.of(2026, 11, 26, 13, 20));
      time.setCellStyle(dateStyle);
      row.createCell(3).setCellValue(true);
      row.createCell(4).setCellFormula("1+1");
      row.createCell(5).setCellValue("글자");
      sheet.createRow(2); // 빈 줄
      workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
      workbook.write(out);
      bytes = out.toByteArray();
    }
    var rows = ExcelRows.read(new ByteArrayInputStream(bytes));
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0))
        .containsEntry("n", "3")
        .containsEntry("d", "2026-11-26")
        .containsEntry("t", "2026-11-26T13:20")
        .containsEntry("b", "true")
        .containsEntry("f", "2")
        .containsEntry("s", "글자");
  }

  @Test
  void notExcelIsUnreadable() {
    assertThatThrownBy(() -> ExcelRows.read(new ByteArrayInputStream("a,b".getBytes())))
        .isInstanceOf(IOException.class);
  }
}
