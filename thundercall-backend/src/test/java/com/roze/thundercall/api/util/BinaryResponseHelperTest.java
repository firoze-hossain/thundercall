package com.roze.thundercall.api.util;

import com.roze.thundercall.api.utils.BinaryResponseHelper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.junit.jupiter.api.Assertions.*;

class BinaryResponseHelperTest {

    @Test
    void testExcelOpenXmlIsRecognizedAsBinary() {
        String xlsxCt = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        assertTrue(BinaryResponseHelper.isBinary(xlsxCt), "OpenXML spreadsheet must be recognized as binary");
        assertEquals(".xlsx", BinaryResponseHelper.guessExtension(xlsxCt));
    }

    @Test
    void testLegacyExcelIsRecognizedAsBinary() {
        String xlsCt = "application/vnd.ms-excel";
        assertTrue(BinaryResponseHelper.isBinary(xlsCt), "Legacy MS Excel must be recognized as binary");
        assertEquals(".xls", BinaryResponseHelper.guessExtension(xlsCt));
    }

    @Test
    void testOfficeDocumentsAreRecognizedAsBinary() {
        String docxCt = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        String pptxCt = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        assertTrue(BinaryResponseHelper.isBinary(docxCt));
        assertTrue(BinaryResponseHelper.isBinary(pptxCt));
        assertEquals(".docx", BinaryResponseHelper.guessExtension(docxCt));
        assertEquals(".pptx", BinaryResponseHelper.guessExtension(pptxCt));
    }

    @Test
    void testPdfIsRecognizedAsBinary() {
        String pdfCt = "application/pdf";
        assertTrue(BinaryResponseHelper.isBinary(pdfCt));
        assertEquals(".pdf", BinaryResponseHelper.guessExtension(pdfCt));
    }

    @Test
    void testMagicBytesDetection() {
        // ZIP / XLSX header: PK\x03\x04
        byte[] zipHeader = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x14, 0x00};
        assertTrue(BinaryResponseHelper.isBinary("application/octet-stream", zipHeader));
        assertTrue(BinaryResponseHelper.isBinary(null, zipHeader));

        // PDF header: %PDF
        byte[] pdfHeader = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D};
        assertTrue(BinaryResponseHelper.isBinary(null, pdfHeader));
    }

    @Test
    void testTextAndXmlAreNotBinary() {
        assertFalse(BinaryResponseHelper.isBinary("application/json"));
        assertFalse(BinaryResponseHelper.isBinary("application/xml"));
        assertFalse(BinaryResponseHelper.isBinary("text/xml"));
        assertFalse(BinaryResponseHelper.isBinary("application/soap+xml"));
        assertFalse(BinaryResponseHelper.isBinary("text/plain"));
        assertFalse(BinaryResponseHelper.isBinary("text/html"));
        assertFalse(BinaryResponseHelper.isBinary("text/csv"));
    }

    @Test
    void testGuessFileNameWithExcelUrl() {
        HttpHeaders headers = new HttpHeaders();
        String url = "http://localhost:8080/bbill/patient_bill_summary_report_print_excel?fromDate=05-10-2026";
        String ct = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        String fileName = BinaryResponseHelper.guessFileName(headers, url, ct);
        assertEquals("patient_bill_summary_report_print_excel.xlsx", fileName);
    }

    @Test
    void testGuessFileNameWithContentDisposition() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"custom_report.xlsx\"");
        String url = "http://localhost:8080/reports/download";
        String ct = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        String fileName = BinaryResponseHelper.guessFileName(headers, url, ct);
        assertEquals("custom_report.xlsx", fileName);
    }

    public static void main(String[] args) {
        BinaryResponseHelperTest test = new BinaryResponseHelperTest();
        test.testExcelOpenXmlIsRecognizedAsBinary();
        test.testLegacyExcelIsRecognizedAsBinary();
        test.testOfficeDocumentsAreRecognizedAsBinary();
        test.testPdfIsRecognizedAsBinary();
        test.testMagicBytesDetection();
        test.testTextAndXmlAreNotBinary();
        test.testGuessFileNameWithExcelUrl();
        test.testGuessFileNameWithContentDisposition();
        System.out.println("ALL BINARY RESPONSE HELPER TESTS PASSED SUCCESSFULLY!");
    }
}
