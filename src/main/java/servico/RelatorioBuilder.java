package servico;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import dominio.Alerta;
import dominio.ComponenteTerritorial;
import dominio.DadosTerritorio;
import dominio.Formato;
import dominio.Limiares;
import dominio.Medicao;
import dominio.NivelAlerta;
import dominio.Sensor;
import dominio.TipoSensor;

/**
 * Monta o relatório de status completo passo a passo (padrão Builder).
 *
 * Cada método "com..." acrescenta uma parte e devolve o próprio builder, para
 * que as chamadas possam ser encadeadas. Quem pede o relatório escolhe o que
 * entra e em que ordem; a formatação fica toda aqui, em um lugar só. No fim,
 * construir() confere se o relatório está completo e devolve o texto pronto.
 */
public class RelatorioBuilder {

    private static final String BARRA = "=".repeat(70);
    private static final String TRACO = "-".repeat(70);

    private final List<String> linhas = new ArrayList<>();
    private String titulo;
    private String secaoAberta;

    public RelatorioBuilder comTitulo(String titulo) {
        this.titulo = titulo;
        return this;
    }

    public RelatorioBuilder comTerritorio(DadosTerritorio territorio) {
        abrirSecao("TERRITÓRIO");
        linhas.add("  Nome: " + territorio.getNome());
        linhas.add("  Região: " + territorio.getRegiao());
        linhas.add("  População estimada: " + Formato.inteiro(territorio.getPopulacao()) + " moradores");
        linhas.add("  Pontos vulneráveis mapeados: " + territorio.getPontosVulneraveis());
        return this;
    }

    public RelatorioBuilder comLimiares(DadosTerritorio territorio) {
        abrirSecao("LIMIARES DE ALERTA (atenção / alerta / crítico)");
        for (TipoSensor tipo : TipoSensor.values()) {
            Limiares limiares = territorio.getLimiares(tipo);
            linhas.add("  " + tipo.getDescricao() + ": " + tipo.formatar(limiares.getAtencao())
                    + " / " + tipo.formatar(limiares.getAlerta())
                    + " / " + tipo.formatar(limiares.getCritico()));
        }
        return this;
    }

    /** Acrescenta um equipamento do território com o status que ele tem no momento. */
    public RelatorioBuilder comComponente(ComponenteTerritorial componente) {
        abrirSecao("EQUIPAMENTOS");
        linhas.add("  " + componente.getId() + " - " + componente.getNome()
                + " (" + componente.getRegiao() + "): " + componente.getStatus().getDescricao());
        return this;
    }

    /**
     * Acrescenta a situação de um sensor.
     *
     * @param ultimaLeitura a leitura mais recente gravada, ou null se ainda não houver nenhuma
     */
    public RelatorioBuilder comSensor(Sensor sensor, int leiturasGravadas, Medicao ultimaLeitura) {
        abrirSecao("SENSORES");
        String resposta = sensor.verificarFuncionamento() ? "respondendo" : "sem resposta na última tentativa";
        String historico;
        if (ultimaLeitura == null) {
            historico = "nenhuma leitura gravada";
        } else {
            historico = leiturasGravadas + " leituras gravadas | última: "
                    + sensor.getTipo().formatar(ultimaLeitura.getValor())
                    + " em " + Formato.dataHora(ultimaLeitura.getDataHora());
        }
        linhas.add("  " + sensor.getCodigo() + " (" + sensor.getTipo().getDescricao() + "): "
                + resposta + " | " + historico);
        return this;
    }

    /**
     * Acrescenta o resumo dos alertas emitidos.
     *
     * @param alertas       todos os alertas, do mais recente para o mais antigo
     * @param maximoListado quantos dos mais recentes aparecem por extenso
     */
    public RelatorioBuilder comAlertas(List<Alerta> alertas, int maximoListado) {
        abrirSecao("ALERTAS EMITIDOS");
        if (alertas.isEmpty()) {
            linhas.add("  Nenhum alerta emitido até agora.");
            return this;
        }

        Map<NivelAlerta, Integer> quantidadePorNivel = new EnumMap<>(NivelAlerta.class);
        for (NivelAlerta nivel : NivelAlerta.values()) {
            quantidadePorNivel.put(nivel, 0);
        }
        for (Alerta alerta : alertas) {
            quantidadePorNivel.merge(alerta.getNivel(), 1, Integer::sum);
        }
        List<String> resumoPorNivel = new ArrayList<>();
        for (NivelAlerta nivel : NivelAlerta.values()) {
            resumoPorNivel.add(nivel.getRotulo() + ": " + quantidadePorNivel.get(nivel));
        }
        linhas.add("  Total: " + alertas.size() + " (" + String.join(" | ", resumoPorNivel) + ")");

        linhas.add("  Mais recentes:");
        int listados = Math.min(maximoListado, alertas.size());
        for (Alerta alerta : alertas.subList(0, listados)) {
            linhas.add("    nº " + alerta.getId() + " em " + Formato.dataHora(alerta.getDataHora())
                    + " - " + alerta.explicar());
        }
        return this;
    }

    /**
     * Devolve o relatório pronto para exibir.
     *
     * @throws IllegalStateException se faltar o título ou se nenhuma parte tiver sido acrescentada
     */
    public String construir() {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalStateException("o relatório precisa de um título antes de ser construído");
        }
        if (linhas.isEmpty()) {
            throw new IllegalStateException("o relatório \"" + titulo + "\" está vazio: acrescente ao menos uma parte");
        }

        List<String> relatorio = new ArrayList<>();
        relatorio.add(BARRA);
        relatorio.add(titulo);
        relatorio.add(BARRA);
        relatorio.addAll(linhas);
        relatorio.add("");
        relatorio.add(TRACO);
        relatorio.add("Relatório gerado em " + Formato.dataHora(LocalDateTime.now()));
        return String.join(System.lineSeparator(), relatorio);
    }

    /** Começa uma seção nova; chamadas seguidas para a mesma seção não repetem o cabeçalho. */
    private void abrirSecao(String nome) {
        if (nome.equals(secaoAberta)) {
            return;
        }
        linhas.add("");
        linhas.add(nome);
        secaoAberta = nome;
    }
}
