package dominio;

import java.time.LocalDateTime;

import excecao.DadosInvalidosException;

/**
 * Uma leitura de um sensor em um instante. É imutável: depois de criada não
 * muda, e só existe se passar pela validação do construtor. Por isso o
 * histórico gravado no banco nunca contém valor impossível.
 */
public class Medicao {

    private final Long id;
    private final String codigoSensor;
    private final TipoSensor tipo;
    private final double valor;
    private final LocalDateTime dataHora;

    /** Cria uma medição que ainda não foi gravada no banco (sem id). */
    public Medicao(String codigoSensor, TipoSensor tipo, double valor, LocalDateTime dataHora) {
        this(null, codigoSensor, tipo, valor, dataHora);
    }

    /** Cria uma medição com o id gerado pelo banco (usado pelo DAO ao ler o histórico). */
    public Medicao(Long id, String codigoSensor, TipoSensor tipo, double valor, LocalDateTime dataHora) {
        if (codigoSensor == null || codigoSensor.isBlank()) {
            throw new DadosInvalidosException("medição sem código do sensor");
        }
        if (tipo == null) {
            throw new DadosInvalidosException("medição do sensor " + codigoSensor + " sem tipo de sensor");
        }
        if (dataHora == null) {
            throw new DadosInvalidosException("medição do sensor " + codigoSensor + " sem data e hora");
        }
        validarValor(tipo, valor);

        this.id = id;
        this.codigoSensor = codigoSensor;
        this.tipo = tipo;
        this.valor = valor;
        this.dataHora = dataHora;
    }

    private static void validarValor(TipoSensor tipo, double valor) {
        if (Double.isNaN(valor) || Double.isInfinite(valor)) {
            throw new DadosInvalidosException("medição de " + tipo.getDescricao() + " sem valor numérico");
        }
        if (valor < 0) {
            throw new DadosInvalidosException(
                    "medição de " + tipo.getDescricao() + " negativa: " + tipo.formatar(valor));
        }
        if (valor < tipo.getMinimo() || valor > tipo.getMaximo()) {
            throw new DadosInvalidosException(
                    "medição de " + tipo.getDescricao() + " fora da faixa: " + tipo.formatar(valor)
                            + " (aceito: de " + tipo.formatar(tipo.getMinimo())
                            + " a " + tipo.formatar(tipo.getMaximo()) + ")");
        }
    }

    /** Devolve uma cópia desta medição com o id que o banco gerou. */
    public Medicao comId(long idGerado) {
        return new Medicao(idGerado, codigoSensor, tipo, valor, dataHora);
    }

    public Long getId() {
        return id;
    }

    public String getCodigoSensor() {
        return codigoSensor;
    }

    public TipoSensor getTipo() {
        return tipo;
    }

    public double getValor() {
        return valor;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
}
