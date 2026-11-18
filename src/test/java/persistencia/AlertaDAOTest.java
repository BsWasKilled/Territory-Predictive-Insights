package persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dominio.Alerta;
import dominio.Medicao;
import dominio.NivelAlerta;
import dominio.TipoSensor;
import excecao.PersistenciaException;

/** Confere o SQL do AlertaDAO em um banco H2 só em memória (nenhum arquivo é criado). */
class AlertaDAOTest {

    private static final double TOLERANCIA = 0.0001;
    private static final String MEDIA_MOVEL = "Média móvel";
    private static final String COMPARACAO = "Comparação com o período anterior";
    private static final LocalDateTime NOVE_HORAS = LocalDateTime.of(2026, 11, 16, 9, 0, 0);

    private AlertaDAO dao;
    private long idDaLeitura;

    @BeforeEach
    void criarBancoComUmaLeitura() {
        ConexaoBanco conexaoBanco = BancoDeTeste.novo();
        dao = new AlertaDAO(conexaoBanco);
        // todo alerta aponta para a leitura em que a previsão se baseou
        idDaLeitura = new MedicaoDAO(conexaoBanco)
                .salvar(new Medicao("PLUV-01", TipoSensor.CHUVA, 30.0, NOVE_HORAS))
                .getId();
    }

    private Alerta alerta(NivelAlerta nivel, String estrategia, double previsto, double limite, int minutos) {
        return new Alerta("PLUV-01", TipoSensor.CHUVA, nivel, estrategia, previsto, limite,
                idDaLeitura, NOVE_HORAS.plusMinutes(minutos));
    }

    @Test
    void deveGravarEDevolverOAlertaComIdGerado() {
        // Arrange
        Alerta novo = alerta(NivelAlerta.ALERTA, MEDIA_MOVEL, 30.0, 25.0, 0);

        // Act
        Alerta gravado = dao.salvar(novo);

        // Assert
        assertNotNull(gravado.getId());
        assertEquals(1, dao.contarTodos());
    }

    @Test
    void deveLerDoBancoExatamenteOQueFoiGravado() {
        // Arrange
        Alerta gravado = dao.salvar(alerta(NivelAlerta.CRITICO, COMPARACAO, 70.5, 50.0, 5));

        // Act
        List<Alerta> alertas = dao.listarTodos();

        // Assert: acentos, números e a explicação voltam iguais
        assertEquals(1, alertas.size());
        Alerta lido = alertas.get(0);
        assertEquals(gravado.getId(), lido.getId());
        assertEquals("PLUV-01", lido.getCodigoSensor());
        assertEquals(TipoSensor.CHUVA, lido.getTipoSensor());
        assertEquals(NivelAlerta.CRITICO, lido.getNivel());
        assertEquals(COMPARACAO, lido.getEstrategia());
        assertEquals(70.5, lido.getValorPrevisto(), TOLERANCIA);
        assertEquals(50.0, lido.getLimiteEstourado(), TOLERANCIA);
        assertEquals(idDaLeitura, lido.getIdUltimaMedicao().longValue());
        assertEquals(NOVE_HORAS.plusMinutes(5), lido.getDataHora());
        assertEquals(gravado.explicar(), lido.explicar());
    }

    @Test
    void deveListarDoMaisRecenteParaOMaisAntigo() {
        // Arrange
        dao.salvar(alerta(NivelAlerta.ATENCAO, MEDIA_MOVEL, 12.0, 10.0, 0));
        dao.salvar(alerta(NivelAlerta.ALERTA, MEDIA_MOVEL, 30.0, 25.0, 10));
        dao.salvar(alerta(NivelAlerta.CRITICO, MEDIA_MOVEL, 55.0, 50.0, 20));

        // Act
        List<Alerta> alertas = dao.listarTodos();

        // Assert
        assertEquals(3, alertas.size());
        assertEquals(NivelAlerta.CRITICO, alertas.get(0).getNivel());
        assertEquals(NivelAlerta.ALERTA, alertas.get(1).getNivel());
        assertEquals(NivelAlerta.ATENCAO, alertas.get(2).getNivel());
    }

    @Test
    void deveBuscarOUltimoAlertaDoSensorParaCadaEstrategia() {
        // Arrange
        dao.salvar(alerta(NivelAlerta.ATENCAO, MEDIA_MOVEL, 12.0, 10.0, 0));
        dao.salvar(alerta(NivelAlerta.CRITICO, COMPARACAO, 70.0, 50.0, 5));
        dao.salvar(alerta(NivelAlerta.ALERTA, MEDIA_MOVEL, 30.0, 25.0, 10));

        // Act
        Optional<Alerta> ultimoDaMediaMovel = dao.buscarUltimo("PLUV-01", MEDIA_MOVEL);
        Optional<Alerta> ultimoDaComparacao = dao.buscarUltimo("PLUV-01", COMPARACAO);

        // Assert
        assertEquals(NivelAlerta.ALERTA, ultimoDaMediaMovel.get().getNivel());
        assertEquals(NivelAlerta.CRITICO, ultimoDaComparacao.get().getNivel());
    }

    @Test
    void deveDevolverVazioQuandoOSensorNuncaTeveAlerta() {
        // Arrange
        dao.salvar(alerta(NivelAlerta.ALERTA, MEDIA_MOVEL, 30.0, 25.0, 0));

        // Act
        Optional<Alerta> ultimo = dao.buscarUltimo("GAL-01", MEDIA_MOVEL);

        // Assert
        assertFalse(ultimo.isPresent());
    }

    @Test
    void deveComecarSemNenhumAlerta() {
        // Arrange: banco recém-criado

        // Act
        List<Alerta> alertas = dao.listarTodos();

        // Assert
        assertTrue(alertas.isEmpty());
        assertEquals(0, dao.contarTodos());
    }

    @Test
    void deveRecusarAlertaQueApontaParaLeituraInexistente() {
        // Arrange: não existe medição com este id no banco
        Alerta semLeitura = new Alerta("PLUV-01", TipoSensor.CHUVA, NivelAlerta.ALERTA, MEDIA_MOVEL,
                30.0, 25.0, 9999L, NOVE_HORAS);

        // Act
        PersistenciaException erro = assertThrows(PersistenciaException.class, () -> dao.salvar(semLeitura));

        // Assert
        assertTrue(erro.getMessage().contains("PLUV-01"), erro.getMessage());
        assertEquals(0, dao.contarTodos());
    }
}
