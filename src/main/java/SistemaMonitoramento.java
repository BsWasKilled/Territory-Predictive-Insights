import java.io.Console;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import dominio.Alerta;
import dominio.ComponenteTerritorial;
import dominio.DadosTerritorio;
import dominio.EstacaoMonitoramento;
import dominio.Formato;
import dominio.Limiares;
import dominio.Medicao;
import dominio.Sensor;
import dominio.SensorChuva;
import dominio.SensorDrenagem;
import dominio.SensorNivelRio;
import dominio.TipoSensor;
import excecao.AcessoNegadoException;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import excecao.PersistenciaException;
import persistencia.AlertaDAO;
import persistencia.ConexaoBanco;
import persistencia.ExportadorCsv;
import persistencia.MedicaoDAO;
import servico.ConfiguracaoSistema;
import servico.RelatorioBuilder;
import servico.ResultadoPrevisao;
import servico.ServicoMonitoramento;

/**
 * Menu principal do Territory Predictive Insights (recorte "clima local").
 *
 * Esta classe só conversa com o usuário: mostra as opções, lê o que foi
 * digitado, chama o serviço e exibe o resultado. É também aqui que as exceções
 * do sistema são capturadas e viram mensagens claras; nenhuma regra de negócio
 * e nenhum SQL ficam no menu.
 */
public class SistemaMonitoramento {

    private static final int MAXIMO_DE_RODADAS = 48;
    private static final int MAXIMO_DE_LINHAS_NA_TELA = 30;
    private static final int ALERTAS_NO_STATUS = 5;
    private static final Path PASTA_DE_EXPORTACAO = Path.of("exportacoes");
    private static final DateTimeFormatter CARIMBO_DE_ARQUIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String BARRA = "=".repeat(70);

    private final Scanner leitor;
    private final ServicoMonitoramento servico;
    private final ConfiguracaoSistema configuracao;
    private final List<EstacaoMonitoramento> estacoes;
    private final String enderecoDoBanco;
    private final ExportadorCsv exportador = new ExportadorCsv();

    public SistemaMonitoramento(Scanner leitor, ServicoMonitoramento servico, ConfiguracaoSistema configuracao,
                                List<EstacaoMonitoramento> estacoes, String enderecoDoBanco) {
        this.leitor = leitor;
        this.servico = servico;
        this.configuracao = configuracao;
        this.estacoes = estacoes;
        this.enderecoDoBanco = enderecoDoBanco;
    }

    public static void main(String[] args) {
        ConexaoBanco conexaoBanco = ConexaoBanco.padrao();
        if (!prepararBanco(conexaoBanco)) {
            System.exit(1);
        }

        ConfiguracaoSistema configuracao = ConfiguracaoSistema.getInstancia();
        ServicoMonitoramento servico = new ServicoMonitoramento(
                new MedicaoDAO(conexaoBanco), new AlertaDAO(conexaoBanco), configuracao);

        try (Scanner leitor = new Scanner(System.in, charsetDoConsole())) {
            new SistemaMonitoramento(leitor, servico, configuracao, montarEstacoes(), conexaoBanco.getUrl())
                    .executar();
        }
    }

    /** Cria as tabelas, se ainda não existirem. Sem banco não há histórico, então o sistema nem abre. */
    private static boolean prepararBanco(ConexaoBanco conexaoBanco) {
        try {
            conexaoBanco.prepararBanco();
            return true;
        } catch (PersistenciaException e) {
            System.out.println("Não foi possível iniciar o sistema: " + e.getMessage() + ".");
            System.out.println("Detalhe técnico: " + detalheTecnico(e));
            System.out.println("Confira se o programa já está aberto em outra janela"
                    + " e se a pasta \"dados\" pode ser gravada.");
            return false;
        }
    }

    /**
     * No terminal do Windows o texto digitado não chega em UTF-8. Ler com o
     * conjunto de caracteres do próprio console preserva os acentos (por
     * exemplo, no nome do território).
     */
    private static Charset charsetDoConsole() {
        Console console = System.console();
        return console != null ? console.charset() : Charset.defaultCharset();
    }

