package persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dominio.Medicao;
import dominio.TipoSensor;
import excecao.PersistenciaException;

/**
 * Confere o SQL do MedicaoDAO em um banco H2 de verdade, mas só em memória
 * (nenhum arquivo é criado). A lógica de negócio é testada sem banco, com
 * Mockito, em ServicoMonitoramentoTest.
 */
class MedicaoDAOTest {

    private static final double TOLERANCIA = 0.0001;
    private static final LocalDateTime OITO_HORAS = LocalDateTime.of(2026, 11, 16, 8, 0, 0);

    private ConexaoBanco conexaoBanco;
    private MedicaoDAO dao;

    @BeforeEach
    void criarBancoVazio() {
        conexaoBanco = BancoDeTeste.novo();
        dao = new MedicaoDAO(conexaoBanco);
    }

    private Medicao chuva(double valor, int minutosDepoisDasOito) {
        return new Medicao("PLUV-01", TipoSensor.CHUVA, valor, OITO_HORAS.plusMinutes(minutosDepoisDasOito));
    }

    @Test
    void deveGravarEDevolverAMedicaoComIdGerado() {
        // Arrange
        Medicao nova = chuva(12.5, 0);

        // Act
        Medicao gravada = dao.salvar(nova);

        // Assert
        assertNotNull(gravada.getId());
        assertEquals(12.5, gravada.getValor(), TOLERANCIA);
        assertEquals(1, dao.contarPorSensor("PLUV-01"));
    }

    @Test
    void deveLerDoBancoExatamenteOQueFoiGravado() {
        // Arrange
        Medicao gravada = dao.salvar(chuva(37.4, 10));

        // Act
        List<Medicao> historico = dao.buscarHistoricoPorSensor("PLUV-01");

        // Assert
        assertEquals(1, historico.size());
        Medicao lida = historico.get(0);
        assertEquals(gravada.getId(), lida.getId());
        assertEquals("PLUV-01", lida.getCodigoSensor());
        assertEquals(TipoSensor.CHUVA, lida.getTipo());
        assertEquals(37.4, lida.getValor(), TOLERANCIA);
        assertEquals(OITO_HORAS.plusMinutes(10), lida.getDataHora());
    }

    @Test
    void deveListarOHistoricoSoDoSensorPedidoEmOrdemCronologica() {
        // Arrange: gravadas fora de ordem e misturadas com outro sensor
        dao.salvar(chuva(30.0, 20));
        dao.salvar(new Medicao("NIVEL-01", TipoSensor.NIVEL_RIO, 200.0, OITO_HORAS));
        dao.salvar(chuva(10.0, 0));
        dao.salvar(chuva(20.0, 10));

        // Act
        List<Medicao> historico = dao.buscarHistoricoPorSensor("PLUV-01");

        // Assert
        assertEquals(3, historico.size());
        assertEquals(10.0, historico.get(0).getValor(), TOLERANCIA);
        assertEquals(20.0, historico.get(1).getValor(), TOLERANCIA);
        assertEquals(30.0, historico.get(2).getValor(), TOLERANCIA);
    }

    @Test
    void deveBuscarSoAsUltimasLeiturasDaMaisAntigaParaAMaisNova() {
        // Arrange: cinco leituras, de 10 a 50 mm/h
        for (int i = 1; i <= 5; i++) {
            dao.salvar(chuva(10.0 * i, 10 * i));
        }

        // Act
        List<Medicao> ultimasTres = dao.buscarUltimasPorSensor("PLUV-01", 3);

        // Assert: ficam 30, 40 e 50, nessa ordem (é como as previsões esperam)
        assertEquals(3, ultimasTres.size());
        assertEquals(30.0, ultimasTres.get(0).getValor(), TOLERANCIA);
        assertEquals(40.0, ultimasTres.get(1).getValor(), TOLERANCIA);
        assertEquals(50.0, ultimasTres.get(2).getValor(), TOLERANCIA);
    }

    @Test
    void deveManterAOrdemDeGravacaoQuandoAsLeiturasTemOMesmoHorario() {
        // Arrange: várias rodadas no mesmo segundo
        dao.salvar(chuva(1.0, 0));
        dao.salvar(chuva(2.0, 0));
        dao.salvar(chuva(3.0, 0));

        // Act
        List<Medicao> ultimasDuas = dao.buscarUltimasPorSensor("PLUV-01", 2);

        // Assert
        assertEquals(2.0, ultimasDuas.get(0).getValor(), TOLERANCIA);
        assertEquals(3.0, ultimasDuas.get(1).getValor(), TOLERANCIA);
    }

    @Test
    void deveDevolverListaVaziaParaSensorSemLeituras() {
        // Arrange
        String sensorSemHistorico = "GAL-01";

        // Act
        List<Medicao> historico = dao.buscarHistoricoPorSensor(sensorSemHistorico);

        // Assert
        assertTrue(historico.isEmpty());
        assertEquals(0, dao.contarPorSensor(sensorSemHistorico));
    }

    @Test
    void deveTratarTextoComAspasComoDadoENaoComoSql() {
        // Arrange: um código malicioso só é texto para o PreparedStatement
        String codigoMalicioso = "X' OR '1'='1";
        dao.salvar(chuva(10.0, 0));

        // Act
        List<Medicao> historico = dao.buscarHistoricoPorSensor(codigoMalicioso);

        // Assert: nenhuma medição "vaza" e a tabela continua intacta
        assertTrue(historico.isEmpty());
        assertEquals(1, dao.contarPorSensor("PLUV-01"));
    }

    @Test
    void deveManterOsDadosParaOutroDaoLigadoAoMesmoBanco() {
        // Arrange
        dao.salvar(chuva(10.0, 0));
        dao.salvar(chuva(20.0, 10));

        // Act: outro DAO, como o criado quando o programa é aberto de novo
        MedicaoDAO outroDao = new MedicaoDAO(conexaoBanco);

        // Assert
        assertEquals(2, outroDao.contarPorSensor("PLUV-01"));
    }

    @Test
    void deveAvisarComPersistenciaExceptionQuandoOBancoFalha() {
        // Arrange: endereço para o qual não existe driver JDBC
        MedicaoDAO daoSemBanco = new MedicaoDAO(new ConexaoBanco("jdbc:inexistente:banco", "sa", ""));

        // Act
        PersistenciaException erro = assertThrows(PersistenciaException.class,
                () -> daoSemBanco.salvar(chuva(10.0, 0)));

        // Assert: a mensagem explica o que falhou e a causa técnica é preservada
        assertTrue(erro.getMessage().contains("PLUV-01"), erro.getMessage());
        assertNotNull(erro.getCause());
    }
}
