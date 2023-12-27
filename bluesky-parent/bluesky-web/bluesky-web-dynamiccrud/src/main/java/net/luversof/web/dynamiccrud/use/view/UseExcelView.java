package net.luversof.web.dynamiccrud.use.view;

import java.awt.Color;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.view.document.AbstractXlsxView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class UseExcelView extends AbstractXlsxView {

	@Override
	protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		var product = String.valueOf(model.get("product"));
		var mainMenu = String.valueOf(model.get("mainMenu"));
		var subMenu = String.valueOf(model.get("subMenu"));
		@SuppressWarnings("unchecked")
		var columnMap = (LinkedHashMap<String, String>) model.get("columnMap");
		
		@SuppressWarnings("unchecked")
		var page = (Page<Map<String, Object>>) model.get("page");
	
		Sheet sheet = workbook.createSheet(subMenu);
		
		var headerCellStyle = createCellStyle(workbook, CellType.HEADER);
		var contentCellStyle = createCellStyle(workbook, CellType.CONTENT);
		
		int rowIdx = 0;
		
		// header 설정
		{
			var row = sheet.createRow(rowIdx++);
		
			int cellIdx = 0;
			for (var entry : columnMap.entrySet()) {
				var cell = row.createCell(cellIdx++);
				cell.setCellValue(entry.getValue());
				cell.setCellStyle(headerCellStyle);
			}
		}
		
		// col data 설정
		{
			List<Map<String, Object>> contentList = page.getContent();
			for (int i = 0 ; i < contentList.size() ; i++) {
				var row = sheet.createRow(rowIdx++);
				
				int cellIdx = 0;
				for (var entry : columnMap.entrySet()) {
					var cell = row.createCell(cellIdx++);
					cell.setCellValue( String.valueOf(contentList.get(i).get(entry.getKey()) ));
					cell.setCellStyle(contentCellStyle);
				}
			}
		}
		
		// width 설정 (데이터가 모두 추가된 이후 설정해야 적용됨)
		{
			int cellIdx = 0;
			for (@SuppressWarnings("unused") var entry : columnMap.entrySet()) {
				sheet.autoSizeColumn(cellIdx++);
			}
		}
		
		// 파일 이름 설정
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
			MessageFormat.format(
				"attachment; filename={0}_{1}_{2}_{3}.xlsx", 
				product,
				mainMenu,
				subMenu,
				ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")))
				);
//		response.setHeader("Content-Transfer-Encoding", "binary");
	}
	
	private CellStyle createCellStyle(Workbook workbook, CellType cellType) {
		var cellStyle = workbook.createCellStyle();
		
		cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		cellStyle.setBorderLeft(BorderStyle.THIN);
		cellStyle.setBorderTop(BorderStyle.THIN);
		cellStyle.setBorderRight(BorderStyle.THIN);
		cellStyle.setBorderBottom(BorderStyle.THIN);
		
		var font = workbook.createFont();
		cellStyle.setFont(font);
		
		if (cellType == CellType.HEADER) {
			cellStyle.setFillForegroundColor(new XSSFColor(new Color(231, 234, 246), new DefaultIndexedColorMap()));			
			cellStyle.setAlignment(HorizontalAlignment.CENTER);
			cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
			font.setBold(true);
		} else if (cellType == CellType.CONTENT) {
			cellStyle.setFillForegroundColor(new XSSFColor(new Color(255, 255, 255), new DefaultIndexedColorMap()));
		}
		return cellStyle;
	}

	private enum CellType {
		HEADER, CONTENT;
	}
	
	
}
