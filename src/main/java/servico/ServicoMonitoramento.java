package servico;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dominio.Alerta;
import dominio.EstrategiaPrevisao;
import dominio.Limiares;
import dominio.Medicao;
import dominio.NivelAlerta;
import dominio.Sensor;
import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;
import persistencia.AlertaDAO;
import persistencia.MedicaoDAO;

/**
 * Regras de negócio do monitoramento: registrar leituras, rodar as previsões
 * sobre o histórico gravado no banco e emitir alertas.
 *
 * Não conversa com o usuário (papel do menu) nem escreve SQL (papel dos DAOs).
 * Recebe os DAOs prontos pelo construtor; por isso os testes conseguem trocar
 * o banco por objetos simulados (mocks) e conferir só a lógica.
 */
public class ServicoMonitoramento {

    private final MedicaoDAO medicaoDAO;
    private final AlertaDAO alertaDAO;
    private final ConfiguracaoSistema configuracao;

    public ServicoMonitoramento(MedicaoDAO medicaoDAO, AlertaDAO alertaDAO, ConfiguracaoSistema configuracao) {
        this.medicaoDAO = medicaoDAO;
        this.alertaDAO = alertaDAO;
        this.configuracao = configuracao;
    }

    /**
     * Lê o sensor, valida a leitura e a grava no banco.
     *
     * @return a medição gravada, já com o id gerado pelo banco
     * @throws FonteIndisponivelException se o sensor não responder ou responder sem um valor
     * @throws DadosInvalidosException    se o valor lido estiver fora da faixa do sensor
     */
    public Medicao registrarLeitura(Sensor sensor) throws FonteIndisponivelException {
        exigirSensor(sensor);
        double valor = sensor.lerValor();
        if (Double.isNaN(valor) || Double.isInfinite(valor)) {
            throw new FonteIndisponivelException(
                    "o sensor " + sensor.getCodigo() + " respondeu sem um valor numérico; a leitura foi ignorada");
        }
        Medicao medicao = new Medicao(sensor.getCodigo(), sensor.getTipo(), valor, agora());
        return medicaoDAO.salvar(medicao);
    }

    /**
     * Aplica todas as estratégias ativas ao histórico do sensor e compara cada
     * previsão com os limiares. Devolve um resultado por estratégia, na ordem
     * em que elas estão na configuração.
     */
    public List<ResultadoPrevisao> rodarPrevisoes(Sensor sensor) {
        exigirSensor(sensor);
        List<EstrategiaPrevisao> estrategias = FabricaPrevisao.criarAtivas(configuracao);

        int maiorNecessidade = 0;
        for (EstrategiaPrevisao estrategia : estrategias) {
            maiorNecessidade = Math.max(maiorNecessidade, leiturasExigidas(estrategia));
        }
        List<Medicao> historico = medicaoDAO.buscarUltimasPorSensor(sensor.getCodigo(), maiorNecessidade);

        List<ResultadoPrevisao> resultados = new ArrayList<>();
        for (EstrategiaPrevisao estrategia : estrategias) {
            resultados.add(aplicarEstrategia(sensor, estrategia, historico));
        }
        return resultados;
    }

    /** Histórico completo de um sensor, da leitura mais antiga para a mais nova. */
    public List<Medicao> buscarHistoricoPorSensor(String codigoSensor) {
        exigirCodigo(codigoSensor);
        return medicaoDAO.buscarHistoricoPorSensor(codigoSensor);
    }

    /** A leitura mais recente gravada para o sensor, se houver alguma. */
    public Optional<Medicao> buscarUltimaLeitura(String codigoSensor) {
        exigirCodigo(codigoSensor);
        List<Medicao> ultimas = medicaoDAO.buscarUltimasPorSensor(codigoSensor, 1);
        if (ultimas.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ultimas.get(0));
    }

    /** Quantas leituras do sensor já estão gravadas. */
    public int contarLeituras(String codigoSensor) {
        exigirCodigo(codigoSensor);
        return medicaoDAO.contarPorSensor(codigoSensor);
    }

    /** Todos os alertas já emitidos, do mais recente para o mais antigo. */
    public List<Alerta> listarAlertasEmitidos() {
        return alertaDAO.listarTodos();
    }

    /** Quantos alertas já foram emitidos. */
    public int contarAlertasEmitidos() {
        return alertaDAO.contarTodos();
    }

    private ResultadoPrevisao aplicarEstrategia(Sensor sensor, EstrategiaPrevisao estrategia,
                                                List<Medicao> historico) {
        int exigidas = leiturasExigidas(estrategia);
        if (historico.size() < exigidas) {
            return ResultadoPrevisao.semHistoricoSuficiente(sensor.getCodigo(), sensor.getTipo(),
                    estrategia.getNome(), historico.size(), exigidas);
        }

        // polimorfismo: o serviço não sabe (nem precisa saber) qual estratégia recebeu
        double valorPrevisto = estrategia.prever(historico);
        String calculo = estrategia.explicar(historico);

        Limiares limiares = configuracao.getLimiares(sensor.getTipo());
        Optional<NivelAlerta> nivel = limiares.classificar(valorPrevisto);
        if (nivel.isEmpty()) {
            return ResultadoPrevisao.semAlerta(sensor.getCodigo(), sensor.getTipo(), estrategia.getNome(),
                    valorPrevisto, calculo, limiares.getAtencao());
        }

        Long idUltimaLeitura = historico.get(historico.size() - 1).getId();
        Alerta alerta = new Alerta(sensor.getCodigo(), sensor.getTipo(), nivel.get(), estrategia.getNome(),
                valorPrevisto, limiares.getLimite(nivel.get()), idUltimaLeitura, agora());
        return emitirSemRepetir(alerta, calculo);
    }

    /**
     * Grava o alerta, a menos que ele só repita o último alerta do mesmo sensor
     * e da mesma estratégia. Rodar a previsão duas vezes sem leitura nova não
     * traz informação nova, e alerta repetido ensina as pessoas a ignorá-lo.
     */
    private ResultadoPrevisao emitirSemRepetir(Alerta alerta, String calculo) {
        Optional<Alerta> anterior = alertaDAO.buscarUltimo(alerta.getCodigoSensor(), alerta.getEstrategia());
        if (anterior.isPresent() && alerta.repete(anterior.get())) {
            return ResultadoPrevisao.comAlerta(anterior.get(), calculo, false);
        }
        Alerta gravado = alertaDAO.salvar(alerta);
        return ResultadoPrevisao.comAlerta(gravado, calculo, true);
    }

    /** A estratégia tem a própria necessidade de leituras, mas nunca menos que o mínimo do sistema. */
    private int leiturasExigidas(EstrategiaPrevisao estrategia) {
        return Math.max(configuracao.getMinimoLeituras(), estrategia.getLeiturasNecessarias());
    }

    private void exigirSensor(Sensor sensor) {
        if (sensor == null) {
            throw new DadosInvalidosException("nenhum sensor foi informado");
        }
    }

    private void exigirCodigo(String codigoSensor) {
        if (codigoSensor == null || codigoSensor.isBlank()) {
            throw new DadosInvalidosException("informe o código do sensor");
        }
    }

    /** Data e hora atuais, sem frações de segundo (é a precisão que o histórico mostra). */
    private LocalDateTime agora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
