package com.stockmarket.service;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SymbolIngestionService {

    private final List<String> securities = new CopyOnWriteArrayList<>();
    private static final String STOCK_SYMBOL_FILE =
            "R:\\stock-market\\backend\\stock-fetcher-service\\src\\main\\resources\\stock-symbol-1.xlsx";

    @PostConstruct
    public void readFromFile(){
        log.info("Started reading symbols from file: {}", STOCK_SYMBOL_FILE);
        try(FileInputStream fis = new FileInputStream(STOCK_SYMBOL_FILE);

            Workbook workbook = new XSSFWorkbook(fis)){
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if(row.getRowNum() == 0 || row.getRowNum() == 1) {
                    continue;
                }
                for (Cell cell : row) {
                    if(cell.getColumnIndex()==2){
                        if(cell.getCellType() == CellType.STRING){
                            securities.add(cell.getStringCellValue());
                        }
                    }
                }
            }

            log.info("Finished reading symbols from file. Loaded {} securities", securities.size());

        } catch (FileNotFoundException e) {
            log.error("Stock symbol source file not found: {}", STOCK_SYMBOL_FILE, e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read stock symbols from workbook", e);
        }
    }



    public List<String> getSecurities(){
        return List.copyOf(this.securities);
    }
}
