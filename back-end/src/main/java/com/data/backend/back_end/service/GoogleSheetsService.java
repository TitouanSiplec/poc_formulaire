package com.data.backend.back_end.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GoogleSheetsService {

    private static final String APPLICATION_NAME = "Spring Boot Google Sheets";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String SPREADSHEET_ID = "1zlC3wQwomiqNueewINCVL3LOsLkyZ1cqMYylnBLF1vQ";
    private static final String RANGE = "Feuille 1"; 

    public static Sheets getSheetsService() throws Exception {
        FileInputStream serviceAccountStream = new FileInputStream("src/main/resources/poc-formulaire-c9fac0332257.json");

        GoogleCredential credential = GoogleCredential.fromStream(serviceAccountStream)
                .createScoped(Arrays.asList("https://www.googleapis.com/auth/spreadsheets"));

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                credential
        )
        .setApplicationName(APPLICATION_NAME)
        .build();
    }

    public static void addRowToSheet(String sheetName, List<Object> rowData) throws Exception {
        Sheets sheetsService = getSheetsService();
    
        String range = "'" + sheetName + "'";
        ValueRange body = new ValueRange().setValues(List.of(rowData));
    
        AppendValuesResponse result = sheetsService.spreadsheets().values()
                .append(SPREADSHEET_ID, range, body)
                .setValueInputOption("RAW")
                .setInsertDataOption("INSERT_ROWS")
                .execute();
    
        System.out.println("Ligne ajoutée dans la feuille " + sheetName + " : " +
                result.getUpdates().getUpdatedCells() + " cellules modifiées.");
    }
    
    public static void createSheetWithHeaders(String sheetName, List<String> headers) throws Exception {
        Sheets sheetsService = getSheetsService();
    
        // 1. Créer une nouvelle feuille
        AddSheetRequest addSheetRequest = new AddSheetRequest()
                .setProperties(new SheetProperties().setTitle(sheetName));
        Request request = new Request().setAddSheet(addSheetRequest);
        BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(List.of(request));
        sheetsService.spreadsheets().batchUpdate(SPREADSHEET_ID, body).execute();
    
        // 2. Déterminer le range en fonction du nombre de colonnes
        char lastColumn = (char) ('A' + headers.size() - 1);  // ex: 3 colonnes → 'C'
        String range = "'" + sheetName + "'!A1:" + lastColumn + "1"; // ex: 'client'!A1:C1
    
        // 3. Convertir headers en List<List<Object>>
        List<List<Object>> headerRow = List.of(headers.stream().map(h -> (Object) h).toList());
    
        ValueRange valueRange = new ValueRange()
                .setRange(range)
                .setValues(headerRow);
    
        sheetsService.spreadsheets().values()
                .update(SPREADSHEET_ID, range, valueRange)
                .setValueInputOption("RAW")
                .execute();
    }
    


    private static String getColumnLetter(int columnNumber) {
        StringBuilder columnName = new StringBuilder();
        while (columnNumber > 0) {
            int rem = (columnNumber - 1) % 26;
            columnName.insert(0, (char) (rem + 'A'));
            columnNumber = (columnNumber - 1) / 26;
        }
        return columnName.toString();
    }
}

