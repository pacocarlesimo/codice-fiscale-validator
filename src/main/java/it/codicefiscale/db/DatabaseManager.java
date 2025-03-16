package it.codicefiscale.db;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestisce l'accesso al database SQLite dei comuni e nazioni.
 * Fornisce metodi per verificare la validità dei codici belfiore
 * e ottenere codici belfiore a partire da provincia e denominazione.
 */
public class DatabaseManager {

    // Nome del file del database nelle risorse
    private static final String DB_RESOURCE_PATH = "/database/comuni_nazioni.db";

    // Percorso del file nella directory temporanea
    private static final String DB_TEMP_PATH = System.getProperty("java.io.tmpdir")
            + File.separator + "cf_validator_db.db";

    // Connessione al database
    private Connection connection;

    // Cache per migliorare le performance
    private final Map<String, String> codiciCache = new HashMap<>();
    private final Map<String, Boolean> validitaCache = new HashMap<>();

    // Istanza singleton
    private static DatabaseManager instance;

    /**
     * Ottiene l'istanza singleton del gestore del database.
     *
     * @return L'istanza del gestore
     * @throws SQLException Se si verifica un errore nell'inizializzazione del database
     */
    public static synchronized DatabaseManager getInstance() throws SQLException {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Costruttore privato (pattern Singleton).
     *
     * @throws SQLException Se si verifica un errore nell'inizializzazione del database
     */
    private DatabaseManager() throws SQLException {
        try {
            initializeDatabase();
        } catch (Exception e) {
            throw new SQLException("Errore nell'inizializzazione del database: " + e.getMessage(), e);
        }
    }

    /**
     * Inizializza il database copiandolo dalle risorse se necessario.
     */
    private void initializeDatabase() throws ClassNotFoundException, SQLException, IOException {
        // Carica il driver JDBC di SQLite
        Class.forName("org.sqlite.JDBC");

        // Verifica se il database esiste già nella directory temporanea
        File dbFile = new File(DB_TEMP_PATH);

        // Se il file non esiste o è vuoto, copialo dalle risorse
        if (!dbFile.exists() || dbFile.length() == 0) {
            copyDatabaseFromResources(dbFile);
        }

        // Crea la connessione al database
        connection = DriverManager.getConnection("jdbc:sqlite:" + DB_TEMP_PATH);
    }

    /**
     * Copia il database dalle risorse alla directory temporanea.
     */
    private void copyDatabaseFromResources(File dbFile) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(DB_RESOURCE_PATH)) {
            if (inputStream == null) {
                throw new IOException("Database non trovato nelle risorse: " + DB_RESOURCE_PATH);
            }

            // Crea la directory temporanea se non esiste
            File parentDir = dbFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Copia il file
            Files.copy(inputStream, dbFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Ottiene il codice belfiore per un comune o nazione.
     *
     * @param siglaProvincia Sigla della provincia (o EE per l'estero)
     * @param denominazione Nome del comune o della nazione
     * @return Il codice belfiore, o null se non trovato
     * @throws SQLException Se si verifica un errore nella query
     */
    public String getCodiceBelfiore(String siglaProvincia, String denominazione) throws SQLException {
        if (siglaProvincia == null || denominazione == null) {
            return null;
        }

        // Normalizza gli input
        String provinciaFormattata = siglaProvincia.toUpperCase().trim();
        String denominazioneFormattata = denominazione.toUpperCase().trim();

        // Chiave per la cache
        String chiaveCache = provinciaFormattata + "|" + denominazioneFormattata;

        // Controlla nella cache
        if (codiciCache.containsKey(chiaveCache)) {
            return codiciCache.get(chiaveCache);
        }

        // Query SQL
        // La query cerca solo corrispondenze esatte
        // e ordina per data_inizio_validita in modo da ottenere il codice più recente
        String sql = "SELECT codice_belfiore FROM comuni_nazioni " +
                "WHERE UPPER(sigla_provincia) = ? " +
                "AND UPPER(denominazione_ita) = ? " +
                "ORDER BY data_inizio_validita DESC " +
                "LIMIT 1";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, provinciaFormattata);
            stmt.setString(2, denominazioneFormattata);

            try (ResultSet rs = stmt.executeQuery()) {
                String codiceBelfiore = rs.next() ? rs.getString("codice_belfiore") : null;

                // Salva il risultato nella cache
                codiciCache.put(chiaveCache, codiceBelfiore);

                return codiceBelfiore;
            }
        }
    }

    /**
     * Verifica se un codice belfiore è valido (esiste nel database).
     *
     * @param codiceBelfiore Il codice belfiore da verificare
     * @return true se il codice esiste, false altrimenti
     * @throws SQLException Se si verifica un errore nella query
     */
    public boolean isCodiceBelfioreValido(String codiceBelfiore) throws SQLException {
        if (codiceBelfiore == null) {
            return false;
        }

        String codiceFormattato = codiceBelfiore.toUpperCase().trim();

        // Controlla nella cache
        if (validitaCache.containsKey(codiceFormattato)) {
            return validitaCache.get(codiceFormattato);
        }

        String sql = "SELECT 1 FROM comuni_nazioni WHERE codice_belfiore = ? LIMIT 1";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, codiceFormattato);

            try (ResultSet rs = stmt.executeQuery()) {
                boolean isValido = rs.next();

                // Salva il risultato nella cache
                validitaCache.put(codiceFormattato, isValido);

                return isValido;
            }
        }
    }

    /**
     * Verifica se un comune o nazione è valido.
     *
     * @param siglaProvincia Sigla della provincia
     * @param denominazione Nome del comune o nazione
     * @return true se la combinazione è valida, false altrimenti
     * @throws SQLException Se si verifica un errore nella query
     */
    public boolean isComuneValido(String siglaProvincia, String denominazione) throws SQLException {
        return getCodiceBelfiore(siglaProvincia, denominazione) != null;
    }

    /**
     * Chiude la connessione al database.
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            // Ignora errori in chiusura
        }
    }

    /**
     * Resetta l'istanza singleton (utile per i test).
     */
    public static void reset() {
        if (instance != null) {
            try {
                instance.close();
            } catch (Exception e) {
                // Ignora
            }
            instance = null;
        }
    }
}
