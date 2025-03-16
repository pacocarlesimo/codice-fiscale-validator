package it.codicefiscale.db;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Classe di utilità per creare il database SQLite dai dati Excel.
 * Questa classe viene utilizzata come strumento di build, non fa parte della libreria in runtime.
 */
public class DatabaseInitializer {

    private static final String EXCEL_PATH = "src/main/resources/comuni_nazioni_cf.xlsx";
    private static final String DB_PATH = "src/main/resources/database/comuni_nazioni.db";

    // Indici delle colonne nell'Excel
    private static final int COL_SIGLA_PROVINCIA = 0;  // Prima colonna
    private static final int COL_DENOMINAZIONE = 1;    // Seconda colonna
    private static final int COL_CODICE_BELFIORE = 2;  // Terza colonna
    private static final int COL_DATA_INIZIO = 3;      // Quarta colonna
    private static final int COL_DATA_FINE = 4;        // Quinta colonna

    public static void main(String[] args) {
        try {
            // Verifica che la directory esista
            File dbFile = new File(DB_PATH);
            File dbDir = dbFile.getParentFile();
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }

            // Elimina il database se già esiste
            if (dbFile.exists()) {
                dbFile.delete();
            }

            // Crea il database
            System.out.println("Creazione del database SQLite: " + DB_PATH);
            createDatabase();
            System.out.println("Database creato con successo.");

        } catch (Exception e) {
            System.err.println("Errore nella creazione del database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Crea il database SQLite con la struttura delle tabelle e importa i dati dall'Excel.
     */
    private static void createDatabase() throws ClassNotFoundException, SQLException, IOException {
        // Carica il driver JDBC
        Class.forName("org.sqlite.JDBC");

        // Crea il database e le tabelle
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH)) {
            // Disabilita auto-commit per migliorare le performance
            conn.setAutoCommit(false);

            // Crea la tabella e gli indici
            createTables(conn);

            // Importa i dati dall'Excel
            importDataFromExcel(conn);

            // Commit delle modifiche
            conn.commit();

            // Ripristina auto-commit
            conn.setAutoCommit(true);
        }
    }

    /**
     * Crea la tabella e gli indici nel database.
     */
    private static void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Crea la tabella comuni_nazioni
            stmt.execute(
                    "CREATE TABLE comuni_nazioni (" +
                            "sigla_provincia TEXT NOT NULL, " +
                            "denominazione_ita TEXT NOT NULL, " +
                            "codice_belfiore TEXT NOT NULL, " +
                            "data_inizio_validita TEXT, " +
                            "data_fine_validita TEXT, " +
                            "PRIMARY KEY (sigla_provincia, denominazione_ita)" +
                            ")"
            );

            // Crea gli indici
            stmt.execute("CREATE INDEX idx_codice_belfiore ON comuni_nazioni(codice_belfiore)");
            stmt.execute("CREATE INDEX idx_denominazione ON comuni_nazioni(denominazione_ita)");

            System.out.println("Tabella e indici creati con successo");
        }
    }

    /**
     * Importa i dati dal file Excel nel database.
     */
    private static void importDataFromExcel(Connection conn) throws IOException, SQLException {
        // Verifica che il file Excel esista
        File excelFile = new File(EXCEL_PATH);
        if (!excelFile.exists()) {
            throw new IOException("File Excel non trovato: " + EXCEL_PATH);
        }

        String sql = "INSERT INTO comuni_nazioni (sigla_provincia, denominazione_ita, " +
                "codice_belfiore, data_inizio_validita, data_fine_validita) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Seleziona il primo foglio
            Sheet sheet = workbook.getSheetAt(0);

            // Contatori per il log
            int totalRows = 0;
            int importedRows = 0;
            int skipRows = 0;

            // Flag per saltare l'intestazione
            boolean headerSkipped = false;

            // Leggi tutte le righe
            for (Row row : sheet) {
                // Salta l'intestazione
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                totalRows++;

                try {
                    // Estrai i dati dalle celle
                    String siglaProvincia = getCellStringValue(row.getCell(COL_SIGLA_PROVINCIA));
                    String denominazione = getCellStringValue(row.getCell(COL_DENOMINAZIONE));
                    String codiceBelfiore = getCellStringValue(row.getCell(COL_CODICE_BELFIORE));
                    String dataInizio = getCellStringValue(row.getCell(COL_DATA_INIZIO));
                    String dataFine = getCellStringValue(row.getCell(COL_DATA_FINE));

                    // Verifica che i dati essenziali siano presenti
                    if (siglaProvincia == null || denominazione == null || codiceBelfiore == null) {
                        System.err.println("Dati incompleti alla riga " + row.getRowNum() + ": " +
                                "sigla=" + siglaProvincia + ", denominazione=" + denominazione +
                                ", codice=" + codiceBelfiore);
                        skipRows++;
                        continue;
                    }

                    // Inserisci nel database
                    pstmt.setString(1, siglaProvincia);
                    pstmt.setString(2, denominazione);
                    pstmt.setString(3, codiceBelfiore);
                    pstmt.setString(4, dataInizio);
                    pstmt.setString(5, dataFine);
                    pstmt.executeUpdate();

                    importedRows++;

                    // Log ogni 1000 righe e commit intermedio
                    if (importedRows % 1000 == 0) {
                        System.out.println("Importate " + importedRows + " righe...");
                        conn.commit();
                    }
                } catch (Exception e) {
                    System.err.println("Errore alla riga " + row.getRowNum() + ": " + e.getMessage());
                    skipRows++;
                }
            }

            // Commit finale
            conn.commit();

            System.out.println("\nImportazione completata:");
            System.out.println("  Righe totali: " + totalRows);
            System.out.println("  Righe importate: " + importedRows);
            System.out.println("  Righe saltate: " + skipRows);
        }
    }

    /**
     * Ottiene il valore di una cella come stringa.
     *
     * @param cell La cella da cui estrarre il valore
     * @return Il valore della cella come stringa, o null se la cella è nulla o vuota
     */
    private static String getCellStringValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                } else {
                    // Per numeri, formatta senza decimali se è un intero
                    double value = cell.getNumericCellValue();
                    if (value == Math.floor(value)) {
                        return String.format("%.0f", value);
                    } else {
                        return String.valueOf(value);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        return "";
                    }
                }
            case BLANK:
                return null;
            default:
                return null;
        }
    }
}
