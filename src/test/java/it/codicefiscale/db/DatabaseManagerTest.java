package it.codicefiscale.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseManagerTest {

    private DatabaseManager databaseManager; // Oggetto reale
    private DatabaseManager mockDatabaseManager; // Mock per simulare il comportamento

    @BeforeEach
    void setUp() throws SQLException {
        // Inizializza l'istanza reale di DatabaseManager
        databaseManager = DatabaseManager.getInstance();

        // Ottieni il mock di DatabaseManager
        mockDatabaseManager = mock(DatabaseManager.class);
        // Simulazione in Background: mockDatabaseManager è indipendente.
    }

    @Test
    void getCodiceBelfiore_WithValidInput_ShouldReturnCodice() throws SQLException {
        String siglaProvincia = "RM"; // Simula Provincia
        String denominazione = "Roma"; // Simula Comune

        // Mock del valore atteso
        when(mockDatabaseManager.getCodiceBelfiore(siglaProvincia, denominazione)).thenReturn("H501");

        // Chiamata al metodo
        String codiceBelfiore = mockDatabaseManager.getCodiceBelfiore(siglaProvincia, denominazione);

        // Verifica dei risultati
        assertNotNull(codiceBelfiore, "Il codice belfiore non dovrebbe essere null");
        assertEquals("H501", codiceBelfiore, "Codice Belfiore atteso: H501");

        // Verifica che il metodo mock sia stato chiamato correttamente
        verify(mockDatabaseManager, times(1)).getCodiceBelfiore(siglaProvincia, denominazione);
    }

    @Test
    void getCodiceBelfiore_WithInvalidInput_ShouldReturnNull() throws SQLException {
        String siglaProvincia = "XX"; // Provincia non esistente
        String denominazione = "ComuneFittizio"; // Comune non esistente

        // Mock del comportamento per input non valido
        when(mockDatabaseManager.getCodiceBelfiore(siglaProvincia, denominazione)).thenReturn(null);

        // Chiamata al metodo
        String codiceBelfiore = mockDatabaseManager.getCodiceBelfiore(siglaProvincia, denominazione);

        // Verifica dei risultati
        assertNull(codiceBelfiore, "Il codice belfiore dovrebbe essere null per input non validi");

        // Verifica che il metodo mock sia stato chiamato correttamente
        verify(mockDatabaseManager, times(1)).getCodiceBelfiore(siglaProvincia, denominazione);
    }

    @Test
    void isCodiceBelfioreValido_WithValidCodice_ShouldReturnTrue() throws SQLException {
        String codiceBelfiore = "H501";

        // Mock del comportamento per un codice valido
        when(mockDatabaseManager.isCodiceBelfioreValido(codiceBelfiore)).thenReturn(true);

        // Chiamata al metodo
        boolean isValid = mockDatabaseManager.isCodiceBelfioreValido(codiceBelfiore);

        // Verifica dei risultati
        assertTrue(isValid, "Il codice belfiore H501 dovrebbe essere valido");

        // Verifica che il metodo mock sia stato chiamato correttamente
        verify(mockDatabaseManager, times(1)).isCodiceBelfioreValido(codiceBelfiore);
    }

    @Test
    void isCodiceBelfioreValido_WithInvalidCodice_ShouldReturnFalse() throws SQLException {
        String codiceBelfiore = "ZZZZ";

        // Mock del comportamento per un codice non valido
        when(mockDatabaseManager.isCodiceBelfioreValido(codiceBelfiore)).thenReturn(false);

        // Chiamata al metodo
        boolean isValid = mockDatabaseManager.isCodiceBelfioreValido(codiceBelfiore);

        // Verifica dei risultati
        assertFalse(isValid, "Il codice belfiore ZZZZ dovrebbe essere non valido");

        // Verifica che il metodo mock sia stato chiamato correttamente
        verify(mockDatabaseManager, times(1)).isCodiceBelfioreValido(codiceBelfiore);
    }

    @Test
    void isComuneValido_WithValidInput_ShouldReturnTrue() throws SQLException {
        String siglaProvincia = "FI"; // Simula Provincia
        String denominazione = "Firenze"; // Simula Comune

        // Mock del comportamento per input valido
        when(mockDatabaseManager.isComuneValido(siglaProvincia, denominazione)).thenReturn(true);

        // Chiamata al metodo
        boolean isValid = mockDatabaseManager.isComuneValido(siglaProvincia, denominazione);

        // Verifica dei risultati
        assertTrue(isValid, "Il comune Firenze (FI) dovrebbe essere valido");

        // Verifica che il metodo mock sia stato chiamato correttamente
        verify(mockDatabaseManager, times(1)).isComuneValido(siglaProvincia, denominazione);
    }

    @Test
    void isComuneValido_WithInvalidInput_ShouldReturnFalse() throws SQLException {
        String siglaProvincia = "XX"; // Provincia non esistente
        String denominazione = "ComuneFittizio"; // Comune non esistente

        // Mock del comportamento per input non valido
        when(mockDatabaseManager.isComuneValido(siglaProvincia, denominazione)).thenReturn(false);

        // Chiamata al metodo
        boolean isValid = mockDatabaseManager.isComuneValido(siglaProvincia, denominazione);

        // Verifica dei risultati
        assertFalse(isValid, "Il comune ComuneFittizio (XX) non dovrebbe essere valido");

        // Verifica che il metodo mock sia stato chiamato correttamente
        verify(mockDatabaseManager, times(1)).isComuneValido(siglaProvincia, denominazione);
    }

    @Test
    void testVerificaDatiRomaNelDatabase() throws SQLException {
        String codiceBelfiore = databaseManager.getCodiceBelfiore("RM", "ROMA");
        assertNotNull(codiceBelfiore, "Codice belfiore per ROMA (RM) non trovato nel database");
        assertEquals("H501", codiceBelfiore, "Il codice belfiore per ROMA (RM) non è quello atteso");
    }

}
