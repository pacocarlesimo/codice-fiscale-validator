package it.codicefiscale;

/**
 * Demo della libreria CodiceFiscaleValidator.
 */
public class Main {
    public static void main(String[] args) {
        // Creare un'istanza del validatore
        CodiceFiscaleValidator validator = new CodiceFiscaleValidator();

        // Esempio: validazione di un codice fiscale
        String codiceFiscale = "RSSMRA80A01H501U";
        CodiceFiscaleValidator.Risultato risultato = validator.validaFormato(codiceFiscale);

        System.out.println("Validazione del codice fiscale: " + codiceFiscale);
        System.out.println("Valido: " + risultato.isValido());
        System.out.println("Messaggio: " + risultato.getMessaggio());
        System.out.println("Tipo errore: " + risultato.getTipoErrore());

        // Esempio: codice fiscale non valido
        String codiceFiscaleErrato = "RSSMRA80A01H501X";
        CodiceFiscaleValidator.Risultato risultatoErrato = validator.validaFormato(codiceFiscaleErrato);

        System.out.println("\nValidazione del codice fiscale errato: " + codiceFiscaleErrato);
        System.out.println("Valido: " + risultatoErrato.isValido());
        System.out.println("Messaggio: " + risultatoErrato.getMessaggio());
        System.out.println("Tipo errore: " + risultatoErrato.getTipoErrore());
    }
}
