package servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dominio.Alerta;
import dominio.DadosTerritorio;
import dominio.EstacaoMonitoramento;
import dominio.Medicao;
import dominio.NivelAlerta;
import dominio.Sensor;
import dominio.TipoSensor;

class RelatorioBuilderTest {

    private static final LocalDateTime OITO_HORAS = LocalDateTime.of(2026, 11, 16, 8, 0, 0);

    private DadosTerritorio territorio;

    @BeforeEach
    void usarTerritorioPadrao() {
        ConfiguracaoSistema.getInstancia().restaurarPadroes();
        territorio = ConfiguracaoSistema.getInstancia().getTerritorio();
    }

    @AfterEach
    void devolverConfiguracaoAoPadrao() {
        ConfiguracaoSistema.getInstancia().restaurarPadroes();
    }

    private static Alerta alerta(long id, NivelAlerta nivel, double previsto, double limite) {
        return new Alerta(id, "PLUV-01", TipoSensor.CHUVA, nivel, "Média móvel", previsto, limite, 1L, OITO_HORAS);
    }

    private static int contarOcorrencias(String texto, String trecho) {
        return texto.split(java.util.regex.Pattern.quote(trecho), -1).length - 1;
    }

    @Test
    void deveMontarORelatorioComAsPartesPedidas() {
        // Arrange
        RelatorioBuilder builder = new RelatorioBuilder()
                .comTitulo("DADOS DO TERRITÓRIO")
                .comTerritorio(territorio)
                .comLimiares(territorio);

        // Act
        String relatorio = builder.construir();

        // Assert
        assertTrue(relatorio.contains("DADOS DO TERRITÓRIO"), relatorio);
        assertTrue(relatorio.contains("Nome: Comunidade Córrego Azul"), relatorio);
        assertTrue(relatorio.contains("População estimada: 18.500 moradores"), relatorio);
        assertTrue(relatorio.contains("chuva: 10,0 mm/h / 25,0 mm/h / 50,0 mm/h"), relatorio);
        assertFalse(relatorio.contains("SENSORES"), relatorio); // parte não pedida não aparece
    }

    @Test
    void deveAgruparEquipamentosNaMesmaSecao() {
        // Arrange
        EstacaoMonitoramento ponte = new EstacaoMonitoramento("EST-01", "Estação Ponte", "Margem do córrego");
        EstacaoMonitoramento baixada = new EstacaoMonitoramento("EST-02", "Estação Baixada", "Baixada central");

        // Act
        String relatorio = new RelatorioBuilder()
                .comTitulo("STATUS")
                .comComponente(ponte)
                .comComponente(baixada)
                .construir();

        // Assert: um cabeçalho só para os dois equipamentos
        assertEquals(1, contarOcorrencias(relatorio, "EQUIPAMENTOS"));
        assertTrue(relatorio.contains("EST-01 - Estação Ponte (Margem do córrego): ainda não verificado"), relatorio);
        assertTrue(relatorio.contains("EST-02 - Estação Baixada (Baixada central)"), relatorio);
    }

    @Test
    void deveDescreverSensorComESemLeituras() {
        // Arrange
        Sensor pluviometro = mock(Sensor.class);
        when(pluviometro.getCodigo()).thenReturn("PLUV-01");
        when(pluviometro.getTipo()).thenReturn(TipoSensor.CHUVA);
        when(pluviometro.verificarFuncionamento()).thenReturn(true);
        Sensor galeria = mock(Sensor.class);
        when(galeria.getCodigo()).thenReturn("GAL-01");
        when(galeria.getTipo()).thenReturn(TipoSensor.DRENAGEM);
        when(galeria.verificarFuncionamento()).thenReturn(false);
        Medicao ultima = new Medicao(8L, "PLUV-01", TipoSensor.CHUVA, 22.4, OITO_HORAS);

        // Act
        String relatorio = new RelatorioBuilder()
                .comTitulo("STATUS")
                .comSensor(pluviometro, 8, ultima)
                .comSensor(galeria, 0, null)
                .construir();

        // Assert
        assertTrue(relatorio.contains(
                        "PLUV-01 (chuva): respondendo | 8 leituras gravadas | última: 22,4 mm/h em 16/11/2026 08:00:00"),
                relatorio);
        assertTrue(relatorio.contains(
                        "GAL-01 (ocupação da galeria pluvial): sem resposta na última tentativa | nenhuma leitura gravada"),
                relatorio);
    }

    @Test
    void deveResumirOsAlertasPorNivelEListarSoOsMaisRecentes() {
        // Arrange: três alertas, do mais recente para o mais antigo
        List<Alerta> alertas = List.of(
                alerta(3L, NivelAlerta.CRITICO, 55.0, 50.0),
                alerta(2L, NivelAlerta.ALERTA, 30.0, 25.0),
                alerta(1L, NivelAlerta.ALERTA, 28.0, 25.0));

        // Act
        String relatorio = new RelatorioBuilder().comTitulo("STATUS").comAlertas(alertas, 2).construir();

        // Assert
        assertTrue(relatorio.contains("Total: 3 (ATENÇÃO: 0 | ALERTA: 2 | CRÍTICO: 1)"), relatorio);
        assertTrue(relatorio.contains("nº 3 em"), relatorio);
        assertTrue(relatorio.contains("nº 2 em"), relatorio);
        assertFalse(relatorio.contains("nº 1 em"), relatorio);
    }

    @Test
    void deveInformarQuandoNaoHaAlertas() {
        // Arrange
        List<Alerta> nenhumAlerta = List.of();

        // Act
        String relatorio = new RelatorioBuilder().comTitulo("STATUS").comAlertas(nenhumAlerta, 5).construir();

        // Assert
        assertTrue(relatorio.contains("Nenhum alerta emitido até agora."), relatorio);
    }

    @Test
    void deveRecusarRelatorioSemTitulo() {
        // Arrange
        RelatorioBuilder semTitulo = new RelatorioBuilder().comTerritorio(territorio);

        // Act + Assert
        assertThrows(IllegalStateException.class, semTitulo::construir);
    }

    @Test
    void deveRecusarRelatorioVazio() {
        // Arrange
        RelatorioBuilder semPartes = new RelatorioBuilder().comTitulo("STATUS");

        // Act + Assert
        assertThrows(IllegalStateException.class, semPartes::construir);
    }
}
