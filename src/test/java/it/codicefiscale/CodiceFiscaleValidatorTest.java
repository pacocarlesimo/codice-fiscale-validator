package it.codicefiscale;

import it.codicefiscale.db.DatabaseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CodiceFiscaleValidatorTest {

    private CodiceFiscaleValidator validator;
    private DatabaseManager mockDbManager;

    @BeforeEach
    void setUp() {
        mockDbManager = mock(DatabaseManager.class);
        validator = new CodiceFiscaleValidator(mockDbManager);
    }

    @Test
    void valida_WithValidCodiceFiscale_ShouldReturnValidResult() throws Exception {
        String codiceFiscale = "RSSMRA85M01H501Q";
        String nome = "Mario";
        String cognome = "Rossi";
        LocalDate dataNascita = LocalDate.of(1985, 8, 1);
        char sesso = 'M';
        String luogoNascita = "Roma";
        String siglaProvincia = "RM";

        when(mockDbManager.isCodiceBelfioreValido("H501")).thenReturn(true);
        when(mockDbManager.getCodiceBelfiore("RM", "Roma")).thenReturn("H501");

        CodiceFiscaleValidator.Risultato risultato =
                validator.valida(codiceFiscale, nome, cognome, dataNascita, sesso, luogoNascita, siglaProvincia);

        // Log temporanei
        System.out.println("Risultato Valido? " + risultato.isValido());
        System.out.println("Messaggio: " + risultato.getMessaggio());
        System.out.println("Errore: " + risultato.getTipoErrore());
        System.out.println("Omocodico? " + risultato.isOmocodico());

        assertTrue(risultato.isValido(), "Il Codice Fiscale dovrebbe essere considerato valido!");
        assertEquals("Codice fiscale valido", risultato.getMessaggio());
        assertEquals(CodiceFiscaleValidator.Risultato.TipoErrore.NESSUN_ERRORE, risultato.getTipoErrore());
        assertFalse(risultato.isOmocodico(), "Il codice fiscale non dovrebbe essere considerato omocodico.");
    }

    @Test
    void valida_WithInvalidFormat_ShouldReturnFormatoNonValidoError() {
        String codiceFiscale = "INVALID";
        String nome = "Mario";
        String cognome = "Rossi";
        LocalDate dataNascita = LocalDate.of(1985, 8, 1);
        char sesso = 'M';
        String luogoNascita = "Roma";
        String siglaProvincia = "RM";

        CodiceFiscaleValidator.Risultato risultato =
                validator.valida(codiceFiscale, nome, cognome, dataNascita, sesso, luogoNascita, siglaProvincia);

        assertFalse(risultato.isValido());
        assertEquals("Il codice fiscale non rispetta il formato corretto", risultato.getMessaggio());
        assertEquals(CodiceFiscaleValidator.Risultato.TipoErrore.FORMATO_NON_VALIDO, risultato.getTipoErrore());
    }

    @Test
    void valida_WithInvalidControlCharacter_ShouldReturnCarattereControlloErratoError() throws Exception {
        String codiceFiscale = "RSSMRA85M01H501X"; // Invalid control character
        String nome = "Mario";
        String cognome = "Rossi";
        LocalDate dataNascita = LocalDate.of(1985, 8, 1);
        char sesso = 'M';
        String luogoNascita = "Roma";
        String siglaProvincia = "RM";

        when(mockDbManager.isCodiceBelfioreValido("H501")).thenReturn(true);

        CodiceFiscaleValidator.Risultato risultato =
                validator.valida(codiceFiscale, nome, cognome, dataNascita, sesso, luogoNascita, siglaProvincia);

        assertFalse(risultato.isValido());
        assertEquals("Il carattere di controllo non è valido", risultato.getMessaggio());
        assertEquals(CodiceFiscaleValidator.Risultato.TipoErrore.CARATTERE_CONTROLLO_ERRATO, risultato.getTipoErrore());
    }

    @Test
    void valida_WithInvalidComuneCode_ShouldReturnComuneNonValidoError() throws Exception {
        String codiceFiscale = "RSSMRA85M01H999Z"; // Invalid comune code
        String nome = "Mario";
        String cognome = "Rossi";
        LocalDate dataNascita = LocalDate.of(1985, 8, 1);
        char sesso = 'M';
        String luogoNascita = "Roma";
        String siglaProvincia = "RM";

        when(mockDbManager.isCodiceBelfioreValido("H999")).thenReturn(false);

        CodiceFiscaleValidator.Risultato risultato =
                validator.valida(codiceFiscale, nome, cognome, dataNascita, sesso, luogoNascita, siglaProvincia);

        assertFalse(risultato.isValido());
        assertEquals("Il codice del comune o nazione non è valido: H999", risultato.getMessaggio());
        assertEquals(CodiceFiscaleValidator.Risultato.TipoErrore.COMUNE_NON_VALIDO, risultato.getTipoErrore());
    }

    @Test
    void testNormalizzazioneOmocodico() {
        String codiceOmocodico = "RSSMRA85M01H501Q";
        String cfNormalizzato = validator.normalizzaCF(codiceOmocodico);
        assertEquals("RSSMRA85M01H501Q", cfNormalizzato, "La normalizzazione del codice omocodico è errata.");
    }
}
