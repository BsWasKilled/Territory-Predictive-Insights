package dominio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import excecao.DadosInvalidosException;

class MedicaoTest {

    private static final LocalDateTime MOMENTO = LocalDateTime.of(2026, 11, 16, 14, 30, 0);

    @Test
    void deveGuardarOsDadosDeUmaLeituraValida() {
        // Arrange
        String codigoSensor = "PLUV-01";
        double chuva = 12.5;

        // Act
        Medicao medicao = new Medicao(codigoSensor, TipoSensor.CHUVA, chuva, MOMENTO);

        // Assert
        assertEquals("PLUV-01", medicao.getCodigoSensor());
        assertEquals(TipoSensor.CHUVA, medicao.getTipo());
        assertEquals(12.5, medicao.getValor());
        assertEquals(MOMENTO, medicao.getDataHora());
        assertNull(medicao.getId(), "medição que ainda não foi gravada não tem id");
    }

    @Test
    void deveRecusarMedicaoDeChuvaNegativa() {
        // Arrange
        double chuvaNegativa = -1.0;

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> new Medicao("PLUV-01", TipoSensor.CHUVA, chuvaNegativa, MOMENTO));

        // Assert
        assertEquals("medição de chuva negativa: -1,0 mm/h", erro.getMessage());
    }

    @Test
    void deveRecusarMedicaoAcimaDaFaixaDoSensor() {
        // Arrange: a galeria não pode estar mais do que 100% ocupada
        double ocupacaoImpossivel = 120.0;

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> new Medicao("GAL-01", TipoSensor.DRENAGEM, ocupacaoImpossivel, MOMENTO));

        // Assert
        assertTrue(erro.getMessage().contains("fora da faixa: 120,0 %"), erro.getMessage());
    }

    @Test
    void deveAceitarOsExtremosDaFaixaDoSensor() {
        // Arrange
        double minimo = TipoSensor.CHUVA.getMinimo();
        double maximo = TipoSensor.CHUVA.getMaximo();

        // Act + Assert
        assertDoesNotThrow(() -> new Medicao("PLUV-01", TipoSensor.CHUVA, minimo, MOMENTO));
        assertDoesNotThrow(() -> new Medicao("PLUV-01", TipoSensor.CHUVA, maximo, MOMENTO));
    }

    @Test
    void deveRecusarMedicaoSemValorNumerico() {
        // Arrange
        double semValor = Double.NaN;

        // Act + Assert
        assertThrows(DadosInvalidosException.class,
                () -> new Medicao("PLUV-01", TipoSensor.CHUVA, semValor, MOMENTO));
    }

    @Test
    void deveRecusarMedicaoSemCodigoDoSensor() {
        // Arrange
        String codigoEmBranco = "   ";

        // Act + Assert
        assertThrows(DadosInvalidosException.class,
                () -> new Medicao(codigoEmBranco, TipoSensor.CHUVA, 10.0, MOMENTO));
    }

    @Test
    void deveManterOsDadosAoReceberOIdGeradoPeloBanco() {
        // Arrange
        Medicao semId = new Medicao("NIVEL-01", TipoSensor.NIVEL_RIO, 180.0, MOMENTO);

        // Act
        Medicao comId = semId.comId(42L);

        // Assert
        assertEquals(42L, comId.getId());
        assertEquals("NIVEL-01", comId.getCodigoSensor());
        assertEquals(180.0, comId.getValor());
        assertEquals(MOMENTO, comId.getDataHora());
    }
}
