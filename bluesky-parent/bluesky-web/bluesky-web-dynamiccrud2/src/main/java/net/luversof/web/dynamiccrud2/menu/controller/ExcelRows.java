package net.luversof.web.dynamiccrud2.menu.controller;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 엑셀로 여러 행 넣기(사용 화면): 첫 시트의 첫 줄 = 칸 이름, 둘째 줄부터 한 줄 = 한 행. 칸 값은 화면 입력과 같은 글자로 - 날짜 칸은
 * ISO(2026-11-26, 시각이 있으면 2026-11-26T13:20), 수는 지수 없는 글자, 참거짓은 true / false, 수식은 계산된 값. 칸이 모두 빈 줄은
 * 뺀다.
 */
final class ExcelRows {

  private ExcelRows() {}

  /**
   * @throws IOException xlsx 가 아니거나 읽을 수 없을 때(POI 의 형식 오류도 여기로)
   */
  static List<Map<String, String>> read(InputStream input) throws IOException {
    try (var workbook = new XSSFWorkbook(input)) {
      var sheet = workbook.getSheetAt(0);
      var header = sheet.getRow(sheet.getFirstRowNum());
      if (header == null) {
        return List.of();
      }
      var names = new ArrayList<String>();
      for (int c = 0; c < header.getLastCellNum(); c++) {
        names.add(text(header.getCell(c)).strip());
      }
      var rows = new ArrayList<Map<String, String>>();
      for (int r = header.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
        var row = rowValues(sheet.getRow(r), names);
        if (row != null) {
          rows.add(row);
        }
      }
      return rows;
    } catch (RuntimeException e) {
      throw new IOException(e);
    }
  }

  /** 칸 이름 → 값. 모두 비었으면 null(뺀다) */
  private static Map<String, String> rowValues(Row row, List<String> names) {
    if (row == null) {
      return null;
    }
    var values = new LinkedHashMap<String, String>();
    var blank = true;
    for (int c = 0; c < names.size(); c++) {
      if (names.get(c).isEmpty()) {
        continue;
      }
      var value = text(row.getCell(c));
      blank &= value.isBlank();
      values.put(names.get(c), value);
    }
    return blank ? null : values;
  }

  static String text(Cell cell) {
    if (cell == null) {
      return "";
    }
    var type =
        cell.getCellType() == CellType.FORMULA
            ? cell.getCachedFormulaResultType()
            : cell.getCellType();
    return switch (type) {
      case STRING -> cell.getStringCellValue();
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      case NUMERIC -> {
        if (DateUtil.isCellDateFormatted(cell)) {
          var value = cell.getLocalDateTimeCellValue();
          yield value.toLocalTime().equals(LocalTime.MIDNIGHT)
              ? value.toLocalDate().toString()
              : value.toString();
        }
        yield BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
      }
      default -> "";
    };
  }
}
