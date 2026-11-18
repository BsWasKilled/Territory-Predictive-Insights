package dominio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Apoio aos testes: monta históricos de leituras em ordem cronológica, com
 * ids 1, 2, 3... como os que o banco geraria. Assim cada teste descreve o
 * cenário só com os valores que interessam.
 */
public final class LeiturasDeTeste {

    private static final LocalDateTime PRIMEIRA_LEITURA = LocalDateTime.of(2026, 11, 16, 8, 0, 0);

    private LeiturasDeTeste() {
        // só tem métodos estáticos: não deve ser instanciada
    }

    /** Leituras do pluviômetro PLUV-01, uma a cada dez minutos, da mais antiga para a mais nova. */
    public static List<Medicao> deChuva(double... valores) {
        return de("PLUV-01", TipoSensor.CHUVA, valores);
    }

    public static List<Medicao> de(String codigoSensor, TipoSensor tipo, double... valores) {
        List<Medicao> leituras = new ArrayList<>();
        for (int i = 0; i < valores.length; i++) {
            long id = i + 1L;
            leituras.add(new Medicao(id, codigoSensor, tipo, valores[i], PRIMEIRA_LEITURA.plusMinutes(10L * i)));
        }
        return leituras;
    }
}
