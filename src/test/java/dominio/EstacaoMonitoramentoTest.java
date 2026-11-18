package dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import excecao.DadosInvalidosException;

class EstacaoMonitoramentoTest {

    private EstacaoMonitoramento estacao;

    @BeforeEach
    void criarEstacao() {
        estacao = new EstacaoMonitoramento("EST-01", "Estação de Teste", "Margem do córrego");
    }

    /** Sensor simulado pelo Mockito que só informa se está respondendo ou não. */
    private static Sensor sensorQue(boolean responde) {
        Sensor sensor = mock(Sensor.class);
        when(sensor.verificarFuncionamento()).thenReturn(responde);
        return sensor;
    }

    @Test
    void deveComecarSemVerificacao() {
        // Arrange + Act
        StatusComponente status = estacao.getStatus();

        // Assert
        assertEquals(StatusComponente.NAO_VERIFICADO, status);
    }

    @Test
    void deveFicarOperacionalQuandoTodosOsSensoresRespondem() {
        // Arrange
        estacao.adicionarSensor(sensorQue(true));
        estacao.adicionarSensor(sensorQue(true));

        // Act
        StatusComponente status = estacao.verificarStatus();

        // Assert
        assertEquals(StatusComponente.OPERACIONAL, status);
        assertEquals(StatusComponente.OPERACIONAL, estacao.getStatus());
    }

    @Test
    void deveFicarParcialQuandoSoParteDosSensoresResponde() {
        // Arrange
        estacao.adicionarSensor(sensorQue(true));
        estacao.adicionarSensor(sensorQue(false));

        // Act
        StatusComponente status = estacao.verificarStatus();

        // Assert
        assertEquals(StatusComponente.PARCIAL, status);
        assertEquals(1, estacao.contarSensoresFuncionando());
    }

    @Test
    void deveFicarInoperanteQuandoNenhumSensorResponde() {
        // Arrange
        estacao.adicionarSensor(sensorQue(false));
        estacao.adicionarSensor(sensorQue(false));

        // Act
        StatusComponente status = estacao.verificarStatus();

        // Assert
        assertEquals(StatusComponente.INOPERANTE, status);
    }

    @Test
    void deveFicarInoperanteQuandoNaoTemSensores() {
        // Arrange: estação recém-criada, sem sensores

        // Act
        StatusComponente status = estacao.verificarStatus();

        // Assert
        assertEquals(StatusComponente.INOPERANTE, status);
    }

    @Test
    void deveRecusarComponenteSemIdentificacao() {
        // Arrange
        String idEmBranco = " ";

        // Act + Assert
        assertThrows(DadosInvalidosException.class,
                () -> new EstacaoMonitoramento(idEmBranco, "Estação de Teste", "Margem do córrego"));
    }

    @Test
    void deveRecusarSensorVazio() {
        // Arrange
        Sensor sensorVazio = null;

        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> estacao.adicionarSensor(sensorVazio));
    }
}
