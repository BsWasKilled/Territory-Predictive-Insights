package dominio;

import java.time.LocalDateTime;

import excecao.DadosInvalidosException;

/**
 * Aviso emitido quando uma previsão atinge um limiar. Guarda tudo o que é
 * preciso para explicar o porquê: qual sensor, qual estratégia, o valor
 * previsto, o limite atingido e a leitura mais recente em que a previsão se
 * baseou. É imutável, como a Medicao.
 */
public class Alerta {

    private final Long id;
    private final String codigoSensor;
    private final TipoSensor tipoSensor;
    private final NivelAlerta nivel;
    private final String estrategia;
    private final double valorPrevisto;
    private final double limiteEstourado;
    private final Long idUltimaMedicao;
    private final LocalDateTime dataHora;

    /** Cria um alerta que ainda não foi gravado no banco (sem id). */
    public Alerta(String codigoSensor, TipoSensor tipoSensor, NivelAlerta nivel, String estrategia,
                  double valorPrevisto, double limiteEstourado, Long idUltimaMedicao, LocalDateTime dataHora) {
        this(null, codigoSensor, tipoSensor, nivel, estrategia, valorPrevisto, limiteEstourado,
                idUltimaMedicao, dataHora);
    }

    /** Cria um alerta com o id gerado pelo banco (usado pelo DAO ao ler os alertas emitidos). */
    public Alerta(Long id, String codigoSensor, TipoSensor tipoSensor, NivelAlerta nivel, String estrategia,
                  double valorPrevisto, double limiteEstourado, Long idUltimaMedicao, LocalDateTime dataHora) {
        if (codigoSensor == null || codigoSensor.isBlank()) {
            throw new DadosInvalidosException("alerta sem código do sensor");
        }
        if (tipoSensor == null || nivel == null || dataHora == null) {
            throw new DadosInvalidosException(
                    "alerta do sensor " + codigoSensor + " incompleto: faltam tipo, nível ou data e hora");
        }
        if (estrategia == null || estrategia.isBlank()) {
            throw new DadosInvalidosException(
                    "alerta do sensor " + codigoSensor + " sem a estratégia de previsão que o gerou");
        }
        if (idUltimaMedicao == null) {
            throw new DadosInvalidosException(
                    "alerta do sensor " + codigoSensor + " sem a leitura em que a previsão se baseou");
        }
        if (Double.isNaN(valorPrevisto) || Double.isNaN(limiteEstourado) || valorPrevisto < limiteEstourado) {
            throw new DadosInvalidosException(
                    "alerta do sensor " + codigoSensor + " incoerente: a previsão ("
                            + Formato.numero(valorPrevisto) + ") não atinge o limite ("
                            + Formato.numero(limiteEstourado) + ")");
        }

        this.id = id;
        this.codigoSensor = codigoSensor;
        this.tipoSensor = tipoSensor;
        this.nivel = nivel;
        this.estrategia = estrategia;
        this.valorPrevisto = valorPrevisto;
        this.limiteEstourado = limiteEstourado;
        this.idUltimaMedicao = idUltimaMedicao;
        this.dataHora = dataHora;
    }

    /** Devolve uma cópia deste alerta com o id que o banco gerou. */
    public Alerta comId(long idGerado) {
        return new Alerta(idGerado, codigoSensor, tipoSensor, nivel, estrategia,
                valorPrevisto, limiteEstourado, idUltimaMedicao, dataHora);
    }

    /** O porquê do alerta em uma frase (sem ponto final): sensor, valor previsto e limite atingido. */
    public String explicar() {
        return nivel.getRotulo() + ": o sensor " + codigoSensor + " (" + tipoSensor.getDescricao()
                + ") tem previsão de " + tipoSensor.formatar(valorPrevisto)
                + " pela estratégia \"" + estrategia + "\", o que atinge o limite de "
                + tipoSensor.formatar(limiteEstourado) + " deste nível";
    }

    /**
     * Diz se este alerta só repete outro: mesmo sensor, mesma estratégia, mesmo
     * nível, mesmos valores e baseado na mesma leitura. Emitir os dois seria
     * ruído, não informação nova.
     */
    public boolean repete(Alerta outro) {
        return outro != null
                && codigoSensor.equals(outro.codigoSensor)
                && estrategia.equals(outro.estrategia)
                && nivel == outro.nivel
                && idUltimaMedicao.equals(outro.idUltimaMedicao)
                && Double.compare(valorPrevisto, outro.valorPrevisto) == 0
                && Double.compare(limiteEstourado, outro.limiteEstourado) == 0;
    }

    public Long getId() {
        return id;
    }

    public String getCodigoSensor() {
        return codigoSensor;
    }

    public TipoSensor getTipoSensor() {
        return tipoSensor;
    }

    public NivelAlerta getNivel() {
        return nivel;
    }

    public String getEstrategia() {
        return estrategia;
    }

    public double getValorPrevisto() {
        return valorPrevisto;
    }

    public double getLimiteEstourado() {
        return limiteEstourado;
    }

    /** Id, no banco, da leitura mais recente considerada na previsão que gerou o alerta. */
    public Long getIdUltimaMedicao() {
        return idUltimaMedicao;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
}
