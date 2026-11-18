package dominio;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatação de números e datas no padrão brasileiro, em um lugar só,
 * para que mensagens, menu e arquivos mostrem os valores do mesmo jeito.
 */
public final class Formato {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private Formato() {
        // classe utilitária: não deve ser instanciada
    }

    /** Número com uma casa decimal e vírgula (ex.: 12,5). */
    public static String numero(double valor) {
        return String.format(PT_BR, "%.1f", valor);
    }

    /** Número inteiro com separador de milhar (ex.: 18.500). */
    public static String inteiro(int valor) {
        return String.format(PT_BR, "%,d", valor);
    }

    /** Data e hora no formato dia/mês/ano hora:minuto:segundo. */
    public static String dataHora(LocalDateTime dataHora) {
        return dataHora.format(DATA_HORA);
    }
}
