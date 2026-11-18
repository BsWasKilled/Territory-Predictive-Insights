package persistencia;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import dominio.Alerta;
import dominio.Formato;
import dominio.Medicao;

/**
 * Exporta para arquivo CSV o histórico que acabou de ser consultado no menu
 * (bônus do enunciado).
 *
 * O arquivo segue o padrão que o Excel em português abre direto: campos
 * separados por ponto e vírgula, números com vírgula decimal e texto em UTF-8.
 */
public class ExportadorCsv {

    private static final String SEPARADOR = ";";
    /** Marca (BOM) no início do arquivo que faz o Excel reconhecer os acentos em UTF-8. */
    private static final String MARCA_UTF8 = "\uFEFF";

    public void exportarMedicoes(List<Medicao> medicoes, Path arquivo) throws IOException {
        criarPasta(arquivo);
        try (BufferedWriter escritor = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8)) {
            escritor.write(MARCA_UTF8);
            escreverLinha(escritor, "id", "sensor", "grandeza", "valor", "unidade", "data_hora");
            for (Medicao medicao : medicoes) {
                escreverLinha(escritor,
                        String.valueOf(medicao.getId()),
                        medicao.getCodigoSensor(),
                        medicao.getTipo().getDescricao(),
                        Formato.numero(medicao.getValor()),
                        medicao.getTipo().getUnidade(),
                        Formato.dataHora(medicao.getDataHora()));
            }
        }
    }

    public void exportarAlertas(List<Alerta> alertas, Path arquivo) throws IOException {
        criarPasta(arquivo);
        try (BufferedWriter escritor = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8)) {
            escritor.write(MARCA_UTF8);
            escreverLinha(escritor, "id", "nivel", "sensor", "grandeza", "estrategia",
                    "valor_previsto", "limite_atingido", "unidade", "data_hora", "explicacao");
            for (Alerta alerta : alertas) {
                escreverLinha(escritor,
                        String.valueOf(alerta.getId()),
                        alerta.getNivel().getRotulo(),
                        alerta.getCodigoSensor(),
                        alerta.getTipoSensor().getDescricao(),
                        alerta.getEstrategia(),
                        Formato.numero(alerta.getValorPrevisto()),
                        Formato.numero(alerta.getLimiteEstourado()),
                        alerta.getTipoSensor().getUnidade(),
                        Formato.dataHora(alerta.getDataHora()),
                        alerta.explicar());
            }
        }
    }

    private void criarPasta(Path arquivo) throws IOException {
        Path pasta = arquivo.toAbsolutePath().getParent();
        if (pasta != null) {
            Files.createDirectories(pasta);
        }
    }

    private void escreverLinha(BufferedWriter escritor, String... campos) throws IOException {
        StringBuilder linha = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                linha.append(SEPARADOR);
            }
            linha.append(protegerCampo(campos[i]));
        }
        escritor.write(linha.toString());
        escritor.newLine();
    }

    /** Campo com separador, aspas ou quebra de linha vai entre aspas, com as aspas internas dobradas. */
    private String protegerCampo(String campo) {
        boolean precisaDeAspas = campo.contains(SEPARADOR) || campo.contains("\"")
                || campo.contains("\n") || campo.contains("\r");
        if (!precisaDeAspas) {
            return campo;
        }
        return "\"" + campo.replace("\"", "\"\"") + "\"";
    }
}
