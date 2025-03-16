package it.codicefiscale;

import it.codicefiscale.db.DatabaseManager;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe per la validazione del codice fiscale italiano,
 * incluso il supporto per i codici fiscali omocodici.
 */
public class CodiceFiscaleValidator {

    // Espressione regolare per il formato del codice fiscale (inclusi omocodici)
    // Le posizioni 6-7, 9-10, 12-13-14-15 possono contenere cifre o lettere omocodiche
    private static final String REGEX_CODICE_FISCALE = "^[A-Z]{6}[0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{3}[A-Z]$";
    private static final Pattern PATTERN_CODICE_FISCALE = Pattern.compile(REGEX_CODICE_FISCALE);

    // Le precedenti definizioni di costanti rimangono invariate
    private static final String MESI = "ABCDEHLMPRST";
    private static final String CONSONANTI = "BCDFGHJKLMNPQRSTVWXYZ";
    private static final String VOCALI = "AEIOU";
    private static final String CARATTERI_CONTROLLO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int[] VALORI_DISPARI = {1, 0, 5, 7, 9, 13, 15, 17, 19, 21, 2, 4, 18, 20, 11, 3, 6, 8, 12, 14, 16, 10, 22, 25, 24, 23};
    private static final int[] VALORI_PARI = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25};

    // Tabella di conversione omocodica (lettera -> numero)
    private static final Map<Character, Character> CONVERSIONE_OMOCODICI = new HashMap<>();
    static {
        CONVERSIONE_OMOCODICI.put('L', '0');
        CONVERSIONE_OMOCODICI.put('M', '1');
        CONVERSIONE_OMOCODICI.put('N', '2');
        CONVERSIONE_OMOCODICI.put('P', '3');
        CONVERSIONE_OMOCODICI.put('Q', '4');
        CONVERSIONE_OMOCODICI.put('R', '5');
        CONVERSIONE_OMOCODICI.put('S', '6');
        CONVERSIONE_OMOCODICI.put('T', '7');
        CONVERSIONE_OMOCODICI.put('U', '8');
        CONVERSIONE_OMOCODICI.put('V', '9');
    }

    // Manager del database
    private final DatabaseManager dbManager;

    /**
     * Risultato della validazione del codice fiscale.
     */
    public static class Risultato {
        private final boolean valido;
        private final String messaggio;
        private final TipoErrore tipoErrore;
        private final boolean isOmocodico;  // Aggiungiamo questo campo per indicare se è un CF omocodico

        /**
         * Tipo di errore nella validazione del codice fiscale.
         */
        public enum TipoErrore {
            NESSUN_ERRORE,
            FORMATO_NON_VALIDO,
            CARATTERE_CONTROLLO_ERRATO,
            DATA_NON_VALIDA,
            COMUNE_NON_VALIDO,
            DATI_ANAGRAFICI_NON_CORRISPONDENTI,
            ERRORE_DATABASE
        }

        /**
         * Costruttore della classe Risultato.
         */
        public Risultato(boolean valido, String messaggio, TipoErrore tipoErrore) {
            this(valido, messaggio, tipoErrore, false);
        }

        /**
         * Costruttore della classe Risultato con flag per omocodici.
         */
        public Risultato(boolean valido, String messaggio, TipoErrore tipoErrore, boolean isOmocodico) {
            this.valido = valido;
            this.messaggio = messaggio;
            this.tipoErrore = tipoErrore;
            this.isOmocodico = isOmocodico;
        }

        public boolean isValido() {
            return valido;
        }

        public String getMessaggio() {
            return messaggio;
        }

        public TipoErrore getTipoErrore() {
            return tipoErrore;
        }

        public boolean isOmocodico() {
            return isOmocodico;
        }

        public boolean isFormaleValido() {
            return valido ||
                    (tipoErrore == TipoErrore.DATI_ANAGRAFICI_NON_CORRISPONDENTI);
        }
    }

    /**
     * Costruttore che inizializza il validator con l'istanza singleton del DatabaseManager.
     */
    public CodiceFiscaleValidator() {
        try {
            this.dbManager = DatabaseManager.getInstance();
        } catch (SQLException e) {
            throw new RuntimeException("Errore nell'inizializzazione del database: " + e.getMessage(), e);
        }
    }

    /**
     * Costruttore per testing.
     */
    public CodiceFiscaleValidator(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Converte un codice fiscale omocodico nella sua forma standard con numeri.
     *
     * @param cf Il codice fiscale da normalizzare
     * @return Il codice fiscale convertito con lettere omocodiche sostituite da numeri
     */
    public String normalizzaCF(String cf) {
        if (cf == null || cf.length() != 16) {
            return cf;
        }

        StringBuilder cfNormalizzato = new StringBuilder(cf);
        boolean isOmocodico = false;

        // Posizioni dei caratteri che possono essere sostituiti
        // (6-7, 9-10, 12-13-14-15)
        int[] posizioniNumeriche = {6, 7, 9, 10, 12, 13, 14};

        for (int pos : posizioniNumeriche) {
            char c = cf.charAt(pos);
            if (CONVERSIONE_OMOCODICI.containsKey(c)) {
                cfNormalizzato.setCharAt(pos, CONVERSIONE_OMOCODICI.get(c));
                isOmocodico = true;
            }
        }

        return cfNormalizzato.toString();
    }

    /**
     * Verifica se un codice fiscale è omocodico (contiene lettere al posto di numeri).
     *
     * @param cf Il codice fiscale da verificare
     * @return true se il codice fiscale è omocodico, false altrimenti
     */
    public boolean isOmocodico(String cf) {
        if (cf == null || cf.length() != 16) {
            return false;
        }

        int[] posizioniNumeriche = {6, 7, 9, 10, 12, 13, 14};

        for (int pos : posizioniNumeriche) {
            char c = cf.charAt(pos);
            if (c >= 'A' && c <= 'Z' && CONVERSIONE_OMOCODICI.containsKey(c)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Valida un codice fiscale verificandone la correttezza formale,
     * supportando anche i codici fiscali omocodici.
     *
     * @param codiceFiscale Codice fiscale da validare
     * @return Risultato della validazione
     */
    public Risultato validaFormato(String codiceFiscale) {
        // Normalizza e verifica il codice fiscale
        if (codiceFiscale == null) {
            return new Risultato(false, "Il codice fiscale è null", Risultato.TipoErrore.FORMATO_NON_VALIDO);
        }

        codiceFiscale = codiceFiscale.toUpperCase().trim();

        // 1. Verifica il formato (anche con gli omocodici)
        if (!PATTERN_CODICE_FISCALE.matcher(codiceFiscale).matches()) {
            return new Risultato(false,
                    "Il codice fiscale non rispetta il formato corretto",
                    Risultato.TipoErrore.FORMATO_NON_VALIDO);
        }

        boolean isOmocodico = isOmocodico(codiceFiscale);
        String cfNormalizzato = isOmocodico ? normalizzaCF(codiceFiscale) : codiceFiscale;

        // 2. Verifica della data di nascita
        try {
            LocalDate dataNascita = estraiDataNascita(cfNormalizzato);
        } catch (DateTimeParseException e) {
            return new Risultato(false,
                    "La data di nascita nel codice fiscale non è valida: " + e.getMessage(),
                    Risultato.TipoErrore.DATA_NON_VALIDA,
                    isOmocodico);
        }

        // 3. Verifica del codice comune/nazione
        String codiceBelfiore = cfNormalizzato.substring(11, 15);
        try {
            if (!dbManager.isCodiceBelfioreValido(codiceBelfiore)) {
                return new Risultato(false,
                        "Il codice del comune o nazione non è valido: " + codiceBelfiore,
                        Risultato.TipoErrore.COMUNE_NON_VALIDO,
                        isOmocodico);
            }
        } catch (SQLException e) {
            return new Risultato(false,
                    "Errore durante la verifica del comune: " + e.getMessage(),
                    Risultato.TipoErrore.ERRORE_DATABASE,
                    isOmocodico);
        }

        // 4. Verifica del carattere di controllo (ultimo controllo)
        String cfSenzaControllo = cfNormalizzato.substring(0, 15);
        char carattereControlloAtteso = calcolaCarattereControllo(cfSenzaControllo);

        if (cfNormalizzato.charAt(15) != carattereControlloAtteso) {
            return new Risultato(false,
                    "Il carattere di controllo non è valido",
                    Risultato.TipoErrore.CARATTERE_CONTROLLO_ERRATO,
                    isOmocodico);
        }

        return new Risultato(true,
                isOmocodico ? "Codice fiscale omocodico valido" : "Codice fiscale valido",
                Risultato.TipoErrore.NESSUN_ERRORE,
                isOmocodico);
    }

    /**
     * Valida un codice fiscale rispetto ai dati anagrafici.
     * Supporta anche i codici fiscali omocodici.
     *
     * @param codiceFiscale Codice fiscale da validare
     * @param nome Nome della persona
     * @param cognome Cognome della persona
     * @param dataNascita Data di nascita (formato LocalDate)
     * @param sesso Sesso ('M' o 'F')
     * @param luogoNascita Comune/nazione di nascita
     * @param siglaProvincia Sigla della provincia (o 'EE' per estero)
     * @return Risultato della validazione
     */
    public Risultato valida(String codiceFiscale, String nome, String cognome,
                            LocalDate dataNascita, char sesso,
                            String luogoNascita, String siglaProvincia) {

        // Prima controlla che il codice fiscale sia formalmente valido
        Risultato risultato = validaFormato(codiceFiscale);
        if (!risultato.isFormaleValido()) {
            return risultato;
        }

        // Normalizza il CF se è omocodico
        boolean isOmocodico = isOmocodico(codiceFiscale);
        String cfNormalizzato = isOmocodico ? normalizzaCF(codiceFiscale) : codiceFiscale;

        // Genera il codice fiscale atteso in base ai dati anagrafici
        String cfAtteso = generaCodiceFiscale(nome, cognome, dataNascita, sesso, luogoNascita, siglaProvincia);
        if (cfAtteso == null) {
            return new Risultato(false,
                    "Impossibile generare il codice fiscale dai dati anagrafici forniti",
                    Risultato.TipoErrore.DATI_ANAGRAFICI_NON_CORRISPONDENTI,
                    isOmocodico);
        }

        // Confronta il codice fiscale normalizzato con quello atteso
        if (!cfNormalizzato.equals(cfAtteso)) {
            return new Risultato(false,
                    "Il codice fiscale non corrisponde ai dati anagrafici forniti",
                    Risultato.TipoErrore.DATI_ANAGRAFICI_NON_CORRISPONDENTI,
                    isOmocodico);
        }

        return new Risultato(true,
                isOmocodico ? "Codice fiscale omocodico valido" : "Codice fiscale valido",
                Risultato.TipoErrore.NESSUN_ERRORE,
                isOmocodico);
    }

    /**
     * Calcola il carattere di controllo per il codice fiscale.
     *
     * @param cfSenzaControllo I primi 15 caratteri del codice fiscale
     * @return Il carattere di controllo calcolato
     */
    char calcolaCarattereControllo(String cfSenzaControllo) {
        int somma = 0;

        for (int i = 0; i < cfSenzaControllo.length(); i++) {
            char c = cfSenzaControllo.charAt(i);
            int valore;

            // Se è una cifra, converti in intero
            if (Character.isDigit(c)) {
                valore = Character.getNumericValue(c);
            } else {
                // Se è una lettera, prendi il valore da 0 a 25 (A=0, B=1, ecc.)
                valore = c - 'A';
            }

            // Applica i valori dispari o pari a seconda della posizione
            if ((i + 1) % 2 == 0) {  // Posizioni pari (2,4,6...)
                somma += VALORI_PARI[valore];
            } else {  // Posizioni dispari (1,3,5...)
                somma += VALORI_DISPARI[valore];
            }
        }

        // Il resto della divisione per 26 dà l'indice del carattere di controllo
        int resto = somma % 26;
        return CARATTERI_CONTROLLO.charAt(resto);
    }

    /**
     * Estrae la data di nascita dal codice fiscale.
     *
     * @param codiceFiscale Codice fiscale (già normalizzato)
     * @return Data di nascita ricavata dal codice fiscale
     * @throws DateTimeParseException Se la data non è valida
     */
    LocalDate estraiDataNascita(String codiceFiscale) throws DateTimeParseException {
        int anno = Integer.parseInt(codiceFiscale.substring(6, 8));
        int indiceMese = MESI.indexOf(codiceFiscale.charAt(8));
        int giorno = Integer.parseInt(codiceFiscale.substring(9, 11));

        // Correggi per il sesso femminile (si sottraggono 40 dal giorno)
        if (giorno > 40) {
            giorno -= 40;
        }

        // Determina il secolo (1900 o 2000)
        int secoloAttuale = LocalDate.now().getYear() / 100;
        int annoCompleto = (secoloAttuale - 1) * 100 + anno;
        if (annoCompleto > LocalDate.now().getYear()) {
            annoCompleto -= 100;
        }

        // Crea data
        return LocalDate.of(annoCompleto, indiceMese + 1, giorno);
    }

    /**
     * Genera il codice fiscale a partire dai dati anagrafici.
     * Per semplicità, si assume che si stia cercando di generare
     * il codice fiscale standard (non omocodico).
     *
     * @return Il codice fiscale generato o null se non è possibile generarlo
     */
    String generaCodiceFiscale(String nome, String cognome,
                               LocalDate dataNascita, char sesso,
                               String luogoNascita, String siglaProvincia) {
        try {
            // Codice per cognome (3 consonanti o integrato con vocali)
            String codiceCognome = generaCodiceCognome(cognome.toUpperCase());

            // Codice per nome (3 consonanti o integrato con vocali)
            String codiceNome = generaCodiceNome(nome.toUpperCase());

            // Codice per data di nascita e sesso
            String annoStr = String.format("%02d", dataNascita.getYear() % 100);
            char codMese = MESI.charAt(dataNascita.getMonthValue() - 1);
            int giornoBase = dataNascita.getDayOfMonth();
            // Per le donne si aggiungono 40 al giorno
            if (sesso == 'F') {
                giornoBase += 40;
            }
            String giornoStr = String.format("%02d", giornoBase);

            // Codice belfiore (comune o nazione)
            String codiceBelfiore = dbManager.getCodiceBelfiore(siglaProvincia, luogoNascita);

            if (codiceBelfiore == null) {
                return null; // Comune/nazione non trovato
            }

            // Assembla il codice fiscale senza carattere di controllo
            String cfSenzaControllo = codiceCognome + codiceNome + annoStr + codMese + giornoStr + codiceBelfiore;

            // Aggiungi carattere di controllo
            char carattereControllo = calcolaCarattereControllo(cfSenzaControllo);

            return cfSenzaControllo + carattereControllo;
        } catch (Exception e) {
            // In caso di errore nel processo di generazione
            return null;
        }
    }

    /**
     * Genera il codice per il cognome (prime 3 lettere del CF).
     */
    String generaCodiceCognome(String cognome) {
        return generaCodice(cognome, false);
    }

    /**
     * Genera il codice per il nome (lettere 4-6 del CF).
     */
    String generaCodiceNome(String nome) {
        return generaCodice(nome, true);
    }

    /**
     * Metodo comune per generare il codice da nome o cognome.
     * Per il nome, se ci sono più di 3 consonanti, si prendono la 1ª, 3ª e 4ª.
     */
    private String generaCodice(String testo, boolean isNome) {
        // Rimuovi spazi, apostrofi e caratteri speciali
        testo = testo.replaceAll("[^a-zA-Z]", "").toUpperCase();

        // Estrai consonanti e vocali
        StringBuilder consonanti = new StringBuilder();
        StringBuilder vocali = new StringBuilder();

        for (char c : testo.toCharArray()) {
            if (CONSONANTI.indexOf(c) >= 0) {
                consonanti.append(c);
            } else if (VOCALI.indexOf(c) >= 0) {
                vocali.append(c);
            }
        }

        // Regola speciale per il nome se ci sono più di 3 consonanti
        String consonantiFinali = consonanti.toString();
        if (isNome && consonantiFinali.length() > 3) {
            consonantiFinali = "" + consonantiFinali.charAt(0) +
                    consonantiFinali.charAt(2) +
                    consonantiFinali.charAt(3);
        }

        // Componi il risultato
        StringBuilder risultato = new StringBuilder();
        risultato.append(consonantiFinali);
        risultato.append(vocali);

        // Se non ci sono abbastanza caratteri, aggiungi 'X'
        while (risultato.length() < 3) {
            risultato.append('X');
        }

        return risultato.substring(0, 3);
    }
}
