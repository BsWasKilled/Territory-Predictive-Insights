package persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dominio.Alerta;
import dominio.Medicao;
import dominio.NivelAlerta;
import dominio.TipoSensor;

class ExportadorCsvTest {

    private static final LocalDateTime OITO_HORAS = LocalDateTime.of(2026, 11, 16, 8, 0, 0);

    @TempDir
    Path pastaTemporaria;

    private final ExportadorCsv exportador = new ExportadorCsv();

    private static List<String> lerLinhas(Path arquivo) throws IOException {
        return Files.readAllLines(arquivo, StandardCharsets.UTF_8);
    }

    @Test
    void deveExportarUmaLinhaPorMedicaoComCabecalho() throws IOException {
        // Arrange
        List<Medicao> medicoes = List.of(
                new Medicao(1L, "PLUV-01", TipoSensor.CHUVA, 12.5, OITO_HORAS),
                new Medicao(2L, "PLUV-01", TipoSensor.CHUVA, 30.0, OITO_HORAS.plusMinutes(10)));
        Path arquivo = pastaTemporaria.resolve("medicoes.csv");

        // Act
        exportador.exportarMedicoes(medicoes, arquivo);

        // Assert: cabeçalho (depois da marca de UTF-8) + 2 linhas, com vírgula decimal
        List<String> linhas = lerLinhas(arquivo);
        assertEquals(3, linhas.size());
        assertEquals("﻿id;sensor;grandeza;valor;unidade;data_hora", linhas.get(0));
        assertEquals("1;PLUV-01;chuva;12,5;mm/h;16/11/2026 08:00:00", linhas.get(1));
        assertEquals("2;PLUV-01;chuva;30,0;mm/h;16/11/2026 08:10:00", linhas.get(2));
    }

    @Test
    void deveExportarSoOCabecalhoQuandoNaoHaMedicoes() throws IOException {
        // Arrange
        Path arquivo = pastaTemporaria.resolve("vazio.csv");

        // Act
        exportador.exportarMedicoes(List.of(), arquivo);

        // Assert
        assertEquals(1, lerLinhas(arquivo).size());
    }

    @Test
    void deveCriarAPastaDeDestinoQuandoElaNaoExiste() throws IOException {
        // Arrange
        Path arquivo = pastaTemporaria.resolve("exportacoes").resolve("2026").resolve("medicoes.csv");
        List<Medicao> medicoes = List.of(new Medicao(1L, "GAL-01", TipoSensor.DRENAGEM, 80.0, OITO_HORAS));

        // Act
        exportador.exportarMedicoes(medicoes, arquivo);

        // Assert
        assertTrue(Files.exists(arquivo));
    }

    @Test
    void deveProtegerComAspasAExplicacaoDoAlerta() throws IOException {
        // Arrange: a explicação traz o nome da estratégia entre aspas
        Alerta alerta = new Alerta(3L, "NIVEL-01", TipoSensor.NIVEL_RIO, NivelAlerta.CRITICO, "Média móvel",
                310.0, 300.0, 9L, OITO_HORAS);
        Path arquivo = pastaTemporaria.resolve("alertas.csv");

        // Act
        exportador.exportarAlertas(List.of(alerta), arquivo);

        // Assert: aspas internas dobradas, como o padrão CSV pede
        List<String> linhas = lerLinhas(arquivo);
        assertEquals(2, linhas.size());
        assertTrue(linhas.get(1).startsWith("3;CRÍTICO;NIVEL-01;nível do córrego;Média móvel;310,0;300,0;cm;"),
                linhas.get(1));
        assertTrue(linhas.get(1).contains("pela estratégia \"\"Média móvel\"\""), linhas.get(1));
    }
}
