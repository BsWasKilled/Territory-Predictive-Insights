package dominio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import excecao.DadosInvalidosException;

class AlertaTest {

    private static final LocalDateTime MOMENTO = LocalDateTime.of(2026, 11, 16, 15, 0, 0);
    private static final long ID_DA_ULTIMA_LEITURA = 12L;

    private static Alerta alertaDeChuva(NivelAlerta nivel, double valorPrevisto, double limite, long idUltimaLeitura) {
        return new Alerta("PLUV-01", TipoSensor.CHUVA, nivel, "Média móvel",
                valorPrevisto, limite, idUltimaLeitura, MOMENTO);
    }

    @Test
    void deveExplicarSensorValorPrevistoELimiteAtingido() {
        // Arrange
        Alerta alerta = alertaDeChuva(NivelAlerta.ALERTA, 31.4, 25.0, ID_DA_ULTIMA_LEITURA);

        // Act
        String explicacao = alerta.explicar();

        // Assert
        assertTrue(explicacao.startsWith("ALERTA"), explicacao);
        assertTrue(explicacao.contains("PLUV-01"), explicacao);
        assertTrue(explicacao.contains("31,4 mm/h"), explicacao);
        assertTrue(explicacao.contains("25,0 mm/h"), explicacao);
        assertTrue(explicacao.contains("Média móvel"), explicacao);
    }

    @Test
    void deveRecusarAlertaCujaPrevisaoNaoAtingeOLimite() {
        // Arrange: previsão de 20 mm/h não atinge o limite de 25 mm/h
        double valorPrevisto = 20.0;
        double limite = 25.0;

        // Act + Assert
        assertThrows(DadosInvalidosException.class,
                () -> alertaDeChuva(NivelAlerta.ALERTA, valorPrevisto, limite, ID_DA_ULTIMA_LEITURA));
    }

    @Test
    void deveReconhecerAlertaQueSoRepeteOAnterior() {
        // Arrange
        Alerta anterior = alertaDeChuva(NivelAlerta.ALERTA, 31.4, 25.0, ID_DA_ULTIMA_LEITURA).comId(7L);
        Alerta novo = alertaDeChuva(NivelAlerta.ALERTA, 31.4, 25.0, ID_DA_ULTIMA_LEITURA);

        // Act
        boolean repetido = novo.repete(anterior);

        // Assert
        assertTrue(repetido);
    }

    @Test
    void deveTratarComoNovoOAlertaBaseadoEmOutraLeitura() {
        // Arrange: mesmos valores, mas chegou uma leitura depois do alerta anterior
        Alerta anterior = alertaDeChuva(NivelAlerta.ALERTA, 31.4, 25.0, ID_DA_ULTIMA_LEITURA).comId(7L);
        Alerta novo = alertaDeChuva(NivelAlerta.ALERTA, 31.4, 25.0, ID_DA_ULTIMA_LEITURA + 1);

        // Act
        boolean repetido = novo.repete(anterior);

        // Assert
        assertFalse(repetido);
    }

    @Test
    void deveTratarComoNovoOAlertaQueMudouDeNivel() {
        // Arrange: a mesma leitura, mas agora a previsão chegou ao nível crítico
        Alerta anterior = alertaDeChuva(NivelAlerta.ALERTA, 31.4, 25.0, ID_DA_ULTIMA_LEITURA).comId(7L);
        Alerta novo = alertaDeChuva(NivelAlerta.CRITICO, 55.0, 50.0, ID_DA_ULTIMA_LEITURA);

        // Act
        boolean repetido = novo.repete(anterior);

        // Assert
        assertFalse(repetido);
        assertFalse(novo.repete(null), "sem alerta anterior, nenhum alerta é repetido");
    }
}
