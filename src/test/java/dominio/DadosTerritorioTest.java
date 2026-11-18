package dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import excecao.AcessoNegadoException;
import excecao.DadosInvalidosException;

class DadosTerritorioTest {

    private static final String CODIGO_CORRETO = "CHAVE-DE-TESTE";

    private DadosTerritorio territorio;

    @BeforeEach
    void criarTerritorio() {
        territorio = new DadosTerritorio("Vila de Teste", "Zona Norte", 5000, 2,
                limiaresParaTodosOsSensores(), CODIGO_CORRETO);
    }

    private static Map<TipoSensor, Limiares> limiaresParaTodosOsSensores() {
        Map<TipoSensor, Limiares> limiares = new EnumMap<>(TipoSensor.class);
        limiares.put(TipoSensor.CHUVA, new Limiares(10.0, 25.0, 50.0));
        limiares.put(TipoSensor.NIVEL_RIO, new Limiares(150.0, 220.0, 300.0));
        limiares.put(TipoSensor.DRENAGEM, new Limiares(60.0, 80.0, 95.0));
        return limiares;
    }

    @Test
    void deveRecusarPopulacaoNegativaEManterAAnterior() {
        // Arrange
        int populacaoNegativa = -1;

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> territorio.setPopulacao(populacaoNegativa));

        // Assert
        assertEquals("população não pode ser negativa (informado: -1)", erro.getMessage());
        assertEquals(5000, territorio.getPopulacao());
    }

    @Test
    void deveAceitarPopulacaoValida() {
        // Arrange
        int novaPopulacao = 7200;

        // Act
        territorio.setPopulacao(novaPopulacao);

        // Assert
        assertEquals(7200, territorio.getPopulacao());
    }

    @Test
    void deveRecusarNomeEmBranco() {
        // Arrange
        String nomeEmBranco = "   ";

        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> territorio.setNome(nomeEmBranco));
        assertEquals("Vila de Teste", territorio.getNome());
    }

    @Test
    void deveAlterarLimiaresQuandoOCodigoDeAcessoEstaCorreto() {
        // Arrange
        Limiares novos = new Limiares(15.0, 30.0, 60.0);

        // Act
        territorio.alterarLimiares(CODIGO_CORRETO, TipoSensor.CHUVA, novos);

        // Assert
        assertEquals(15.0, territorio.getLimiares(TipoSensor.CHUVA).getAtencao());
        assertEquals(30.0, territorio.getLimiares(TipoSensor.CHUVA).getAlerta());
        assertEquals(60.0, territorio.getLimiares(TipoSensor.CHUVA).getCritico());
    }

    @Test
    void deveNegarAlteracaoDeLimiaresComCodigoErradoEManterOsAtuais() {
        // Arrange
        Limiares novos = new Limiares(15.0, 30.0, 60.0);

        // Act
        AcessoNegadoException erro = assertThrows(AcessoNegadoException.class,
                () -> territorio.alterarLimiares("codigo-errado", TipoSensor.CHUVA, novos));

        // Assert
        assertEquals("código de acesso incorreto", erro.getMessage());
        assertEquals(10.0, territorio.getLimiares(TipoSensor.CHUVA).getAtencao());
    }

    @Test
    void deveRecusarLimiteCriticoAcimaDoQueOSensorMede() {
        // Arrange: ocupação da galeria vai só até 100%
        Limiares impossiveis = new Limiares(60.0, 80.0, 120.0);

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> territorio.alterarLimiares(CODIGO_CORRETO, TipoSensor.DRENAGEM, impossiveis));

        // Assert
        assertTrue(erro.getMessage().startsWith("limiar inválido"), erro.getMessage());
        assertEquals(95.0, territorio.getLimiares(TipoSensor.DRENAGEM).getCritico());
    }

    @Test
    void deveExigirLimiaresParaTodosOsTiposDeSensor() {
        // Arrange: falta o limiar de chuva
        Map<TipoSensor, Limiares> incompletos = limiaresParaTodosOsSensores();
        incompletos.remove(TipoSensor.CHUVA);

        // Act + Assert
        assertThrows(DadosInvalidosException.class,
                () -> new DadosTerritorio("Vila de Teste", "Zona Norte", 5000, 2, incompletos, CODIGO_CORRETO));
    }

    @Test
    void deveExigirUmCodigoDeAcesso() {
        // Arrange
        String codigoEmBranco = "";

        // Act + Assert
        assertThrows(DadosInvalidosException.class,
                () -> new DadosTerritorio("Vila de Teste", "Zona Norte", 5000, 2,
                        limiaresParaTodosOsSensores(), codigoEmBranco));
    }
}
