package dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import excecao.DadosInvalidosException;

class PrevisaoComparacaoTest {

    private static final double TOLERANCIA = 0.0001;

    @Test
    void deveRepetirAAltaDoPeriodoRecenteNaPrevisao() {
        // Arrange: média 10 no período anterior e 20 no recente; subiu 10, então prevê 20 + 10
        EstrategiaPrevisao estrategia = new PrevisaoComparacao(3);
        List<Medicao> historico = LeiturasDeTeste.deChuva(10, 10, 10, 20, 20, 20);

        // Act
        double previsto = estrategia.prever(historico);

        // Assert
        assertEquals(30.0, previsto, TOLERANCIA);
    }

    @Test
    void deveRepetirAQuedaDoPeriodoRecenteNaPrevisao() {
        // Arrange: média 40 no período anterior e 30 no recente; caiu 10, então prevê 30 - 10
        EstrategiaPrevisao estrategia = new PrevisaoComparacao(3);
        List<Medicao> historico = LeiturasDeTeste.deChuva(40, 40, 40, 30, 30, 30);

        // Act
        double previsto = estrategia.prever(historico);

        // Assert
        assertEquals(20.0, previsto, TOLERANCIA);
    }

    @Test
    void deveManterOValorQuandoAsLeiturasEstaoEstaveis() {
        // Arrange
        EstrategiaPrevisao estrategia = new PrevisaoComparacao(3);
        List<Medicao> historico = LeiturasDeTeste.deChuva(15, 15, 15, 15, 15, 15);

        // Act
        double previsto = estrategia.prever(historico);
        String explicacao = estrategia.explicar(historico);

        // Assert
        assertEquals(15.0, previsto, TOLERANCIA);
        assertTrue(explicacao.contains("ficou estável"), explicacao);
    }

    @Test
    void deveUsarSomenteOsDoisPeriodosMaisRecentes() {
        // Arrange: as duas primeiras leituras (100 e 100) são mais antigas que os dois períodos
        EstrategiaPrevisao estrategia = new PrevisaoComparacao(3);
        List<Medicao> historico = LeiturasDeTeste.deChuva(100, 100, 10, 10, 10, 20, 20, 20);

        // Act
        double previsto = estrategia.prever(historico);

        // Assert
        assertEquals(30.0, previsto, TOLERANCIA);
    }

    @Test
    void naoDevePreverChuvaNegativaQuandoAQuedaEForte() {
        // Arrange: de 30 para 5 a queda é de 25; 5 - 25 daria -20 mm/h, que não existe
        EstrategiaPrevisao estrategia = new PrevisaoComparacao(3);
        List<Medicao> historico = LeiturasDeTeste.deChuva(30, 30, 30, 5, 5, 5);

        // Act
        double previsto = estrategia.prever(historico);

        // Assert
        assertEquals(0.0, previsto, TOLERANCIA);
    }

    @Test
    void naoDevePreverAcimaDoMaximoQueOSensorMede() {
        // Arrange: de 40% para 90% a alta é de 50; 90 + 50 daria 140% de ocupação
        EstrategiaPrevisao estrategia = new PrevisaoComparacao(3);
        List<Medicao> historico = LeiturasDeTeste.de("GAL-01", TipoSensor.DRENAGEM, 40, 40, 40, 90, 90, 90);

        // Act
        double previsto = estrategia.prever(historico);

        // Assert
        assertEquals(100.0, previsto, TOLERANCIA);
    }

    @Test
    void deveExigirDoisPeriodosCompletosDeLeituras() {
        // Arrange: dois períodos de 3 leituras pedem 6; só há 5
        EstrategiaPrevisao estrategia = new PrevisaoComparacao(3);
        List<Medicao> historicoCurto = LeiturasDeTeste.deChuva(10, 10, 10, 20, 20);

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> estrategia.prever(historicoCurto));

        // Assert
        assertEquals(6, estrategia.getLeiturasNecessarias());
        assertTrue(erro.getMessage().contains("há 5 de 6 leituras necessárias"), erro.getMessage());
    }

    @Test
    void deveExplicarAsMediasComparadasEATendencia() {
        // Arrange
        EstrategiaPrevisao estrategia = new PrevisaoComparacao(3);
        List<Medicao> historico = LeiturasDeTeste.deChuva(10, 10, 10, 20, 20, 20);

        // Act
        String explicacao = estrategia.explicar(historico);

        // Assert
        assertTrue(explicacao.contains("média de 20,0 mm/h"), explicacao);
        assertTrue(explicacao.contains("contra 10,0 mm/h"), explicacao);
        assertTrue(explicacao.contains("subiu 10,0 mm/h"), explicacao);
    }
}
