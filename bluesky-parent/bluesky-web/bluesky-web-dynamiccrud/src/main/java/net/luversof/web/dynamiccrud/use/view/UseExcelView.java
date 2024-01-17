package net.luversof.web.dynamiccrud.use.view;

import java.awt.Color;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.view.document.AbstractXlsxView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.luversof.web.dynamiccrud.use.domain.ContentInfo;

public class UseExcelView extends AbstractXlsxView {

	@Override
	protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		var projectId = String.valueOf(model.get("projectId"));
		var mainMenuId = String.valueOf(model.get("mainMenuId"));
		var subMenuId = String.valueOf(model.get("subMenuId"));
//		@SuppressWarnings("unchecked")
//		var columnMap = (LinkedHashMap<String, String>) model.get("columnMap");
		
		
		// 기존 방식의 데이터 호출. 제거 예정
//		@SuppressWarnings("unchecked")
//		var page = (Page<Map<String, Object>>) model.get("page");
		
		var contentInfo = (ContentInfo) model.get("contentInfo");
	
		Sheet sheet = workbook.createSheet(subMenuId);
		
		var headerCellStyle = createCellStyle(workbook, CellType.HEADER);
		var contentCellStyle = createCellStyle(workbook, CellType.CONTENT);
		
		int rowIdx = 0;
		
		// header 설정
		{
			var row = sheet.createRow(rowIdx++);
		
			int cellIdx = 0;
			
			for (var contentKey : contentInfo.getContentKeyList()) {
				var cell = row.createCell(cellIdx++);
				cell.setCellValue(contentKey.getKey());
				cell.setCellStyle(headerCellStyle);
			}
		}
		
		// col data 설정
		{
			List<Map<String, Object>> processedContentMapList = contentInfo.getProcessedContentMapList();
			for (int i = 0 ; i < processedContentMapList.size() ; i++) {
				var row = sheet.createRow(rowIdx++);
				
				int cellIdx = 0;
				for (var entry : processedContentMapList.get(i).entrySet()) {
					var cell = row.createCell(cellIdx++);
					cell.setCellValue(String.valueOf(entry.getValue()));
					cell.setCellStyle(contentCellStyle);
				}
			}
		}
		
		// width 설정 (데이터가 모두 추가된 이후 설정해야 적용됨)
		{
			int cellIdx = 0;
			for (@SuppressWarnings("unused") var contentKey : contentInfo.getContentKeyList()) {
				sheet.autoSizeColumn(cellIdx++);
			}
		}
		
		// 파일 이름 설정
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
			MessageFormat.format(
				"attachment; filename={0}_{1}_{2}_{3}.xlsx", 
				projectId,
				mainMenuId,
				subMenuId,
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
