package servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dominio.EstrategiaPrevisao;
import dominio.PrevisaoComparacao;
import dominio.PrevisaoMediaMovel;
import excecao.DadosInvalidosException;

class FabricaPrevisaoTest {

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
    void deveCriarMediaMovelComAJanelaDaConfiguracao() {
        // Arrange
        String nome = FabricaPrevisao.MEDIA_MOVEL;

        // Act
        EstrategiaPrevisao estrategia = FabricaPrevisao.criar(nome, configuracao);

        // Assert
        assertEquals(PrevisaoMediaMovel.class, estrategia.getClass());
        assertEquals(configuracao.getJanelaMediaMovel(), estrategia.getLeiturasNecessarias());
    }

    @Test
    void deveCriarComparacaoComOPeriodoDaConfiguracao() {
        // Arrange
        String nome = FabricaPrevisao.COMPARACAO;

        // Act
        EstrategiaPrevisao estrategia = FabricaPrevisao.criar(nome, configuracao);

        // Assert: a comparação precisa de dois períodos
        assertEquals(PrevisaoComparacao.class, estrategia.getClass());
        assertEquals(2 * configuracao.getPeriodoComparacao(), estrategia.getLeiturasNecessarias());
    }

    @Test
    void deveAceitarONomeDaEstrategiaEmMinusculasEComEspacos() {
        // Arrange
        String nomeDigitado = "  media_movel ";

        // Act
        EstrategiaPrevisao estrategia = FabricaPrevisao.criar(nomeDigitado, configuracao);

        // Assert
        assertEquals(PrevisaoMediaMovel.class, estrategia.getClass());
    }

    @Test
    void deveRecusarEstrategiaDesconhecidaDizendoQuaisExistem() {
        // Arrange
        String nomeInexistente = "REGRESSAO";

        // Act
        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class,
                () -> FabricaPrevisao.criar(nomeInexistente, configuracao));

        // Assert
        assertTrue(erro.getMessage().contains("REGRESSAO"), erro.getMessage());
        assertTrue(erro.getMessage().contains("MEDIA_MOVEL, COMPARACAO"), erro.getMessage());
    }

    @Test
    void deveRecusarEstrategiaSemNome() {
        // Arrange
        String semNome = null;

        // Act + Assert
        assertThrows(DadosInvalidosException.class, () -> FabricaPrevisao.criar(semNome, configuracao));
    }

    @Test
    void deveCriarTodasAsEstrategiasAtivasNaOrdemDaConfiguracao() {
        // Arrange: a configuração padrão ativa a média móvel e, depois, a comparação

        // Act
        List<EstrategiaPrevisao> estrategias = FabricaPrevisao.criarAtivas(configuracao);

        // Assert
        assertEquals(2, estrategias.size());
        assertEquals(PrevisaoMediaMovel.class, estrategias.get(0).getClass());
        assertEquals(PrevisaoComparacao.class, estrategias.get(1).getClass());
    }
}
