package servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dominio.Limiares;
import dominio.TipoSensor;
import excecao.AcessoNegadoException;

class ConfiguracaoSistemaTest {

    private static final double TOLERANCIA = 0.0001;

    private ConfiguracaoSistema configuracao;

    @BeforeEach
    void usarConfiguracaoPadrao() {
        configuracao = ConfiguracaoSistema.getInstancia();
        configuracao.restaurarPadroes();
    }

    @AfterEach
    void devolverConfiguracaoAoPadrao() {
        configuracao.restaurarPadroes();
    }

    @Test
    void deveDevolverSempreAMesmaInstancia() {
        // Arrange
        ConfiguracaoSistema primeira = ConfiguracaoSistema.getInstancia();

        // Act
        ConfiguracaoSistema segunda = ConfiguracaoSistema.getInstancia();

        // Assert
        assertSame(primeira, segunda);
    }

    @Test
    void deveComecarComOsLimiaresEParametrosPadrao() {
        // Arrange + Act
        Limiares chuva = configuracao.getLimiares(TipoSensor.CHUVA);

        // Assert
        assertEquals(10.0, chuva.getAtencao(), TOLERANCIA);
        assertEquals(25.0, chuva.getAlerta(), TOLERANCIA);
        assertEquals(50.0, chuva.getCritico(), TOLERANCIA);
        assertEquals(5, configuracao.getMinimoLeituras());
        assertEquals(5, configuracao.getJanelaMediaMovel());
        assertEquals(3, configuracao.getPeriodoComparacao());
        assertEquals(List.of(FabricaPrevisao.MEDIA_MOVEL, FabricaPrevisao.COMPARACAO),
                configuracao.getEstrategiasAtivas());
    }

    @Test
    void deveRefletirNaHoraOLimiarAlteradoNoTerritorio() {
        // Arrange
        Limiares novos = new Limiares(15.0, 30.0, 60.0);

        // Act
        configuracao.getTerritorio().alterarLimiares(ConfiguracaoSistema.CODIGO_ACESSO_INICIAL,
                TipoSensor.CHUVA, novos);

        // Assert: quem consulta a configuração já enxerga os novos limites
        assertSame(novos, configuracao.getLimiares(TipoSensor.CHUVA));
        assertSame(novos, ConfiguracaoSistema.getInstancia().getLimiares(TipoSensor.CHUVA));
    }

    @Test
    void deveManterOsLimiaresQuandoOCodigoDeAcessoEstaErrado() {
        // Arrange
        Limiares novos = new Limiares(15.0, 30.0, 60.0);

        // Act
        assertThrows(AcessoNegadoException.class,
                () -> configuracao.getTerritorio().alterarLimiares("codigo-errado", TipoSensor.CHUVA, novos));

        // Assert
        assertEquals(10.0, configuracao.getLimiares(TipoSensor.CHUVA).getAtencao(), TOLERANCIA);
    }

    @Test
    void deveVoltarAosValoresIniciaisAoRestaurarPadroes() {
        // Arrange
        configuracao.getTerritorio().setNome("Outro território");
        configuracao.getTerritorio().alterarLimiares(ConfiguracaoSistema.CODIGO_ACESSO_INICIAL,
                TipoSensor.DRENAGEM, new Limiares(10.0, 20.0, 30.0));

        // Act
        configuracao.restaurarPadroes();

        // Assert
        assertEquals("Comunidade Córrego Azul", configuracao.getTerritorio().getNome());
        assertEquals(60.0, configuracao.getLimiares(TipoSensor.DRENAGEM).getAtencao(), TOLERANCIA);
    }
}