    /** Equipamentos instalados no território. São descritos por região, nunca por pessoas. */
    private static List<EstacaoMonitoramento> montarEstacoes() {
        EstacaoMonitoramento ponte =
                new EstacaoMonitoramento("EST-01", "Estação Ponte do Córrego", "Margem do córrego");
        ponte.adicionarSensor(new SensorChuva("PLUV-01"));
        ponte.adicionarSensor(new SensorNivelRio("NIVEL-01"));

        EstacaoMonitoramento baixada =
                new EstacaoMonitoramento("EST-02", "Estação Baixada da Feira", "Baixada central");
        baixada.adicionarSensor(new SensorDrenagem("GAL-01"));

        return List.of(ponte, baixada);
    }

    // ------------------------------------------------------------------
    // Laço principal
    // ------------------------------------------------------------------

    public void executar() {
        exibirAbertura();
        try {
            boolean sair = false;
            while (!sair) {
                exibirMenuPrincipal();
                sair = executarOpcaoPrincipal(lerLinha("Opção: "));
            }
        } catch (EntradaEncerradaException e) {
            System.out.println();
            System.out.println("A entrada de dados foi encerrada; saindo do sistema.");
        }
        System.out.println("Sistema encerrado.");
    }

    private void exibirAbertura() {
        DadosTerritorio territorio = configuracao.getTerritorio();
        System.out.println(BARRA);
        System.out.println(" TERRITORY PREDICTIVE INSIGHTS");
        System.out.println(" Clima local: chuva intensa e risco de alagamento em pontos vulneráveis");
        System.out.println(BARRA);
        System.out.println("Território: " + territorio.getNome() + " (" + territorio.getRegiao() + ")");
        System.out.println("Banco de dados: " + enderecoDoBanco);
        executarProtegido(this::exibirResumoDoHistorico);
    }

    /** Mostra, logo na abertura, o que as execuções anteriores deixaram gravado. */
    private void exibirResumoDoHistorico() {
        int medicoes = 0;
        for (Sensor sensor : todosOsSensores()) {
            medicoes += servico.contarLeituras(sensor.getCodigo());
        }
        int alertas = servico.contarAlertasEmitidos();
        if (medicoes == 0 && alertas == 0) {
            System.out.println("Histórico: banco novo, ainda sem medições. Comece pela opção 1.");
        } else {
            System.out.println("Histórico encontrado no banco: " + medicoes + " medições e "
                    + alertas + " alertas de execuções anteriores.");
        }
    }

    private void exibirMenuPrincipal() {
        System.out.println();
        System.out.println("----- MENU PRINCIPAL -----");
        System.out.println(" 1 - Verificar sensores (ler e gravar medições)");
        System.out.println(" 2 - Rodar previsões e emitir alertas");
        System.out.println(" 3 - Ver histórico (medições por sensor e alertas emitidos)");
        System.out.println(" 4 - Gerenciar dados do território");
        System.out.println(" 5 - Status completo");
        System.out.println(" 0 - Sair");
    }

    /** @return true quando o usuário pede para sair */
    private boolean executarOpcaoPrincipal(String opcao) {
        switch (opcao) {
            case "1" -> executarProtegido(this::verificarSensores);
            case "2" -> executarProtegido(this::rodarPrevisoes);
            case "3" -> menuHistorico();
            case "4" -> menuTerritorio();
            case "5" -> executarProtegido(this::exibirStatusCompleto);
            case "0" -> {
                return true;
            }
            default -> System.out.println("Opção inválida: digite um número de 0 a 5.");
        }
        return false;
    }

    /**
     * Executa uma ação do menu. Se ela for recusada por uma exceção do sistema,
     * o usuário recebe a explicação e o programa continua de pé.
     */
    private void executarProtegido(Runnable acao) {
        try {
            acao.run();
        } catch (DadosInvalidosException e) {
            System.out.println("[DADOS INVÁLIDOS] " + e.getMessage() + ". A operação não foi realizada.");
        } catch (AcessoNegadoException e) {
            System.out.println("[ACESSO NEGADO] " + e.getMessage() + ". Os limiares continuam como estavam.");
        } catch (PersistenciaException e) {
            System.out.println("[BANCO DE DADOS] " + e.getMessage() + ".");
            System.out.println("  Detalhe técnico: " + detalheTecnico(e));
        } catch (EntradaEncerradaException e) {
            throw e; // fim da entrada: quem encerra o programa é o laço principal
        } catch (RuntimeException e) {
            System.out.println("[ERRO INESPERADO] " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("  A operação foi interrompida, mas o sistema continua funcionando.");
        }
    }

    private static String detalheTecnico(PersistenciaException e) {
        return e.getCause() != null ? e.getCause().getMessage() : "sem detalhe adicional";
    }

    // ------------------------------------------------------------------
    // 1 - Verificar sensores
    // ------------------------------------------------------------------

    private void verificarSensores() {
        int rodadas = lerInteiroOuPadrao(
                "Quantas rodadas de leitura? (1 a " + MAXIMO_DE_RODADAS + ", Enter = 1): ", 1);
        if (rodadas < 1 || rodadas > MAXIMO_DE_RODADAS) {
            throw new DadosInvalidosException("a quantidade de rodadas deve ficar entre 1 e "
                    + MAXIMO_DE_RODADAS + " (informado: " + rodadas + ")");
        }

        int gravadas = 0;
        int semResposta = 0;
        int descartadas = 0;
        for (int rodada = 1; rodada <= rodadas; rodada++) {
            System.out.println();
            System.out.println("Rodada " + rodada + " de " + rodadas);
            for (Sensor sensor : todosOsSensores()) {
                try {
                    Medicao medicao = servico.registrarLeitura(sensor);
                    gravadas++;
                    System.out.println("  [GRAVADA]     " + descrever(sensor) + ": "
                            + sensor.getTipo().formatar(medicao.getValor())
                            + " (medição nº " + medicao.getId() + ")");
                } catch (FonteIndisponivelException e) {
                    semResposta++;
                    System.out.println("  [SEM LEITURA] " + descrever(sensor) + ": " + e.getMessage());
                } catch (DadosInvalidosException e) {
                    descartadas++;
                    System.out.println("  [DESCARTADA]  " + descrever(sensor) + ": " + e.getMessage());
                }
            }
        }

        System.out.println();
        System.out.println("Resumo: gravadas no banco: " + gravadas
                + " | sem resposta do sensor: " + semResposta
                + " | descartadas por valor inválido: " + descartadas);
        System.out.println("Situação das estações:");
        for (ComponenteTerritorial componente : estacoes) {
            // polimorfismo: cada componente sabe conferir a própria situação
            System.out.println("  " + componente.getId() + " - " + componente.getNome() + ": "
                    + componente.verificarStatus().getDescricao());
        }
    }

    // ------------------------------------------------------------------
    // 2 - Rodar previsões
    // ------------------------------------------------------------------

    private void rodarPrevisoes() {
        for (Sensor sensor : todosOsSensores()) {
            System.out.println();
            System.out.println(descrever(sensor));
            for (ResultadoPrevisao resultado : servico.rodarPrevisoes(sensor)) {
                exibirResultado(resultado);
            }
        }
    }

    private void exibirResultado(ResultadoPrevisao resultado) {
        if (!resultado.isEmitida()) {
            System.out.println("  " + resultado.getEstrategia() + ": " + resultado.getSignificado() + ".");
            return;
        }

        System.out.println("  " + resultado.getEstrategia() + ": previsão de "
                + resultado.getTipoSensor().formatar(resultado.getValorPrevisto()));
        System.out.println("      Cálculo: " + resultado.getCalculo() + ".");
        System.out.println("      Significado: " + resultado.getSignificado() + ".");

        if (resultado.getAlerta().isPresent()) {
            Alerta alerta = resultado.getAlerta().get();
            System.out.println("      Sugestão: " + alerta.getNivel().getOrientacao() + ".");
            if (resultado.isAlertaNovo()) {
                System.out.println("      Alerta nº " + alerta.getId() + " gravado.");
            } else {
                System.out.println("      O alerta nº " + alerta.getId() + ", de "
                        + Formato.dataHora(alerta.getDataHora())
                        + ", já avisou sobre estas mesmas leituras; não foi emitido de novo.");
            }
        }
    }

    // ------------------------------------------------------------------
    // 3 - Histórico
    // ------------------------------------------------------------------

    private void menuHistorico() {
        boolean voltar = false;
        while (!voltar) {
            System.out.println();
            System.out.println("----- HISTÓRICO -----");
            System.out.println(" 1 - Medições de um sensor");
            System.out.println(" 2 - Alertas já emitidos");
            System.out.println(" 0 - Voltar");
            switch (lerLinha("Opção: ")) {
                case "1" -> executarProtegido(this::consultarMedicoesDeUmSensor);
                case "2" -> executarProtegido(this::consultarAlertasEmitidos);
                case "0" -> voltar = true;
                default -> System.out.println("Opção inválida: digite um número de 0 a 2.");
            }
        }
    }

    private void consultarMedicoesDeUmSensor() {
        Sensor sensor = escolherSensor();
        List<Medicao> historico = servico.buscarHistoricoPorSensor(sensor.getCodigo());

        System.out.println();
        if (historico.isEmpty()) {
            System.out.println("Ainda não há medições gravadas para " + descrever(sensor)
                    + ". Use a opção 1 do menu principal para ler os sensores.");
            return;
        }

        System.out.println("Histórico de " + descrever(sensor) + ": " + historico.size() + " medições gravadas");
        int primeira = Math.max(0, historico.size() - MAXIMO_DE_LINHAS_NA_TELA);
        if (primeira > 0) {
            System.out.println("(na tela, as " + MAXIMO_DE_LINHAS_NA_TELA
                    + " mais recentes; a exportação em CSV leva todas)");
        }
        for (Medicao medicao : historico.subList(primeira, historico.size())) {
            System.out.println("  nº " + medicao.getId() + " | " + Formato.dataHora(medicao.getDataHora())
                    + " | " + sensor.getTipo().formatar(medicao.getValor()));
        }

        if (confirmar("Exportar este histórico para um arquivo CSV? (s/n): ")) {
            Path arquivo = PASTA_DE_EXPORTACAO.resolve(
                    "medicoes_" + sensor.getCodigo() + "_" + carimboDeArquivo() + ".csv");
            try {
                exportador.exportarMedicoes(historico, arquivo);
                System.out.println("Arquivo gravado: " + arquivo.toAbsolutePath());
            } catch (IOException e) {
                avisarFalhaAoGravar(arquivo, e);
            }
        }
    }

    private void consultarAlertasEmitidos() {
        List<Alerta> alertas = servico.listarAlertasEmitidos();

        System.out.println();
        if (alertas.isEmpty()) {
            System.out.println("Nenhum alerta foi emitido até agora."
                    + " Eles surgem quando uma previsão (opção 2) atinge um limiar.");
            return;
        }

        System.out.println("Alertas emitidos: " + alertas.size() + " (do mais recente para o mais antigo)");
        int mostrados = Math.min(MAXIMO_DE_LINHAS_NA_TELA, alertas.size());
        if (mostrados < alertas.size()) {
            System.out.println("(na tela, os " + mostrados + " mais recentes; a exportação em CSV leva todos)");
        }
        for (Alerta alerta : alertas.subList(0, mostrados)) {
            System.out.println("  nº " + alerta.getId() + " | " + Formato.dataHora(alerta.getDataHora())
                    + " | " + alerta.explicar());
        }

        if (confirmar("Exportar estes alertas para um arquivo CSV? (s/n): ")) {
            Path arquivo = PASTA_DE_EXPORTACAO.resolve("alertas_" + carimboDeArquivo() + ".csv");
            try {
                exportador.exportarAlertas(alertas, arquivo);
                System.out.println("Arquivo gravado: " + arquivo.toAbsolutePath());
            } catch (IOException e) {
                avisarFalhaAoGravar(arquivo, e);
            }
        }
    }

    private void avisarFalhaAoGravar(Path arquivo, IOException e) {
        System.out.println("[ARQUIVO] não foi possível gravar " + arquivo.toAbsolutePath()
                + " (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ").");
    }

    private String carimboDeArquivo() {
        return LocalDateTime.now().format(CARIMBO_DE_ARQUIVO);
    }

    // ------------------------------------------------------------------
    // 4 - Dados do território
    // ------------------------------------------------------------------

    private void menuTerritorio() {
        boolean voltar = false;
        while (!voltar) {
            System.out.println();
            System.out.println("----- DADOS DO TERRITÓRIO -----");
            System.out.println(" 1 - Ver dados e limiares");
            System.out.println(" 2 - Alterar nome");
            System.out.println(" 3 - Alterar região");
            System.out.println(" 4 - Alterar população estimada");
            System.out.println(" 5 - Alterar quantidade de pontos vulneráveis");
            System.out.println(" 6 - Alterar limiares de um sensor (exige código de acesso)");
            System.out.println(" 0 - Voltar");
            switch (lerLinha("Opção: ")) {
                case "1" -> executarProtegido(this::exibirTerritorio);
                case "2" -> executarProtegido(this::alterarNome);
                case "3" -> executarProtegido(this::alterarRegiao);
                case "4" -> executarProtegido(this::alterarPopulacao);
                case "5" -> executarProtegido(this::alterarPontosVulneraveis);
                case "6" -> executarProtegido(this::alterarLimiares);
                case "0" -> voltar = true;
                default -> System.out.println("Opção inválida: digite um número de 0 a 6.");
            }
        }
    }

    private void exibirTerritorio() {
        DadosTerritorio territorio = configuracao.getTerritorio();
        System.out.println();
        System.out.println(new RelatorioBuilder()
                .comTitulo("DADOS DO TERRITÓRIO")
                .comTerritorio(territorio)
                .comLimiares(territorio)
                .construir());
    }

    private void alterarNome() {
        DadosTerritorio territorio = configuracao.getTerritorio();
        territorio.setNome(lerLinha("Novo nome (atual: " + territorio.getNome() + "): "));
        System.out.println("Nome atualizado para \"" + territorio.getNome() + "\".");
    }

    private void alterarRegiao() {
        DadosTerritorio territorio = configuracao.getTerritorio();
        territorio.setRegiao(lerLinha("Nova região (atual: " + territorio.getRegiao() + "): "));
        System.out.println("Região atualizada para \"" + territorio.getRegiao() + "\".");
    }

    private void alterarPopulacao() {
        DadosTerritorio territorio = configuracao.getTerritorio();
        territorio.setPopulacao(lerInteiro(
                "Nova população estimada (atual: " + Formato.inteiro(territorio.getPopulacao()) + "): "));
        System.out.println("População atualizada para " + Formato.inteiro(territorio.getPopulacao())
                + " moradores.");
    }

    private void alterarPontosVulneraveis() {
        DadosTerritorio territorio = configuracao.getTerritorio();
        territorio.setPontosVulneraveis(lerInteiro(
                "Nova quantidade de pontos vulneráveis (atual: " + territorio.getPontosVulneraveis() + "): "));
        System.out.println("Pontos vulneráveis atualizados para " + territorio.getPontosVulneraveis() + ".");
    }

    private void alterarLimiares() {
        DadosTerritorio territorio = configuracao.getTerritorio();
        TipoSensor tipo = escolherTipoDeSensor();
        Limiares atuais = territorio.getLimiares(tipo);
        System.out.println("Limiares atuais de " + tipo.getDescricao() + ": atenção "
                + tipo.formatar(atuais.getAtencao()) + " | alerta " + tipo.formatar(atuais.getAlerta())
                + " | crítico " + tipo.formatar(atuais.getCritico()));

        String codigo = lerLinha("Código de acesso: ");
        territorio.conferirCodigoAcesso(codigo); // código errado é recusado antes de pedir os valores

        double atencao = lerDecimal("Novo limite de ATENÇÃO, em " + tipo.getUnidade() + ": ");
        double alerta = lerDecimal("Novo limite de ALERTA, em " + tipo.getUnidade() + ": ");
        double critico = lerDecimal("Novo limite CRÍTICO, em " + tipo.getUnidade() + ": ");
        territorio.alterarLimiares(codigo, tipo, new Limiares(atencao, alerta, critico));

        System.out.println("Limiares de " + tipo.getDescricao() + " atualizados: atenção "
                + tipo.formatar(atencao) + " | alerta " + tipo.formatar(alerta)
                + " | crítico " + tipo.formatar(critico) + ". Já valem para as próximas previsões.");
    }

    // ------------------------------------------------------------------
    // 5 - Status completo
    // ------------------------------------------------------------------

    private void exibirStatusCompleto() {
        DadosTerritorio territorio = configuracao.getTerritorio();
        RelatorioBuilder relatorio = new RelatorioBuilder()
                .comTitulo("STATUS COMPLETO - " + territorio.getNome())
                .comTerritorio(territorio)
                .comLimiares(territorio);

        for (ComponenteTerritorial componente : estacoes) {
            componente.verificarStatus();
            relatorio.comComponente(componente);
        }
        for (Sensor sensor : todosOsSensores()) {
            relatorio.comSensor(sensor,
                    servico.contarLeituras(sensor.getCodigo()),
                    servico.buscarUltimaLeitura(sensor.getCodigo()).orElse(null));
        }
        relatorio.comAlertas(servico.listarAlertasEmitidos(), ALERTAS_NO_STATUS);

        System.out.println();
        System.out.println(relatorio.construir());
    }

    // ------------------------------------------------------------------
    // Apoio: sensores e leitura do teclado
    // ------------------------------------------------------------------

    private List<Sensor> todosOsSensores() {
        List<Sensor> sensores = new ArrayList<>();
        for (EstacaoMonitoramento estacao : estacoes) {
            sensores.addAll(estacao.getSensores());
        }
        return sensores;
    }

    private String descrever(Sensor sensor) {
        return sensor.getCodigo() + " (" + sensor.getTipo().getDescricao() + ")";
    }

    private Sensor escolherSensor() {
        List<Sensor> sensores = todosOsSensores();
        System.out.println("Sensores:");
        for (int i = 0; i < sensores.size(); i++) {
            System.out.println(" " + (i + 1) + " - " + descrever(sensores.get(i)));
        }
        int numero = lerInteiro("Número do sensor: ");
        if (numero < 1 || numero > sensores.size()) {
            throw new DadosInvalidosException("não existe sensor de número " + numero
                    + " (escolha de 1 a " + sensores.size() + ")");
        }
        return sensores.get(numero - 1);
    }

    private TipoSensor escolherTipoDeSensor() {
        TipoSensor[] tipos = TipoSensor.values();
        System.out.println("Tipos de sensor:");
        for (int i = 0; i < tipos.length; i++) {
            System.out.println(" " + (i + 1) + " - " + tipos[i].getDescricao() + " (" + tipos[i].getUnidade() + ")");
        }
        int numero = lerInteiro("Número do tipo: ");
        if (numero < 1 || numero > tipos.length) {
            throw new DadosInvalidosException("não existe tipo de sensor de número " + numero
                    + " (escolha de 1 a " + tipos.length + ")");
        }
        return tipos[numero - 1];
    }

    private String lerLinha(String rotulo) {
        System.out.print(rotulo);
        if (!leitor.hasNextLine()) {
            throw new EntradaEncerradaException();
        }
        return leitor.nextLine().trim();
    }

    private int lerInteiro(String rotulo) {
        return converterParaInteiro(lerLinha(rotulo));
    }

    /** Lê um número inteiro; se o usuário só apertar Enter, vale o valor padrão. */
    private int lerInteiroOuPadrao(String rotulo, int padrao) {
        String texto = lerLinha(rotulo);
        return texto.isEmpty() ? padrao : converterParaInteiro(texto);
    }

    private int converterParaInteiro(String texto) {
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new DadosInvalidosException("\"" + texto + "\" não é um número inteiro (digite só os algarismos)");
        }
    }

    /** Lê um número com casas decimais; aceita vírgula ou ponto (12,5 ou 12.5). */
    private double lerDecimal(String rotulo) {
        String texto = lerLinha(rotulo);
        try {
            return Double.parseDouble(texto.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new DadosInvalidosException("\"" + texto + "\" não é um número (digite algo como 12,5)");
        }
    }

    private boolean confirmar(String pergunta) {
        String resposta = lerLinha(pergunta).toLowerCase();
        return resposta.equals("s") || resposta.equals("sim");
    }

    /** Sinaliza que não há mais nada para ler do teclado (a entrada foi encerrada). */
    private static final class EntradaEncerradaException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
